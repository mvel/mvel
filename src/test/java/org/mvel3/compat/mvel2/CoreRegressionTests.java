package org.mvel3.compat.mvel2;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mvel3.compat.mvel2.res.res2.ClassProvider;

import static org.mvel3.compat.mvel2.Mvel3TestSupport.ExpressionCompiler;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.compileExpression;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.eval;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.executeExpression;

/**
 * Imported from JIRA/MVEL-number regression tests in
 * org.mvel2.tests.core.CoreConfidenceTests (mvel2 branch).
 *
 * <p>Expressions, input values, expected values, and assertions are retained
 * unchanged where MVEL3 has an equivalent. Failures are intentionally visible;
 * known behavioural differences are annotated with {@link KnownCompatibility}.
 *
 * <p>Not imported, because they use MVEL2-only APIs:
 * {@code testJIRA96} ({@code ExpressionCompiler.getParserContextState});
 * {@code testJIRA103} ({@code MVEL.setProperty});
 * {@code testJIRA139} ({@code ReflectionUtil.getGetter}, MVEL2 internal);
 * {@code testJIRA153} (already in {@code CoreOperatorsTests});
 * {@code testJIRA154}/{@code b}, {@code testJIRA155}/{@code b}
 * ({@code MVEL.getStaticMethod});
 * {@code testJIRA168} ({@code MVEL.COMPILER_OPT_ALLOW_NAKED_METH_CALL} global flag);
 * {@code testJIRA170}/{@code b} ({@code ParserContext.addInput} with generic bounds);
 * {@code testMVEL187}, {@code testMVEL190}, {@code testMVEL226}
 * ({@code MapVariableResolverFactory});
 * {@code testMVEL238} (reads external {@code .mvel} file);
 * {@code testRandomExpression4} (calls {@code org.mvel2.MVEL.*} inside an expression string);
 * {@code testEvaluationRegression}, {@code testAssignmentRegression}
 * (assert on {@code ExpressionCompiler.getParserContextState}; already in
 * {@code CoreOperatorsTests}).
 *
 * <p>Adaptations: {@code ParserContext}/{@code ParserConfiguration} become
 * {@link TestCompilerContext} ({@code addImport}); optimizer selection and
 * strong typing flags are removed; {@code VariableResolverFactory} replaced by
 * plain variable maps.  {@code Az}/{@code Bz} are kept as nested public static
 * classes (MVEL3 requires public types as input). {@code ClassProvider}/
 * {@code PublicClass} copied to {@code res/res2/}.  The {@code minim} helper
 * is a {@code public static} method of this class so MVEL3 can find it by
 * fully-qualified name.
 */
