package org.mule.mvel2.tests.core;

import static org.mule.mvel2.MVEL.compileExpression;
import static org.mule.mvel2.MVEL.compileSetExpression;
import static org.mule.mvel2.MVEL.executeExpression;
import static org.mule.mvel2.MVEL.executeSetExpression;
import static org.mule.mvel2.MVEL.setProperty;

import org.mule.mvel2.CompileException;
import org.mule.mvel2.ConversionHandler;
import org.mule.mvel2.DataConversion;
import org.mule.mvel2.MVEL;
import org.mule.mvel2.ParserConfiguration;
import org.mule.mvel2.ParserContext;
import org.mule.mvel2.compiler.CompiledAccExpression;
import org.mule.mvel2.compiler.CompiledExpression;
import org.mule.mvel2.compiler.ExecutableAccessor;
import org.mule.mvel2.compiler.ExecutableStatement;
import org.mule.mvel2.compiler.ExpressionCompiler;
import org.mule.mvel2.integration.impl.CachedMapVariableResolverFactory;
import org.mule.mvel2.integration.impl.DefaultLocalVariableResolverFactory;
import org.mule.mvel2.integration.impl.StaticMethodImportResolverFactory;
import org.mule.mvel2.optimizers.OptimizerFactory;
import org.mule.mvel2.tests.core.res.Bar;
import org.mule.mvel2.tests.core.res.Base;
import org.mule.mvel2.tests.core.res.Cheese;
import org.mule.mvel2.tests.core.res.Foo;
import org.mule.mvel2.tests.core.res.MyInterface2;
import org.mule.mvel2.tests.core.res.MyInterface3;
import org.mule.mvel2.tests.core.res.SampleBean;
import org.mule.mvel2.util.MethodStub;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * @author Mike Brock .
 */
public class TypesAndInferenceTests extends AbstractTest {

  public void testGenericInference() {
    String expression = "$result = person.footributes[0].name";

    ParserContext ctx;
    MVEL.analysisCompile(expression,
            ctx = ParserContext.create().stronglyTyped().withInput("person", Person.class));

    assertEquals(String.class, ctx.getVarOrInputTypeOrNull("$result"));

    Serializable s =
            MVEL.compileExpression(expression, ParserContext.create().stronglyTyped().withInput("person", Person.class));


    Map<String, Object> vars = new HashMap<String, Object>();
    Person p = new Person();
    p.setFootributes(new ArrayList<Foo>());
    p.getFootributes().add(new Foo());

    vars.put("person", p);

    assertEquals("dog", executeExpression(s, vars));
  }

  public void testGenericInference2() {
    ParserContext ctx;
    MVEL.analysisCompile("$result = person.maptributes['fooey'].name",
            ctx = ParserContext.create().stronglyTyped().withInput("person", Person.class));

    assertEquals(String.class, ctx.getVarOrInputTypeOrNull("$result"));
  }

  public void testVarInputs() {
    ParserContext pCtx = ParserContext.create();
    MVEL.analysisCompile("test != foo && bo.addSomething(trouble) " +
            "&& 1 + 2 / 3 == 1; String bleh = foo; twa = bleh;", pCtx);

    assertEquals(4,
            pCtx.getInputs().size());

    assertTrue(pCtx.getInputs().containsKey("test"));
    assertTrue(pCtx.getInputs().containsKey("foo"));
    assertTrue(pCtx.getInputs().containsKey("bo"));
    assertTrue(pCtx.getInputs().containsKey("trouble"));

    assertEquals(2,
            pCtx.getVariables().size());

    assertTrue(pCtx.getVariables().containsKey("bleh"));
    assertTrue(pCtx.getVariables().containsKey("twa"));

    assertEquals(String.class,
            pCtx.getVarOrInputType("bleh"));
  }

  public void testVarInputs2() {
    ExpressionCompiler compiler =
            new ExpressionCompiler("test != foo && bo.addSomething(trouble); String bleh = foo; twa = bleh;");

    ParserContext ctx = new ParserContext();

    compiler.compile(ctx);

    System.out.println(ctx.getVarOrInputType("bleh"));
  }

  public void testVarInputs3() {
    ExpressionCompiler compiler = new ExpressionCompiler("addresses['home'].street");
    compiler.compile();

    assertFalse(compiler.getParserContextState().getInputs().keySet().contains("home"));
  }

  public void testVarInputs4() {
    ExpressionCompiler compiler = new ExpressionCompiler("System.out.println( message );");
    compiler.compile();

    assertTrue(compiler.getParserContextState().getInputs().keySet().contains("message"));
  }

  public void testVarInputs5() {
    ParserContext pCtx = ParserContext.create().withInput("list", List.class);
    MVEL.analysisCompile("String nodeName = list[0];\nSystem.out.println(nodeName);nodeName = list[1];\nSystem.out.println(nodeName);", pCtx);

    assertEquals(1,
            pCtx.getInputs().size());

    assertTrue(pCtx.getInputs().containsKey("list"));

    assertEquals(1,
            pCtx.getVariables().size());

    assertTrue(pCtx.getVariables().containsKey("nodeName"));

    assertEquals(List.class,
            pCtx.getVarOrInputType("list"));
    assertEquals(String.class,
            pCtx.getVarOrInputType("nodeName"));
  }

  public void testDetermineRequiredInputsInConstructor() throws Exception {
    ParserContext ctx = new ParserContext();
    ctx.setStrictTypeEnforcement(false);
    ctx.setStrongTyping(false);
    ctx.addImport(Foo.class);

    ExpressionCompiler compiler = new ExpressionCompiler("new Foo244( $bar,  $bar.age );");

    Serializable compiled = compiler.compile(ctx);

    Set<String> requiredInputs = compiler.getParserContextState().getInputs().keySet();
    assertEquals(1, requiredInputs.size());
    assertTrue(requiredInputs.contains("$bar"));

  }


  public void testAnalyzer() {
    ParserContext ctx = new ParserContext();
    MVEL.compileExpression("order.id == 10", ctx);

    for (String input : ctx.getInputs().keySet()) {
      System.out.println("input>" + input);
    }

    assertEquals(1, ctx.getInputs().size());
    assertTrue(ctx.getInputs().containsKey("order"));
  }


