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

---

## Chained null-coalescing `or` operator (chor)

> `@KnownCompatibility("chor-operator")`

MVEL2 supports `or` as a chained null-coalescing / fallback operator (`a or b or c`, `a or 'fubar'`).
MVEL3 does not support `or` keyword for coalescing. In MVEL3 / Java, use ternary operators or `Optional`.

---

## Regular expression matching operator (`~=`)

> `@KnownCompatibility("regex-operator")`

MVEL2 supports regex matching via `~=` / `=~` (`foo.bar.name ~= '[a-z].+'`).
MVEL3 does not support the `~=` regex binary operator. Use Java `Pattern` or `String.matches(...)`.

---

## `empty` keyword / test

> `@KnownCompatibility("empty-operator")`

MVEL2 supports `empty` as a pseudo-literal/keyword to test for null, empty strings, collections, or arrays (`'' == empty`, `[] == empty`, `['a'] != empty`).
MVEL3 does not treat `empty` as a keyword. Use standard methods such as `.isEmpty()` or null checks.

---

## `is` operator for type testing

> `@KnownCompatibility("is-operator")`

MVEL2 allows `is` as a synonym for `instanceof` (`c is java.lang.String`).
MVEL3 requires standard Java `instanceof`.

---

## `contains` operator

> `@KnownCompatibility("contains-operator")`

MVEL2 provides an infix `contains` operator for strings and collections (`list contains 'Happy!'`, `sentence contains 'fox'`).
MVEL3 does not support infix `contains`. Use Java method calls: `list.contains("Happy!")` or `sentence.contains("fox")`.

---

## Phonetic / similarity operators (`soundslike`, `strsim`)

> `@KnownCompatibility("soundslike-operator")`, `@KnownCompatibility("strsim-operator")`

MVEL2 includes built-in string phonetic matching (`soundslike` using Soundex) and Levenshtein similarity (`strsim`).
MVEL3 omits these custom operators. Use custom helper methods or dedicated libraries.

---

## `this` reference in evaluation context

> `@KnownCompatibility("this-reference")`

In MVEL2, `this` refers to the root context object (e.g. `Base`).
In MVEL3 Map evaluation context, expressions are compiled into evaluator classes where `this` refers to the generated evaluator instance itself.

---

## Date comparison with relational operators (`<`, `>`)

> `@KnownCompatibility("date-comparison")`

MVEL2 supports `<` and `>` directly on `java.util.Date` instances (`dt1 < dt2`).
MVEL3 transpiles directly to Java bytecode, where relational operators `<` and `>` cannot be applied to `Date` objects. Use `dt1.before(dt2)` or `dt1.compareTo(dt2) < 0`.

---

## `convertable_to` operator

> `@KnownCompatibility("convertable-to-operator")`

MVEL2 supports `convertable_to` to check whether a value can be converted to a target type (`pi convertable_to Integer`).
MVEL3 does not support this operator.

---

## `isdef` operator

> `@KnownCompatibility("isdef-operator")`

MVEL2 provides `isdef` to check if a variable or property is defined in scope (`isdef _v1`).
MVEL3 resolves variable names statically at compile time and does not have an `isdef` operator.

---

## Hash string-coercion concat operator (`#`)

> `@KnownCompatibility("hash-concat-operator")`

MVEL2 provides a `#` binary operator that coerces both operands to `String` and
concatenates them:

```
// MVEL2
0 # 1      → "01"
0 # "bar"  → "0bar"
```

MVEL3 uses `#` for inline casts, such as `value#int#`. It does not support
MVEL2's `a # b` string-coercion concatenation syntax. The lexer emits a `HASH`
token for `#`; line comments begin with `//`.

Use `"" + a + b` or explicit `String.valueOf(a) + String.valueOf(b)` as the
MVEL3 equivalent.

---

## Inline `import` statement in expressions

> `@KnownCompatibility("inline-import")`

MVEL2 allows an `import` statement as a standalone statement inside an
expression script:

```
// MVEL2
import org.mvel2.tests.core.res.Foo;
[ 10, 20 ]
```

MVEL3 transpiles to Java source code.  `import` is only valid at the top level
of a compilation unit, not inside a method body, so it is treated as an
extraneous token and causes a parse error:

```
line 1:1 extraneous input 'import' expecting ...
```

