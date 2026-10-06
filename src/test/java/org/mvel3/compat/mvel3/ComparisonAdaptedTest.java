/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Borrowed from MVEL, under the ASL2.0 license.
 * 
 */

package org.mvel3.compat.mvel3;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mvel3.Base;
import org.mvel3.Evaluator;
import org.mvel3.Foo;
import org.mvel3.MVEL;
import org.mvel3.MiscTestClass;
import org.mvel3.Order;
import org.mvel3.TestInterface;
import org.mvel3.Type;
import org.mvel3.transpiler.context.Declaration;

import static java.lang.System.currentTimeMillis;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Porting of ComparisonTests from MVEL2 adapted for MVEL3 syntax and features.
 */
public class ComparisonAdaptedTest {

  public static Set<String> getImports() {
    Set<String> imports = new HashSet<>();
    imports.add("java.util.List");
    imports.add("java.util.ArrayList");
    imports.add("java.util.HashMap");
    imports.add("java.util.Map");
    imports.add("java.math.BigDecimal");
    imports.add(Foo.class.getCanonicalName());
    imports.add(Base.class.getCanonicalName());
    return imports;
  }

  public static Object executeExpression(final String expression, final Map<String, Object> vars) {
    MVEL mvel = new MVEL();
    return mvel.executeExpression(expression, getImports(), vars);
  }

  public static Object eval(final String expression, final Object root) {
    Evaluator<Object, Void, Object> runtime = MVEL.pojo(root.getClass()).out(Type.OBJECT).expression(expression).imports(getImports()).compile();
    return runtime.eval(root);
  }

  public static Object eval(final String expression) {
    return executeExpressionWithDefaultVariables(expression);
  }

  public static Object test(final String expression) {
    return executeExpressionWithDefaultVariables(expression);
  }

  public static Object _test(final String expression) {
    return executeExpressionWithDefaultVariables(expression);
  }

  public static Object executeExpressionWithDefaultVariables(final String expression) {
    MVEL mvel = new MVEL();
    if (expression.indexOf(';') > 0) {
      Map<String, Object> vars = createTestMap();
      Map<String, Type<?>> types = MVEL.getTypeMap(vars);
      Declaration[] declarations = types.entrySet().stream()
          .map(entry -> new Declaration(entry.getKey(), entry.getValue()))
          .collect(Collectors.toList())
          .toArray(new Declaration[0]);
      Evaluator<Map<String, Object>, Void, Object> runtime = MVEL.map(declarations).out(Type.OBJECT).block(expression).imports(getImports()).compile();
      return runtime.eval(vars);
    } else {
      return mvel.executeExpression(expression, getImports(), createTestMap());
    }
  }

  protected static Map<String, Object> createTestMap() {
    Map<String, Object> map = new HashMap<>();
    map.put("foo", new Foo());
    map.put("a", null);
    map.put("b", null);
    map.put("c", "cat");
    map.put("BWAH", "");

    map.put("misc", new MiscTestClass());

    map.put("pi", "3.14");
    map.put("hour", 60);
    map.put("zero", 0);

    map.put("array", new String[]{"", "blip"});

    map.put("order", new Order());
    map.put("$id", 20);

    map.put("five", 5);

    map.put("testImpl",
        new TestInterface() {
          public String getName() {
            return "FOOBAR!";
          }

          public boolean isFoo() {
            return true;
          }
        });

    map.put("ipaddr", "10.1.1.2");

    map.put("dt1", new Date(currentTimeMillis() - 100000));
    map.put("dt2", new Date(currentTimeMillis()));

    map.put("list", new ArrayList<String>() {{
      add("Happy!");
    }});
    map.put("sentence", "The quick brown fox jumps over the lazy dog");

    Base base = new Base();
    map.put("fun", base.fun);
    map.put("funMap", base.funMap);
    map.put("fooMap", base.fooMap);
    map.put("data", base.data);
    map.put("things", base.things);
    map.put("this", base);

    return map;
  }