  public void testAnalysisCompile() {
    ParserContext pCtx = new ParserContext();
    ExpressionCompiler e = new ExpressionCompiler("foo.aValue = 'bar'");
    e.setVerifyOnly(true);

    e.compile(pCtx);

    assertTrue(pCtx.getInputs().keySet().contains("foo"));
    assertEquals(1,
            pCtx.getInputs().size());
    assertEquals(0,
            pCtx.getVariables().size());
  }

  public void testMultiVarDeclr() {
    String ex = "var a, b, c";

    ParserContext ctx = new ParserContext();
    ExpressionCompiler compiler = new ExpressionCompiler(ex);
    compiler.setVerifyOnly(true);
    compiler.compile(ctx);

    assertEquals(3,
            ctx.getVariables().size());
  }

  public void testVarDeclr() {
    String ex = "var a";

    ParserContext ctx = new ParserContext();
    ExpressionCompiler compiler = new ExpressionCompiler(ex);
    compiler.setVerifyOnly(true);
    compiler.compile(ctx);

    assertEquals(1,
            ctx.getVariables().size());
  }

  public void testMultiTypeVarDeclr() {
    String ex = "String a, b, c";
    ParserContext ctx = new ParserContext();
    ExpressionCompiler compiler = new ExpressionCompiler(ex);
    compiler.compile(ctx);

    assertNotNull(ctx.getVariables());
    assertEquals(3,
            ctx.getVariables().entrySet().size());
    for (Map.Entry<String, Class> entry : ctx.getVariables().entrySet()) {
      assertEquals(String.class,
              entry.getValue());
    }
  }

  public void testMultiTypeVarDeclr2() {
    String ex = "String a = 'foo', b = 'baz', c = 'bar'";
    ParserContext ctx = new ParserContext();
    ExpressionCompiler compiler = new ExpressionCompiler(ex);
    compiler.compile(ctx);

    assertNotNull(ctx.getVariables());
    assertEquals(3,
            ctx.getVariables().entrySet().size());
    for (Map.Entry<String, Class> entry : ctx.getVariables().entrySet()) {
      assertEquals(String.class,
              entry.getValue());
    }
  }

  public void testMultiTypeVarDeclr3() {
    String ex = "int a = 52 * 3, b = 8, c = 16;";
    ParserContext ctx = new ParserContext();
    ExpressionCompiler compiler = new ExpressionCompiler(ex);
    Serializable s = compiler.compile(ctx);

    assertNotNull(ctx.getVariables());
    assertEquals(3,
            ctx.getVariables().entrySet().size());
    for (Map.Entry<String, Class> entry : ctx.getVariables().entrySet()) {
      assertEquals(int.class,
              entry.getValue());
    }

    Map vars = new HashMap();
    executeExpression(s,
            vars);

    assertEquals(52 * 3,
            vars.get("a"));
    assertEquals(8,
            vars.get("b"));
    assertEquals(16,
            vars.get("c"));

  }

  public void testTypeVarDeclr() {
    String ex = "String a;";
    ParserContext ctx = new ParserContext();
    ExpressionCompiler compiler = new ExpressionCompiler(ex);
    compiler.compile(ctx);

    assertNotNull(ctx.getVariables());
    assertEquals(1,
            ctx.getVariables().entrySet().size());
    for (Map.Entry<String, Class> entry : ctx.getVariables().entrySet()) {
      assertEquals(String.class,
              entry.getValue());
    }
  }

  public void testStrictTypingCompilation4() throws NoSuchMethodException {
    ParserContext ctx = new ParserContext();

    ctx.addImport(Foo.class);
    ctx.setStrictTypeEnforcement(true);

    ExpressionCompiler compiler = new ExpressionCompiler("x_a = new Foo()");

    compiler.compile(ctx);

    assertEquals(Foo.class,
            ctx.getVariables().get("x_a"));
  }

  public void testEgressType() {
    ExpressionCompiler compiler = new ExpressionCompiler("( $cheese )");
    ParserContext context = new ParserContext();
    context.addInput("$cheese",
            Cheese.class);

    assertEquals(Cheese.class,
            compiler.compile(context).getKnownEgressType());
  }

  public void testEgressTypeCorrect() {
    ExecutableStatement stmt = (ExecutableStatement)
            MVEL.compileExpression("type", ParserContext.create().stronglyTyped()
                    .withInput("this", Cheese.class));

    assertEquals(String.class,
            stmt.getKnownEgressType());
  }

  public void testEgressTypeCorrect2() {

    ParserContext context = new ParserContext();
    context.setStrongTyping(true);
    context.addInput("this",
            SampleBean.class);
    ExecutableStatement stmt = (ExecutableStatement) MVEL.compileExpression("( map2[ 'yyy' ] )", context);

    SampleBean s = new SampleBean();
    s.getMap2().put("yyy", 1);

    assertEquals(new Integer(1),
            MVEL.executeExpression(stmt, s));
  }


  public static final List<String> STRINGS = Arrays.asList("hi",
          "there");

  public static class A {
    public void foo(String s) {
    }

    public void bar(String s) {
    }

    public List<String> getStrings() {
      return STRINGS;
    }
  }

  public static class B extends A {
    @Override
    public void foo(String s) {
      super.foo(s);
    }

    public void bar(int s) {
    }

  }

  public static class C extends A {
  }

  public final void testDetermineEgressParametricType() {
    final ParserContext parserContext = new ParserContext();
    parserContext.setStrongTyping(true);

    parserContext.addInput("strings",
            List.class,
            new Class[]{String.class});

    final CompiledExpression expr = new ExpressionCompiler("strings").compile(parserContext);

    assertTrue(STRINGS.equals(executeExpression(expr,
            new A())));

/*
    final Type[] typeParameters = expr.getParserContext().getLastTypeParameters();
    assertTrue(typeParameters != null);
    assertTrue(String.class.equals(typeParameters[0]));
*/
  }


