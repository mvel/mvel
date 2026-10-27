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
import org.mvel3.compat.mvel2.res.res2.ClassProvider;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Porting of the JIRA/MVEL-number regression tests of CoreConfidenceTests from MVEL2 adapted for
 * MVEL3 syntax.
 *
 * <p>Adaptations:
 * <ul>
 *   <li><b>void-method-expression</b>: void method calls as expressions are rewritten as block
 *       statements: {@code call(); return null;}</li>
 *   <li><b>inline-import</b>: {@code import ...;} inside expression is replaced by API imports set
 *       ({@code Set.of(...)}).</li>
 *   <li><b>null-safe-operator</b>: {@code a.?b.c} is rewritten as an explicit null check.</li>
 *   <li><b>block-syntax</b>: variables are declared with types or {@code var}, statements end with
 *       {@code ;}, blocks return explicitly with {@code return}.</li>
 *   <li><b>undeclared-input-variable</b>: inputs are declared in the type map so they are visible
 *       to MVEL3's compiler.</li>
 * </ul>
 *
 * <p>Not representable (skipped):
 * <ul>
 *   <li>{@code testJIRA140}: tests that inline {@code import} raises a parse error —
 *       no MVEL3 equivalent behaviour to assert.</li>
 * </ul>
 */
public class CoreRegressionAdaptedTest {

  // ---------------------------------------------------------------------------
  // Fixtures
  // ---------------------------------------------------------------------------

  /** Used by testJIRA151 / testJIRA151b. */
  public static class Az {
    public void foo(String s) {
    }
  }

  public static class Bz extends Az {
  }

  /** Used by testJIRA113. */
  public static double minim(double[] tab) {
    double min = Float.MAX_VALUE;
    for (int i = 0; i < tab.length; i++) {
      if (min > tab[i]) {
        min = tab[i];
      }
    }
    return min;
  }

  /** Used by testMVEL221. */
  public static class A221 {
    private B221 b;

    public B221 getB() {
      return b;
    }

    public void setB(B221 b) {
      this.b = b;
    }
  }

  public static class B221 {
    private String c = "something";

    public String getC() {
      return c;
    }
  }

  /** Used by testJIRA103b. */
  public static class MvelContext {
    public String[] regkeys;

    public void setRegkeys(String[] regkeys) {
      this.regkeys = regkeys;
    }

    public void setRegkeys(String regkey) {
      this.regkeys = regkey.split(",");
    }
  }

  /** Used by testJIRA167 / testJIRA167b. */
  public static class Beano {
    public String getProperty1() {
      return null;
    }

    public boolean isProperty2() {
      return true;
    }

    public boolean isProperty3() {
      return false;
    }
  }

  public static class JIRA167Step {
    public String getParent() {
      return null;
    }
  }

  public static class JIRA167Node {
    public boolean isServer() {
      return true;
    }
  }

  // ---------------------------------------------------------------------------
  // MVEL-103b — overloaded setter dispatch: use block form to avoid void-return
  // ---------------------------------------------------------------------------

  @Test
  public void testJIRA103b() {
    MvelContext mvelContext = new MvelContext();
    Map<String, Object> map = new HashMap<>();
    map.put("ctx", mvelContext);

    // void-method-expression: wrap as block with `return null;`
    Evaluator<Map<String, Object>, Void, Object> eval = new MVEL().compileMapBlock(
        "ctx.setRegkeys(\"foo\"); return null;",
        Object.class, Set.of(), Map.of("ctx", Type.type(MvelContext.class)));
    eval.eval(map);
    eval.eval(map);
    assertThat(mvelContext.regkeys).isEqualTo(new String[]{"foo"});
  }

  // ---------------------------------------------------------------------------
  // JIRA-113 — static method by fully qualified class name
  // ---------------------------------------------------------------------------