  @Test
  public void testBooleanOperator() {
    assertEquals(true, test("foo.bar.woof == true"));
  }

  @Test
  public void testBooleanOperator2() {
    assertEquals(false, test("foo.bar.woof == false"));
  }

  @Test
  public void testBooleanOperator3() {
    assertEquals(true, test("foo.bar.woof== true"));
  }

  @Test
  public void testBooleanOperator4() {
    assertEquals(false, test("foo.bar.woof ==false"));
  }

  @Test
  public void testBooleanOperator5() {
    assertEquals(true, test("foo.bar.woof == true"));
  }

  @Test
  public void testBooleanOperator6() {
    assertEquals(false, test("foo.bar.woof==false"));
  }

  @Test
  public void testTextComparison() {
    // MVEL3: Double quotes for string literals
    assertEquals(true, test("foo.bar.name == \"dog\""));
  }

  @Test
  public void testNETextComparison() {
    // MVEL3: Double quotes for string literals
    assertEquals(true, test("foo.bar.name != \"foo\""));
  }

  @Test
  public void testChor() {
    // MVEL3: Chained null-coalescing using ternary operator
    assertEquals("cat", test("a != null ? a : (b != null ? b : c)"));
  }

  @Test
  @Disabled("Mvel3 doesn't support 'or' as null-coalescing operator")
  public void testChor_OriginalSyntax() {
    assertEquals("cat", test("a or b or c"));
  }

  @Test
  public void testChorWithLiteral() {
    // MVEL3: Null-coalescing with ternary and double-quote string
    assertEquals("fubar", test("a != null ? a : \"fubar\""));
  }

  @Test
  public void testNullCompare() {
    assertEquals(true, test("c != null"));
  }

  @Test
  public void testLessThan() {
    // MVEL3: Explicit type coercion hint #double#
    assertEquals(true, test("pi#double# < 3.15"));
    assertEquals(true, test("pi#double# <= 3.14"));
    assertEquals(false, test("pi#double# > 3.14"));
    assertEquals(true, test("pi#double# >= 3.14"));
  }

  @Test
  public void testNegation() {
    String ex = "!fun && !fun";
    assertEquals(true, test(ex));
  }

  @Test
  public void testNegation2() {
    assertEquals(false, test("fun && !fun"));
  }

  @Test
  public void testNegation3() {
    assertEquals(true, test("!(fun && fun)"));
  }

  @Test
  public void testNegation4() {
    assertEquals(false, test("(fun && fun)"));
  }

  @Test
  public void testNegation5() {
    assertEquals(true, test("!false"));
  }

  @Test
  public void testNegation6() {
    assertEquals(false, test("!true"));
  }

  @Test
  public void testNegation7() {
    // MVEL3 block syntax: var declarations and return
    assertEquals(true, test("var s = false; var t = !s; return t;"));
  }

  @Test
  public void testNegation8() {
    // MVEL3 block syntax: var declarations and return
    assertEquals(true, test("var s = false; var t = !s; return t;"));
  }

  @Test
  @Disabled("Mvel3 doesn't support regex operator ~=")
  public void testRegEx() {
    assertEquals(true, test("foo.bar.name ~= '[a-z].+'"));
  }

  @Test
  @Disabled("Mvel3 doesn't support regex operator ~=")
  public void testRegExNegate() {
    assertEquals(false, test("!(foo.bar.name ~= '[a-z].+')"));
  }

  @Test
  @Disabled("Mvel3 doesn't support regex operator ~=")
  public void testRegEx2() {
    assertEquals(true, test("foo.bar.name ~= '[a-z].+' && foo.bar.name != null"));
  }

  @Test
  @Disabled("Mvel3 doesn't support regex operator ~=")
  public void testRegEx3() {
    assertEquals(true, test("foo.bar.name~='[a-z].+'&&foo.bar.name!=null"));
  }