  public final void testDetermineEgressParametricType2() {
    final ParserContext parserContext = new ParserContext();
    parserContext.setStrongTyping(true);
    parserContext.addInput("strings",
            List.class,
            new Class[]{String.class});

    final CompiledExpression expr = new ExpressionCompiler("strings",
            parserContext).compile();

    assertTrue(STRINGS.equals(executeExpression(expr,
            new A())));
/*
    final Type[] typeParameters = expr.getParserContext().getLastTypeParameters();
    assertTrue(null != typeParameters);
    assertTrue(String.class.equals(typeParameters[0]));
*/
  }

  public void testJIRA151c() {
    OptimizerFactory.setDefaultOptimizer(OptimizerFactory.SAFE_REFLECTIVE);
    A b = new B();
    A c = new C();

    ParserContext context = new ParserContext();
    Object expression = MVEL.compileExpression("a.foo(value)",
            context);

    for (int i = 0; i < 100; i++) {
      System.out.println("i: " + i);
      System.out.flush();

      {
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("a", b);
        variables.put("value", 123);
        executeExpression(expression, variables);
      }
      {
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("a", c);
        variables.put("value", 123);
        executeExpression(expression, variables);
      }

    }
  }

  public void testJIRA151d() {
    OptimizerFactory.setDefaultOptimizer("ASM");
    A b = new B();
    A c = new C();

    ParserContext context = new ParserContext();
    Object expression = MVEL.compileExpression("a.foo(value)",
            context);

    for (int i = 0; i < 100; i++) {
      System.out.println("i: " + i);
      System.out.flush();

      {
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("a", b);
        variables.put("value", 123);
        executeExpression(expression, variables);
      }
      {
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("a", c);
        variables.put("value", 123);
        executeExpression(expression, variables);
      }

    }
  }


  public void testJIRA165b() {
    OptimizerFactory.setDefaultOptimizer("ASM");
    A b = new B();
    A a = new A();
    ParserContext context = new ParserContext();
    Object expression = MVEL.compileExpression("a.bar(value)",
            context);

    for (int i = 0; i < 100; i++) {
      System.out.println("i: " + i);
      System.out.flush();

      {
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("a", b);
        variables.put("value", 123);
        executeExpression(expression, variables);
      }
      {
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("a", a);
        variables.put("value", 123);
        executeExpression(expression, variables);
      }
    }

  }

  public void testJIRA165() {
    OptimizerFactory.setDefaultOptimizer(OptimizerFactory.SAFE_REFLECTIVE);
    A b = new B();
    A a = new A();
    ParserContext context = new ParserContext();
    Object expression = MVEL.compileExpression("a.bar(value)",
            context);
    for (int i = 0; i < 100; i++) {
      System.out.println("i: " + i);
      System.out.flush();

      {
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("a", b);
        variables.put("value", 123);
        executeExpression(expression, variables);
      }
      {
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("a", a);
        variables.put("value", 123);
        executeExpression(expression, variables);
      }
    }
  }


  public void testStrictTypingCompilationWithVarInsideConstructor() {
    ParserContext ctx = new ParserContext();
    ctx.addInput("$likes", String.class);
    ctx.addInput("results", List.class);
    ctx.addImport(Cheese.class);
    ctx.setStrongTyping(true);

    Serializable expr = null;
    try {
      expr = MVEL.compileExpression("Cheese c = new Cheese( $likes, 15 );\nresults.add( c ); ", ctx);
    }
    catch (CompileException e) {
      e.printStackTrace();
      fail("This should not fail:\n" + e.getMessage());
    }
    List results = new ArrayList();

    Map vars = new HashMap();
    vars.put("$likes", "stilton");
    vars.put("results", results);
    executeExpression(expr, vars);

    assertEquals(new Cheese("stilton", 15), results.get(0));
  }

  public void testParameterizedTypeInStrictMode2() {
    ParserContext ctx = new ParserContext();
    ctx.setStrongTyping(true);
    ctx.addInput("ctx",
            Object.class);

    ExpressionCompiler compiler =
            new ExpressionCompiler("org.mule.mvel2.DataConversion.convert(ctx, String).toUpperCase()");
    assertEquals(String.class,
            compiler.compile(ctx).getKnownEgressType());
  }

  public void testParameterizedTypeInStrictMode3() {
    ParserContext ctx = new ParserContext();
    ctx.setStrongTyping(true);
    ctx.addInput("base",
            Base.class);

    ExpressionCompiler compiler = new ExpressionCompiler("base.list");

//    assertTrue(compiler.compile(ctx).getParserContext().getLastTypeParameters()[0].equals(String.class));
  }

  public void testParameterizedTypeInStrictMode4() {
    ParserContext ctx = new ParserContext();
    ctx.setStrongTyping(true);
    ctx.addInput("base",
            Base.class);

    ExpressionCompiler compiler = new ExpressionCompiler("base.list.get(1).toUpperCase()");
    CompiledExpression ce = compiler.compile(ctx);

    assertEquals(String.class,
            ce.getKnownEgressType());
  }

  public void testReturnType1() {
    assertEquals(Double.class,
            new ExpressionCompiler("100.5").compile().getKnownEgressType());
  }

  public void testReturnType2() {
    assertEquals(Integer.class,
            new ExpressionCompiler("1").compile().getKnownEgressType());
  }

  public void testStrongTyping3() {
    ParserContext ctx = new ParserContext();
    ctx.setStrongTyping(true);

    try {
      new ExpressionCompiler("foo.toUC(100.5").compile(ctx);
    }
    catch (Exception e) {
      // should fail.
      return;
    }

    assertTrue(false);
  }

  public void testEgressType1() {
    assertEquals(Boolean.class,
            new ExpressionCompiler("foo != null").compile().getKnownEgressType());
  }

  public void testStrictStaticMethodCall() {
    ExpressionCompiler compiler = new ExpressionCompiler("Bar.staticMethod()");
    ParserContext ctx = new ParserContext();
    ctx.addImport("Bar",
            Bar.class);
    ctx.setStrictTypeEnforcement(true);

    Serializable s = compiler.compile(ctx);

    assertEquals(1,
            executeExpression(s));
  }