Pass the type as an import via the API instead:
```java
Set<String> imports = new HashSet<>();
imports.add(Foo.class.getCanonicalName());
mvel.executeExpression(expression, imports, vars);
```

---

## MVEL2 `foreach` statement syntax

> `@KnownCompatibility("foreach-syntax")`

MVEL2 accepts statements such as `foreach (item : 10) { ... }`. MVEL3 does not
yet support this syntax. The lexer defines a `FOREACH` token, but the parser's
statement rule accepts Java-style `for` and has no `foreach` alternative.

These inputs currently fail during AST conversion with a null-node error:

```
A reference was unexpectedly null.
```

The failure occurs even with an empty loop body. Comments and quotes in comments
are not the cause. The imported tests retain the original syntax and remain
executable diagnostics; the corresponding adapted tests are disabled while
`foreach` support is missing.

---

## JavaParser null-node transpiler crashes

> `@KnownCompatibility("javaparser-null-node")`

Certain malformed or unsupported syntax constructs (such as invalid binary expressions in fuzz strings or `foreach` loops) cause the MVEL3 transpiler to construct an incomplete AST or pass null into JavaParser node setters (e.g. `BinaryExpr.setLeft`), resulting in:

```
java.lang.AssertionError: A reference was unexpectedly null.
```

Because JavaParser throws `java.lang.AssertionError` (an `Error` rather than an `Exception`), these failures bypass normal `catch (Exception e)` exception handlers in negative tests. These are transpiler / AST visitor defects with no user-level workaround.

---

## Qualified class-name literals

> `@KnownCompatibility("class-literal")`

MVEL2 evaluates a bare qualified class name such as `java.lang.String` as a
`Class` object at runtime by navigating through the package tree:

```
// MVEL2
java.lang.String   → String.class
```

MVEL3 transpiles the expression to Java source code.  The transpiler treats
`java.lang.String` as a field access chain (`java` → `lang` → `String`), which
fails to compile because `java` is not a variable in scope:

```
cannot find symbol: class lang
  location: package java
```

Use `String.class` (or any fully-qualified `.class` literal) in MVEL3
expressions that need a `Class` object.

---

## Void method call as an expression

> `@KnownCompatibility("void-method-expression")`

MVEL2 evaluates `testee.add('a')` and ignores the (void) result.  MVEL3 wraps a
single expression as the `Object`-typed return value, so the generated code does
not compile:

```
incompatible types: void cannot be converted to java.lang.Object
```

Use a block with an explicit return (the block form requires a `return`):

```java
testee.add("a"); return null;
```

---

## Nested class referenced by binary name

> `@KnownCompatibility("inner-class-binary-name")`

`"new " + Outer.Inner.class.getName()` produces `Outer$Inner`.  MVEL2 resolves
that name reflectively; MVEL3 emits it into Java source, where it fails:

```
cannot find symbol
  symbol:   class VarargTests$MyVarargTestee
```

Add the class to the API imports set (`getCanonicalName()`) and use the simple
name in the expression.

---

## Compound assignment on a property used as an expression value

> `@KnownCompatibility("compound-assign-void-setter")`

MVEL2 returns the new value of `holder.intPrimitive += 10`.  MVEL3 rewrites the
property compound assignment into a setter call, which is `void`:

```
// generated
return holder.setIntPrimitive(holder.getIntPrimitive() + 10);
// incompatible types: void cannot be converted to java.lang.Object
```

For `BigDecimal` properties the generated code also uses `BigDecimal.valueOf(..)`
by simple name, which fails with `cannot find symbol` unless
`java.math.BigDecimal` is in the imports (see `missing-auto-import`).

Workaround: write the assignment as a statement and read the property back,
and add the import when needed:

```java
holder.intPrimitive += 10; return holder.intPrimitive;
```

Local-variable compound assignment (`simpleInteger += 10`) is unaffected.

---

## Null comparison on BigInteger / BigDecimal (`==`, `!=`)

> `@KnownCompatibility("big-number-null-comparison")`

In MVEL2, `==` and `!=` on `BigInteger` and `BigDecimal` handles `null` variables gracefully (e.g., `bigDecimalValue == new BigDecimal("0")` returns `false` when `bigDecimalValue` is `null`).

