# MVEL2 / MVEL3 Compatibility Notes

Known behavioural differences between MVEL2 and MVEL3 relevant to the imported
test suite in this package.

---

## Power operator (`**`)

MVEL2's `**` calls `Math.pow` internally but **casts the result back to `int`**
when both operands are integers (`5 ** 2` → `25`). MVEL3 translates `a ** b`
to `Math.pow(a, b)` at parse time, so the result is always `double`
(`5 ** 2` → `25.0`).

The imported `ArithmeticTests` retains MVEL2's `int`-typed expected values, so
`**` cases fail on type mismatch. The failures are intentional and record a
genuine numeric-type difference between the two engines.

---

## Block syntax: variable declarations and trailing semicolons

MVEL2 allows bare assignments (`a = 100`), omits `var`, and does not require a
trailing semicolon on the last expression in a multi-statement script:

```
// MVEL2 — valid
a = 100; b = 50; (a - b) * 2
```

MVEL3 transpiles to Java and therefore follows Java block syntax. Inside a
block (any expression containing `;`), every variable must be declared with
`var` (or an explicit type), every statement must end with `;`, and a value
must be returned explicitly with `return`:

```java
// MVEL3 — required form
var a = 100; var b = 50; return (a - b) * 2;
```

This is a deliberate design choice: MVEL3 compiles to Java via `javac`, so the
syntax it accepts must be valid Java. Silently relaxing these constraints would
require heuristics that reduce reliability.

The imported `ArithmeticTests` retains MVEL2's bare-assignment style, so
multi-statement cases that rely on it produce parse errors. The MVEL3 regression
suite (`ArithmeticTest`) uses `var` declarations and explicit `return` statements
throughout.
