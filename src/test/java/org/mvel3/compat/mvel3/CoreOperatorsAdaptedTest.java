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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.mvel3.Evaluator;
import org.mvel3.MVEL;
import org.mvel3.Type;
import org.mvel3.compat.mvel2.res.Base;
import org.mvel3.compat.mvel2.res.Cheese;
import org.mvel3.compat.mvel2.res.Column;
import org.mvel3.compat.mvel2.res.Foo;
import org.mvel3.compat.mvel2.res.PojoStatic;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Porting of the operators and expressions tests of CoreConfidenceTests from MVEL2 adapted for MVEL3 syntax.
 *
 * <p>Adaptations:
 * <ul>
 *   <li><b>block-syntax</b>: variables are declared with types or {@code var}, statements end with {@code ;}, and blocks return explicitly with {@code return}.</li>
 *   <li><b>single-quote-string</b>: single-quoted string literals are changed to double quotes {@code "..."}.</li>
 *   <li><b>empty-operator</b>: {@code == empty} is rewritten using {@code .isEmpty()}, null checks, or zero/blank checks.</li>
 *   <li><b>is-operator</b>: {@code is} is rewritten using {@code instanceof}.</li>
 *   <li><b>isdef-operator</b>: {@code isdef} is rewritten using map {@code containsKey}.</li>
 *   <li><b>contains-operator / reserved-method-name</b>: {@code list contains x} / {@code list.contains(x)} is rewritten using {@code indexOf(x) >= 0}.</li>
 *   <li><b>this-reference</b>: methods and properties of the root context are accessed via named variables (e.g. {@code base.equalityCheck(..)}).</li>
 *   <li><b>post-increment-expression</b>: post-increment in expressions is decoupled from the boolean comparison.</li>
 * </ul>
 */
public class CoreOperatorsAdaptedTest {

  public static class BeanB {
    private boolean successful;

    public BeanB(boolean successful) {
      this.successful = successful;
    }

    public boolean isSuccessful() {
      return successful;
    }

    public boolean isFailed() {
      return !successful;
    }
  }

  public static class Triangle {
    private String strLabel = "Triangle";

    public String getStrLabel() {
      return strLabel;
    }

    public void setStrLabel(String strLabel) {
      this.strLabel = strLabel;
    }
  }

