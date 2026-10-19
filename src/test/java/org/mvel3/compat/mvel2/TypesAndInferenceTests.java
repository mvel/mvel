package org.mvel3.compat.mvel2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mvel3.MVEL;
import org.mvel3.Type;
import org.mvel3.compat.mvel2.res.Bar;
import org.mvel3.compat.mvel2.res.Base;
import org.mvel3.compat.mvel2.res.Cheese;
import org.mvel3.compat.mvel2.res.Foo;
import org.mvel3.compat.mvel2.res.MyInterface2;
import org.mvel3.compat.mvel2.res.MyInterface3;
import org.mvel3.compat.mvel2.res.SampleBean;

import static org.mvel3.compat.mvel2.Mvel3TestSupport.compileExpression;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.compileSetExpression;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.eval;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.executeExpression;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.executeSetExpression;

/**
 * Imported from org.mvel2.tests.core.TypesAndInferenceTests (mvel2 branch).
 *
 * <p>Expressions, input values, expected values, and assertions are retained
 * unchanged where MVEL3 has an equivalent. Failures are intentionally visible;
 * known behavioural differences are annotated with {@link KnownCompatibility}.
 *
 * <p>Not imported, because they assert on the MVEL2 compiler's analysis results
 * ({@code ParserContext.getInputs()/getVariables()/getVarOrInputType()},
 * {@code getKnownEgressType()}, {@code MVEL.analysisCompile/analyze}), which have no MVEL3 equivalent:
 * {@code testGenericInference2}, {@code testVarInputs}, {@code testVarInputs2..5},
 * {@code testDetermineRequiredInputsInConstructor}, {@code testAnalyzer}, {@code testAnalysisCompile},
 * {@code testMultiVarDeclr}, {@code testVarDeclr}, {@code testMultiTypeVarDeclr},
 * {@code testMultiTypeVarDeclr2}, {@code testTypeVarDeclr}, {@code testStrictTypingCompilation4},
 * {@code testEgressType}, {@code testEgressTypeCorrect}, {@code testParameterizedTypeInStrictMode2..4},
 * {@code testReturnType1}, {@code testReturnType2}, {@code testEgressType1},
 * {@code testStaticFieldAccessForInputs}, {@code testStaticFieldAccessForInputsWithStrictStrong},
 * {@code testGetCorrectInputs}, {@code testTypeCalculation}, {@code testOptionalOfGenerics}.
 * Also not imported (MVEL2-only APIs): {@code testStrictTypingCompilation2}
 * ({@code MethodStub}, {@code StaticMethodImportResolverFactory}), {@code testDataConverterStrictMode}
 * ({@code DataConversion}), {@code testStaticMethodsInInputsBug} and
 * {@code testStaticMethodCallThrowsException} (static {@code Method} imports through
 * {@code ParserConfiguration}), and the five {@code testBigDecimal*} / {@code testCollection*}
 * {@code SimpleVariableSpaceModel} tests (indexed variable resolvers).
 *
 * <p>Adaptations: {@code setStrongTyping}/{@code setStrictTypeEnforcement} calls are removed (MVEL3 has no
 * such modes); optimizer selection is removed. {@code testGenericInference} and {@code testSetCoercion}
 * keep only the execution part (the analysis assertion and {@code MVEL.setProperty} have no MVEL3 equivalent).
 * The adapter's {@code compileExpression} is lazy and does not compile, so the tests that only compile an
 * expression (or expect a compile-time exception) use {@link #compileEagerly}, which compiles at once.
 * Input generics (for example {@code List<String>}) cannot be declared through {@code TestCompilerContext}.
 */
@Tag("mvel2-compatibility")
public class TypesAndInferenceTests extends AbstractTest {

