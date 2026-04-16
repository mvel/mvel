# MVEL3 Design

Architectural overview for MVEL3. Intended as a stable complement to
`README.md` (user-facing) and `CLAUDE.md` (contributor quick-reference).

## Goals

- Compile MVEL expressions to Java bytecode via javac at build/runtime,
  so the hot path is pure Java invocation with no interpreter overhead.
- Preserve MVEL2 surface syntax where practical; break compatibility
  only where MVEL2's interpreter semantics cannot be reconciled with
  Java's type system.

## Compilation pipeline

```
MVEL source
  → ANTLR4 parser (Mvel3Parser.g4 / Mvel3Lexer.g4)
  → JavaParser AST (via Mvel3ToJavaParserVisitor)
  → MVELToJavaRewriter (coercion, overload, method/field resolution)
  → CompilationUnitGenerator (wrap in Evaluator<C,W,O> class)
  → ClassFilterValidator (optional; rejects forbidden class references)
  → KieMemoryCompiler (in-memory javac)
  → ClassManager.define (hidden-class loader)
  → Evaluator instance
```

Each stage is deterministic and observable — the intermediate Java
source is what runs.

## Key design decisions

### Transpile, don't interpret
The v2 interpreter was the bottleneck for large rule sets. v3 compiles
once and invokes as Java, trading first-call latency for steady-state
throughput.

### Three context shapes (MAP / LIST / POJO)
Each evaluator is generic in `<C, W, O>` (context, with-variable, out).
The same compiler pipeline emits three slightly different bindings
depending on how variables are passed in — get-by-key, get-by-index,
or get-by-getter. See `ContextType`.

### Public `ClassFilter` (issue #413)
Opt-in compile-time restriction on the classes an expression may
reference. Defense-in-depth, not a sandbox — source-level filtering
cannot stop reflection (see `SECURITY.md`). Implemented as a
post-transpile AST walk in `ClassFilterValidator`, so every class
reference funnels through one place regardless of which rewriter
resolved it. Framework scaffolding in the generated class header is
deliberately skipped to avoid false positives.

### TypeSolver wiring
The JavaParser symbol solver is built from
`CombinedTypeSolver(ReflectionTypeSolver, ClassLoaderTypeSolver)` so
application classes visible via `CompilerParameters.classLoader`
resolve during transpilation, not just JRE classes.

### Lambda caching
Compiled evaluators can be cached and persisted by shape via
`LambdaRegistry` / `LambdaKey`. Same expression text + same variable
shape = same compiled class, reused across calls.

## Security model

MVEL3 assumes expression authors are trusted. `ClassFilter` reduces
the blast radius of mistakes; it is not a boundary against adversarial
input. See `SECURITY.md` for the full stance and reporting process.

## Known limitations

Tracked in `README.md` / `CLAUDE.md`. Notable gaps that affect design:
- Custom operators (after/before/in/matches) not yet implemented
- `**`, `<<<` operators not supported / incorrect
- `HalfBinaryExpr` rewriting not implemented

## Module / package layout

See `CLAUDE.md#Key-Packages` for the current list; this document tracks
design decisions rather than module inventory.