  public void testStrictTypingCompilation2() throws Exception {
    ParserContext ctx = new ParserContext();
    //noinspection RedundantArrayCreation
    ctx.addImport("getRuntime",
            new MethodStub(Runtime.class.getMethod("getRuntime",
                    new Class[]{})));

    ctx.setStrictTypeEnforcement(true);

    ExpressionCompiler compiler = new ExpressionCompiler("getRuntime()");
    StaticMethodImportResolverFactory si = new StaticMethodImportResolverFactory(ctx);

    Serializable expression = compiler.compile(ctx);

    serializationTest(expression);

    assertTrue(executeExpression(expression,
            si) instanceof Runtime);
  }

  public void testStrictTypingCompilation3() throws NoSuchMethodException {
    ParserContext ctx = new ParserContext();

    ctx.setStrictTypeEnforcement(true);

    ExpressionCompiler compiler =
            new ExpressionCompiler("message='Hello';b=7;\nSystem.out.println(message + ';' + b);\n"
                    + "System.out.println(message + ';' + b); b");

    assertEquals(7,
            executeExpression(compiler.compile(ctx),
                    new DefaultLocalVariableResolverFactory()));
  }


  public void testStrictStrongTypingCompilationErrors1() throws Exception {
    ParserContext ctx = new ParserContext();
    ctx.setStrictTypeEnforcement(true);
    ctx.setStrongTyping(true);
    ctx.addImport(Foo.class);
    ctx.addInput("$bar", Bar.class);

    try {
      ExpressionCompiler compiler = new ExpressionCompiler("System.out.println( $ba );");

      compiler.compile(ctx);
      fail("This should not compileShared");
    }
    catch (Exception e) {
    }
  }