  /** Compiles at once (the adapter's compileExpression only creates a lazy carrier). */
  private static void compileEagerly(final String expression, final TestCompilerContext ctx) {
    final Map<String, Type<?>> types = new LinkedHashMap<>();
    ctx.getInputs().forEach((name, type) -> types.put(name, Type.type(type)));
    final Set<String> imports = new LinkedHashSet<>();
    ctx.getImports().forEach(type -> imports.add(type.getCanonicalName()));
    if (expression.indexOf(';') > 0) {
      new MVEL().compileMapBlock(expression, Object.class, imports, types);
    } else {
      new MVEL().compileMapExpression(expression, Object.class, imports, types);
    }
  }

  public static class Person {
    private String name;
    private int age;
    private String likes;
    private List<Foo> footributes;
    private Map<String, Foo> maptributes;
    private Map<Object, Foo> objectKeyMaptributes;

    public Person() {
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public int getAge() {
      return age;
    }

    public void setAge(int age) {
      this.age = age;
    }

    public List<Foo> getFootributes() {
      return footributes;
    }

    public void setFootributes(List<Foo> footributes) {
      this.footributes = footributes;
    }

    public Map<String, Foo> getMaptributes() {
      return maptributes;
    }

    public void setMaptributes(Map<String, Foo> maptributes) {
      this.maptributes = maptributes;
    }

    public Map<Object, Foo> getObjectKeyMaptributes() {
      return objectKeyMaptributes;
    }

    public void setObjectKeyMaptributes(Map<Object, Foo> objectKeyMaptributes) {
      this.objectKeyMaptributes = objectKeyMaptributes;
    }
  }

  @Test
  @KnownCompatibility("block-syntax")
  public void testGenericInference() {
    String expression = "$result = person.footributes[0].name";

    // MVEL.analysisCompile(..) and the assertion on ctx.getVarOrInputTypeOrNull("$result") have no MVEL3 equivalent.
    TestCompilerContext ctx = TestCompilerContext.create().withInput("person", Person.class);

    Serializable s = compileExpression(expression, ctx);

    Map<String, Object> vars = new HashMap<String, Object>();
    Person p = new Person();
    p.setFootributes(new ArrayList<Foo>());
    p.getFootributes().add(new Foo());

    vars.put("person", p);

    assertEquals("dog", executeExpression(s, vars));
  }