In MVEL3, equality comparisons on `BigInteger` and `BigDecimal` are transpiled to `.compareTo(...) == 0`. When the variable is `null`, invoking `.compareTo()` results in a `NullPointerException`.

Workaround in MVEL3: Use `java.util.Objects.equals(a, b)` or null checks before comparison.

---

## Null with relational operators (`<`, `>`, `<=`, `>=`)

> `@KnownCompatibility("null-relational-comparison")`

In MVEL2, comparing `null` with relational operators (`<`, `>`, `<=`, `>=`) against numbers/characters always evaluates to `false` (e.g. `null > 0` → `false`, `0 > null` → `false`).

In MVEL3/Java, relational operators trigger unboxing or `.compareTo()` on the operand, throwing a `NullPointerException` when an operand is `null`.

---

## Property assignment used as an expression value

> `@KnownCompatibility("property-assign-void-setter")`

MVEL2 returns the assigned value of `foo.bar.age = 21`.  MVEL3 rewrites the
property assignment into a setter call, which is `void`:

```
// generated
return foo.getBar().setAge(21);
// incompatible types: void cannot be converted to java.lang.Object
```

Workaround: write the assignment as a statement and return afterwards:

```java
foo.bar.age = 21; return null;
```

---

## Redeclaring a variable that is already an input

> `@KnownCompatibility("input-variable-redeclaration")`

MVEL2 allows `java.math.BigDecimal a = ...` even when the variable map already
contains `a` (here `a` and `b` are `null` entries of `createTestMap()`).  MVEL3
declares every map entry as a local variable, so the redeclaration fails:

```
variable a is already defined in method eval(java.util.Map)
```

Workaround: use variable names that are not in the input map.

---

## Set expressions (`compileSetExpression` / `executeSetExpression`)

> `@KnownCompatibility("set-expression-api")`

MVEL2 can compile a property path such as `bar.intarray[0]` as a *set expression* and
later assign a value to it, coercing the value to the target type (`"12"` becomes
`Integer` 12):

```java
Serializable s = compileSetExpression("bar.intarray[0]", ctx);
executeSetExpression(s, foo, "12");
```

MVEL3 has no such API.  The test adapter emulates it by compiling
`bar.intarray[0] = setValue` against a POJO root, which currently fails with an NPE
inside JavaParser.

Workaround: write a normal assignment in a block and pass the value as an input:

```java
foo.bar.intarray[0] = value; return null;
```

With a map holding `foo` and `value`, MVEL3 also coerces the `String` `"12"` to `Integer`.


---

## Collection projection (`(name in things)`)

> `@KnownCompatibility("projection-operator")`

MVEL2 projects a property or method over a collection, optionally filtering with `if`
(`$` is the current element):

```
(name in things)
(toUpperCase() in ['bar', 'foo'] if ($ == 'bar'))
```

MVEL3 has no projection syntax; the parser stops at `in`:

```
Parse error: Parse errors: line 1:6 no viable alternative at input '(name in'
```

Workaround: write an explicit `for` loop. The collection input must be cast to a typed
collection, because a loop variable over a raw `List` input is unresolved, and
`res.contains(..)` on a local variable currently fails ("Unsupported member reference"),
so `indexOf(..) >= 0` is used instead:

```java
var res = new ArrayList<String>();
for (Thing t : (List<Thing>) things) { res.add(t.getName()); }
return res.indexOf("Bob") >= 0;
```

---

## Inline array literal (`{1,2,3}`)

> `@KnownCompatibility("inline-array-literal")`

MVEL2 creates an array from `{1,2,3}`, `{}` and `{{1,2},{3}}`. MVEL3 follows Java and
has no bare-brace array literal; the parser stops at `{`:

```
Parse error: Parse errors: line 1:0 mismatched input '{' ...
```

Workaround: use Java array syntax: `new int[] {1,2,3}`, `new Object[0]`,
`new int[][] {{1,2},{3}}`. List `[..]` and map `[k : v]` literals are supported.

---

## List concatenation (`list + list`, `list + element`)

> `@KnownCompatibility("list-concat-operator")`

MVEL2 returns a new list for `[1,2,3] + [4,5,6]` and `[1,2,3] + 4`. MVEL3 generates a Java `+`
on `List` operands:

```
bad operand types for binary operator '+'
```

