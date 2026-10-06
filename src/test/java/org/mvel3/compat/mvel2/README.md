# MVEL2 arithmetic tests on MVEL3

`ArithmeticTests` is imported from the mvel2 branch at
`d7c96a36c692df6a9a027a8416e275253fdfda01`.
All 121 declared test methods are present, including `testIssue321` and the
cases disabled in the existing `org.mvel3.ArithmeticTest`.
The existing test and MVEL3 implementation are unchanged.

The imported expressions, input values, expected values, and assertions are
retained. No casts, numeric suffixes, `var`, `return`, or terminators are added
to make the expressions pass. Failures are intentionally visible.

## Running

The diagnostic class has the JUnit Jupiter tag `mvel2-compatibility`, excluded
by default through Surefire's `excludedGroups` property. The adapter's own
regression tests run normally. No imported test is individually disabled.

After changing the module, install it before testing:

```bash
mvn -pl :mvel3 -am -DskipTests install
```

Run all imported cases (a nonzero Maven exit is expected while differences remain):

```bash
mvn -DexcludedGroups= -Dtest=org.mvel3.compat.mvel2.ArithmeticTests test
```

Run all imported cases, skipping tests whose failure reason is documented in
`Compatibility.md` (annotated with `@KnownCompatibility`):

```bash
mvn -DexcludedGroups="mvel2-known-compatibility-difference" \
    -Dtest=org.mvel3.compat.mvel2.ArithmeticTests test
```

Run one original case:

```bash
mvn -DexcludedGroups= -Dtest=org.mvel3.compat.mvel2.ArithmeticTests#testMath3 test
```

Run the existing suite and adapter checks:

```bash
mvn test
```

Surefire writes the current failures, exceptions, and stack traces to
`target/surefire-reports/TEST-org.mvel3.compat.mvel2.ArithmeticTests.xml` and
the corresponding `.txt` file. `RESULTS.md` records the initial run for
method-by-method investigation. Expected values should stay unchanged while
investigating differences.

## Adaptations

- Jupiter `@Test` annotations are added for discovery. JUnit 4 assertions are
  kept through `junit.framework.Assert` and `org.junit.Assert`.
- `AbstractTest` retains the original `createTestMap`, required nested fixtures,
  `assertNumEquals`, and `assertEqualsByComparingTo`. Its `test` and
  `testCompiledSimple` helpers evaluate through MVEL3. Eight supporting fixture
  classes are copied into this package's `res` subpackage.
- Direct `MVEL` evaluation calls use `Mvel3TestSupport`. MVEL2's
  `CompiledExpression`, `ExecutableStatement`, and `ExpressionCompiler` references
  use the test adapter's carrier/compiler. The carrier implements `Serializable`
  to keep the original variable declarations; MVEL2 expression serialization is
  not tested here.
- `ParserContext` and `ParserConfiguration` are replaced with the test-local
  `TestCompilerContext`, which stores declared input types and class imports.
  Strong typing and strict type enforcement flags are removed explicitly;
  the previous adapter also did not emulate these MVEL2 modes.
- The single `Make.Map` call is replaced with a mutable standard `HashMap` with
  the same key and value. `DataConversion` is replaced with local Integer/Double
  conversions in `AbstractTest`, retaining truncation, the Integer upper-bound
  check, and the existing comparison tolerance/rounding logic. Other MVEL2
  conversion types are not implemented because this suite does not use them.
- There is no MVEL2 Maven dependency or `org.mvel2` reference in the test code.
  JUnit 4 remains a test dependency for the original assertions.
- MVEL2 optimizer selection and the Java-style class literal flag assignments
  are removed with comments at their original locations. In `testIssue249`,
  the unused ParserContext root argument is removed; the original expression,
  input Map, and assertion stay the same. The null root argument in `testModExpr`
  remains supported.
- The MVEL2 helper's root-object, thread, optimizer, debug-symbol, and
  serialization matrix is not reproduced. This adapter uses Map context.
  For an anonymous `TestInterface` fixture, the declaration uses its public
  interface, preserving the original fixture instance.
- MVEL3 needs types before compilation. A compiled-expression carrier compiles
  at its first evaluation using declared types, otherwise runtime input types,
  and reuses the evaluator afterward. This is not MVEL2's compile-time type
  inference or optimizer lifecycle.
- MVEL3 has no interpreter. `eval` compiles and evaluates; `executeExpression`
  evaluates a reusable carrier. Consequently, the original interpreted/compiled
  comparison cases compare those MVEL3 paths, not two different engines.
  A passing comparison alone does not prove agreement with MVEL2.

## Initial verification

On 2026-10-05, with JDK 17.0.19 and Maven 3.9.16, on MVEL3 commit
`1aaa255c3421b72a050e3015815e15925b5b3546` plus this migration:

- Imported suite: 121 cases, 67 passed, 22 assertion failures, 32 exceptions,
  zero skipped.
- Adapter checks: 4 passed.
- Default suite: 785 cases, zero failures/errors, 117 skipped (includes the four
  adapter checks; excludes the diagnostic class).
- An automated method-body comparison confirmed that all 121 methods match the
  upstream source after only the API/annotation adaptations listed above.

These are results against the retained MVEL2 assertions. The MVEL2 engine was
not run as a baseline. No production behavior was changed to address failures.
The original `assertNumEquals` permits type conversion and rounding, so a PASS
does not necessarily establish equality of the raw result values or types.

## Verification after removing the MVEL2 dependency

On 2026-10-05:

- All 121 imported methods retained their result category: 67 passed,
  22 assertion failures, 32 exceptions. Assertion failure messages also match
  the run before dependency removal.
- Seven comparison checks passed both before and after the replacement;
  five adapter checks passed afterward.
- The default suite ran 793 cases with zero failures/errors and 117 skipped.
- `mvn dependency:tree -Dincludes=org.mvel:mvel2` returned no matching dependency.
  The diagnostic test JVM's classpath also contained no MVEL2 jar.
- The imported method bodies were compared with the version before removal;
  only configuration/API adaptations and the Map construction changed.