  // Category not isolated: observed error is "missing return statement" (block-syntax); with a trailing return it
  // compiles. The assertions on vars.get("a"/"b"/"c") (locals written back to the variable map) were not tested
  // separately.
  @Test
  @KnownCompatibility("block-syntax")
  public void testMultiTypeVarDeclr3() {
    String ex = "int a = 52 * 3, b = 8, c = 16;";
    // The ParserContext.getVariables() assertions are removed (MVEL2 analysis API).
    Serializable s = compileExpression(ex);

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

  // Category not isolated: observed error is a parse error from map2[ 'yyy' ] (single-quote-string). With double quotes
  // and "this" as the root it fails with "Unsolved symbol map2" (this-reference); a named variable (s.map2["yyy"])
  // works.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testEgressTypeCorrect2() {
    TestCompilerContext context = new TestCompilerContext();
    context.addInput("this",
        SampleBean.class);
    Serializable stmt = compileExpression("( map2[ 'yyy' ] )", context);

    SampleBean s = new SampleBean();
    s.getMap2().put("yyy", 1);

    assertEquals(new Integer(1),
        executeExpression(stmt, s));
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

  @Test
  public final void testDetermineEgressParametricType() {
    final TestCompilerContext parserContext = new TestCompilerContext();
    parserContext.addInput("strings",
        List.class);

    final Serializable expr = compileExpression("strings", parserContext);

    assertTrue(STRINGS.equals(executeExpression(expr,
        new A())));
  }

  @Test
  public final void testDetermineEgressParametricType2() {
    final TestCompilerContext parserContext = new TestCompilerContext();
    parserContext.addInput("strings",
        List.class);

    final Serializable expr = compileExpression("strings",
        parserContext);

    assertTrue(STRINGS.equals(executeExpression(expr,
        new A())));
  }

  @Test
  @KnownCompatibility("void-method-expression")
  public void testJIRA151c() {
    // OptimizerFactory.setDefaultOptimizer(SAFE_REFLECTIVE) removed (MVEL2 optimizer)
    A b = new B();
    A c = new C();

    Object expression = compileExpression("a.foo(value)");

    for (int i = 0; i < 100; i++) {
      System.out.println("i: " + i);
      System.out.flush();

      {
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("a", b);
        variables.put("value", 123);
        executeExpression((Serializable) expression, variables);
      }
      {
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("a", c);
        variables.put("value", 123);
        executeExpression((Serializable) expression, variables);
      }

    }
  }

  @Test
  @KnownCompatibility("void-method-expression")
  public void testJIRA151d() {
    // OptimizerFactory.setDefaultOptimizer("ASM") removed (MVEL2 optimizer)
    A b = new B();
    A c = new C();

    Object expression = compileExpression("a.foo(value)");

    for (int i = 0; i < 100; i++) {
      System.out.println("i: " + i);
      System.out.flush();

      {
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("a", b);
        variables.put("value", 123);
        executeExpression((Serializable) expression, variables);
      }
      {
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("a", c);
        variables.put("value", 123);
        executeExpression((Serializable) expression, variables);
      }

    }
  }

  @Test
  @KnownCompatibility("void-method-expression")
  public void testJIRA165b() {
    // OptimizerFactory.setDefaultOptimizer("ASM") removed (MVEL2 optimizer)
    A b = new B();
    A a = new A();
    Object expression = compileExpression("a.bar(value)");

    for (int i = 0; i < 100; i++) {
      System.out.println("i: " + i);
      System.out.flush();

      {
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("a", b);
        variables.put("value", 123);
        executeExpression((Serializable) expression, variables);
      }
      {
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("a", a);
        variables.put("value", 123);
        executeExpression((Serializable) expression, variables);
      }
    }

  }

  @Test
  @KnownCompatibility("void-method-expression")
  public void testJIRA165() {
    // OptimizerFactory.setDefaultOptimizer(SAFE_REFLECTIVE) removed (MVEL2 optimizer)
    A b = new B();
    A a = new A();
    Object expression = compileExpression("a.bar(value)");
    for (int i = 0; i < 100; i++) {
      System.out.println("i: " + i);
      System.out.flush();

      {
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("a", b);
        variables.put("value", 123);
        executeExpression((Serializable) expression, variables);
      }
      {
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("a", a);
        variables.put("value", 123);
        executeExpression((Serializable) expression, variables);
      }
    }
  }

  @Test
  @KnownCompatibility("block-syntax")
  public void testStrictTypingCompilationWithVarInsideConstructor() {
    TestCompilerContext ctx = new TestCompilerContext();
    ctx.addInput("$likes", String.class);
    ctx.addInput("results", List.class);
    ctx.addImport(Cheese.class);

    Serializable expr = null;
    try {
      expr = compileExpression("Cheese c = new Cheese( $likes, 15 );\nresults.add( c ); ", ctx);
    }
    catch (RuntimeException e) {
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

  @Test
  public void testStrongTyping3() {
    TestCompilerContext ctx = new TestCompilerContext();

    try {
      compileEagerly("foo.toUC(100.5", ctx);
    }
    catch (Exception e) {
      // should fail.
      return;
    }

    assertTrue(false);
  }

  @Test
  public void testStrictStaticMethodCall() {
    TestCompilerContext ctx = new TestCompilerContext();
    // ctx.addImport("Bar", Bar.class): the simple-name alias form has no TestCompilerContext equivalent.
    ctx.addImport(Bar.class);

    Serializable s = compileExpression("Bar.staticMethod()", ctx);

    assertEquals(1,
        executeExpression(s));
  }

  @Test
  @KnownCompatibility("block-syntax")
  public void testStrictTypingCompilation3() {
    TestCompilerContext ctx = new TestCompilerContext();

    // DefaultLocalVariableResolverFactory is replaced by an empty variable map.
    assertEquals(7,
        executeExpression(compileExpression("message='Hello';b=7;\nSystem.out.println(message + ';' + b);\n"
            + "System.out.println(message + ';' + b); b", ctx),
            new HashMap()));
  }

  @Test
  public void testStrictStrongTypingCompilationErrors1() throws Exception {
    TestCompilerContext ctx = new TestCompilerContext();
    ctx.addImport(Foo.class);
    ctx.addInput("$bar", Bar.class);

    try {
      compileEagerly("System.out.println( $ba );", ctx);
      fail("This should not compileShared");
    }
    catch (Exception e) {
    }
  }

  @Test
  public void testStrictStrongTypingCompilationErrors2() throws Exception {
    TestCompilerContext ctx = new TestCompilerContext();
    ctx.addImport(Foo.class);
    ctx.addInput("$bar", Bar.class);

    try {
      compileEagerly("x_a = new Foo244( $ba ); x_a.equals($ba);", ctx);
      fail("This should not compileShared");
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testProvidedExternalTypes() {
    TestCompilerContext ctx = new TestCompilerContext();
    ctx.addInput("foo",
        Foo.class);

    compileEagerly("foo.bar", ctx);
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

  @Test
  public void testMVEL228() {
    TestCompilerContext ctx = new TestCompilerContext();
    HashMap<String, Class> params = new HashMap<String, Class>();
    params.put("helper", ScriptHelper228.class);
    params.put("person", Person228.class);

    params.forEach(ctx::addInput);

    String script = "helper.methodB(2);\n" +
        "person.getName2();";
    try {
      compileEagerly(script, ctx);
    }
    catch (Exception e) {
      return;
    }

    fail("Should have thrown an exception");
  }

  @Test
  public void testMVEL232() {
    TestCompilerContext ctx = new TestCompilerContext();

    String script = "for(int i=0;i<2;i++) { " +
        "  System.out.println(i+\"\");" +
        "} " +
        " return true;";

    try {
      Serializable compiled = compileExpression(script, ctx);
      HashMap<String, Object> map = new HashMap<String, Object>();
      executeExpression(compiled, map);
    }
    catch (Exception e) {
      e.printStackTrace();
      fail("should now throw an exception");
    }
  }

  @Test
  @KnownCompatibility("reserved-method-name")
  public void testMVEL234() {
    StringBuffer buffer = new StringBuffer();

    buffer.append("import java.text.SimpleDateFormat;");
    buffer.append("if (\"test\".matches(\"[0-9]\")) {");
    buffer.append("  return false;");
    buffer.append("}else{");
    buffer.append("  SimpleDateFormat sqf = new SimpleDateFormat(\"yyyyMMdd\");");
    buffer.append("}");

    TestCompilerContext ctx = new TestCompilerContext();

    try {
      compileEagerly(buffer.toString(), ctx);
    }
    catch (Exception e) {
      fail(e.getMessage());
    }
  }

  // Category not isolated: observed error is "missing return statement" (the if-block has no return on the else path).
  // Not probed with a trailing return added.
  @Test
  @KnownCompatibility("block-syntax")
  public void testMVEL235() {
    StringBuffer buffer = new StringBuffer();

    buffer.append("if(var1.equals(var2)) {");
    buffer.append("return true;");
    buffer.append("}");

    TestCompilerContext ctx = new TestCompilerContext();
    ctx.addInput("var1", MyInterface2.class);
    ctx.addInput("var2", MyInterface2.class);

    try {
      compileEagerly(buffer.toString(), ctx);
    }
    catch (Exception e) {
      fail(e.getMessage());
    }
  }

  // Category not isolated: observed error is "missing return statement" (the block only declares a variable). Not
  // probed with a trailing return added.
  @Test
  @KnownCompatibility("block-syntax")
  public void testMVEL236() {
    StringBuffer buffer = new StringBuffer();

    buffer.append("MyInterface2 var2 = (MyInterface2)var1;");

    TestCompilerContext ctx = new TestCompilerContext();
    ctx.addInput("var1", MyInterface3.class);
    ctx.addImport(MyInterface2.class);
    ctx.addImport(MyInterface3.class);

    try {
      compileEagerly(buffer.toString(), ctx);
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

  @Test
  public void testMapPropertyAccess() {
    TestCompilerContext ctx = new TestCompilerContext();
    ctx.addImport(MapWrapper.class);
    ctx.addInput("wrapper", MapWrapper.class);

    Serializable expr = compileExpression("wrapper.map[\"key\"]", ctx);

    MapWrapper wrapper = new MapWrapper();
    wrapper.getMap().put("key", "value");
    Map vars = new HashMap();
    vars.put("wrapper", wrapper);

    assertEquals("value", executeExpression(expr, vars));
  }

  @Test
  public void testTypeCast3() {
    Map map = new HashMap();
    map.put("foo",
        new Foo());

    TestCompilerContext pCtx = new TestCompilerContext();
    pCtx.addInput("foo",
        Foo.class);

    Serializable s = compileExpression("((org.mvel3.compat.mvel2.res.Bar) foo.getBar()).name != null",
        pCtx);

    assertEquals(true,
        executeExpression(s,
            map));

    // The pCtx.getInputs() assertions are removed (MVEL2 analysis API).
  }

  // Category not isolated: the result is the HashMap instead of a Boolean. The single-quoted 'KEY1' is read as a char,
  // but the test also passes a Map as the root, which MVEL3 merges into the inputs. With double quotes and the same
  // root the expression returns true, so the single-quote literal is the visible cause.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testMapWithStrictTyping() {
    TestCompilerContext ctx = new TestCompilerContext();
    ctx.addInput("$msg",
        String.class);
    ctx.addInput("map",
        Map.class);
    Serializable expr = compileExpression("map['KEY1'] == $msg", ctx);

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

  @Test
  @KnownCompatibility("this-reference")
  public void testMapAsContextWithStrictTyping() {
    TestCompilerContext ctx = new TestCompilerContext();
    ctx.addInput("$msg",
        String.class);
    ctx.addInput("this",
        Map.class);
    Serializable expr = compileExpression("this['KEY1'] == $msg", ctx);

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

  @Test
  public void testStrongTyping() {
    TestCompilerContext ctx = new TestCompilerContext();

    try {
      compileEagerly("blah", ctx);
    }
    catch (Exception e) {
      // should fail
      return;
    }

    assertTrue(false);
  }

  @Test
  public void testStrongTyping2() {
    TestCompilerContext ctx = new TestCompilerContext();

    ctx.addInput("blah",
        String.class);

    try {
      compileEagerly("1-blah", ctx);
    }
    catch (Exception e) {
      e.printStackTrace();
      return;
    }

    assertTrue(false);
  }

  // Category not isolated: observed error is a JavaParser null-node NPE from 'bar' (single-quote-string). With double
  // quotes it fails with "cannot find symbol toUpperCase() on Object" because TestCompilerContext cannot declare
  // HashMap<String,String> (dynamic-property-type); a cast works.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testParameterizedTypeInStrictMode() {
    TestCompilerContext ctx = new TestCompilerContext();
    // The type arguments {String.class, String.class} of the input cannot be declared through TestCompilerContext.
    ctx.addInput("foo",
        HashMap.class);
    compileEagerly("foo.get('bar').toUpperCase()", ctx);
  }

  @Test
  @KnownCompatibility("single-quote-string")
  public void testSetAccessorOverloadedEqualsStrictMode2() {
    TestCompilerContext ctx = new TestCompilerContext();
    ctx.addInput("foo",
        Foo.class);

    try {
      compileEagerly("foo.aValue = 'bar'", ctx);
    }
    catch (RuntimeException e) {
      assertTrue(false);
    }
  }

  // Category not isolated: observed error is "Unsolved symbol in ue" from the single-quoted 'true'
  // (single-quote-string). With double quotes the comparison foo.bar.woof == "true" returns false instead of true
  // (string-coercion); with a boolean literal it returns true.
  @Test
  @KnownCompatibility("string-coercion")
  public void testCompileTimeCoercion() {
    TestCompilerContext ctx = new TestCompilerContext();
    ctx.addInput("foo",
        Foo.class);

    assertEquals(true,
        executeExpression(compileExpression("foo.bar.woof == 'true'", ctx),
            createTestMap()));
  }

  // Category not isolated: fails with an NPE ("method" is null) through the adapter's POJO-root path. Map-input
  // equivalents of the same assignments pass. Whether the cause is the adapter emulation or MVEL3 was not isolated.
  @Test
  @KnownCompatibility("set-expression-api")
  public void testSetCoercion() {
    Serializable s = compileSetExpression("name", new TestCompilerContext());

    Foo foo = new Foo();
    executeSetExpression(s,
        foo,
        12);
    assertEquals("12",
        foo.getName());

    // setProperty(foo, "name", 12) (MVEL.setProperty) has no MVEL3 equivalent and is removed.
  }

  // Category not isolated: fails with an NPE because Map.get returns null while the setter is generated
  // (Integer.intValue()). Whether the String to Integer coercion itself works was not isolated.
  @Test
  @KnownCompatibility("set-expression-api")
  public void testSetCoercion2() {
    TestCompilerContext ctx = new TestCompilerContext();
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

    // MVEL2 optimizer selection (OptimizerFactory.setDefaultOptimizer("ASM")) has no MVEL3 equivalent.

    ctx = new TestCompilerContext();
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

  // Category not isolated: fails with an NPE ("method" is null) through the adapter's POJO-root path. Map-input
  // equivalents pass; the list element String to Integer coercion was not isolated.
  @Test
  @KnownCompatibility("set-expression-api")
  public void testListCoercion() {
    TestCompilerContext ctx = new TestCompilerContext();
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

    // MVEL2 optimizer selection (OptimizerFactory.setDefaultOptimizer("ASM")) has no MVEL3 equivalent.

    ctx = new TestCompilerContext();
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

  // Category not isolated: fails with an NPE ("method" is null) through the adapter's POJO-root path. Map-input
  // equivalents pass; the field coercion itself was not isolated.
  @Test
  @KnownCompatibility("set-expression-api")
  public void testFieldCoercion1() {
    TestCompilerContext ctx = new TestCompilerContext();
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

    // MVEL2 optimizer selection (OptimizerFactory.setDefaultOptimizer("ASM")) has no MVEL3 equivalent.

    ctx = new TestCompilerContext();
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

  @Test
  @KnownCompatibility("block-syntax")
  public void testStaticTyping2() {
    String exp = "int x = 5; int y = 2; new int[] { x, y }";
    int[] res = (int[]) eval(exp, new HashMap());

    assertEquals(5, res[0]);
    assertEquals(2, res[1]);
  }

  @Test
  public void testMVEL190a() {
    TestCompilerContext ctx = new TestCompilerContext().withInput("a", String.class);
    compileEagerly("a.toString()", ctx);
  }

  @Test
  @KnownCompatibility("block-syntax")
  public void testPrimitiveTypes() {
    TestCompilerContext ctx = new TestCompilerContext();
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

  // Category not isolated: observed error is a parse error because the last expression is not a return statement
  // (block-syntax). The undeclared assignment (list = new java.util.ArrayList()) additionally fails with "Unsolved
  // symbol list" even with a return; var list = ... works.
  @Test
  @KnownCompatibility("block-syntax")
  public void testAutoBoxing() {
    TestCompilerContext ctx = new TestCompilerContext();

    Serializable s = compileExpression("(list = new java.util.ArrayList()).add( 5 ); list",
        ctx);

    Map vars = new HashMap();

    List list = (List) executeExpression(s,
        vars);

    assertEquals(1,
        list.size());

  }

  // Category not isolated: observed error is a parse error because the last expression is not a return statement
  // (block-syntax). The undeclared assignment (list = new java.util.ArrayList()) additionally fails with "Unsolved
  // symbol list" even with a return; var list = ... works.
  @Test
  @KnownCompatibility("block-syntax")
  public void testAutoBoxing2() {
    TestCompilerContext ctx = new TestCompilerContext();
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

  // Category not isolated: observed error is "cannot find symbol: variable BigInteger" in the generated code for x +
  // base.intValue; it passes once java.math.BigInteger is also in the import set. The expression additionally lacks a
  // trailing return (block-syntax).
  @Test
  @KnownCompatibility("missing-auto-import")
  public void testTypeCoercion() {
    // OptimizerFactory.setDefaultOptimizer("ASM") removed (MVEL2 optimizer)
    TestCompilerContext ctx = new TestCompilerContext();
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

  // Category not isolated: observed error is "cannot find symbol: variable BigInteger" in the generated code for x +
  // base.intValue; it passes once java.math.BigInteger is also in the import set. The expression additionally lacks a
  // trailing return (block-syntax).
  @Test
  @KnownCompatibility("missing-auto-import")
  public void testTypeCoercion2() {
    // OptimizerFactory.setDefaultOptimizer("reflective") removed (MVEL2 optimizer)
    TestCompilerContext ctx = new TestCompilerContext();
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

  public static class EchoContext {
    public String echo(String str) {
      return str;
    }
  }

  // Category not isolated: observed error is a parse error from the single-quoted 'Mac'. With double quotes,
  // this.echo("Mac") fails with "cannot find symbol: method echo(String)" and a plain echo("Mac") with "No method
  // candidates" because a POJO root "this" is not resolvable (this-reference).
  @Test
  @KnownCompatibility("single-quote-string")
  public void testContextMethodCallsInStrongMode() {
    TestCompilerContext context = new TestCompilerContext();
    context.addInput("this",
        EchoContext.class);

    Serializable stmt = compileExpression("this.echo( 'Mac')", context);
    stmt = compileExpression("echo( 'Mac')", context);

    assertEquals("Mac", executeExpression(stmt, new EchoContext()));
  }

  @Test
  @KnownCompatibility("block-syntax")
  public void testForLoopTypeCoercion() {
    TestCompilerContext pCtx = new TestCompilerContext();
    pCtx.addInput("$type", String.class);
    pCtx.addInput("l", List.class);

    Map<String, Object> vars = new HashMap<String, Object>();
    vars.put("$type", "pc!!");
    List list = new ArrayList();
    vars.put("l", list);
    Serializable stmt = compileExpression("for (byte bt:$type.getBytes()) {l.add( bt);}", pCtx);
    executeExpression(stmt, null, vars);

    byte[] exp = "pc!!".getBytes();
    //  byte[] res = new byte[list.size()];

    for (int i = 0; i < exp.length; i++) {
      assertEquals(exp[i], list.get(i));
    }
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

  @Test
  public void testGenericMethods() {
    String str = "Integer.parseInt( a.getMap().get(\"x\") )";

    TestCompilerContext pctx = new TestCompilerContext();
    pctx.addInput("a", AGenericTestClass.class);

    Serializable stmt = compileExpression(str, pctx);

    AGenericTestClass a = new AGenericTestClass();
    a.setMap(new HashMap<String, String>());
    a.getMap().put("x", "10");
    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("a", a);
    Number result = (Number) executeExpression(stmt, null, variables);
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

  // Category not isolated: observed error is "Unsolved symbol in addresses" (this-reference: the root is addressed
  // implicitly). It passes with a named variable (pa.addresses[0]).
  @Test
  @KnownCompatibility("this-reference")
  public void testGenerics1() {
    String str = "addresses[0] == new Address(\"s1\") && addresses[0].street == new Address(\"s1\").street";

    TestCompilerContext pctx = new TestCompilerContext();
    pctx.addInput("this", PersonAddresses.class);
    pctx.addImport(Address.class);
    pctx.addImport(PersonAddresses.class);
    Serializable stmt = compileExpression(str, pctx);
    PersonAddresses ctx = new PersonAddresses();
    ctx.getAddresses().add(new Address("s1"));
    Boolean result = (Boolean) executeExpression(stmt, ctx);
    assertTrue(result);
  }
}
