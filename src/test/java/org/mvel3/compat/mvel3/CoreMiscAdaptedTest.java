/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mvel3.compat.mvel3;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mvel3.Evaluator;
import org.mvel3.MVEL;
import org.mvel3.Type;
import org.mvel3.compat.mvel2.res.Bar;
import org.mvel3.compat.mvel2.res.Base;
import org.mvel3.compat.mvel2.res.Foo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Porting of the miscellaneous tests of CoreConfidenceTests from MVEL2 adapted for MVEL3 syntax.
 *
 * <p>Adaptations:
 * <ul>
 *   <li><b>block-syntax</b>: variables declared with {@code var}/typed declarations, blocks end
 *       with explicit {@code return}.</li>
 *   <li><b>single-quote-string</b>: single-quoted string literals replaced with double quotes.</li>
 *   <li><b>null-safe-operator</b>: {@code .?} rewritten as explicit {@code != null} check.</li>
 *   <li><b>this-reference</b>: root POJO properties accessed via a named map input.</li>
 *   <li><b>runtime-type-method-resolution</b>: raw-Map element cast to its concrete type so
 *       MVEL3's static type analysis can resolve the property/method.</li>
 *   <li><b>undeclared-input-variable</b>: inputs declared explicitly in the type map.</li>
 *   <li><b>with-block-syntax</b>: rewritten as plain setter calls.</li>
 *   <li><b>reserved-method-name</b>: {@code containsKey} rewritten as map lookup via
 *       {@code .get("x") != null}.</li>
 *   <li><b>inline-array-literal</b> / <b>void-method-expression</b>: array literals use Java
 *       syntax; void calls wrapped in a block with {@code return null;}.</li>
 *   <li><b>input-variable-redeclaration</b>: re-assign under a fresh name not in the map.</li>
 * </ul>
 *
 * <p>Not representable (skipped):
 * <ul>
 *   <li>{@code testAssertKeyword}: the {@code assert} keyword is not in the MVEL3 grammar.</li>
 *   <li>{@code testCompilerExceptionFormatting} / {@code testDuplicateVariableDeclaration}: test
 *       that compilation fails — MVEL3 does not raise the same errors, no equivalent to assert.</li>
 * </ul>
 */
public class CoreMiscAdaptedTest {

  // ---------------------------------------------------------------------------
  // Fixtures
  // ---------------------------------------------------------------------------

  public static class MyBean {
    private int var;

    public int getVar() {
      return var;
    }

    public void setVar(int var) {
      this.var = var;
    }
  }

  public static class TestHelper {
    public static void method(int id, Object[] arr) {
      System.out.println(id + " -> " + arr.length);
    }

    public static void method(Object obj1, Object obj2) {
      System.out.println(obj1 + "-> " + obj2);
    }
  }

  public static class Fooz {
    public Fooz(String id) {
    }
  }

  public static class Underscore {
    public String get_id() {
      return "test";
    }
  }

  public static class ARef {
    public static int getSize(String s) {
      return 0;
    }
  }

  public static class BRef extends ARef {
    public static int getSize(String s) {
      return s.length();
    }
  }

  public static class Triangle {
    private java.util.List<Map<String, Object>> deliveries = new java.util.ArrayList<>();

    public java.util.List<Map<String, Object>> getDeliveries() {
      return deliveries;
    }
  }

  public static class Order {
    private int number = 20;

    public int getNumber() {
      return number;
    }

    public void setNumber(int number) {
      this.number = number;
    }
  }

  // ---------------------------------------------------------------------------
  // testParsingStability1
  // ---------------------------------------------------------------------------

