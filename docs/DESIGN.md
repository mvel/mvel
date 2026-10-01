# MVEL3 Design

Architectural overview for MVEL3. Intended as a stable complement to
`README.md` (user-facing) and `CLAUDE.md` (contributor quick-reference).

## Goals

- Compile MVEL expressions to Java bytecode via javac at build/runtime,
  so the hot path is pure Java invocation with no interpreter overhead.
- Preserve MVEL2 surface syntax where practical; break compatibility
  only where MVEL2's interpreter semantics cannot be reconciled with
  Java's type system.
- Support content-addressed lambda deduplication and persistence, so
  identical expressions compile once and share a single class across
  calls, rules, and restarts.

## Compilation pipeline

```
MVEL source string
  → ANTLR4 Lexer/Parser (Mvel3Lexer.g4 / Mvel3Parser.g4, importing JavaLexer/JavaParser)
  → ANTLR4 parse tree
  → Mvel3ToJavaParserVisitor (delegates to specialised converter classes)
  → JavaParser AST (including MVEL-specific nodes from javaparser-mvel fork)
  → VariableAnalyser (discovers used context variables and read properties)
  → MVELTranspiler (wraps in CompilationUnit with Evaluator<C,W,O> class)
  → EvalPre callback (injects variable-extraction statements for MAP/LIST/POJO)
  → MVELToJavaRewriter (coercion, overload, getter/setter, null-safe, modify/with expansion, == → Objects.equals)
  → ClassFilterValidator (optional; rejects forbidden class references)
  → KieMemoryCompiler (in-memory javac)
  → ClassManager.define (hidden-class loader via MethodHandles.Lookup.defineHiddenClass)
  → Evaluator instance
```

Each stage is deterministic and observable — the intermediate Java
source is what javac compiles and what runs.

## Key design decisions

### Transpile, don't interpret

The v2 interpreter was the bottleneck for large rule sets. v3 compiles
once and invokes as Java, trading first-call latency for steady-state
throughput.

### Two-phase AST: ANTLR4 → JavaParser

The ANTLR4 grammar (`Mvel3Parser.g4` imports `JavaParser.g4`) parses
the full Java 17 syntax plus MVEL extensions (null-safe `!.`, inline
cast `#Type#`, list/map literals `[...]`, `modify`/`with`/compact-with,
BigDecimal/BigInteger suffixes, temporal literals).

The parse tree is immediately converted to a JavaParser AST by
`Mvel3ToJavaParserVisitor`, which delegates to specialised converter
classes in `parser.antlr4.mveltojavaparser.*`. This lets every
downstream stage (rewriting, code generation, filtering) operate on
a single, well-typed AST with JavaParser's symbol solver available.

MVEL-specific AST nodes (`NullSafeFieldAccessExpr`, `NullSafeMethodCallExpr`,
`BigDecimalLiteralExpr`, `ModifyStatement`, `WithStatement`,
`CompactWithExpression`, `InlineCastExpr`, `ListCreationLiteralExpression`,
`MapCreationLiteralExpression`, `TemporalLiteralExpr`, `HalfBinaryExpr`,
`DrlNameExpr`, etc.) live in the javaparser-mvel fork under
`org.mvel3.parser.ast.expr`.

### Three context shapes (MAP / LIST / POJO)

Each evaluator is generic in `<C, W, O>` (context, with-variable, out).
The same compiler pipeline emits three slightly different bindings
depending on how variables are passed in:

| ContextType | Variable extraction | Write-back |
|---|---|---|
| MAP | `(Type) map.get("name")` | `map.put("name", val)` |
| LIST | `(Type) list.get(index)` | `list.set(index, val)` |
| POJO | `pojo.getName()` | `pojo.setName(val)` |
| NONE | no extraction | n/a |

The `EvalPre` callback (injected by `MVELCompiler.transpile()`) generates
variable-extraction statements before the rewriter runs. Context
write-back is handled by `MVELToJavaRewriter`.

### MVELToJavaRewriter: the semantic bridge

`MVELToJavaRewriter` is the largest component — it closes the semantic
gap between MVEL's dynamic, property-oriented model and Java's static
type system. It dispatches on AST node type and handles:

- **Property access → getters**: `obj.name` → `obj.getName()` (resolves
  via reflection; leaves public fields unchanged).
- **Assignments → setters/put/set**: `name = val` → `map.put("name", val)`
  or `pojo.setName(val)`, depending on context type. Compound operators
  (`+=`, `-=`) are expanded with BigDecimal overloading.
