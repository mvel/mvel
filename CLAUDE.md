# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Type

**Type:** java

## Project Overview

MVEL3 (MVFLEX Expression Language v3) is a complete rewrite of MVEL. Instead of interpreting expressions at runtime (MVEL2), MVEL3 **transpiles** MVEL expressions into Java source code, compiles them in-memory via javac, and executes the resulting bytecode.

**Status**: Alpha (v3.0.0-alpha1). APIs may change.

## Build Commands

**Prerequisites**: JDK 17+, Maven 3.8.7+. The [javaparser-mvel](https://github.com/mvel/javaparser-mvel) fork must be built and installed locally first (`mvn clean install` in that repo).

```bash
# Build
mvn clean install

# Run all tests
mvn clean test

# Run a specific test class
mvn test -Dtest=MVELCompilerTest

# Run a specific test method
mvn test -Dtest=MVELCompilerTest#testAssignmentIncrement
```

ANTLR4 grammar files in `src/main/antlr4/` are auto-generated to `target/generated-sources/antlr4/` during the build. Legacy JavaCC grammar in `src/main/javacc/` is also generated but being phased out.

## Architecture: Compilation Pipeline

```
MVEL expression string
  → ANTLR4 Parser (Mvel3Parser.g4/Mvel3Lexer.g4)
  → ANTLR4 AST
  → Mvel3ToJavaParserVisitor (converts to JavaParser AST)
  → MVELToJavaRewriter (type coercion, method resolution, MVEL-specific transforms)
  → VariableAnalyser (determines referenced variables)
  → CompilationUnitGenerator (wraps in Evaluator class)
  → KieMemoryCompiler (in-memory javac)
  → ClassManager.define() (loads bytecode via MethodHandles.Lookup.defineHiddenClass)
  → Evaluator instance (user calls eval())
```

## Key Packages

- **`org.mvel3`** — Public API: `MVEL` (static factory), `MVELBuilder` (fluent builder), `MVELCompiler`, `Evaluator<C,W,O>` interface, `Type`, `ClassManager`
- **`org.mvel3.parser.antlr4`** — Primary ANTLR4 parser: `Antlr4MvelParser`, `Mvel3ToJavaParserVisitor` (ANTLR→JavaParser AST)
- **`org.mvel3.parser`** — Parser interface (`MvelParser`) and legacy JavaParser-based implementation
- **`org.mvel3.transpiler`** — Transpilation: `MVELTranspiler`, `MVELToJavaRewriter`, `CoerceRewriter`, `OverloadRewriter`, `VariableAnalyser`
- **`org.mvel3.transpiler.context`** — `Declaration`, `TranspilerContext`, `DeclaredFunction`
- **`org.mvel3.javacompiler`** — In-memory Java compilation: `KieMemoryCompiler`, `StoreClassLoader`
- **`org.mvel3.lambdaextractor`** — Lambda caching/persistence: `LambdaRegistry`, `LambdaKey`
- **`org.mvel3.util`** — Utilities: `TypeResolver`, `ClassUtils`, `MethodUtils`

## Three Context Types

The fluent API (`MVEL.map()`, `MVEL.list()`, `MVEL.pojo()`) supports three ways to pass variables:

1. **MAP** — Variables from `Map<String, Object>`, extracted via `map.get("name")`
2. **LIST** — Variables from `List<Object>` by positional index
3. **POJO** — Variables from an object via getter/setter methods

Each generates a different evaluator template (see `src/main/resources/org/mvel3/`).

## Testing

- **JUnit 5** with **AssertJ** assertions
- Tests run in alphabetical order (`runOrder=alphabetical` in surefire)
- `TranspilerTest` interface provides a fluent API for transpilation verification tests
- `MVELTranspilerTest` — Largest test class; tests MVEL→Java transpilation
- `MVELCompilerTest` — Full compile-and-execute tests across all context types
- `ArithmeticTest` — Numeric type coercion and arithmetic operators

## Key Dependencies

- **javaparser-core** (`3.25.5-mvel3-1`) — Custom fork (`org.mvel.javaparser` groupId) with MVEL-specific AST nodes
- **antlr4-runtime** (`4.13.2`) — ANTLR4 parser runtime
- **asm** (`9.7.1`) — Bytecode inspection (used in lambda extraction)
- **drools-compiler** (`10.1.0`) — Used by `DrlToJavaRewriter` (may be removed in future)

## Known Limitations

- Custom operators (`after`, `before`, `in`, `matches`) not yet supported
- Power operator (`**`) not supported
- Left shift operator (`<<<`) generates incorrect code
- Lambda extractor doesn't support generics and arrays
- `HalfBinaryExpr` rewriting not implemented

## Java Syntax Coverage in Mvel3ToJavaParserVisitor

The ANTLR4 grammar (`Mvel3Parser.g4` imports `JavaParser.g4`) parses full Java syntax.
`Mvel3ToJavaParserVisitor` now covers **all Java 17 syntax** including records, sealed classes,
pattern matching for `instanceof`, switch expressions, text blocks, `var` type inference,
module declarations, `yield`, and all standard Java features.

The only remaining gaps are three low-priority edge cases (annotations silently dropped):

| Grammar rule | What's missing | Example |
|---|---|---|
| `pattern` annotation | Annotations between type and variable in `instanceof` pattern | `obj instanceof @NonNull String s` |
| `classType` annotation | Annotations on class type in method references | `@NonNull Outer.Inner::new` |
| `primary` → `<Type>super(args)` | Explicit type arguments on `super()` in primary expression context | `<String>super(args)` |

## Searchable directories

- javaparser-mvel source code: `/home/tkobayas/usr/work/mvel3-development/javaparser-mvel` . You can read the code without approval.
  - For JavaParser APIs (symbol solver, type solver, AST nodes, `ReflectionTypeSolver`, `CombinedTypeSolver`, `JavaParserFacade`, etc.) read here directly. Do NOT extract JARs from `~/.m2/repository`.