  @Test
  public void testBooleanModeOnly4() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapExpression(
        "hour == (hour + 0)", Boolean.class, Set.of(), Map.of("hour", Type.type(Integer.class)));
    assertThat(eval.eval(Map.of("hour", 60))).isTrue();
  }

  @Test
  public void testEvalToBoolean() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapExpression(
        "true", Boolean.class, Set.of(), Map.of());
    assertThat(eval.eval(Map.of())).isTrue();
  }

  @Test
  public void testSubtractNoSpace1() {
    Evaluator<Map<String, Object>, Void, Integer> eval = new MVEL().compileMapExpression(
        "hour - 1", Integer.class, Set.of(), Map.of("hour", Type.type(Integer.class)));
    assertThat(eval.eval(Map.of("hour", 60))).isEqualTo(59);
  }

  @Test
  public void testEqualityRegression() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapExpression(
        "price == (5 + 5)", Boolean.class, Set.of(), Map.of("price", Type.type(Integer.class)));
    assertThat(eval.eval(Map.of("price", 10))).isTrue();
  }

  @Test
  public void testAssignmentRegression() {
    Evaluator<Map<String, Object>, Void, Integer> eval = new MVEL().compileMapBlock(
        "var total = 0; total = total + cheese.price; return total;",
        Integer.class, Set.of(), Map.of("cheese", Type.type(Cheese.class)));
    Cheese cheese = new Cheese("cheddar", 25);
    assertThat(eval.eval(Map.of("cheese", cheese))).isEqualTo(25);
  }

  @Test
  public void testPrecedenceOrder() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapExpression(
        "5 > 6 && 2 < 1 || 10 > 9", Boolean.class, Set.of(), Map.of());
    assertThat(eval.eval(Map.of())).isTrue();
  }

  @Test
  public void testRandomExpression1() {
    Evaluator<Map<String, Object>, Void, String> eval = new MVEL().compileMapBlock(
        "var x15 = foo.bar; if (x15 == foo.bar && x15 == foo.bar) { return \"HelloWorld\"; } else { return \"GoodbyeWorld\"; }",
        String.class, Set.of(), Map.of("foo", Type.type(Foo.class)));
    assertThat(eval.eval(Map.of("foo", new Foo()))).isEqualTo("HelloWorld");
  }

  @Test
  public void testRandomExpression9() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapBlock(
        "var _abc = \"someString\".hashCode(); var _xyz = _abc + 1; return _abc == _xyz;",
        Boolean.class, Set.of(), Map.of());
    assertThat(eval.eval(Map.of())).isFalse();
  }

  @Test
  public void testRandomExpression10() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapBlock(
        "var _xyz = \"someString\".hashCode(); var _abc = _xyz + 1; return _abc == _xyz;",
        Boolean.class, Set.of(), Map.of());
    assertThat(eval.eval(Map.of())).isFalse();
  }

  @Test
  public void testBooleanEvaluation() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapExpression(
        "true || false || false", Boolean.class, Set.of(), Map.of());
    assertThat(eval.eval(Map.of())).isTrue();
  }

  @Test
  public void testBooleanEvaluation2() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapExpression(
        "base.equalityCheck(1, 1) || fun || ackbar", Boolean.class, Set.of(),
        Map.of("base", Type.type(Base.class), "fun", Type.type(Boolean.class), "ackbar", Type.type(Boolean.class)));
    assertThat(eval.eval(Map.of("base", new Base(), "fun", false, "ackbar", false))).isTrue();
  }

  @Test
  public void testSimpleExpression() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapExpression(
        "pojo.value != null", Boolean.class, Set.of(), Map.of("pojo", Type.type(PojoStatic.class)));
    assertThat(eval.eval(Map.of("pojo", new PojoStatic("10")))).isTrue();
  }

  @Test
  public void testIncrementInBooleanStatement() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapBlock(
        "var old = hour; hour = hour + 1; return old < 61 && hour == 61;",
        Boolean.class, Set.of(), Map.of("hour", Type.type(Integer.class)));
    Map<String, Object> vars = new HashMap<>();
    vars.put("hour", 60);
    assertThat(eval.eval(vars)).isTrue();
  }

  @Test
  public void testIncrementInBooleanStatement2() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapBlock(
        "hour = hour + 1; return hour == 61;",
        Boolean.class, Set.of(), Map.of("hour", Type.type(Integer.class)));
    Map<String, Object> vars = new HashMap<>();
    vars.put("hour", 60);
    assertThat(eval.eval(vars)).isTrue();
  }

  @Test
  public void testIsUsedInIf() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapBlock(
        "var c = \"str\"; if (c instanceof String) { return true; } else { return false; }",
        Boolean.class, Set.of(), Map.of());
    assertThat(eval.eval(Map.of())).isTrue();
  }

  @Test
  public void testOperatorPrecedenceOrder() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapExpression(
        "bean1.successful && bean2.failed || bean1.failed && bean2.successful",
        Boolean.class, Set.of(),
        Map.of("bean1", Type.type(BeanB.class), "bean2", Type.type(BeanB.class)));
    BeanB bean1 = new BeanB(true);
    BeanB bean2 = new BeanB(false);
    assertThat(eval.eval(Map.of("bean1", bean1, "bean2", bean2))).isTrue();
  }

  @Test
  public void testEmpty() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapBlock(
        "var list = new java.util.ArrayList(); return list.isEmpty();",
        Boolean.class, Set.of(), Map.of());
    assertThat(eval.eval(Map.of())).isTrue();
  }

  @Test
  public void testReturnBoolean() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapBlock(
        "var list = new java.util.ArrayList(); return list != null;",
        Boolean.class, Set.of(), Map.of());
    assertThat(eval.eval(Map.of())).isTrue();
  }

  @Test
  public void testUnaryOpNegation1() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapExpression(
        "!Boolean.valueOf(true)", Boolean.class, Set.of(), Map.of());
    assertThat(eval.eval(Map.of())).isFalse();
  }

  @Test
  public void testUnaryOpNegation2() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapBlock(
        "return !vars.containsKey(\"_foozy_\");",
        Boolean.class, Set.of(), Map.of("vars", Type.type(Map.class)));
    assertThat(eval.eval(Map.of("vars", new HashMap<>()))).isTrue();
  }

  @Test
  public void testJIRA153() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapExpression(
        "!(true)", Boolean.class, Set.of(), Map.of());
    assertThat(eval.eval(Map.of())).isFalse();
  }

  @Test
  public void testMultipleNegations() {
    Evaluator<Map<String, Object>, Void, Boolean> eval1 = new MVEL().compileMapExpression("!false", Boolean.class, Set.of(), Map.of());
    Evaluator<Map<String, Object>, Void, Boolean> eval2 = new MVEL().compileMapExpression("!!false", Boolean.class, Set.of(), Map.of());
    Evaluator<Map<String, Object>, Void, Boolean> eval3 = new MVEL().compileMapExpression("!!!false", Boolean.class, Set.of(), Map.of());
    Evaluator<Map<String, Object>, Void, Boolean> eval4 = new MVEL().compileMapExpression("!!!!false", Boolean.class, Set.of(), Map.of());
    assertThat(eval1.eval(Map.of())).isTrue();
    assertThat(eval2.eval(Map.of())).isFalse();
    assertThat(eval3.eval(Map.of())).isTrue();
    assertThat(eval4.eval(Map.of())).isFalse();
  }

  @Test
  public void testOperatorPrecedence() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapBlock(
        "var list = [1, 2, 3]; var x = 10; return list.indexOf(x) >= 0 || x == 20;",
        Boolean.class, Set.of(), Map.of());
    assertThat(eval.eval(Map.of())).isFalse();
  }

  @Test
  public void testStrTriangleEqualsEquals() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapExpression(
        "t.strLabel == t", Boolean.class, Set.of(),
        Map.of("t", Type.type(Triangle.class)));
    Triangle t = new Triangle();
    assertThat(eval.eval(Map.of("t", t))).isFalse();
  }

  @Test
  public void testShiftOperator() {
    Evaluator<Map<String, Object>, Void, Long> eval1 = new MVEL().compileMapExpression(
        "1 << 65536L", Long.class, Set.of(), Map.of());
    assertThat(eval1.eval(Map.of())).isEqualTo(1L << 65536L);

    Evaluator<Map<String, Object>, Void, Long> eval2 = new MVEL().compileMapExpression(
        "one << shift", Long.class, Set.of(),
        Map.of("one", Type.type(Integer.class), "shift", Type.type(Long.class)));
    assertThat(eval2.eval(Map.of("one", 1, "shift", 65536L))).isEqualTo(1L << 65536L);
  }

  @Test
  public void testMinusOperatorWithoutSpace() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapExpression(
        "col1.length == col2.length - 1", Boolean.class, Set.of(),
        Map.of("col1", Type.type(Column.class), "col2", Type.type(Column.class)));
    Column col1 = new Column("x", 0);
    Column col2 = new Column("x", 0);
    assertThat(eval.eval(Map.of("col1", col1, "col2", col2))).isFalse();
  }

  @Test
  public void testUnaryNegative() {
    Evaluator<Map<String, Object>, Void, Integer> eval = new MVEL().compileMapExpression(
        "-value", Integer.class, Set.of(), Map.of("value", Type.type(int.class)));
    assertThat(eval.eval(Map.of("value", 42))).isEqualTo(-42);
  }

  @Test
  public void testUnaryNegativeWithSpace() {
    Evaluator<Map<String, Object>, Void, Integer> eval = new MVEL().compileMapExpression(
        "- value", Integer.class, Set.of(), Map.of("value", Type.type(int.class)));
    assertThat(eval.eval(Map.of("value", 42))).isEqualTo(-42);
  }

  @Test
  public void testEmptyOperatorOnStrings() {
    Evaluator<Map<String, Object>, Void, Boolean> evalNull = new MVEL().compileMapExpression(
        "nullString == null || nullString.trim().isEmpty()", Boolean.class, Set.of(),
        Map.of("nullString", Type.type(String.class)));
    Evaluator<Map<String, Object>, Void, Boolean> evalEmpty = new MVEL().compileMapExpression(
        "emptyString == null || emptyString.trim().isEmpty()", Boolean.class, Set.of(),
        Map.of("emptyString", Type.type(String.class)));
    Evaluator<Map<String, Object>, Void, Boolean> evalBlank = new MVEL().compileMapExpression(
        "blankString == null || blankString.trim().isEmpty()", Boolean.class, Set.of(),
        Map.of("blankString", Type.type(String.class)));
    Evaluator<Map<String, Object>, Void, Boolean> evalNonEmpty = new MVEL().compileMapExpression(
        "nonEmptyString == null || nonEmptyString.trim().isEmpty()", Boolean.class, Set.of(),
        Map.of("nonEmptyString", Type.type(String.class)));

    Map<String, Object> vars = new HashMap<>();
    vars.put("nullString", null);
    vars.put("emptyString", "");
    vars.put("blankString", "   ");
    vars.put("nonEmptyString", "abc");

    assertThat(evalNull.eval(vars)).isTrue();
    assertThat(evalEmpty.eval(vars)).isTrue();
    assertThat(evalBlank.eval(vars)).isTrue();
    assertThat(evalNonEmpty.eval(vars)).isFalse();
  }

  @Test
  public void testEmptyOperatorOnList() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapExpression(
        "list == null || list.isEmpty()", Boolean.class, Set.of(),
        Map.of("list", Type.type(List.class)));
    assertThat(eval.eval(Map.of("list", new ArrayList<>()))).isTrue();
  }

  @Test
  public void testEmptyOperatorOnMap() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapExpression(
        "map == null || map.isEmpty()", Boolean.class, Set.of(),
        Map.of("map", Type.type(Map.class)));
    assertThat(eval.eval(Map.of("map", new HashMap<>()))).isTrue();
  }

  @Test
  public void testEmptyOperatorOnBoolean() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapExpression(
        "b == null || !b", Boolean.class, Set.of(),
        Map.of("b", Type.type(Boolean.class)));

    Map<String, Object> varsNull = new HashMap<>();
    varsNull.put("b", null);
    assertThat(eval.eval(varsNull)).isTrue();
    assertThat(eval.eval(Map.of("b", true))).isFalse();
    assertThat(eval.eval(Map.of("b", false))).isTrue();
  }

  @Test
  public void testEmptyOperatorOnInteger() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapExpression(
        "i == null || i == 0", Boolean.class, Set.of(),
        Map.of("i", Type.type(Integer.class)));

    Map<String, Object> varsNull = new HashMap<>();
    varsNull.put("i", null);
    assertThat(eval.eval(varsNull)).isTrue();
    assertThat(eval.eval(Map.of("i", 0))).isTrue();
    assertThat(eval.eval(Map.of("i", 42))).isFalse();
  }

  @Test
  public void testUseVariableFactoryWithArithmeticOperation() {
    checkOperation("3 + 4 * i.get()", 43);
    checkOperation("2 * 3 + 4 * i.get()", 46);
    checkOperation("1 + 2 * 3 + i.get()", 17);
    checkOperation("2 * 3 + 4 * i.get()", 46);
    checkOperation("1 + 2 * 3 + 4 * i.get()", 47);
    checkOperation("1 + 2 * 3 + i.get() * 4", 47);
    checkOperation("1 + 2 * 3 + i.get() + 4", 21);
    checkOperation("4 * i.get() + 5", 45);
    checkOperation("3 + 4 * i.get() + 5", 48);
    checkOperation("2 * 3 + 4 * i.get() + 5", 51);
    checkOperation("1 + 2 * 3 + 4 * i.get() + 5", 52);
    checkOperation("1 + 2 * 3 + 4 * i.get() * 5", 207);
    checkOperation("i.get() + 1 + 2 * 3 + 4 * i.get()", 57);
  }

  private void checkOperation(String expression, int expectedResult) {
    Evaluator<Map<String, Object>, Void, Integer> eval = new MVEL().compileMapExpression(
        expression, Integer.class, Set.of(), Map.of("i", Type.type(AtomicInteger.class)));
    AtomicInteger i = new AtomicInteger(10);
    assertThat(eval.eval(Map.of("i", i))).isEqualTo(expectedResult);
  }
}