- **Null-safe access** (`!.`): `p!.getName()` → `p != null ? p.getName() : null`,
  with recursive nesting for chained access.
- **Modify / with / compact-with**: Expanded into flat Java blocks scoped
  to the target object, with `update(obj)` appended for modify.
- **BigDecimal / BigInteger operator overloading**: Arithmetic operators →
  method calls (`add`, `subtract`, etc.) with `MathContext.DECIMAL128`.
  Comparison operators → `compareTo()` chains.
- **Type coercion** (via `CoerceRewriter`): Registered coercion pairs
  (e.g., int→BigDecimal, String→int, Date→long) produce wrapping
  expressions (`BigDecimal.valueOf()`, `Integer.parseInt()`, etc.).
- **Reference equality** (`==`, `!=`): Rewritten to `java.util.Objects.equals()`
  for reference types, preserving `==` for primitives, null comparisons,
  and enums.
- **Increment/decrement write-back**: `x++`/`--x` on context variables
  writes the updated value back to the context map/list/pojo.
- **List/map literals**: `[1, 2]` → `Arrays.asList(1, 2)`,
  `["a": 1]` → `Map.of("a", 1)`.
- **Temporal literals**: `3h30m` → `Duration.ofHours(3).plusMinutes(30)`.
- **Method call scoping**: Bare method calls are scoped to the root/with
  object; primitives auto-boxed for method dispatch.

### Read-property tracking (property reactivity)

`VariableAnalyser` records every `NameExpr`/`DrlNameExpr` visited.
`MVELTranspiler` emits a `getReadProperties()` override on the
generated class, returning a `String[]` of identifier names. These may
include names that are not properties; downstream consumers filter
them against the context type's settable properties before constructing
Drools alpha-node masks for property-reactive dirty tracking.

### Public `ClassFilter` (issue #413)

Opt-in compile-time restriction on the classes an expression may
reference. Defense-in-depth, not a sandbox — source-level filtering
cannot stop reflection (see `SECURITY.md`). Key decisions:

- Implemented as a single post-transpile AST walk in
  `ClassFilterValidator`, so every class reference funnels through one
  place regardless of which rewriter resolved it.
- Framework scaffolding in the generated class header (`implements
  Evaluator<Map<...>>`) is deliberately skipped to avoid false positives.
- Scope-prefix `ClassOrInterfaceType` nodes (package-path fragments like
  `java.lang` in `java.lang.Runtime`) are skipped — only the outermost
  type is validated.
- If neither JavaParser's symbol solver nor `Class.forName` can resolve
  a reference, it is silently skipped — javac will reject genuinely
  invalid references a moment later.
- All violations are batched into a single `ClassFilterException` with
  line/column positions.
- `SAFE_PRESET` blocks common dangerous classes and packages as a
  documented starting point.

### TypeSolver wiring

`MVELTranspiler.buildTypeSolver()` constructs a
`CombinedTypeSolver(ReflectionTypeSolver, ClassLoaderTypeSolver)` when a
custom classloader is provided via `CompilerParameters.classLoader`, so
application classes resolve during transpilation, not just JRE classes.

### Batch compilation

`MVELBatchCompiler` accumulates multiple expressions via `add()`,
deduplicates them through `LambdaCatalog`, and compiles all unique
sources in a single `KieMemoryCompiler` call. This amortises javac
startup cost across hundreds of expressions. Supports two modes:

- **With persistence**: Uses the global `LambdaRuntime` singleton.
  Pre-persisted lambdas are loaded from disk; newly compiled ones are
  persisted.
- **Without persistence**: Uses a batch-local `LambdaCatalog`. No
  disk I/O.

### Class deduplication in ClassManager

