# MVEL2 / MVEL3 Compatibility Notes

Known behavioural differences between MVEL2 and MVEL3 relevant to the imported
test suite in this package.

---

## Power operator (`**`)

> `@KnownCompatibility("power-operator-type")`

MVEL2's `**` calls `Math.pow` internally but **casts the result back to `int`**
when both operands are integers (`5 ** 2` → `25`). MVEL3 translates `a ** b`
to `Math.pow(a, b)` at parse time, so the result is always `double`
(`5 ** 2` → `25.0`).

The imported `ArithmeticTests` retains MVEL2's `int`-typed expected values, so
`**` cases fail on type mismatch. The failures are intentional and record a
genuine numeric-type difference between the two engines.

---

## Integer division always returns `double` in MVEL2

> `@KnownCompatibility("integer-division")`

In MVEL2, dividing two integers returns `double` regardless of whether the
result is whole:

```
// MVEL2
20 / 4   → 5.0  (double)
10 / 3   → 3.333...  (double)
```

This is a special case in `MathProcessor`: the integer branch uses
`toDouble(val1) / toInteger(val2)` for division while keeping `+`, `-`, `*`,
and `%` as integer operations.

In MVEL3 (and Java), integer division truncates toward zero:

```
// MVEL3 / Java
20 / 4   → 5    (int)
10 / 3   → 3    (int)
```

The imported `ArithmeticTests` passes `double` expected values to assertions
for division-containing expressions (e.g. `testMath29`: `expected:<11.25>`),
so these fail against MVEL3's integer result. Adding a `double` literal
(`20.0 / 4`) or an explicit cast (`(double) 20 / 4`) produces `double` in
both engines.

---

## Block syntax: variable declarations and trailing semicolons

> `@KnownCompatibility("block-syntax")`

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

---

## String-to-number coercion

> `@KnownCompatibility("string-coercion")`

MVEL2 implicitly coerces `String` values to numbers when they appear in arithmetic
expressions. If a variable holds `String "3.14"` and another holds `Integer 60`,
MVEL2 evaluates `pi * hour` by parsing the string at runtime:

```
// MVEL2
pi = "3.14", hour = 60
pi * hour   → 188.4  (String coerced to double)
```

MVEL3 resolves types at compile time. It sees `String * Integer` and rejects it
with `bad operand types for binary operator '*'`. Use an explicit inline type hint
(`pi#double# * hour`) or store `pi` as a number in the variable map.

---

## Unsigned left shift (`<<<`)

> `@KnownCompatibility("unsigned-left-shift")`

MVEL2 defines a `<<<` operator that performs an unsigned (rotating) left shift.
Java and MVEL3 do not have a `<<<` operator.

```
// MVEL2
-2 <<< 0   → 2   (Math.abs(-2) << 0)
```

MVEL3 has no equivalent and will fail to parse expressions that use `<<<`.

---

## Single-quoted string literals

> `@KnownCompatibility("single-quote-string")`

MVEL2 accepts single-quoted literals of any length as `String` values:

```
// MVEL2
c + 'bar'   → "catbar"   ('bar' is a String)
```

MVEL3 follows Java char-literal semantics. A single-quoted literal must be
exactly one character; anything longer is a parse error. Use double-quoted
`"bar"` instead.

---

## Missing auto-import for `java.math.*`

> `@KnownCompatibility("missing-auto-import")`

MVEL2 automatically imports `java.math.BigDecimal`, `java.math.BigInteger`,
and other `java.math` types, so expressions can use them by simple name.

MVEL3 does not add these imports automatically. When a variable's declared or
inferred type is `BigDecimal`, the generated Java source uses the simple name
`BigDecimal` without an import, causing a compilation error:

```
cannot find symbol: class BigDecimal
```

Use the fully qualified name `java.math.BigDecimal` in the expression, or pass
a `ParserContext` that includes the import.

---

## Dynamic property access on `Map` values

> `@KnownCompatibility("dynamic-property-type")`

MVEL2 resolves property access dynamically at runtime. When a variable holds a
`Map<String, Object>`, `param.value` navigates to the `"value"` entry and uses
whatever runtime type it finds (e.g. `Integer 10`), so arithmetic works:

```
// MVEL2 — runtime type is Integer
1 + 2 * param.value   → 21
```

MVEL3 resolves types statically. The compiler sees the value as `Object` and
rejects it as an operand for `*`:

```
bad operand types for binary operator '*'
  first type:  int
  second type: java.lang.Object
```

Add the variable to a `ParserContext` with the concrete type, or cast the
property access explicitly: `1 + 2 * (int) param.value`.