  @Test
  @Disabled("Mvel3 doesn't support 'empty' keyword")
  public void testBlank() {
    assertEquals(true, test("'' == empty"));
  }

  @Test
  @Disabled("Mvel3 doesn't support 'empty' keyword")
  public void testBlank2() {
    assertEquals(true, test("BWAH == empty"));
  }

  @Test
  @Disabled("Mvel3 doesn't support 'empty' keyword")
  public void testBlank3() {
    assertEquals(true, _test("[] == empty"));
  }

  @Test
  @Disabled("Mvel3 doesn't support 'empty' keyword")
  public void testBlank5() {
    assertEquals(true, _test("['a'] != empty"));
  }

  @Test
  @Disabled("Mvel3 doesn't support 'empty' keyword")
  public void testBlank6() {
    assertEquals(true, _test("empty != ['a']"));
  }

  @Test
  @Disabled("Mvel3 doesn't support 'empty' keyword")
  public void testBlank7() {
    assertEquals(false, _test("[] != empty"));
  }

  @Test
  @Disabled("Mvel3 doesn't support 'empty' keyword")
  public void testBlank9() {
    assertEquals(false, _test("['a'] == empty"));
  }

  @Test
  @Disabled("Mvel3 doesn't support 'empty' keyword")
  public void testBlank10() {
    assertEquals(false, _test("empty == ['a']"));
  }

  @Test
  public void testInstanceCheck1() {
    // MVEL3: Use standard instanceof operator
    assertEquals(true, test("c instanceof java.lang.String"));
  }

  @Test
  @Disabled("Mvel3 adheres to Java semantics; String cannot be instanceof Integer at compile time")
  public void testInstanceCheck2() {
    assertEquals(false, test("pi instanceof java.lang.Integer"));
  }

  @Test
  public void testInstanceCheck3() {
    // MVEL3: Use standard instanceof operator
    assertEquals(true, test("foo instanceof org.mvel3.Foo"));
  }

  @Test
  @Disabled("Mvel3 doesn't support infix 'contains' operator")
  public void testContains1() {
    assertEquals(true, test("list contains 'Happy!'"));
  }

  @Test
  @Disabled("Mvel3 doesn't support infix 'contains' operator")
  public void testContains2() {
    assertEquals(false, test("list contains 'Foobie'"));
  }

  @Test
  @Disabled("Mvel3 doesn't support infix 'contains' operator")
  public void testContains3() {
    assertEquals(true, test("sentence contains 'fox'"));
  }

  @Test
  @Disabled("Mvel3 doesn't support infix 'contains' operator")
  public void testContains4() {
    assertEquals(false, test("sentence contains 'mike'"));
  }

  @Test
  @Disabled("Mvel3 doesn't support infix 'contains' operator")
  public void testContains5() {
    assertEquals(true, test("!(sentence contains 'mike')"));
  }

  @Test
  @Disabled("Mvel3 doesn't support infix 'contains' operator")
  public void testContains6() {
    assertEquals(true, test("bwahbwah = 'mikebrock'; testVar10 = 'mike'; bwahbwah contains testVar10"));
  }

  @Test
  @Disabled("Mvel3 doesn't support infix 'contains' operator")
  public void testContains7() {
    assertEquals(true, test("sentence contains ('fox')"));
  }

  @Test
  @Disabled("Mvel3 doesn't support soundslike operator")
  public void testSoundex() {
    assertEquals(true, test("'foobar' soundslike 'fubar'"));
  }

  @Test
  @Disabled("Mvel3 doesn't support soundslike operator")
  public void testSoundex2() {
    assertEquals(false, test("'flexbar' soundslike 'fubar'"));
  }

  @Test
  @Disabled("Mvel3 doesn't support soundslike operator")
  public void testSoundex3() {
    assertEquals(true, test("(c soundslike 'kat')"));
  }

  @Test
  @Disabled("Mvel3 doesn't support strsim operator")
  public void testSimilarity1() {
    assertEquals(0.6666667f, test("c strsim 'kat'"));
  }