@Tag("mvel2-compatibility")
public class CoreRegressionTests extends AbstractTest {

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
    public B221 b;
  }

  public static class B221 {
    public String c = "something";
  }

  // ---------------------------------------------------------------------------
  // MVEL-103b  — overloaded setter dispatch
  // ---------------------------------------------------------------------------

  public static class MvelContext {
    public String[] regkeys;

    public void setRegkeys(String[] regkeys) {
      this.regkeys = regkeys;
    }

    public void setRegkeys(String regkey) {
      this.regkeys = regkey.split(",");
    }
  }

  // Category not isolated: `ctx.regkeys = "foo"` invokes `setRegkeys(String)` which returns void.
  @Test
  @KnownCompatibility("void-method-expression")
  public void testJIRA103b() {
    MvelContext mvelContext = new MvelContext();
    Map<String, Object> map = new HashMap<>();
    map.put("ctx", mvelContext);
    var compiled = compileExpression("ctx.regkeys = \"foo\"");
    executeExpression(compiled, map);
    executeExpression(compiled, map);
  }

  // ---------------------------------------------------------------------------
  // JIRA-113  — static method call by fully qualified class name
  // ---------------------------------------------------------------------------

  @Test
  public void testJIRA113() {
    assertEquals(true,
        test("org.mvel3.compat.mvel2.CoreRegressionTests.minim( new double[] {456.2, 2.3} ) == 2.3"));
  }

  // ---------------------------------------------------------------------------
  // JIRA-140  — inline import inside expression
  // ---------------------------------------------------------------------------

  // Category not isolated: `import org.mvel3.compat.mvel2.res.*;` inside expression — inline import.
  @Test
  @KnownCompatibility("inline-import")
  public void testJIRA140() {
    TestCompilerContext ctx = new TestCompilerContext();
    ExpressionCompiler compiler = new ExpressionCompiler(
        "import org.mvel3.compat.mvel2.res.*;"
            + "cols = new Column[] { new Column(\"name\", 20), new Column(\"age\", 2) };"
            + "grid = new Grid(new Model(cols));",
        ctx);
    compiler.compile();
  }

  // Category not isolated: `a.foo(value)` — `Az.foo(String)` returns void.
  @Test
  @KnownCompatibility("void-method-expression")
  public void testJIRA151() {
    Bz b = new Bz();
    var compiled = compileExpression("a.foo(value)");
    Map<String, Object> variables = new HashMap<>();
    variables.put("a", b);
    variables.put("value", "123");
    for (int i = 0; i < 100; i++) {
      executeExpression(compiled, variables);
    }
  }

  // Category not isolated: `a.foo(value)` — `Az.foo(String)` returns void.
  @Test
  @KnownCompatibility("void-method-expression")
  public void testJIRA151b() {
    Bz b = new Bz();
    var compiled = compileExpression("a.foo(value)");
    Map<String, Object> variables = new HashMap<>();
    variables.put("a", b);
    variables.put("value", "123");
    for (int i = 0; i < 100; i++) {
      executeExpression(compiled, variables);
    }
  }

  // ---------------------------------------------------------------------------
  // JIRA-156 — method call through an interface on an instance of a package-private class
  // ---------------------------------------------------------------------------

  // Category not isolated: `provider.getPrivate().foo()` — `PublicClass.foo()` returns void.
  @Test
  @KnownCompatibility("void-method-expression")
  public void testJIRA156() {
    ClassProvider provider = new ClassProvider();
    provider.getPrivate().foo();

    String script = "provider.getPrivate().foo()";
    HashMap<String, Object> vars = new HashMap<>();
    vars.put("provider", provider);
    eval(script, vars);
  }

  // Category not isolated: `provider.getPrivate().foo()` — `PublicClass.foo()` returns void.
  @Test
  @KnownCompatibility("void-method-expression")
  public void testJIRA156b() {
    ClassProvider provider = new ClassProvider();
    var compiled = compileExpression("provider.getPrivate().foo()");
    HashMap<String, Object> vars = new HashMap<>();
    vars.put("provider", provider);
    executeExpression(compiled, vars);
    executeExpression(compiled, vars);
  }

  // Category not isolated: `provider.getPublic().foo()` — `PublicClass.foo()` returns void.
  @Test
  @KnownCompatibility("void-method-expression")
  public void testJIRA156c() {
    ClassProvider provider = new ClassProvider();
    String script = "provider.getPublic().foo()";
    var compiled = compileExpression(script);
    HashMap<String, Object> vars = new HashMap<>();
    vars.put("provider", provider);
    eval(script, vars);
    executeExpression(compiled, vars);
    executeExpression(compiled, vars);
  }

  // ---------------------------------------------------------------------------
  // JIRA-166  — regex with Pattern.compile inside expression
  // ---------------------------------------------------------------------------

  // Category not isolated: `import java.util.regex.Matcher;` inside expression — inline import.
  @Test
  @KnownCompatibility("inline-import")
  public void testJIRA166() {
    Object v = eval(
        "import java.util.regex.Matcher; import java.util.regex.Pattern;"
            + " if (Pattern.compile(\"hoge\").matcher(\"hogehogehoge\").find()) { return \"foo\"; } else { return \"bar\"; }",
        new HashMap<>());
    assertEquals("foo", v);
  }

  // ---------------------------------------------------------------------------
  // JIRA-167 / 167b / 167c — ternary with null property access
  // ---------------------------------------------------------------------------

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

  @Test
  public void testJIRA167() {
    Map<String, Object> context = new HashMap<>();
    context.put("bean", new Beano());
    eval("bean.property1==null?bean.isProperty2():bean.isProperty3()", context);
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

  // Category not isolated: `node==current.parent.node` — when `current.parent` is null the ternary
  // short-circuits, but MVEL3 type-solves the whole expression; `node` on `current.parent` (a
  // String-returning getter) is unsolved.
  @Test
  @KnownCompatibility("undeclared-input-variable")
  public void testJIRA167b() {
    Map<String, Object> context = new HashMap<>();
    context.put("current", new JIRA167Step());
    context.put("node", new JIRA167Node());
    eval("current.parent==null?node.isServer():(node==current.parent.node)", context);
  }

  @Test
  public void testJIRA167c() {
    eval("true?true:(false)", new HashMap<>());
  }

  // ---------------------------------------------------------------------------
  // JIRA-176 / 176b — map access with key containing brackets
  // ---------------------------------------------------------------------------

  @Test
  public void testJIRA176() {
    Map<String, String> innerMap = new HashMap<>();
    innerMap.put("testKey[MyValue=newValue]", "test");

    Map<String, Object> vars = new HashMap<>();
    vars.put("mappo", innerMap);

    assertEquals("test", eval("mappo[\"testKey[MyValue=newValue]\"]", vars));
  }

  @Test
  public void testJIRA176b() {
    Map<String, String> innerMap = new HashMap<>();
    innerMap.put("testKey[MyValue=newValue]", "test");

    Map<String, Object> vars = new HashMap<>();
    vars.put("mappo", innerMap);

    var compiled = compileExpression("mappo[\"testKey[MyValue=newValue]\"]");
    assertEquals("test", executeExpression(compiled, vars));
    assertEquals("test", executeExpression(compiled, vars));
  }

  // ---------------------------------------------------------------------------
  // JIRA-209 — BigDecimal boolean expressions (or/and / ||/&&)
  // ---------------------------------------------------------------------------

  @Test
  public void testJIRA209() {
    Map<String, Object> vars = new LinkedHashMap<>();
    vars.put("bal", new BigDecimal("999.99"));

    String[] testCases = {
        "bal > 200 or bal < 100",
        "bal > 200 || bal < 100",
        "bal < 100 and bal > 200",
        "bal < 100 && bal > 200",
        "bal > 200 and bal < 100",
        "bal > 200 && bal < 100"
    };

    TestCompilerContext ctx = new TestCompilerContext();
    ctx.addImport(BigDecimal.class);
    for (String expr : testCases) {
      Object val1 = executeExpression(compileExpression(expr, ctx), vars);
      assertNotNull(val1);
      var compiled = compileExpression(expr, ctx);
      Object val2 = executeExpression(compiled, vars);
      assertNotNull(val2);
      assertEquals("expression did not evaluate correctly: " + expr, val1, val2);
    }
  }

  // ---------------------------------------------------------------------------
  // MVEL-221 — null-safe operator `?.`
  // ---------------------------------------------------------------------------

  // MVEL2: `this.?b.c` — null-safe chained access; null when b is null, "something" when set.
  // MVEL3: null-safe operator `?.` returns the left-hand object; reclassified accordingly.
  @Test
  @KnownCompatibility("null-safe-operator")
  public void testMVEL221() {
    A221 a1 = new A221();
    a1.b = new B221();

    A221 a2 = new A221();

    String expression = "this.?b.c";
    var compiledExpression = compileExpression(expression);

    assertEquals(null, executeExpression(compiledExpression, a2, new HashMap<>()));
    assertEquals("something", executeExpression(compiledExpression, a1, new HashMap<>()));
  }

  // ---------------------------------------------------------------------------
  // MVEL-222 — for loop assigning into double[] element
  // ---------------------------------------------------------------------------

  // Category not isolated: `for (...){ values[i] = 1.0; }` — the for block has no return,
  // so MVEL3 emits "missing return statement".  Use block form with explicit `return null;`.
  @Test
  @KnownCompatibility("block-syntax")
  public void testMVEL222() {
    String script = "for (int i= 0; i < 10; i++ ){ values[i] = 1.0; }";
    Map<String, Object> scriptVars = new HashMap<>();
    double[] values = new double[10];
    scriptVars.put("values", values);
    var expression = compileExpression(script);
    for (int i = 0; i < 6; i++) {
      scriptVars.put("values", values);
      executeExpression(expression, scriptVars);
    }
  }

  // ---------------------------------------------------------------------------
  // MVEL-224 — simple compile of `(pin == 1)` with ParserContext
  // ---------------------------------------------------------------------------

  @Test
  public void testMVEL224() {
    // MVEL2: `new ParserContext(); MVEL.compileExpression("(pin == 1)", ctx)`
    // MVEL3: equivalent — just compile with a context; no assertion, just "must not throw".
    TestCompilerContext ctx = new TestCompilerContext();
    ExpressionCompiler compiler = new ExpressionCompiler("(pin == 1)", ctx);
    compiler.compile();
  }

  // ---------------------------------------------------------------------------
  // Parsing regression — if/else with System.out.println
  // ---------------------------------------------------------------------------

  // Category not isolated: if/else block with `println` (void) as last statement — block-syntax
  // requires explicit `return null;` or a non-void last statement.
  @Test
  @KnownCompatibility("block-syntax")
  public void testParsingRegression() {
    String expr = "if (false) {System.out.println(\" foo\");} else {System.out.println(\" bar\");}";
    eval(expr, new HashMap<>());
  }
}