  public void testStrictStrongTypingCompilationErrors2() throws Exception {
    ParserContext ctx = new ParserContext();
    ctx.setStrictTypeEnforcement(true);
    ctx.setStrongTyping(true);
    ctx.addImport(Foo.class);
    ctx.addInput("$bar", Bar.class);

    try {
      MVEL.compileExpression("x_a = new Foo244( $ba ); x_a.equals($ba);", ctx);
      fail("This should not compileShared");
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }


  public void testProvidedExternalTypes() {
    ExpressionCompiler compiler = new ExpressionCompiler("foo.bar");
    ParserContext ctx = new ParserContext();
    ctx.setStrictTypeEnforcement(true);
    ctx.addInput("foo",
            Foo.class);

    compiler.compile(ctx);
  }


  public static class ScriptHelper228 {
    public void methodA() {
    }

    public void methodB(int param1) {
    }
  }

  public static class Person228 {
    public String getName() {
      return "foo";
    }
  }


  public void testMVEL228() {
    ParserContext ctx = new ParserContext();
    ctx.setStrongTyping(true);
    ctx.setStrictTypeEnforcement(true);
    HashMap<String, Class> params = new HashMap<String, Class>();
    params.put("helper", ScriptHelper228.class);
    params.put("person", Person228.class);

    ctx.setInputs(params);

    String script = "helper.methodB(2);\n" +
            "person.getName2();";
    try {
      CompiledExpression compiled = (CompiledExpression) MVEL.compileExpression(script, ctx);
    }
    catch (Exception e) {
      return;
    }

    fail("Should have thrown an exception");
  }


  public void testMVEL232() {
    ParserContext ctx = new ParserContext();
    ctx.setStrongTyping(true);
    ctx.setStrictTypeEnforcement(true);

    String script = "for(int i=0;i<2;i++) { " +
            "  System.out.println(i+\"\");" +
            "} " +
            " return true;";

    try {
      CompiledExpression compiled = (CompiledExpression) MVEL.compileExpression(script, ctx);
      HashMap<String, Object> map = new HashMap<String, Object>();
      MVEL.executeExpression(compiled, map);
    }
    catch (Exception e) {
      e.printStackTrace();
      fail("should now throw an exception");
    }
  }

  public void testMVEL234() {
    StringBuffer buffer = new StringBuffer();

    buffer.append("import java.text.SimpleDateFormat;");
    buffer.append("if (\"test\".matches(\"[0-9]\")) {");
    buffer.append("  return false;");
    buffer.append("}else{");
    buffer.append("  SimpleDateFormat sqf = new SimpleDateFormat(\"yyyyMMdd\");");
    buffer.append("}");

    ParserContext ctx = new ParserContext();
    ctx.setStrongTyping(true);

    try {
      CompiledExpression compiled = (CompiledExpression) MVEL.compileExpression(buffer.toString(), ctx);
    }
    catch (Exception e) {
      fail(e.getMessage());
    }
  }

  public void testMVEL235() {
    StringBuffer buffer = new StringBuffer();

    buffer.append("if(var1.equals(var2)) {");
    buffer.append("return true;");
    buffer.append("}");

    ParserContext ctx = new ParserContext();
    ctx.setStrongTyping(true);
    ctx.addInput("var1", MyInterface2.class);
    ctx.addInput("var2", MyInterface2.class);

    try {
      Serializable compiled = (Serializable) MVEL.compileExpression(buffer.toString(), ctx);
      System.out.println(compiled);
    }
    catch (Exception e) {
      fail(e.getMessage());
    }
  }

  public void testMVEL236() {
    StringBuffer buffer = new StringBuffer();

    buffer.append("MyInterface2 var2 = (MyInterface2)var1;");

    ParserContext ctx = new ParserContext();
    ctx.setStrongTyping(true);
    ctx.addInput("var1", MyInterface3.class);
    ctx.addImport(MyInterface2.class);
    ctx.addImport(MyInterface3.class);

    try {
      CompiledExpression compiled = (CompiledExpression) MVEL.compileExpression(buffer.toString(), ctx);
    }
    catch (Exception e) {
      fail(e.getMessage());
    }
  }

  public static class MapWrapper {
    private Map map = new HashMap();

    public Map getMap() {
      return map;
    }

    public void setMap(Map map) {
      this.map = map;
    }
  }

  public void testMapPropertyAccess() {
    ParserContext ctx = new ParserContext();
    ctx.addImport(MapWrapper.class);
    ctx.addInput("wrapper", MapWrapper.class);
    ctx.setStrongTyping(true);

    Serializable expr = MVEL.compileExpression("wrapper.map[\"key\"]", ctx);

    MapWrapper wrapper = new MapWrapper();
    wrapper.getMap().put("key", "value");
    Map vars = new HashMap();
    vars.put("wrapper", wrapper);

    assertEquals("value", MVEL.executeExpression(expr, vars));
  }


  public void testTypeCast3() {
    Map map = new HashMap();
    map.put("foo",
            new Foo());

    ParserContext pCtx = new ParserContext();
    pCtx.setStrongTyping(true);
    pCtx.addInput("foo",
            Foo.class);

    Serializable s = MVEL.compileExpression("((org.mule.mvel2.tests.core.res.Bar) foo.getBar()).name != null",
            pCtx);

    assertEquals(true,
            executeExpression(s,
                    map));

    assertEquals(1,
            pCtx.getInputs().size());
    assertEquals(true,
            pCtx.getInputs().containsKey("foo"));
  }

  public void testMapWithStrictTyping() {
    ExpressionCompiler compiler = new ExpressionCompiler("map['KEY1'] == $msg");
    ParserContext ctx = new ParserContext();
    ctx.setStrictTypeEnforcement(true);
    ctx.setStrongTyping(true);
    ctx.addInput("$msg",
            String.class);
    ctx.addInput("map",
            Map.class);
    Serializable expr = compiler.compile(ctx);

    Map map = new HashMap();
    map.put("KEY1",
            "MSGONE");
    Map vars = new HashMap();
    vars.put("$msg",
            "MSGONE");
    vars.put("map",
            map);

    Boolean bool = (Boolean) executeExpression(expr,
            map,
            vars);
    assertEquals(Boolean.TRUE,
            bool);
  }

  public void testMapAsContextWithStrictTyping() {
    ExpressionCompiler compiler = new ExpressionCompiler("this['KEY1'] == $msg");
    ParserContext ctx = new ParserContext();
    ctx.setStrictTypeEnforcement(true);
    ctx.setStrongTyping(true);
    ctx.addInput("$msg",
            String.class);
    ctx.addInput("this",
            Map.class);
    Serializable expr = compiler.compile(ctx);

    Map map = new HashMap();
    map.put("KEY1",
            "MSGONE");
    Map vars = new HashMap();
    vars.put("$msg",
            "MSGONE");

    Boolean bool = (Boolean) executeExpression(expr,
            map,
            vars);
    assertEquals(Boolean.TRUE,
            bool);
  }

  public void testStrongTyping() {
    ParserContext ctx = new ParserContext();
    ctx.setStrongTyping(true);

    try {
      new ExpressionCompiler("blah").compile(ctx);
    }
    catch (Exception e) {
      // should fail
      return;
    }

    assertTrue(false);
  }

  public void testStrongTyping2() {
    ParserContext ctx = new ParserContext();
    ctx.setStrongTyping(true);

    ctx.addInput("blah",
            String.class);

    try {
      new ExpressionCompiler("1-blah").compile(ctx);
    }
    catch (Exception e) {
      e.printStackTrace();
      return;
    }

    assertTrue(false);
  }


  public void testParameterizedTypeInStrictMode() {
    ParserContext ctx = new ParserContext();
    ctx.setStrongTyping(true);
    ctx.addInput("foo",
            HashMap.class,
            new Class[]{String.class, String.class});
    ExpressionCompiler compiler = new ExpressionCompiler("foo.get('bar').toUpperCase()");
    compiler.compile(ctx);
  }

  public void testSetAccessorOverloadedEqualsStrictMode2() {
    ParserContext ctx = new ParserContext();
    ctx.setStrongTyping(true);
    ctx.addInput("foo",
            Foo.class);

    try {
      CompiledExpression expr = new ExpressionCompiler("foo.aValue = 'bar'").compile(ctx);
    }
    catch (CompileException e) {
      assertTrue(false);
    }
  }

  public void testDataConverterStrictMode() throws Exception {
    OptimizerFactory.setDefaultOptimizer("ASM");

    DataConversion.addConversionHandler(Date.class,
            new MVELDateCoercion());

    ParserContext ctx = new ParserContext();
    ctx.addImport("Cheese",
            Cheese.class);
    ctx.setStrongTyping(true);
    ctx.setStrictTypeEnforcement(true);

    Locale.setDefault(Locale.US);

    Cheese expectedCheese = new Cheese();
    expectedCheese.setUseBy(new SimpleDateFormat("dd-MMM-yyyy").parse("10-Jul-1974"));

    ExpressionCompiler compiler = new ExpressionCompiler("c = new Cheese(); c.useBy = '10-Jul-1974'; return c");
    Cheese actualCheese = (Cheese) executeExpression(compiler.compile(ctx),
            createTestMap());
    assertEquals(expectedCheese.getUseBy(),
            actualCheese.getUseBy());
  }


  public static class MVELDateCoercion implements ConversionHandler {
    public boolean canConvertFrom(Class cls) {
      return cls == String.class || cls.isAssignableFrom(Date.class);
    }

    public Object convertFrom(Object o) {
      try {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");
        if (o instanceof String) {
          return sdf.parse((String) o);
        } else {
          return o;
        }
      }
      catch (Exception e) {
        throw new RuntimeException("Exception was thrown",
                e);
      }
    }
  }

  public void testCompileTimeCoercion() {
    ParserContext ctx = new ParserContext();
    ctx.setStrongTyping(true);
    ctx.addInput("foo",
            Foo.class);

    assertEquals(true,
            executeExpression(new ExpressionCompiler("foo.bar.woof == 'true'").compile(ctx),
                    createTestMap()));
  }

  public void testSetCoercion() {
    Serializable s = compileSetExpression("name");

    Foo foo = new Foo();
    executeSetExpression(s,
            foo,
            12);
    assertEquals("12",
            foo.getName());

    foo = new Foo();
    setProperty(foo,
            "name",
            12);
    assertEquals("12",
            foo.getName());
  }

  public void testSetCoercion2() {
    ParserContext ctx = new ParserContext();
    ctx.setStrongTyping(true);
    ctx.addInput("sampleBean",
            SampleBean.class);

    Serializable s = compileSetExpression("sampleBean.map2['bleh']",
            ctx);

    Foo foo = new Foo();
    executeSetExpression(s,
            foo,
            "12");

    assertEquals(12,
            foo.getSampleBean().getMap2().get("bleh").intValue());

    foo = new Foo();
    executeSetExpression(s,
            foo,
            "13");

    assertEquals(13,
            foo.getSampleBean().getMap2().get("bleh").intValue());

    OptimizerFactory.setDefaultOptimizer("ASM");

    ctx = new ParserContext();
    ctx.setStrongTyping(true);
    ctx.addInput("sampleBean",
            SampleBean.class);

    s = compileSetExpression("sampleBean.map2['bleh']",
            ctx);

    foo = new Foo();
    executeSetExpression(s,
            foo,
            "12");

    assertEquals(12,
            foo.getSampleBean().getMap2().get("bleh").intValue());

    executeSetExpression(s,
            foo,
            new Integer(12));

    assertEquals(12,
            foo.getSampleBean().getMap2().get("bleh").intValue());
  }

  public void testListCoercion() {
    ParserContext ctx = new ParserContext();
    ctx.setStrongTyping(true);
    ctx.addInput("bar",
            Bar.class);

    Serializable s = compileSetExpression("bar.testList[0]",
            ctx);

    Foo foo = new Foo();
    foo.getBar().getTestList().add(new Integer(-1));

    executeSetExpression(s,
            foo,
            "12");

    assertEquals(12,
            foo.getBar().getTestList().get(0).intValue());

    foo = new Foo();
    foo.getBar().getTestList().add(new Integer(-1));

    executeSetExpression(s,
            foo,
            "13");

    assertEquals(13,
            foo.getBar().getTestList().get(0).intValue());

    OptimizerFactory.setDefaultOptimizer("ASM");

    ctx = new ParserContext();
    ctx.setStrongTyping(true);
    ctx.addInput("bar",
            Bar.class);

    s = compileSetExpression("bar.testList[0]",
            ctx);

    foo = new Foo();
    foo.getBar().getTestList().add(new Integer(-1));

    executeSetExpression(s,
            foo,
            "12");

    assertEquals(12,
            foo.getBar().getTestList().get(0).intValue());

    executeSetExpression(s,
            foo,
            "13");

    assertEquals(13,
            foo.getBar().getTestList().get(0).intValue());
  }

  public void testFieldCoercion1() {
    ParserContext ctx = new ParserContext();
    ctx.setStrongTyping(true);
    ctx.addInput("bar",
            Bar.class);

    Serializable s = compileSetExpression("bar.assignTest",
            ctx);

    Foo foo = new Foo();

    executeSetExpression(s,
            foo,
            12);

    assertEquals("12",
            foo.getBar().getAssignTest());

    foo = new Foo();

    executeSetExpression(s,
            foo,
            13);

    assertEquals("13",
            foo.getBar().getAssignTest());

    OptimizerFactory.setDefaultOptimizer("ASM");

    ctx = new ParserContext();
    ctx.setStrongTyping(true);
    ctx.addInput("bar",
            Bar.class);

    s = compileSetExpression("bar.assignTest",
            ctx);

    foo = new Foo();

    executeSetExpression(s,
            foo,
            12);

    assertEquals("12",
            foo.getBar().getAssignTest());

    executeSetExpression(s,
            foo,
            13);

    assertEquals("13",
            foo.getBar().getAssignTest());
  }


  public void testStaticTyping2() {
    String exp = "int x = 5; int y = 2; new int[] { x, y }";
    int[] res = (int[]) MVEL.eval(exp, new HashMap());

    assertEquals(5, res[0]);
    assertEquals(2, res[1]);
  }

  public void testMVEL190a() {
    Serializable compiled = MVEL.compileExpression("a.toString()", ParserContext.create().stronglyTyped().withInput("a", String.class));
  }

  public void testPrimitiveTypes() {
    ParserContext ctx = new ParserContext();
    ctx.setStrongTyping(true);
    ctx.addInput("base",
            Base.class);

    Serializable s = compileExpression("int x = 5; x = x + base.intValue; x",
            ctx);

    Map vars = new HashMap();
    vars.put("base",
            new Base());

    Number x = (Number) executeExpression(s,
            vars);

    assertEquals(15,
            x.intValue());

  }

  public void testAutoBoxing() {
    ParserContext ctx = new ParserContext();
    ctx.setStrongTyping(true);
    //ctx.addInput("base", Base.class);

    Serializable s = compileExpression("(list = new java.util.ArrayList()).add( 5 ); list",
            ctx);

    Map vars = new HashMap();
    //vars.put("base", new Base());

    List list = (List) executeExpression(s,
            vars);

    assertEquals(1,
            list.size());

  }

  public void testAutoBoxing2() {
    ParserContext ctx = new ParserContext();
    ctx.setStrongTyping(true);
    ctx.addInput("base",
            Base.class);

    Serializable s = compileExpression("java.util.List list = new java.util.ArrayList(); " +
            "list.add( base.intValue ); list",
            ctx);

    Map vars = new HashMap();
    vars.put("base",
            new Base());

    List list = (List) executeExpression(s,
            vars);

    assertEquals(1,
            list.size());
  }

  public void testTypeCoercion() {
    OptimizerFactory.setDefaultOptimizer("ASM");
    ParserContext ctx = new ParserContext();
    ctx.setStrongTyping(true);
    ctx.addInput("base",
            Base.class);

    Serializable s = compileExpression("java.math.BigInteger x = new java.math.BigInteger( \"5\" );" +
            " x + base.intValue;",
            ctx);

    Map vars = new HashMap();
    vars.put("base",
            new Base());

    Number x = (Number) executeExpression(s,
            vars);

    assertEquals(15,
            x.intValue());
  }

  public void testTypeCoercion2() {
    OptimizerFactory.setDefaultOptimizer("reflective");
    ParserContext ctx = new ParserContext();
    ctx.setStrongTyping(true);
    ctx.addInput("base",
            Base.class);

    Serializable s = compileExpression("java.math.BigInteger x = new java.math.BigInteger( \"5\" );" +
            " x + base.intValue;",
            ctx);

    Map vars = new HashMap();
    vars.put("base",
            new Base());

    Number x = (Number) executeExpression(s,
            vars);

    assertEquals(15,
            x.intValue());
  }

  public void testStaticFieldAccessForInputs() {
    MVEL.COMPILER_OPT_ALLOW_NAKED_METH_CALL = true;
    ParserContext pCtx = ParserContext.create();
    MVEL.analysisCompile("java.math.BigDecimal.TEN", pCtx);

    assertFalse(pCtx.getInputs().containsKey("java"));

    assertEquals(0,
            pCtx.getInputs().size());

//        MVEL.COMPILER_OPT_ALLOW_NAKED_METH_CALL = true;
//
//        pCtx = ParserContext.create();
//        MVEL.analysisCompile("java.math.BigDecimal.TEN", pCtx);
//
//        assertFalse(pCtx.getInputs().containsKey("java"));
//
//        assertEquals(0,
//                pCtx.getInputs().size());
  }


  public void testStaticFieldAccessForInputsWithStrictStrong() {
    ParserContext pCtx = ParserContext.create();
    pCtx.setStrictTypeEnforcement(true);
    pCtx.setStrongTyping(true);
    MVEL.analysisCompile("java.math.BigDecimal.TEN", pCtx);

    assertFalse(pCtx.getInputs().containsKey("java"));

    assertEquals(0,
            pCtx.getInputs().size());

    MVEL.COMPILER_OPT_ALLOW_NAKED_METH_CALL = true;
    pCtx = ParserContext.create();
    pCtx.setStrictTypeEnforcement(true);
    pCtx.setStrongTyping(true);
    MVEL.analysisCompile("java.math.BigDecimal.TEN", pCtx);

    assertFalse(pCtx.getInputs().containsKey("java"));

    assertEquals(0,
            pCtx.getInputs().size());
  }


  public void testStaticMethodsInInputsBug() {
    String text = " getList( java.util.Formatter )";

    ParserConfiguration pconf = new ParserConfiguration();
    for (Method m : CoreConfidenceTests.StaticMethods.class.getMethods()) {
      if (Modifier.isStatic(m.getModifiers())) {
        pconf.addImport(m.getName(), m);

      }
    }
    ParserContext pctx = new ParserContext(pconf);
    pctx.setStrictTypeEnforcement(false);
    pctx.setStrongTyping(false);

    Map<String, Object> vars = new HashMap<String, Object>();

    Serializable expr = MVEL.compileExpression(text, pctx);
    List list = (List) MVEL.executeExpression(expr, null, vars);
    assertEquals(Formatter.class, list.get(0));

    assertEquals(0, pctx.getInputs().size());
  }


  public static class EchoContext {
    public String echo(String str) {
      return str;
    }
  }

  public void testStaticMethodCallThrowsException() {
    String text = " ( throwException( ) ) ";

    ParserConfiguration pconf = new ParserConfiguration();
    for (Method m : CoreConfidenceTests.StaticMethods.class.getMethods()) {
      if (Modifier.isStatic(m.getModifiers())) {
        pconf.addImport(m.getName(), m);

      }
    }
    ParserContext pctx = new ParserContext(pconf);
    pctx.setStrictTypeEnforcement(true);
    pctx.setStrongTyping(true);

    Map<String, Object> vars = new HashMap<String, Object>();
    Serializable expr = MVEL.compileExpression(text, pctx);
    try {
      MVEL.executeExpression(expr);
      fail("this should throw an exception");
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void testContextMethodCallsInStrongMode() {
    ParserContext context = new ParserContext();
    context.setStrongTyping(true);
    context.addInput("this",
            EchoContext.class);

    ExecutableStatement stmt = (ExecutableStatement) MVEL.compileExpression("this.echo( 'Mac')", context);
    stmt = (ExecutableStatement) MVEL.compileExpression("echo( 'Mac')", context);

    assertEquals("Mac", MVEL.executeExpression(stmt, new EchoContext()));
  }

  public void testForLoopTypeCoercion() {
    ParserContext pCtx = ParserContext.create();
    pCtx.setStrongTyping(true);
    pCtx.addInput("$type", String.class);
    pCtx.addInput("l", List.class);

    Map<String, Object> vars = new HashMap<String, Object>();
    vars.put("$type", "pc!!");
    List list = new ArrayList();
    vars.put("l", list);
    ExecutableStatement stmt = (ExecutableStatement) MVEL.compileExpression("for (byte bt:$type.getBytes()) {l.add( bt);}", pCtx);
    MVEL.executeExpression(stmt, null, vars);

    byte[] exp = "pc!!".getBytes();
    //  byte[] res = new byte[list.size()];

    for (int i = 0; i < exp.length; i++) {
      assertEquals(exp[i], list.get(i));
    }
  }

  public void testGetCorrectInputs() {
    String str = "total = total + $cheese.price";

    ParserConfiguration pconf = new ParserConfiguration();

    ParserContext pctx = new ParserContext(pconf);
    pctx.setStrongTyping(true);
    pctx.addInput("total", int.class);
    pctx.addInput("$cheese", Cheese.class);

    ExecutableStatement stmt = (ExecutableStatement) MVEL.compileExpression(str, pctx);
    assertTrue("Should not contain" + pctx.getVariables(), pctx.getVariables().isEmpty());
  }

  public void testTypeCalculation() {
    ParserContext ctx = ParserContext.create().stronglyTyped();
    ctx.addInput("foo", Foo.class);

    Class cls = MVEL.analyze("foo.bar.testList.get(0)", ctx);

    assertTrue(Integer.class.isAssignableFrom(cls));
  }

  public static class AGenericTestClass {
    private Map<String, String> map;

    /**
     * @return the map
     */
    public Map<String, String> getMap() {
      return map;
    }

    /**
     * @param map the map to set
     */
    public void setMap(Map<String, String> map) {
      this.map = map;
    }
  }


  public void testGenericMethods() {
    String str = "Integer.parseInt( a.getMap().get(\"x\") )";

    ParserConfiguration pconf = new ParserConfiguration();
    ParserContext pctx = new ParserContext(pconf);
    pctx.setStrongTyping(true);
    pctx.addInput("a", AGenericTestClass.class);

    ExecutableStatement stmt = (ExecutableStatement) MVEL.compileExpression(str, pctx);


    AGenericTestClass a = new AGenericTestClass();
    a.setMap(new HashMap<String, String>());
    a.getMap().put("x", "10");
    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("a", a);
    Number result = (Number) MVEL.executeExpression(stmt, null, variables);
    assertEquals(10, result.intValue());
  }

  public static class Address {
    private String street;

    public Address(String street) {
      this.street = street;
    }

    public String getStreet() {
      return street;
    }

    public void setStreet(String street) {
      this.street = street;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((street == null) ? 0 : street.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      Address other = (Address) obj;
      if (street == null) {
        if (other.street != null) return false;
      } else if (!street.equals(other.street)) return false;
      return true;
    }

  }

  public static class PersonAddresses {
    private List<Address> addresses = new ArrayList<Address>();

    public List<Address> getAddresses() {
      return addresses;
    }

    public void setAddresses(List<Address> addresses) {
      this.addresses = addresses;
    }
  }

  public void testGenerics1() {
    String str = "addresses[0] == new Address(\"s1\") && addresses[0].street == new Address(\"s1\").street";

    ParserConfiguration pconf = new ParserConfiguration();
    ParserContext pctx = new ParserContext(pconf);
    pctx.setStrongTyping(true);
    pctx.addInput("this", PersonAddresses.class);
    pctx.addImport(Address.class);
    pctx.addImport(PersonAddresses.class);
    ExecutableStatement stmt = (ExecutableStatement) MVEL.compileExpression(str, pctx);
    PersonAddresses ctx = new PersonAddresses();
    ctx.getAddresses().add(new Address("s1"));
    Boolean result = (Boolean) MVEL.executeExpression(stmt, ctx);
    assertTrue(result);
  }

  private static final String ASSIGNMENT_LEFT_EXPRESSION = "payload.details";
  private static final String VARIABLE = "VARIABLE";
  private static final String ASSIGNMENT_EXPRESSION = ASSIGNMENT_LEFT_EXPRESSION + "=" + VARIABLE;
  private static final String SETTER_EXPRESSION = "payload.setDetails(" + VARIABLE + ")";

  public void testCacheCreatesNewCompiledExpressionInstances() throws Exception {
    Map<String, Object> vars = new HashMap();
    vars.put(VARIABLE, new FooDetails());
    ExecutableAccessor compiledExpr = (ExecutableAccessor) MVEL.compileExpression(ASSIGNMENT_EXPRESSION, new ParserContext(new ParserConfiguration()));
    MVEL.executeExpression(compiledExpr, new Message(new Foo2()), vars);

    BarDetails barDetails = new BarDetails();
    vars.put(VARIABLE, barDetails);
    Object result = MVEL.executeExpression(compiledExpr, new Message(new Bar2()), vars);
    assertEquals(barDetails, result);
  }

  public void testCacheCreatesNewCompiledExpressionInstancesSetter() throws Exception {
    Map<String, Object> vars = new HashMap();
    vars.put(VARIABLE, new FooDetails());
    ExecutableAccessor compiledExpr = (ExecutableAccessor) MVEL.compileExpression(SETTER_EXPRESSION, new ParserContext(new ParserConfiguration()));
    MVEL.executeExpression(compiledExpr, new Message(new Foo2()), vars);

    BarDetails barDetails = new BarDetails();
    vars.put(VARIABLE, barDetails);
    Bar2 payload = new Bar2();
    assertNull(payload.getDetails());
    Object result = MVEL.executeExpression(compiledExpr, new Message(payload), vars);
    assertNull(result);
    assertEquals(barDetails, payload.getDetails());
  }


  public void testParametersWithNull() throws Exception {
    Map<String, Object> vars = new HashMap();
    vars.put(VARIABLE, new FooDetails());
    ExecutableAccessor compiledExpr = (ExecutableAccessor) MVEL.compileExpression(SETTER_EXPRESSION, new ParserContext(new ParserConfiguration()));
    MVEL.executeExpression(compiledExpr, new Message(new Foo2()), vars);
    vars.put(VARIABLE, null);
    Bar2 payload = new Bar2();
    assertNull(payload.getDetails());
    Object result = MVEL.executeExpression(compiledExpr, new Message(payload), vars);
    assertNull(result);
    assertNull(payload.getDetails());
  }
  public void testCompiledAccExpressionSerialization() throws IOException, ClassNotFoundException {
    Map<String, Object> vars = new HashMap();
    vars.put(VARIABLE, new FooDetails());

    ParserContext pCtx = new ParserContext(new ParserConfiguration());
    CompiledAccExpression compiledAccExpression = new CompiledAccExpression(ASSIGNMENT_LEFT_EXPRESSION.toCharArray(), Object.class, pCtx);
    assertNull(compiledAccExpression.getAccessor());

    Message msg = new Message(new Foo2());
    compiledAccExpression.setValue(msg, msg, new CachedMapVariableResolverFactory(vars), new FooDetails());
    assertNotNull(compiledAccExpression.getAccessor());

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    new ObjectOutputStream(baos).writeObject(compiledAccExpression);
    CompiledAccExpression readCompiledAccExpression = (CompiledAccExpression) new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray())).readObject();
    assertNull(readCompiledAccExpression.getAccessor());
  }

  public static class Message {
    public Object payload;

    public Message(Object payload) {
      this.payload = payload;
    }
  }

  public static class Foo2 {

    public FooDetails details;

    public FooDetails getDetails() {
      return details;
    }

    public void setDetails(FooDetails details) {
      this.details = details;
    }
  }

  public static class FooDetails {

  }

  public static class Bar2 {

    public BarDetails details;

    public BarDetails getDetails() {
      return details;
    }

    public void setDetails(BarDetails details) {
      this.details = details;
    }
  }

  public static class BarDetails {

  }
}