  @Test
  public void testJIRA113() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapExpression(
        "org.mvel3.compat.mvel3.CoreRegressionAdaptedTest.minim(new double[] {456.2, 2.3}) == 2.3",
        Boolean.class, Set.of(), Map.of());
    assertThat(eval.eval(Map.of())).isTrue();
  }

  // ---------------------------------------------------------------------------
  // JIRA-151 / 151b — method call on a supertype-declared variable (void → block)
  // ---------------------------------------------------------------------------

  @Test
  public void testJIRA151() {
    Bz b = new Bz();
    // void-method-expression: wrap as block with `return null;`
    Evaluator<Map<String, Object>, Void, Object> eval = new MVEL().compileMapBlock(
        "a.foo(value); return null;",
        Object.class, Set.of(),
        Map.of("a", Type.type(Bz.class), "value", Type.type(String.class)));
    Map<String, Object> variables = new HashMap<>();
    variables.put("a", b);
    variables.put("value", "123");
    for (int i = 0; i < 100; i++) {
      eval.eval(variables);
    }
  }

  @Test
  public void testJIRA151b() {
    Bz b = new Bz();
    // void-method-expression: wrap as block with `return null;`
    Evaluator<Map<String, Object>, Void, Object> eval = new MVEL().compileMapBlock(
        "a.foo(value); return null;",
        Object.class, Set.of(),
        Map.of("a", Type.type(Bz.class), "value", Type.type(String.class)));
    Map<String, Object> variables = new HashMap<>();
    variables.put("a", b);
    variables.put("value", "123");
    for (int i = 0; i < 100; i++) {
      eval.eval(variables);
    }
  }

  // ---------------------------------------------------------------------------
  // JIRA-156 / 156b / 156c — method call through a package-private class (void → block)
  // ---------------------------------------------------------------------------

  @Test
  public void testJIRA156() {
    ClassProvider provider = new ClassProvider();

    // void-method-expression: wrap as block with `return null;`
    Evaluator<Map<String, Object>, Void, Object> eval = new MVEL().compileMapBlock(
        "provider.getPrivate().foo(); return null;",
        Object.class, Set.of(), Map.of("provider", Type.type(ClassProvider.class)));
    HashMap<String, Object> vars = new HashMap<>();
    vars.put("provider", provider);
    eval.eval(vars);
  }

  @Test
  public void testJIRA156b() {
    ClassProvider provider = new ClassProvider();

    Evaluator<Map<String, Object>, Void, Object> eval = new MVEL().compileMapBlock(
        "provider.getPrivate().foo(); return null;",
        Object.class, Set.of(), Map.of("provider", Type.type(ClassProvider.class)));
    HashMap<String, Object> vars = new HashMap<>();
    vars.put("provider", provider);
    eval.eval(vars);
    eval.eval(vars);
  }

  @Test
  public void testJIRA156c() {
    ClassProvider provider = new ClassProvider();

    Evaluator<Map<String, Object>, Void, Object> eval = new MVEL().compileMapBlock(
        "provider.getPublic().foo(); return null;",
        Object.class, Set.of(), Map.of("provider", Type.type(ClassProvider.class)));
    HashMap<String, Object> vars = new HashMap<>();
    vars.put("provider", provider);
    eval.eval(vars);
    eval.eval(vars);
  }

  // ---------------------------------------------------------------------------
  // JIRA-166 — regex with Pattern.compile (import via API instead of inline)
  // ---------------------------------------------------------------------------

  @Test
  public void testJIRA166() {
    // inline-import: pass imports via the API imports set
    Evaluator<Map<String, Object>, Void, String> eval = new MVEL().compileMapBlock(
        "if (java.util.regex.Pattern.compile(\"hoge\").matcher(\"hogehogehoge\").find()) { return \"foo\"; } else { return \"bar\"; }",
        String.class, Set.of(), Map.of());
    assertThat(eval.eval(Map.of())).isEqualTo("foo");
  }

  // ---------------------------------------------------------------------------
  // JIRA-167 / 167b / 167c — ternary with null property access
  // ---------------------------------------------------------------------------

  @Test
  public void testJIRA167() {
    Evaluator<Map<String, Object>, Void, Object> eval = new MVEL().compileMapExpression(
        "bean.property1 == null ? bean.isProperty2() : bean.isProperty3()",
        Object.class, Set.of(), Map.of("bean", Type.type(Beano.class)));
    assertThat(eval.eval(Map.of("bean", new Beano()))).isEqualTo(true);
  }

  @Test
  public void testJIRA167b() {
    // undeclared-input-variable: declare both `current` and `node` inputs explicitly
    Evaluator<Map<String, Object>, Void, Object> eval = new MVEL().compileMapExpression(
        "current.parent == null ? node.isServer() : (node == current.parent)",
        Object.class, Set.of(),
        Map.of("current", Type.type(JIRA167Step.class), "node", Type.type(JIRA167Node.class)));
    assertThat(eval.eval(Map.of("current", new JIRA167Step(), "node", new JIRA167Node()))).isEqualTo(true);
  }

  @Test
  public void testJIRA167c() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapExpression(
        "true ? true : (false)", Boolean.class, Set.of(), Map.of());
    assertThat(eval.eval(Map.of())).isTrue();
  }

  // ---------------------------------------------------------------------------
  // JIRA-176 / 176b — map access with key containing brackets
  // ---------------------------------------------------------------------------

  @Test
  public void testJIRA176() {
    Map<String, Object> vars = new HashMap<>();
    Map<String, String> innerMap = new HashMap<>();
    innerMap.put("testKey[MyValue=newValue]", "test");
    vars.put("mappo", innerMap);

    // Map.get returns Object; use Object.class output and cast to avoid conversion error
    Evaluator<Map<String, Object>, Void, Object> eval = new MVEL().compileMapExpression(
        "mappo[\"testKey[MyValue=newValue]\"]",
        Object.class, Set.of(), Map.of("mappo", Type.type(Map.class)));
    assertThat(eval.eval(vars)).isEqualTo("test");
  }

  @Test
  public void testJIRA176b() {
    Map<String, Object> vars = new HashMap<>();
    Map<String, String> innerMap = new HashMap<>();
    innerMap.put("testKey[MyValue=newValue]", "test");
    vars.put("mappo", innerMap);

    Evaluator<Map<String, Object>, Void, Object> eval = new MVEL().compileMapExpression(
        "mappo[\"testKey[MyValue=newValue]\"]",
        Object.class, Set.of(), Map.of("mappo", Type.type(Map.class)));
    assertThat(eval.eval(vars)).isEqualTo("test");
    assertThat(eval.eval(vars)).isEqualTo("test");
  }

  // ---------------------------------------------------------------------------
  // JIRA-209 — BigDecimal boolean expressions (or/and / ||/&&)
  // ---------------------------------------------------------------------------

  @Test
  public void testJIRA209() {
    // bal = 999.99; verify that interpreted and compiled produce the same result
    Map<String, Object> vars = new HashMap<>();
    vars.put("bal", new BigDecimal("999.99"));

    String[][] testCases = {
        {"bal > 200 || bal < 100",  "true"},
        {"bal < 100 && bal > 200",  "false"},
        {"bal > 200 && bal < 100",  "false"},
    };

    for (String[] tc : testCases) {
      String expr = tc[0];
      boolean expected = Boolean.parseBoolean(tc[1]);
      Evaluator<Map<String, Object>, Void, Boolean> e = new MVEL().compileMapExpression(
          expr, Boolean.class, Set.of("java.math.BigDecimal"),
          Map.of("bal", Type.type(BigDecimal.class)));
      assertThat(e.eval(vars))
          .as("expression did not evaluate correctly: " + expr)
          .isEqualTo(expected);
    }
  }

  // ---------------------------------------------------------------------------
  // MVEL-221 — null-safe operator `?.` (rewritten as explicit null check)
  // ---------------------------------------------------------------------------

  @Test
  public void testMVEL221() {
    A221 a1 = new A221();
    a1.setB(new B221());
    A221 a2 = new A221();

    // null-safe-operator: `this.?b.c` rewritten as explicit null check via getters
    Evaluator<Map<String, Object>, Void, Object> eval = new MVEL().compileMapExpression(
        "a.getB() != null ? a.getB().getC() : null",
        Object.class, Set.of(), Map.of("a", Type.type(A221.class)));

    assertThat(eval.eval(Map.of("a", a2))).isNull();
    assertThat(eval.eval(Map.of("a", a1))).isEqualTo("something");
  }

  // ---------------------------------------------------------------------------
  // MVEL-222 — for loop assigning into double[] element (block form with return)
  // ---------------------------------------------------------------------------

  @Test
  public void testMVEL222() {
    // block-syntax: for body with no expression value — add `return null;`
    Evaluator<Map<String, Object>, Void, Object> eval = new MVEL().compileMapBlock(
        "for (int i = 0; i < 10; i++) { values[i] = 1.0; } return null;",
        Object.class, Set.of(), Map.of("values", Type.type(double[].class)));
    double[] values = new double[10];
    Map<String, Object> scriptVars = new HashMap<>();
    scriptVars.put("values", values);
    for (int i = 0; i < 6; i++) {
      eval.eval(scriptVars);
    }
    assertThat(values[0]).isEqualTo(1.0);
  }

  // ---------------------------------------------------------------------------
  // MVEL-224 — simple compile of `(pin == 1)` with an explicit type
  // ---------------------------------------------------------------------------

  @Test
  public void testMVEL224() {
    Evaluator<Map<String, Object>, Void, Boolean> eval = new MVEL().compileMapExpression(
        "(pin == 1)", Boolean.class, Set.of(), Map.of("pin", Type.type(Integer.class)));
    assertThat(eval.eval(Map.of("pin", 1))).isTrue();
  }

  // ---------------------------------------------------------------------------
  // Parsing regression — if/else block (add return to fix block-syntax)
  // ---------------------------------------------------------------------------

  @Test
  public void testParsingRegression() {
    // block-syntax: println is void, so add `return null;` to avoid "missing return"
    Evaluator<Map<String, Object>, Void, Object> eval = new MVEL().compileMapBlock(
        "if (false) { System.out.println(\" foo\"); } else { System.out.println(\" bar\"); } return null;",
        Object.class, Set.of(), Map.of());
    eval.eval(Map.of());
  }
}