Workaround: `List<Integer> l = new ArrayList<>([1,2,3]); l.addAll([4,5,6]); return l;`

---

## Trailing comma in a list literal

> `@KnownCompatibility("trailing-comma-list")`

MVEL2 accepts `[2, 3, 4, ]` (3 elements). MVEL3 fails with
`A reference was unexpectedly null`. Remove the trailing comma.

---

## Nested inline collections

> `@KnownCompatibility("nested-inline-collection")`

MVEL2 accepts a list or map literal inside another list or map literal:

```
["k" : [1, 2]]
[[1, 2], [3]]
```

MVEL3 generates invalid Java for the inner literal and javac rejects it:

```
illegal start of expression
```

Workaround: declare the inner collection first and reference it by name:

```java
List<Integer> a = [1, 2];
var m = ["k" : a];
```

A `Map` whose value is a `List` literal can also fail inside JavaParser type solving
(`Equivalent types are: ...`); build such a map with `put(..)`.

No test in `compat.mvel2` is annotated with this category yet: the seven affected tests in
`InlineCollectionsTests` fail first for another reason (`single-quote-string`; for
`testSubListInMap` also `block-syntax`) and carry a `Category not isolated:` comment.

---

## Reserved method names (`contains`, `matches`)

> `@KnownCompatibility("reserved-method-name")`

> **IMPORTANT: to be addressed with priority.** `contains` is a very common `java.util` method name,
> so many MVEL2 expressions are affected.

A method call named `contains` or `matches` fails on any receiver (input variable, local variable,
`String`, `List.of(..)`):

```
Unsupported member reference: <expr>.contains
```

Cause (from reading the grammar, not changed or fixed here): `Mvel3Lexer.g4` defines the tokens
`CONTAINS : 'contains'` and `MATCHES : 'matches'` (operators). The `identifier` rule in `JavaParser.g4`
does not include them, so `expression '.' (identifier | methodCall | ...)` does not match and
`MemberReferenceExpressionConverter.convertMemberReferenceExpression` throws
`IllegalArgumentException("Unsupported member reference: ...")`.

Observation (cause not investigated): in expression mode (no `return`), `l.contains("Y")` returned
the receiver `[X, Y]` instead of a boolean.

Workarounds: `indexOf(..) >= 0`, `containsKey`, `containsAll`, `isEmpty`.

A possible production fix is to allow `CONTAINS` / `MATCHES` after `.` (e.g. in `identifier`).

No test in `compat.mvel2` is annotated with this category yet. Tests annotated `contains-operator`
or `regex-operator`-style categories were not re-tested for this cause.

---

## Function declarations (`def`, `function`, anonymous functions)

> `@KnownCompatibility("function-declaration")`

MVEL2 lets an expression declare functions and call them later:

```
def fooFunction(x) x; fooFunction('foo')
function testFunction(x, y) { return x + y; }; testFunction(7, 3);
a = function { 'foobar' }; a();
```

The MVEL3 grammar has no `def` / `function` keyword. The declaration is read as a method call and
the parser fails at the opening parenthesis or at the `{`:

```
no viable alternative at input 'def fooFunction('
no viable alternative at input 'function testFunction('
```

Other shapes were observed (no further investigation):

- `a = function { "foobar" }; a();` → `A string was unexpectedly empty` (double-quoted) /
  parse-tree-is-null (single-quoted).
- A Java-style method declaration inside the expression (`int f(int x) { return x; }`) →
  `Cannot invoke "HasParentNode.setParentNode(Node)" because "current" is null`.
- A Java lambda (`Function<Integer,Integer> f = x -> x + 1; return f.apply(1);`) compiled but failed
  at run time with `NoClassDefFoundError: org/mvel3/GeneratorEvaluator___N/0x...`.

Workarounds: call a static method of a class passed in the imports
(`Funcs.add(7, 3)`), declare local variables, or inline the logic.

---

## `until` loop

> `@KnownCompatibility("until-statement")`

MVEL2 has `until (cond) { .. }` and `do { .. } until (cond)`, which loop while the condition is false.
MVEL3 has the `while` / `do .. while` statements only. `until (i == 6) { .. }` fails with
`missing ';' at '{'` (it is read as a method call); `do { .. } until (cond)` crashes the parser with
`Cannot invoke ...ParExpressionContext.expression() because ...parExpression() is null`.
Use `while (!(cond))`.

