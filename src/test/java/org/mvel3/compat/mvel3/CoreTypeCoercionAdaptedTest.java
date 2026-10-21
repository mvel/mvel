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
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mvel3.Evaluator;
import org.mvel3.MVEL;
import org.mvel3.Type;
import org.mvel3.compat.mvel2.res.Bar;
import org.mvel3.compat.mvel2.res.Foo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Porting of the type-coercion and conversion tests of CoreConfidenceTests from MVEL2 adapted for MVEL3 syntax.
 *
 * <p>Adaptations:
 * <ul>
 *   <li><b>class-literal</b>: {@code String} class literal is written {@code String.class}.</li>
 *   <li><b>missing-auto-import</b>: {@code java.math.BigInteger}, {@code java.math.BigDecimal}, and {@code java.math.RoundingMode}
 *       are supplied via the API imports set.</li>
 *   <li><b>single-quote-string</b>: single-quoted string literals are changed to double quotes {@code "..."}.</li>
 *   <li><b>string-coercion</b>: {@code (String) 10} becomes {@code String.valueOf(10)}; {@code (char[]) 'abcd'} becomes {@code "abcd".toCharArray()}.</li>
 *   <li><b>block-syntax</b>: variables are declared with types or {@code var}, statements end with {@code ;}, and blocks return explicitly with {@code return}.</li>
 *   <li><b>inline-import</b>: inline {@code import ...;} statements inside expressions are replaced by the compiler imports set.</li>
 *   <li><b>void-method-expression</b>: {@code void} method calls are written as statements followed by {@code return null;}.</li>
 *   <li><b>contains-operator</b>: infix {@code contains} on arrays is rewritten as explicit element checks or loops.</li>
 *   <li><b>regex-operator</b>: {@code ~=} is rewritten using {@code java.util.regex.Pattern}.</li>
 *   <li><b>set-expression-api</b>: {@code compileSetExpression} is replaced by direct setter/field assignment expressions.</li>
 * </ul>
 */
public class CoreTypeCoercionAdaptedTest {

  public static class TestClass2 {
    public void addEqualAuthorizationConstraint(Foo foo, Bar bar, Integer i) {
    }
  }

  public static class B {
    private int value;

    public B() {
    }

    public B(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }

    @Override
    public boolean equals(Object other) {
      return other != null && other instanceof B && value == ((B) other).value;
    }

    @Override
    public int hashCode() {
      return value;
    }
  }

  @Test
  public void testTestIntToLong() {
    Evaluator<Map<String, Object>, Void, Long> eval = new MVEL().compileMapExpression(
        "1 + (long) a", Long.class, Set.of(), Map.of("a", Type.type(Integer.class)));
    assertThat(eval.eval(Map.of("a", 1))).isEqualTo(2L);
  }

  @Test
  public void testTypeCast() {
    Evaluator<Map<String, Object>, Void, String> eval = new MVEL().compileMapExpression(
        "String.valueOf(10)", String.class, Set.of(), Map.of());
    assertThat(eval.eval(Map.of())).isEqualTo("10");
  }

  @Test
  public void testTypeCast2() {
    Evaluator<Map<String, Object>, Void, Integer> eval = new MVEL().compileMapBlock(
        "var map = new java.util.HashMap(); map.put(\"doggie\", new java.util.ArrayList()); return ((java.util.ArrayList) map.get(\"doggie\")).size();",
        Integer.class, Set.of(), Map.of());
    assertThat(eval.eval(Map.of())).isEqualTo(0);
  }

  @Test
  public void testJIRA93() {
    Evaluator<Map<String, Object>, Void, Object> eval = new MVEL().compileMapBlock(
        "testClass2.addEqualAuthorizationConstraint(foo, foo.bar, 5); return null;",
        Object.class, Set.of(), Map.of("testClass2", Type.type(TestClass2.class), "foo", Type.type(Foo.class)));
    Foo foo = new Foo();
    assertThat(eval.eval(Map.of("testClass2", new TestClass2(), "foo", foo))).isNull();
  }

