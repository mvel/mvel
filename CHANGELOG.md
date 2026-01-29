# Changelog

## 3.0.0-alpha1

MVEL3 is a complete rewrite of MVEL. Instead of interpreting expressions at runtime, MVEL3 transpiles MVEL expressions into Java source code, compiles them in-memory, and executes the resulting bytecode.

### New Features

- **Transpiler-based architecture**: MVEL expressions are transpiled to Java source, compiled via in-memory javac, and executed as bytecode
- **Fluent builder API**: Type-safe compilation via `MVEL.map(...)`, `MVEL.list(...)`, `MVEL.pojo(...)` builders
- **Triple-typed Evaluator**: `Evaluator<C, W, O>` with Context, With, and Output type parameters
- **ANTLR4 parser**: Primary parser using ANTLR4 grammar (`Mvel3Parser.g4`, `Mvel3Lexer.g4`)
- **Lambda persistence**: Compiled lambda classes are cached and persisted to disk for reuse, with subtype overload detection to avoid redundant compilation
- **Temporal literals**: Support for temporal expressions (e.g., `DAY_LITERAL`, `HOUR_LITERAL`)

### Breaking Changes from MVEL2

- **Complete API rewrite**: MVEL2 API is not compatible. Expressions are now compiled via the fluent builder API
- **Java 17+ required**: Minimum Java version raised from Java 8 to Java 17
- **Build dependency**: Requires [javaparser-mvel](https://github.com/mvel/javaparser-mvel) fork to be built locally before building MVEL3

### Known Limitations

- Power operator (`**`) is not yet supported (DROOLS-6572)
- Left shift operator (`<<<`) generates incorrect code
- Method expression handling is not yet supported in some cases
- `JavaSymbolResolver` doesn't work with text blocks
- `HalfBinaryExpr` rewriting is not implemented
- Legacy JavaCC parser is included but being phased out in favor of ANTLR4