---

## `with` block and inline-with syntax

> `@KnownCompatibility("with-block-syntax")`

MVEL2 supports a `with` block for bulk property assignment with comma-separated entries:

```
with (foo) { aValue = 'One', bValue = 'Two' }
```

and an inline form applied to a receiver expression:

```
foo.{ name = 'poopy', aValue = 'bar' }
```

### What MVEL3 does

- **Comma-separated block** (`with (foo) { aValue = 'One', bValue = 'Two' }`) — parse error:
  ```
  mismatched input ',' expecting ...
  ```
- **Inline form** (`foo.{ ... }`) — compiles silently but acts as a no-op; property
  assignments inside `{...}` are not applied to the receiver (observed failure:
  `expected:<[poopy]> but was:<[dog]>`).

### Semicolon form works in MVEL3

A `with` block **does** work in MVEL3 if every statement (including the last one)
ends with a semicolon:

```
with (foo) { aValue = "One"; bValue = "Two"; }
```

Verified to work: multi-property assignment, compound assignment (`+=`, `*=`),
method calls (`setName(..)`), and sequential `with` blocks on the same object.

Limitations of the semicolon form:

- **Outer-variable capture** — writing `with (foo) { aValue = (y = "One"); }` to
  capture a result in an outer variable `y` fails: MVEL3 resolves `y` as a property
  of `foo` rather than a declared variable.
- **Nested inside another expression** — e.g. `[ 'foo': (with (new Foo()) { ... }) ]`
  or `(Foo) with (foo) { ... }` gives a parse error at `(`.
- **Inline form on a new object** — `new Foo().{ name = 'bar' }` is a no-op;
  use `Foo f = new Foo(); with (f) { setName("bar"); }` instead.
- **Implied-this leading-dot form** — `.{ data = 'foo' }` (evaluated against a root
  object) is not supported.

### For MVEL2 compatibility tests

The MVEL2 tests use the comma form, so they still fail with `with-block-syntax`.
The adapted tests (`WithAdaptedTest`) use the semicolon form where possible.

---

## `array.size()` on Java arrays

> `@KnownCompatibility("array-size-method")`

MVEL2 treats `stringArray.size()` / `intArray.size()` as a "magic" size accessor on arrays.
In MVEL3 the array keeps its Java type, so javac reports "cannot find symbol: method size()".
Use `array.length`.

---

## Index access on a `String`

> `@KnownCompatibility("string-index-access")`

MVEL2 returns the `char` at that position for `str[1]`. MVEL3 transpiles an index on a non-array
to `.get(1)`, which `String` does not have ("cannot find symbol: method get(int)"). Use `str.charAt(1)`.

---

## Map entry assignment through property syntax

> `@KnownCompatibility("map-property-assignment")`

`mp.test = "bar"` where `mp` is a `Map` assigns the entry `test` in MVEL2. MVEL3 looks for a field or
setter on the map type and fails with "The node has neither a public field or a getter method for :
mp.test". Use `mp.put("test", "bar")`.

---

## Null-safe access `a.?map['foo']`

> `@KnownCompatibility("null-safe-operator")`

In MVEL2 the result is `null` when `a.map` is `null`, and the member value otherwise. MVEL3 compiles
`a.?map["foo"]` (and `a?.map["foo"]`, `foo.?bar.baz`, `foo.?bar.convert(x)`) but the result is the
object on the left of `.?` (`a`, `foo`), whether the member is null or not. Plain `a.map["foo"]` on a
null map throws an NPE as in MVEL2. Write an explicit null check: `a.map != null ? a.map.get("foo") : null`.

---

## Compiled expression reused with a different input type

> `@KnownCompatibility("static-input-type")`

MVEL2 (reflective optimizer) re-resolves the accessor when the runtime type changes. MVEL3 generates code
for the input type given at compile time. The adapter compiles on first evaluation with the runtime
type of the first input, so a later input of another type raises `ClassCastException`.

---

## Error type and message for inaccessible methods

> `@KnownCompatibility("exception-type-mismatch")`

`service.hello()` on a `private` method throws `PropertyAccessException` with "Error: unable to resolve
method" in MVEL2. MVEL3 fails while compiling the generated Java ("hello() has private access"),
wrapped in `KieMemoryCompilerException`.