`ClassManager.define()` hashes the extracted `eval` method bytecode
(via ASM's `MethodByteCodeExtractor` + Murmur3) for each class. It skips
definition when an equivalent entry already exists. Other methods,
including `getReadProperties()`, are not part of this comparison, and
a skipped class's FQN is not registered as an alias. Compiler-level
deduplication therefore needs to retain the existing evaluator FQN.

## Lambda subsystem (content-addressed persistence)

The lambda subsystem deduplicates compiled evaluators and optionally
persists them to disk so subsequent runs skip recompilation. After the
LambdaRegistry refactor (#429), responsibilities are split across
purpose-specific classes:

### Content-addressed keying

`LambdaKey` is the identity of a lambda expression. Equality is based
on `(methodSignature, normalisedBody)`. The hash is Murmur3 of
`normalisedBody` only — this is deliberate: it clusters keys by body
content so `LambdaCatalog.findSubtypeOverloadReuse()` can efficiently
find candidates with the same body but compatible parameter types.

`VariableNameNormalizerVisitor` (a JavaParser `ModifierVisitor`)
renames all local variables to a canonical sequence (`v1`, `v2`, `v3`)
in declaration order. This makes structurally identical lambdas with
different variable names hash-equal. Only declared variables are
renamed; method names, class names, and field references are preserved.

`LambdaUtils` is the factory: parse source → normalize variables →
extract method signature → compute Murmur3 hash → build `LambdaKey`.

### Deduplication

`LambdaCatalog` is a pure in-memory data structure — no filesystem, no
system properties. Every `register(LambdaKey)` call allocates a new
logical ID. It then attempts three paths:

1. **Exact match**: If the key is already registered, reuse its physical
   ID (`reused=true`).
2. **Subtype-overload reuse**: If another key has the same normalised
   body, same return type, same method name, same parameter count, and
   all parameter types are assignable (the new key's params are
   subtypes), reuse that physical ID.
3. **No match**: Allocate a new physical ID.

The result is a `RegistrationResult(logicalId, physicalId, reused)`.
Physical IDs are sequential catalog IDs, not content hashes. The
compiler appends the physical ID to the generated evaluator class name.
Catalog reuse alone does not skip compilation: a persisted artifact
must also exist, or the source must already be pending in the same batch.

### Persistence

```
LambdaRuntime (singleton, composition root)
  ├── RuntimeConfig (system-property-driven configuration)
  ├── LambdaCatalog (in-memory dedup)
  ├── LambdaPersistenceManager (physicalId → ArtifactRef mapping)
  │     └── LambdaArtifactStore (raw classfile byte I/O)
  └── LambdaRegistryStore (Properties-file serialization)
```

- **`LambdaRuntime`**: Lazy singleton via double-checked locking. On
  first access, reads `RuntimeConfig` from system properties, constructs
  all components, and rehydrates from disk if persistence is enabled
  and a registry file exists.
- **`LambdaPersistenceManager`**: Maps physical IDs to `ArtifactRef`
  (FQN + classfile path). `attachArtifact()` triggers an immediate
  synchronous snapshot save.
- **`LambdaRegistryStore`**: Serialises a snapshot
  (`LambdaPersistenceSnapshot` = `CatalogSnapshot` + artifact map) to a
  versioned Properties file (`lambda-registry.dat`). The snapshot
  contains catalog keys, their physical IDs, the next physical/logical
  ID counters, and artifact references; historical logical-to-physical
  ID mappings are not saved. Validates on load: format version, required
  keys, integer values, duplicate artifact physical IDs, orphan artifact
  entries, and invalid paths. Multiple catalog keys may share a physical
  ID after subtype-overload reuse; all are retained when loading.
- **`LambdaArtifactStore`**: Dumb byte I/O — `exists()`, `readBytes()`,
  `deleteAll()`.
- **`LambdaArtifactLoader`**: Idempotent loading — if the FQN is already
  in the `ClassManager`, returns the existing class; otherwise reads
  bytes and defines.

Persistence stores evaluator bytecode and lookup information, not
captured values, evaluator instances, or rule-session state. The
compile-versus-load decision remains in `MVELCompiler` and
`MVELBatchCompiler`: an attached artifact whose classfile exists is
loaded; a missing artifact or classfile is compiled and persisted.
Existing but unreadable or invalid classfiles cause a load error rather
than automatic recompilation.

### Registry format and lifecycle

`lambda-registry.dat` is a Properties file with `format.version=2`:

| Key | Content |
|---|---|
| `catalog.nextPhysicalId`, `catalog.nextLogicalId` | Next ID counters |
| `catalog.entry.<n>.physicalId` | Catalog entry's physical ID |
| `catalog.entry.<n>.methodSignature`, `.normalizedBody` | Normalised evaluator method |
| `artifact.<n>.physicalId`, `.fqn`, `.classFile` | Physical ID, evaluator FQN, and classfile path |

`KieMemoryCompiler.compileAndPersist()` writes classfiles under the
output directory using the FQN's package path. Attaching each new
artifact synchronously rewrites the complete registry snapshot, even
after a batch compile. Paths are stored as supplied; relative paths
remain relative to the runtime working directory. Registry validation
does not check classfile existence; that is checked before reuse.

`LambdaRuntime.reset()` clears in-memory state only.
`resetAndRemoveAllPersistedFiles()` also removes the persistence-root
contents and registry file. Runtime configuration is read on first
singleton access and must not be changed afterwards.

### DRLX metadata boundary

DRLX pre-build consumes `MVELBatchCompiler.getArtifactRef()` and saves
`drlx-lambda-metadata.properties` with `format.version=2`. Each
`rule.<ruleName>.<counter>` entry has separate `expression`, `fqn`, and
`classFile` keys. No physical ID crosses this persistence boundary.
Multiple rule entries can refer to the same persisted evaluator.

At runtime, DRLX compares each saved expression with the current
expression and loads its `ArtifactRef` directly through
`LambdaArtifactLoader`, without a catalog lookup. DRLX still consults
`LambdaRuntime` configuration when constructing its compiler. Its
custom pre-build output directory does not change the global MVEL
registry-file setting.

DRLX's default behavior for a missing entry, expression mismatch, or
class-load failure is fail-fast; `drlx.compiler.metadataMismatch=fallback`
returns that expression to the normal compiler path. Malformed metadata
fails during file loading before this per-expression fallback applies.
A separate optional `drlx-rule-ast.pb` cache stores the rule IR with a
source SHA-256 hash. Neither artifact stores a completed KieBase;
DRLX reconstructs it at runtime.

### Configuration (system properties)

| Property | Default | Purpose |
|---|---|---|
| `mvel3.compiler.lambda.persistence` | `true` | Enables persistence in the single compiler and registry snapshots |
| `mvel3.compiler.lambda.persistence.path` | `target/generated-classes/mvel` | Default classfile output directory |
| `mvel3.compiler.lambda.registry.file` | `<path>/lambda-registry.dat` | Registry file location |
| `mvel3.compiler.lambda.resetOnTestStartup` | `false` | Wipe all state on init (for tests) |

`MVELBatchCompiler` selects its persistence mode through its constructor:
a non-null output directory enables classfile persistence and the global
catalog; no directory selects in-memory compilation with a batch-local
catalog. Callers must choose the constructor consistently with their
runtime configuration. DRLX's `noPersist()` facade requires
`mvel3.compiler.lambda.persistence=false` and uses the local-catalog path.

## Security model

MVEL3 assumes expression authors are trusted. `ClassFilter` reduces
the blast radius of mistakes; it is not a boundary against adversarial
input. See `SECURITY.md` for the full stance and reporting process.

## Known limitations

Tracked in `README.md` / `CLAUDE.md`. Notable gaps that affect design:
- Custom operators (after/before/in/matches) not yet implemented
- `**`, `<<<` operators not supported / incorrect
- `HalfBinaryExpr` rewriting not implemented
- Lambda extractor doesn't support generics and arrays

## Module / package layout

| Package | Responsibility |
|---|---|
| `org.mvel3` | Public API: `MVEL`, `MVELBuilder`, `MVELCompiler`, `MVELBatchCompiler`, `Evaluator`, `ClassManager`, `ClassFilter`, `CompilerParameters` |
| `org.mvel3.parser.antlr4` | ANTLR4 parser: `Antlr4MvelParser`, `Mvel3ToJavaParserVisitor` |
| `org.mvel3.parser.antlr4.mveltojavaparser` | Specialised converter classes for grammar constructs |
| `org.mvel3.parser` | Parser interfaces, legacy JavaParser-based parser, DRL parser |
| `org.mvel3.transpiler` | `MVELTranspiler`, `MVELToJavaRewriter`, `CoerceRewriter`, `OverloadRewriter`, `VariableAnalyser` |
| `org.mvel3.transpiler.context` | `TranspilerContext`, `Declaration`, `DeclaredFunction`, `StaticMethod` |
| `org.mvel3.javacompiler` | `KieMemoryCompiler`, `StoreClassLoader`, `JavaCompilerFactory` |
| `org.mvel3.lambdaextractor` | `LambdaCatalog`, `LambdaRuntime`, `LambdaKey`, `LambdaArtifactStore`/`Loader`, `LambdaPersistenceManager`, `LambdaRegistryStore`, `VariableNameNormalizerVisitor` |
| `org.mvel3.javalambda` | Java-source lambda extraction and deduplication with a local catalog, registry-source generation, and source rewriting; separate from evaluator classfile persistence |
| `org.mvel3.util` | `TypeResolver`, `ClassTypeResolver`, `ClassUtils`, `MethodUtils` |
| `org.mvel3.parser.ast.expr` (javaparser-mvel fork) | MVEL-specific AST nodes: `NullSafeFieldAccessExpr`, `CompactWithExpression`, `BigDecimalLiteralExpr`, `ModifyStatement`, `InlineCastExpr`, `TemporalLiteralExpr`, etc. |