  @Test
  @Disabled("Mvel3 doesn't support soundslike operator")
  public void testSoundex4() {
    assertEquals(true, test("_xx1 = 'cat'; _xx2 = 'katt'; (_xx1 soundslike _xx2)"));
  }

  @Test
  @Disabled("Mvel3 doesn't support soundslike operator")
  public void testSoundex5() {
    assertEquals(true, test("_type = 'fubar';_type soundslike \"foobar\""));
  }

  @Test
  @Disabled("In MVEL3 'this' refers to Evaluator instance, not root context")
  public void testThisReference3() {
    assertEquals(true, test("this is org.mvel3.compat.mvel2.res.Base"));
  }

  @Test
  public void testThisReference4() {
    // MVEL3: Access property directly
    assertEquals(true, test("funMap instanceof java.util.Map"));
  }

  @Test
  public void testThisReference5() {
    // MVEL3: Access property directly with double quotes
    assertEquals(true, test("data == \"cat\""));
  }

  @Test
  public void testDateComparison() {
    // MVEL3: Use Date before() or getTime() comparison
    assertEquals(true, test("dt1.before(dt2)"));
  }

  @Test
  @Disabled("Mvel3 doesn't support convertable_to operator")
  public void testConvertableTo() {
    assertEquals(true, test("pi convertable_to Integer"));
  }

  @Test
  public void testStringEquals() {
    // MVEL3: Double quotes for string literals
    assertEquals(true, test("ipaddr == \"10.1.1.2\""));
  }

  @Test
  public void testCharComparison() {
    assertEquals(true, test("'z' > 'a'"));
  }

  @Test
  public void testCharComparison2() {
    assertEquals(false, test("'z' < 'a'"));
  }

  @Test
  public void testJIRA100b() {
    // MVEL3: Expression without trailing semicolon
    String expression = "(8 / 10) * 100 <= 80";
    assertEquals((8 / 10) * 100 <= 80, test(expression));
  }

  @Test
  @Disabled("Java doesn't support > operator on null")
  public void testJIRA92() {
    assertEquals(false, test("\"stringValue\" > null"));
  }

  @Test
  @Disabled("Mvel3 doesn't support isdef operator")
  public void testIsDefOperator() {
    assertEquals(true, test("_v1 = 'bar'; isdef _v1"));
  }

  @Test
  @Disabled("Mvel3 doesn't support isdef operator")
  public void testIsDefOperator2() {
    assertEquals(false, test("isdef _v1"));
  }

  @Test
  @Disabled("Mvel3 doesn't support isdef operator")
  public void testIsDefOperator3() {
    assertEquals(true, test("!(isdef _v1)"));
  }

  @Test
  @Disabled("Mvel3 doesn't support isdef operator")
  public void testIsDefOperator4() {
    assertEquals(true, test("! (isdef _v1)"));
  }

  @Test
  @Disabled("Mvel3 doesn't support isdef operator")
  public void testIsDefOperator5() {
    assertEquals(true, test("!isdef _v1"));
  }

  @Test
  @Disabled("Mvel3 doesn't support isdef operator")
  public void testIsDefOperator6() {
    Foo foo = new Foo();
    assertEquals(true, eval("isdef name", foo));
  }

  @Test
  public void testJIRA152() {
    assertEquals(true, eval("1== -(-1)"));
  }

  @Test
  public void testJIRA157() {
    assertEquals(true, eval("1 == ((byte) 1)"));
  }

  @Test
  public void testJIRA181() {
    assertEquals(false, eval("0<-1"));
  }

  @Test
  public void testStringCoercionForComparison() {
    assertEquals(false, eval("36 > 242"));
    assertEquals(false, eval("\"36\"#int# > 242"));
    assertEquals(false, eval("36 > \"242\"#int#"));
    assertEquals(true, eval("\"36\".compareTo(\"242\") > 0"));
  }
}