---

## Non-ASCII identifiers on Windows

> `@KnownCompatibility("windows-encoding")`

On Windows, `KieMemoryCompiler` passes the generated Java source to `javac` using the JVM's
platform default encoding (e.g. `Windows-1252`). Non-ASCII characters in variable names,
property names, and string literals (漢字, Japanese kana, etc.) are replaced with `?` before
the source reaches the compiler, causing `KieMemoryCompilerException` with "illegal start of
expression".

The same expression compiles and runs correctly on Linux, where the default encoding is UTF-8.

This is a **MVEL3 bug**, not a MVEL2/MVEL3 design difference. `KieMemoryCompiler` should pass
sources to `javac` with an explicit `StandardCharsets.UTF_8` option. The `I18nTest` test passes
on Linux and fails on Windows; the failure is recorded here so the Windows CI failure is visible
rather than silently skipped.

---

## Undeclared input variable at compile time

> `@KnownCompatibility("undeclared-input-variable")`

MVEL2 compiles `bar.add("hello")` without knowing what `bar` is (it resolves it at run time). MVEL3
generates Java source, so every variable must be declared with a type before compilation; an undeclared
one fails with `UnsolvedSymbolException: Unsolved symbol in bar`. Declare the input
(`TestCompilerContext.addInput` / the `types` map).

---

## Class aliasing (`Foo244 = String; new Foo244('foobar')`)

> `@KnownCompatibility("class-alias")`

MVEL2 lets a variable hold a `Class` and then use it after `new`. MVEL3 translates to Java, where `new`
needs a type name, so the construct has no equivalent; the assignment `Foo244 = String;` itself fails to
resolve `Foo244`. Import the class or use `String` directly.

---

## Classes from a separate jar class loader

> `@KnownCompatibility("external-jar-classloader")`

`CoreImportsTests#testNestedEnumFromJar` imports `org.drools.examples.eventing.EventRequest` from
`eventing-example.jar` through a `URLClassLoader`. With the MVEL3 compile call that takes a class loader:

- the original jar (no directory entries) is not seen by the compiler:
  `package org.drools.examples.eventing does not exist`;
- a repacked jar with directory entries is found, but `EventRequest.Status.ACTIVE` is compiled as the
  instance property `status` of the `EventRequest` class and fails with
  `non-static method getStatus() cannot be referenced from a static context` (the nested enum `Status`
  and the property `status` clash). A nested enum without a same-named property (`Triangle.Foo.OBTUSE`) works.

---

## Static members through a package import

> `@KnownCompatibility("static-member-unqualified")`

MVEL2 makes the constants of an imported class usable by simple name (`new Status( START )` after
`addPackageImport("...res.Status")`). MVEL3 reports `cannot find symbol: variable START`; write
`Status.START`. A static import through the API (`static org...Status.*`) was observed to work.

---

## Index access on a `Set`

> `@KnownCompatibility("set-index-access")`

MVEL2 reads `pojo.dates[0]` on a `Set<Date>`. MVEL3 rewrites `[0]` to `.get(0)`, which `Set` does not have:

```
cannot find symbol: method get(int)  location: interface java.util.Set<java.util.Date>
```

Workaround: `pojo.dates.iterator().next()`. Index access on a `List` (`pojo.list[0]`) works.

---

## List to array coercion

> `@KnownCompatibility("list-to-array-coercion")`

MVEL2 coerces a `List` (also a list of lists) to `char[]` / `char[][]` when it is bound to an array property. MVEL3 assigns it as written and javac reports
`incompatible types: java.util.ArrayList cannot be converted to char[]`. Workaround: convert explicitly,
e.g. `foo.charArray = value.toString().toCharArray()` or build the array in the expression.

---

## Non-public runtime class as the input type

> `@KnownCompatibility("non-public-input-type")`

The type of a root or variable is taken from the runtime class of the value. For `Collections.singleton(..)` and
`Collections.unmodifiableCollection(..)` this is a non-public JDK class, and javac rejects the generated code
(`Collections.SingletonSet has private access`, `UnmodifiableCollection is not public`). Workaround: declare the
input with a public type (`Collection`) and call the method on the named input (`c.size()`).