  @Test
  public void testParsingStability1() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapExpression(
        "order.number == 1 || order.number == (1 + 1) || order.number == $id",
        Boolean.class, Set.of(),
        Map.of("order", Type.type(Order.class), "$id", Type.type(Integer.class)));
    assertThat(eval.eval(Map.of("order", new Order(), "$id", 20))).isTrue();
  }

  // ---------------------------------------------------------------------------
  // testDifferentImplSameCompile
  // ---------------------------------------------------------------------------

  @Test
  public void testDifferentImplSameCompile() {
    Evaluator<Map<String, Object>, Void, Object> eval = new MVEL().compileMapExpression(
        "a.funMap[\"hello\"]",
        Object.class, Set.of(), Map.of("a", Type.type(Base.class)));
    Map<String, Object> testMap = new HashMap<>();

    for (int i = 0; i < 100; i++) {
      Base b = new Base();
      b.funMap.put("hello", "dog");
      testMap.put("a", b);
      assertThat(eval.eval(testMap)).isEqualTo("dog");

      b = new Base();
      b.funMap.put("hello", "cat");
      testMap.put("a", b);
      assertThat(eval.eval(testMap)).isEqualTo("cat");
    }
  }

  // ---------------------------------------------------------------------------
  // testUseOfVarKeyword — single-quote → double-quote
  // ---------------------------------------------------------------------------

  @Test
  public void testUseOfVarKeyword() {
    Evaluator<Map<String, Object>, Void, String> eval = new MVEL().compileMapBlock(
        "var barfoo = \"FOO_BAR\"; return barfoo;",
        String.class, Set.of(), Map.of());
    assertThat(eval.eval(Map.of())).isEqualTo("FOO_BAR");
  }

  // ---------------------------------------------------------------------------
  // testAssignment5 — block-syntax: explicit var + return
  // ---------------------------------------------------------------------------

  @Test
  public void testAssignment5() {
    Evaluator<Map<String, Object>, Void, Integer> eval = new MVEL().compileMapBlock(
        "var x = (10) + (5); return x;",
        Integer.class, Set.of(), Map.of());
    assertThat(eval.eval(Map.of())).isEqualTo(15);
  }

  // ---------------------------------------------------------------------------
  // testDeepProperty / testDeepProperty2 — runtime-type: cast map element
  // ---------------------------------------------------------------------------

  @Test
  public void testDeepProperty() {
    MyBean bean = new MyBean();
    bean.setVar(4);
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put("bean", bean);
    Map<String, Object> context = new HashMap<>();
    context.put("map", innerMap);

    // runtime-type-method-resolution: cast the map element to MyBean
    Evaluator<Map<String, Object>, Void, Integer> eval = new MVEL().compileMapExpression(
        "((MyBean) map[\"bean\"]).getVar()",
        Integer.class,
        Set.of(CoreMiscAdaptedTest.class.getCanonicalName() + ".MyBean"),
        Map.of("map", Type.type(Map.class)));
    assertThat(eval.eval(context)).isEqualTo(4);
  }

  @Test
  public void testDeepProperty2() {
    MyBean bean = new MyBean();
    bean.setVar(4);
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put("bean", bean);
    Map<String, Object> context = new HashMap<>();
    context.put("map", innerMap);

    Evaluator<Map<String, Object>, Void, Integer> eval = new MVEL().compileMapExpression(
        "((MyBean) map[\"bean\"]).getVar()",
        Integer.class,
        Set.of(CoreMiscAdaptedTest.class.getCanonicalName() + ".MyBean"),
        Map.of("map", Type.type(Map.class)));
    assertThat(eval.eval(context)).isEqualTo(4);
  }

  // ---------------------------------------------------------------------------
  // testNullSafe / testNullSafe2 — null-safe rewritten as explicit null check
  // ---------------------------------------------------------------------------

  @Test
  public void testNullSafe() {
    Foo foo = new Foo();

    // null-safe-operator: `foo.?bar.name == null` rewritten as explicit null check
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapExpression(
        "foo.getBar() == null || foo.getBar().getName() == null",
        Boolean.class, Set.of(), Map.of("foo", Type.type(Foo.class)));

    Map<String, Object> map = new HashMap<>();
    foo.setBar(new Bar());
    map.put("foo", foo);
    assertThat(eval.eval(map)).isFalse();

    foo.setBar(null);
    assertThat(eval.eval(map)).isTrue();
  }

  @Test
  public void testNullSafe2() {
    Foo foo = new Foo();
    Bar bar = new Bar();
    foo.setBar(bar);
    bar.setName(null);

    // null-safe-operator + input-variable-redeclaration: rewrite as explicit null check.
    // MVEL2: `x = foo.bar.?name.length == null` — true when name is null, false when set.
    // MVEL3: check `foo.getBar().getName() == null` directly (int.length() cannot be == null).
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapExpression(
        "foo.getBar() == null || foo.getBar().getName() == null",
        Boolean.class, Set.of(), Map.of("foo", Type.type(Foo.class)));

    Map<String, Object> map = new HashMap<>();
    map.put("foo", foo);
    assertThat(eval.eval(map)).isTrue();   // bar.name is null

    bar.setName("x");
    assertThat(eval.eval(map)).isFalse(); // bar.name is "x", not null
  }

  // ---------------------------------------------------------------------------
  // testContextAssignments — this-reference: use named input
  // ---------------------------------------------------------------------------

  @Test
  public void testContextAssignments() {
    // this-reference: `this.name = 'bar'` → access via named `foo` input
    Evaluator<Map<String, Object>, Void, Object> eval = new MVEL().compileMapBlock(
        "foo.setName(\"bar\"); return null;",
        Object.class, Set.of(), Map.of("foo", Type.type(Foo.class)));
    Foo foo = new Foo();
    eval.eval(Map.of("foo", foo));
    assertThat(foo.getName()).isEqualTo("bar");
  }

  // ---------------------------------------------------------------------------
  // testWithInsideBlock — with-block-syntax: use plain setter calls
  // ---------------------------------------------------------------------------

  @Test
  public void testWithInsideBlock() {
    // with-block-syntax: `with(f) { setBoolTest(true) }` → explicit setter calls
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapBlock(
        "var f = new Foo(); f.setBoolTest(true); return f.isBoolTest();",
        Boolean.class,
        Set.of(Foo.class.getCanonicalName()),
        Map.of());
    assertThat(eval.eval(Map.of())).isTrue();
  }

  // ---------------------------------------------------------------------------
  // testGenericsMap — reserved-method-name: containsKey → get(..) != null
  // ---------------------------------------------------------------------------

  @Test
  public void testGenericsMap() {
    Triangle triangle = new Triangle();
    triangle.getDeliveries().add(new HashMap<>());

    // reserved-method-name: `containsKey` keyword → workaround with `.get(..) != null`
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapExpression(
        "triangle.getDeliveries().get(0).get(\"x\") != null",
        Boolean.class, Set.of(), Map.of("triangle", Type.type(Triangle.class)));
    assertThat(eval.eval(Map.of("triangle", triangle))).isFalse();
  }

  // ---------------------------------------------------------------------------
  // testArray / testArray2 — void-method-expression: wrap in block
  // ---------------------------------------------------------------------------

  @Test
  public void testArray() {
    // void-method-expression: wrap calls in a block
    Evaluator<Map<String, Object>, Void, Object> eval = new MVEL().compileMapBlock(
        "TestHelper.method(1, new String[]{\"a\", \"b\"});"
            + " TestHelper.method(2, new String[]{new String(\"a\"), new String(\"b\")});"
            + " TestHelper.method(3, new Fooz[]{new Fooz(\"a\"), new Fooz(\"b\")});"
            + " return null;",
        Object.class,
        Set.of(TestHelper.class.getCanonicalName(), Fooz.class.getCanonicalName()),
        Map.of());
    eval.eval(Map.of());
  }

  @Test
  public void testArray2() {
    // inline-array-literal: {..} → new Type[]{..}; void-method-expression wrapped in block
    Evaluator<Map<String, Object>, Void, Object> eval = new MVEL().compileMapBlock(
        "TestHelper.method(1, new String[]{\"a\", \"b\"});"
            + " TestHelper.method(2, new String[]{\"a\", \"b\"});"
            + " TestHelper.method(3, new Fooz[]{new Fooz(\"a\"), new Fooz(\"b\")});"
            + " return null;",
        Object.class,
        Set.of(TestHelper.class.getCanonicalName(), Fooz.class.getCanonicalName()),
        Map.of());
    eval.eval(Map.of());
  }

  // ---------------------------------------------------------------------------
  // testArrays
  // ---------------------------------------------------------------------------

  @Test
  public void testArrays() {
    // block-syntax: add `return null;` for void block
    Evaluator<Map<String, Object>, Void, Object> eval = new MVEL().compileMapBlock(
        "Object[] a = new Object[3]; a[0] = \"a\"; a[1] = \"b\"; a[2] = \"c\";"
            + " System.out.println(java.util.Arrays.toString(a)); return null;",
        Object.class, Set.of(), Map.of());
    eval.eval(Map.of());
  }

  // ---------------------------------------------------------------------------
  // testFieldNameWithUnderscore — undeclared-input: declare input explicitly
  // ---------------------------------------------------------------------------

  @Test
  public void testFieldNameWithUnderscore() {
    Underscore underscore = new Underscore();

    // undeclared-input-variable: pass the Underscore as a named input
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapExpression(
        "u.get_id() == \"test\"",
        Boolean.class,
        Set.of(CoreMiscAdaptedTest.class.getCanonicalName() + ".Underscore"),
        Map.of("u", Type.type(Underscore.class)));
    assertThat(eval.eval(Map.of("u", underscore))).isTrue();
  }

  // ---------------------------------------------------------------------------
  // testStaticMethodInvocation
  // ---------------------------------------------------------------------------

  @Test
  public void testStaticMethodInvocation() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("value", "1234");

    Evaluator<Map<String, Object>, Void, Integer> evalA = new MVEL().compileMapExpression(
        "ARef.getSize(value)", Integer.class,
        Set.of(ARef.class.getCanonicalName()),
        Map.of("value", Type.type(String.class)));
    Evaluator<Map<String, Object>, Void, Integer> evalB = new MVEL().compileMapExpression(
        "BRef.getSize(value)", Integer.class,
        Set.of(BRef.class.getCanonicalName()),
        Map.of("value", Type.type(String.class)));
    assertThat(evalA.eval(vars)).isEqualTo(0);
    assertThat(evalB.eval(vars)).isEqualTo(4);
  }

  // ---------------------------------------------------------------------------
  // testMultiplyIntByDouble
  // ---------------------------------------------------------------------------

  @Test
  public void testMultiplyIntByDouble() {
    Evaluator<Map<String, Object>, Void, Double> eval = new MVEL().compileMapExpression(
        "i * d", Double.class, Set.of(),
        Map.of("i", Type.type(Integer.class), "d", Type.type(Double.class)));
    assertThat(eval.eval(Map.of("i", 10, "d", 0.3))).isEqualTo(3.0);

    Evaluator<Map<String, Object>, Void, Double> eval2 = new MVEL().compileMapExpression(
        "i * 0.3", Double.class, Set.of(), Map.of("i", Type.type(Integer.class)));
    assertThat(eval2.eval(Map.of("i", 10))).isEqualTo(3.0);
  }

  // ---------------------------------------------------------------------------
  // testCharToStringCoercionForComparison
  // ---------------------------------------------------------------------------

  @Test
  public void testCharToStringCoercionForComparison() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapExpression(
        "ch == \"a\"", Boolean.class, Set.of(),
        Map.of("ch", Type.type(Character.class)));
    assertThat(eval.eval(Map.of("ch", 'a'))).isTrue();

    Evaluator<Map<String, Object>, Void, Boolean> eval2 = new MVEL().compileMapExpression(
        "ch == \"b\"", Boolean.class, Set.of(),
        Map.of("ch", Type.type(Character.class)));
    assertThat(eval2.eval(Map.of("ch", 'a'))).isFalse();
  }

  // ---------------------------------------------------------------------------
  // testInstanceofOnInnerClass / testInstanceofWithPackageImport /
  // testInstanceofWithPackageImportAndInnerClass
  // ---------------------------------------------------------------------------

  @Test
  public void testInstanceofOnInnerClass() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapExpression(
        "value instanceof ARef", Boolean.class,
        Set.of(ARef.class.getCanonicalName()),
        Map.of("value", Type.type(Object.class)));
    assertThat(eval.eval(Map.of("value", new ARef()))).isTrue();
  }

  @Test
  public void testInstanceofWithPackageImport() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapExpression(
        "value instanceof CoreMiscAdaptedTest", Boolean.class,
        Set.of(CoreMiscAdaptedTest.class.getCanonicalName()),
        Map.of("value", Type.type(Object.class)));
    assertThat(eval.eval(Map.of("value", new CoreMiscAdaptedTest()))).isTrue();
  }

  @Test
  public void testInstanceofWithPackageImportAndInnerClass() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapExpression(
        "value instanceof ARef", Boolean.class,
        Set.of(ARef.class.getCanonicalName()),
        Map.of("value", Type.type(Object.class)));
    assertThat(eval.eval(Map.of("value", new ARef()))).isTrue();
  }

  // ---------------------------------------------------------------------------
  // testNullBigDecimal
  // ---------------------------------------------------------------------------

  @Test
  public void testNullBigDecimal() {
    checkBigDecimalOp("a + b", new BigDecimal("1"), new BigDecimal("2"), new BigDecimal("3"));
    checkBigDecimalOp("a - b", new BigDecimal("5"), new BigDecimal("3"), new BigDecimal("2"));
    checkBigDecimalOp("a * b", new BigDecimal("2"), new BigDecimal("3"), new BigDecimal("6"));
    checkBigDecimalOp("a / b", new BigDecimal("6"), new BigDecimal("3"), new BigDecimal("2"));

    // When one operand is null, MVEL3 should throw (NPE or compilation error)
    checkBigDecimalOpNull("a + b");
    checkBigDecimalOpNull("a - b");
    checkBigDecimalOpNull("a * b");
    checkBigDecimalOpNull("a / b");
  }

  private void checkBigDecimalOp(String expr, BigDecimal a, BigDecimal b, BigDecimal expected) {
    Evaluator<Map<String, Object>, Void, BigDecimal> eval = new MVEL().compileMapExpression(
        expr, BigDecimal.class,
        Set.of("java.math.BigDecimal"),
        Map.of("a", Type.type(BigDecimal.class), "b", Type.type(BigDecimal.class)));
    assertThat(eval.eval(Map.of("a", a, "b", b))).isEqualByComparingTo(expected);
  }

  @SuppressWarnings("unchecked")
  private void checkBigDecimalOpNull(String expr) {
    Evaluator<Map<String, Object>, Void, ?> eval = new MVEL().compileMapExpression(
        expr, BigDecimal.class,
        Set.of("java.math.BigDecimal"),
        Map.of("a", Type.type(BigDecimal.class), "b", Type.type(BigDecimal.class)));

    Map<String, Object> varsANull = new HashMap<>();
    varsANull.put("a", null);
    varsANull.put("b", new BigDecimal("2"));
    assertThatThrownBy(() -> ((Evaluator<Map<String, Object>, Void, ?>) eval).eval(varsANull));

    Map<String, Object> varsBNull = new HashMap<>();
    varsBNull.put("a", new BigDecimal("1"));
    varsBNull.put("b", null);
    assertThatThrownBy(() -> ((Evaluator<Map<String, Object>, Void, ?>) eval).eval(varsBNull));
  }
}