  @Test
  public void testStringToArrayCast() {
    Evaluator<Map<String, Object>, Void, Object> eval = new MVEL().compileMapExpression(
        "\"abcd\".toCharArray()", Object.class, Set.of(), Map.of());
    assertThat((char[]) eval.eval(Map.of())).isEqualTo("abcd".toCharArray());
  }

  @Test
  public void testStringToArrayCast2() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapBlock(
        "char[] _xyxy = \"abcd\".toCharArray(); return _xyxy[0] == 'a';",
        Boolean.class, Set.of(), Map.of());
    assertThat(eval.eval(Map.of())).isTrue();
  }

  @Test
  public void testStaticallyTypedArrayVar() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapBlock(
        "char[] _c___ = new char[10]; return _c___ instanceof char[];",
        Boolean.class, Set.of(), Map.of());
    assertThat(eval.eval(Map.of())).isTrue();
  }

  @Test
  public void testJIRA100() {
    Evaluator<Map<String, Object>, Void, BigDecimal> eval = new MVEL().compileMapBlock(
        "BigDecimal axx = new BigDecimal(10.0); BigDecimal bxx = new BigDecimal(10.0); BigDecimal cxx = axx + bxx; return cxx;",
        BigDecimal.class, Set.of("java.math.BigDecimal"), Map.of());
    assertThat(eval.eval(Map.of())).isEqualTo(new BigDecimal(20));
  }

  @Test
  public void testJIRA100b() {
    Evaluator<Map<String, Object>, Void, BigDecimal> eval = new MVEL().compileMapBlock(
        "BigDecimal axx = new BigDecimal(10.0); BigDecimal bxx = new BigDecimal(10.0); BigDecimal cxx = axx + bxx; return cxx;",
        BigDecimal.class, Set.of("java.math.BigDecimal"), Map.of());
    assertThat(eval.eval(Map.of())).isEqualTo(new BigDecimal(20));
  }

  @Test
  public void testStaticallyTypedLong() {
    Evaluator<Map<String, Object>, Void, Long> eval = new MVEL().compileMapExpression(
        "10L", Long.class, Set.of(), Map.of());
    assertThat(eval.eval(Map.of())).isEqualTo(10L);
  }

  @Test
  public void testDecl() {
    Evaluator<Map<String, Object>, Void, Character> eval = new MVEL().compileMapBlock(
        "char chr; chr = 100; return chr;",
        Character.class, Set.of(), Map.of());
    assertThat(eval.eval(Map.of())).isEqualTo((char) 100);
  }

  @Test
  public void testBigDecimalOutput() {
    Evaluator<Map<String, Object>, Void, Object> eval = new MVEL().compileMapBlock(
        "BigDecimal test = new BigDecimal(\"50000\"); System.out.println(test / new BigDecimal(\"1.13\")); return null;",
        Object.class, Set.of("java.math.BigDecimal"), Map.of());
    assertThat(eval.eval(Map.of())).isNull();
  }

  @Test
  public void testBindingNullToPrimitiveTypes() {
    Foo foo = new Foo();
    foo.setCountTest(10);
    Evaluator<Map<String, Object>, Void, Object> eval = new MVEL().compileMapBlock(
        "foo.setCountTest(0); return null;",
        Object.class, Set.of(), Map.of("foo", Type.type(Foo.class)));
    eval.eval(Map.of("foo", foo));
    assertThat(foo.getCountTest()).isEqualTo(0);
  }

  @Test
  public void testBindingNullToPrimitiveTypes2() {
    Foo foo = new Foo();
    foo.setCountTest(10);
    Evaluator<Map<String, Object>, Void, Object> eval = new MVEL().compileMapBlock(
        "foo.setBoolTest(false); return null;",
        Object.class, Set.of(), Map.of("foo", Type.type(Foo.class)));
    eval.eval(Map.of("foo", foo));
    assertThat(foo.isBoolTest()).isFalse();
  }

  @Test
  public void testBindingNullToPrimitiveTypes3() {
    Foo foo = new Foo();
    foo.setCharTest('a');
    Evaluator<Map<String, Object>, Void, Object> eval = new MVEL().compileMapBlock(
        "foo.setCharTest((char) 0); return null;",
        Object.class, Set.of(), Map.of("foo", Type.type(Foo.class)));
    eval.eval(Map.of("foo", foo));
    assertThat(foo.getCharTest()).isEqualTo((char) 0);
  }

  @Test
  public void testBindingNullToPrimitiveTypes4() {
    Foo foo = new Foo();
    foo.charTestFld = 'a';
    Evaluator<Map<String, Object>, Void, Object> eval = new MVEL().compileMapBlock(
        "foo.charTestFld = (char) 0; return null;",
        Object.class, Set.of(), Map.of("foo", Type.type(Foo.class)));
    eval.eval(Map.of("foo", foo));
    assertThat(foo.charTestFld).isEqualTo((char) 0);
  }

  @Test
  public void testNarrowToWideCompare() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapExpression(
        "new String(\"foo\") == new Object()",
        Boolean.class, Set.of(), Map.of());
    assertThat(eval.eval(Map.of())).isFalse();
  }

  @Test
  public void testPrimitiveArray() {
    Evaluator<Map<String, Object>, Void, Boolean> evalBool = new MVEL().compileMapBlock(
        "var a = true; return array[0] == a || array[1] == a;",
        Boolean.class, Set.of(), Map.of("array", Type.type(boolean[].class)));
    assertThat(evalBool.eval(Map.of("array", new boolean[]{true, false}))).isTrue();

    Evaluator<Map<String, Object>, Void, Boolean> evalInt = new MVEL().compileMapBlock(
        "var a = 2; for (int x : array) { if (x == a) return true; } return false;",
        Boolean.class, Set.of(), Map.of("array", Type.type(int[].class)));
    assertThat(evalInt.eval(Map.of("array", new int[]{1, 3, 4}))).isFalse();

    Evaluator<Map<String, Object>, Void, Boolean> evalInt2 = new MVEL().compileMapBlock(
        "var a = 3; for (int x : array) { if (x == a) return true; } return false;",
        Boolean.class, Set.of(), Map.of("array", Type.type(int[].class)));
    assertThat(evalInt2.eval(Map.of("array", new int[]{1, 3, 4}))).isTrue();

    Evaluator<Map<String, Object>, Void, Boolean> evalBool2 = new MVEL().compileMapBlock(
        "var a = false; for (boolean x : array) { if (x == a) return true; } return false;",
        Boolean.class, Set.of(), Map.of("array", Type.type(boolean[].class)));
    assertThat(evalBool2.eval(Map.of("array", new boolean[]{true, false}))).isTrue();
  }

  @Test
  public void testPrimitiveArrayWithStrongTyping() {
    Evaluator<Map<String, Object>, Void, Boolean> eval1 = new MVEL().compileMapBlock(
        "var a = true; boolean[] arr = new boolean[] { true, false }; return arr[0] == a || arr[1] == a;",
        Boolean.class, Set.of(), Map.of());
    assertThat(eval1.eval(Map.of())).isTrue();

    Evaluator<Map<String, Object>, Void, Boolean> eval2 = new MVEL().compileMapBlock(
        "var a = 2; int[] arr = new int[] { 1, 3 }; for (int x : arr) { if (x == a) return true; } return false;",
        Boolean.class, Set.of(), Map.of());
    assertThat(eval2.eval(Map.of())).isFalse();

    Evaluator<Map<String, Object>, Void, Boolean> eval3 = new MVEL().compileMapBlock(
        "var a = true; var array = new boolean[] { true, false }; return array[0] == a || array[1] == a;",
        Boolean.class, Set.of(), Map.of());
    assertThat(eval3.eval(Map.of())).isTrue();
  }

  @Test
  public void testArrayCreation() {
    Evaluator<Map<String, Object>, Void, Object> eval0 = new MVEL().compileMapExpression(
        "new Object[0]", Object.class, Set.of(), Map.of());
    assertThat((Object[]) eval0.eval(Map.of())).isEqualTo(new Object[0]);

    Evaluator<Map<String, Object>, Void, Object> eval1 = new MVEL().compileMapExpression(
        "new String[0]", Object.class, Set.of(), Map.of());
    assertThat((String[]) eval1.eval(Map.of())).isEqualTo(new String[0]);

    Evaluator<Map<String, Object>, Void, Object> eval2 = new MVEL().compileMapExpression(
        "new String[] { \"xyz\" }", Object.class, Set.of(), Map.of());
    assertThat((String[]) eval2.eval(Map.of())).isEqualTo(new String[]{"xyz"});

    Evaluator<Map<String, Object>, Void, Object> eval3 = new MVEL().compileMapExpression(
        "new String[] { new String(\"xyz\") }", Object.class, Set.of(), Map.of());
    assertThat((String[]) eval3.eval(Map.of())).isEqualTo(new String[]{"xyz"});

    Evaluator<Map<String, Object>, Void, Object> eval4 = new MVEL().compileMapExpression(
        "new String[] { new String(\"xyz\"), new String(\"abc\") }", Object.class, Set.of(), Map.of());
    assertThat((String[]) eval4.eval(Map.of())).isEqualTo(new String[]{"xyz", "abc"});

    Evaluator<Map<String, Object>, Void, Object> eval5 = new MVEL().compileMapExpression(
        "new B[0]", Object.class, Set.of("org.mvel3.compat.mvel3.CoreTypeCoercionAdaptedTest.B"), Map.of());
    assertThat((B[]) eval5.eval(Map.of())).isEqualTo(new B[0]);

    Evaluator<Map<String, Object>, Void, Object> eval6 = new MVEL().compileMapExpression(
        "new B[] { new B(5) }", Object.class, Set.of("org.mvel3.compat.mvel3.CoreTypeCoercionAdaptedTest.B"), Map.of());
    assertThat((B[]) eval6.eval(Map.of())).isEqualTo(new B[]{new B(5)});

    Evaluator<Map<String, Object>, Void, Object> eval7 = new MVEL().compileMapExpression(
        "new B[] { new B(), new B(), new B() }", Object.class, Set.of("org.mvel3.compat.mvel3.CoreTypeCoercionAdaptedTest.B"), Map.of());
    assertThat((B[]) eval7.eval(Map.of())).isEqualTo(new B[]{new B(), new B(), new B()});
  }

  @Test
  public void testExponentialNotation() {
    Evaluator<Map<String, Object>, Void, Double> eval1 = new MVEL().compileMapExpression(
        "10 + 1.0E+2", Double.class, Set.of(), Map.of());
    assertThat(eval1.eval(Map.of())).isEqualTo(110.0);

    Evaluator<Map<String, Object>, Void, Double> eval2 = new MVEL().compileMapExpression(
        "1.0E+2 - 10", Double.class, Set.of(), Map.of());
    assertThat(eval2.eval(Map.of())).isEqualTo(90.0);

    Evaluator<Map<String, Object>, Void, Double> eval3 = new MVEL().compileMapExpression(
        "10 + 1.0E-2", Double.class, Set.of(), Map.of());
    assertThat(eval3.eval(Map.of())).isEqualTo(10.01);
  }

  @Test
  public void testPrimitiveNumberCoercion() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapExpression(
        "a > b", Boolean.class, Set.of(), Map.of("a", Type.type(int.class), "b", Type.type(double.class)));
    assertThat(eval.eval(Map.of("a", 1, "b", 2.0))).isFalse();
  }

  @Test
  public void testClassLiteral() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapExpression(
        "a == String.class", Boolean.class, Set.of(), Map.of("a", Type.type(Class.class)));
    assertThat(eval.eval(Map.of("a", String.class))).isTrue();
  }

  @Test
  public void testBigIntegerWithZeroValue() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapExpression(
        "a == 0I", Boolean.class, Set.of("java.math.BigInteger"), Map.of("a", Type.type(BigInteger.class)));
    assertThat(eval.eval(Map.of("a", new BigInteger("0")))).isTrue();
  }

  @Test
  public void testBigDecimalWithZeroValue() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapExpression(
        "a == 0B", Boolean.class, Set.of("java.math.BigDecimal"), Map.of("a", Type.type(BigDecimal.class)));
    assertThat(eval.eval(Map.of("a", new BigDecimal("0.0")))).isTrue();
  }

  @Test
  public void testDivisionType() {
    Evaluator<Map<String, Object>, Void, BigDecimal> eval = new MVEL().compileMapExpression(
        "(a / 3).setScale(2, RoundingMode.HALF_UP)",
        BigDecimal.class, Set.of("java.math.BigDecimal", "java.math.RoundingMode"), Map.of("a", Type.type(BigDecimal.class)));
    assertThat(eval.eval(Map.of("a", new BigDecimal("3.0")))).isEqualTo(new BigDecimal("1.00"));
  }

  @Test
  public void testPrimitiveNumberCoercionDuringDivisionShouldWorkOnBothSide() {
    Evaluator<Map<String, Object>, Void, Boolean> evalLeft = new MVEL().compileMapExpression(
        "0.99 >= a / b", Boolean.class, Set.of(), Map.of("a", Type.type(int.class), "b", Type.type(int.class)));
    assertThat(evalLeft.eval(Map.of("a", 1, "b", 2))).isTrue();

    Evaluator<Map<String, Object>, Void, Boolean> evalRight = new MVEL().compileMapExpression(
        "a / b < 0.99", Boolean.class, Set.of(), Map.of("a", Type.type(int.class), "b", Type.type(int.class)));
    assertThat(evalRight.eval(Map.of("a", 1, "b", 2))).isTrue();

    Evaluator<Map<String, Object>, Void, Boolean> evalDouble = new MVEL().compileMapExpression(
        "c / d > 0", Boolean.class, Set.of(), Map.of("c", Type.type(double.class), "d", Type.type(double.class)));
    assertThat(evalDouble.eval(Map.of("c", 1.0, "d", 2.0))).isTrue();
  }

  @Test
  public void testNumberCoercion() {
    Evaluator<Map<String, Object>, Void, Double> eval1 = new MVEL().compileMapExpression(
        "15 * Math.round(new BigDecimal(\"49.4\").doubleValue()) / 100.0",
        Double.class, Set.of("java.math.BigDecimal"), Map.of());
    assertThat(eval1.eval(Map.of())).isEqualTo(7.35);

    Evaluator<Map<String, Object>, Void, BigDecimal> eval2 = new MVEL().compileMapExpression(
        "new BigDecimal(a) / new BigDecimal(\"0.5\")",
        BigDecimal.class, Set.of("java.math.BigDecimal"), Map.of("a", Type.type(int.class)));
    assertThat(eval2.eval(Map.of("a", 10)).intValue()).isEqualTo(20);

    Evaluator<Map<String, Object>, Void, Double> eval3 = new MVEL().compileMapExpression(
        "a / 0.5", Double.class, Set.of(), Map.of("a", Type.type(int.class)));
    assertThat(eval3.eval(Map.of("a", 10)).intValue()).isEqualTo(20);
  }

  @Test
  public void testRegExWithCast() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapExpression(
        "java.util.regex.Pattern.compile(\"[a-z].+\").matcher((String) foo.getBar().getName()).find()",
        Boolean.class, Set.of(), Map.of("foo", Type.type(Foo.class)));
    assertThat(eval.eval(Map.of("foo", new Foo()))).isTrue();
  }

  @Test
  public void testMultiplyIntByDouble() {
    Evaluator<Map<String, Object>, Void, Double> eval1 = new MVEL().compileMapExpression(
        "i*d", Double.class, Set.of(), Map.of("i", Type.type(Integer.class), "d", Type.type(Double.class)));
    assertThat(eval1.eval(Map.of("i", 10, "d", 0.3))).isEqualTo(3.0);

    Evaluator<Map<String, Object>, Void, Double> eval2 = new MVEL().compileMapExpression(
        "i*0.3", Double.class, Set.of(), Map.of("i", Type.type(Integer.class)));
    assertThat(eval2.eval(Map.of("i", 10))).isEqualTo(3.0);
  }

  @Test
  public void testCharToStringCoercionForComparison() {
    Evaluator<Map<String, Object>, Void, Boolean> evalA = new MVEL().compileMapExpression(
        "ch == \"a\"", Boolean.class, Set.of(), Map.of("ch", Type.type(Character.class)));
    assertThat(evalA.eval(Map.of("ch", 'a'))).isTrue();

    Evaluator<Map<String, Object>, Void, Boolean> evalB = new MVEL().compileMapExpression(
        "ch == \"b\"", Boolean.class, Set.of(), Map.of("ch", Type.type(Character.class)));
    assertThat(evalB.eval(Map.of("ch", 'a'))).isFalse();
  }

  @Test
  public void testNullBigDecimal() {
    analyzeBigDecimalOperation("a + b", new BigDecimal("1"), new BigDecimal("2"), new BigDecimal("3"));
    analyzeBigDecimalOperation("a + b", new BigDecimal("1"), null, null);
    analyzeBigDecimalOperation("a + b", null, new BigDecimal("2"), null);
    analyzeBigDecimalOperation("a + b", null, null, null);

    analyzeBigDecimalOperation("a - b", new BigDecimal("5"), new BigDecimal("3"), new BigDecimal("2"));
    analyzeBigDecimalOperation("a - b", new BigDecimal("5"), null, null);
    analyzeBigDecimalOperation("a - b", null, new BigDecimal("3"), null);
    analyzeBigDecimalOperation("a - b", null, null, null);

    analyzeBigDecimalOperation("a * b", new BigDecimal("2"), new BigDecimal("3"), new BigDecimal("6"));
    analyzeBigDecimalOperation("a * b", new BigDecimal("2"), null, null);
    analyzeBigDecimalOperation("a * b", null, new BigDecimal("3"), null);
    analyzeBigDecimalOperation("a * b", null, null, null);

    analyzeBigDecimalOperation("a / b", new BigDecimal("6"), new BigDecimal("3"), new BigDecimal("2"));
    analyzeBigDecimalOperation("a / b", new BigDecimal("6"), null, null);
    analyzeBigDecimalOperation("a / b", null, new BigDecimal("3"), null);
    analyzeBigDecimalOperation("a / b", null, null, null);
  }

  private void analyzeBigDecimalOperation(String expression, BigDecimal a, BigDecimal b, BigDecimal expected) {
    Evaluator<Map<String, Object>, Void, BigDecimal> eval = new MVEL().compileMapExpression(
        expression, BigDecimal.class, Set.of("java.math.BigDecimal"),
        Map.of("a", Type.type(BigDecimal.class), "b", Type.type(BigDecimal.class)));

    Map<String, Object> vars = new HashMap<>();
    vars.put("a", a);
    vars.put("b", b);

    if (a == null || b == null) {
      assertThatThrownBy(() -> eval.eval(vars)).isInstanceOf(NullPointerException.class);
    } else {
      assertThat(eval.eval(vars)).isEqualTo(expected);
    }
  }
}
