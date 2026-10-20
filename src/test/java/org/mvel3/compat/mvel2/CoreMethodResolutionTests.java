package org.mvel3.compat.mvel2;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mvel3.MVEL;
import org.mvel3.Type;
import org.mvel3.compat.mvel2.res.Bar;
import org.mvel3.compat.mvel2.res.Cheese;
import org.mvel3.compat.mvel2.res.DefaultKnowledgeHelper;
import org.mvel3.compat.mvel2.res.Foo;
import org.mvel3.compat.mvel2.res.KnowledgeHelper;
import org.mvel3.compat.mvel2.res.PojoStatic;
import org.mvel3.compat.mvel2.res.RuleBase;
import org.mvel3.compat.mvel2.res.RuleBaseImpl;
import org.mvel3.compat.mvel2.res.WorkingMemory;
import org.mvel3.compat.mvel2.res.WorkingMemoryImpl;

import static org.mvel3.compat.mvel2.Mvel3TestSupport.ExpressionCompiler;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.compileExpression;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.eval;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.executeExpression;

/**
 * Imported from the method-resolution tests of org.mvel2.tests.core.CoreConfidenceTests (mvel2 branch): method
 * calls, overloads, varargs, static methods, constructors, naked method calls and calls written with spaces.
 *
 * <p>Expressions, input values, expected values, and assertions are retained
 * unchanged where MVEL3 has an equivalent. Failures are intentionally visible;
 * known behavioural differences are annotated with {@link KnownCompatibility}.
 *
 * <p>Not imported, because they use MVEL2-only APIs: {@code testNoArgMethodInheritance},
 * {@code testMethodReturningPrimitiveTypeAnalysis} and {@code testUntypedClone} (they only check the result of
 * {@code MVEL.analyze}); {@code testMethodOverloadMatch}, {@code testGetBestCandidateForBigDecimalArg},
 * {@code testGetBestConstructorCandidateOfBigDecimal} and {@code testHandleNumericConversionBug}
 * ({@code ParseTools}); {@code testBug4} ({@code MVEL.getProperty}).
 *
 * <p>Adaptations: {@code ParserContext}/{@code ParserConfiguration} become {@link TestCompilerContext};
 * strong typing / strict type enforcement flags, optimizer selection and {@code COMPILER_OPT_ALLOW_NAKED_METH_CALL}
 * are removed; {@code compileAndExecuteWithStrongTyping} is replaced by {@code eval} on an empty variable map. The
 * {@code MVEL.analyze} calls of {@code testCovariance}, {@code testPippo}, {@code testReturnTypeExtendingGeneric}
 * and {@code testInvokeMethodInAbstractClass} are removed and only the executed result is asserted. Method imports
 * ({@code ParserConfiguration.addImport(name, Method)}) become static imports ({@code static pkg.Class.*}). The
 * adapter's {@code compileExpression} is lazy and does not compile, so the tests that only compile an expression use
 * {@link #compileEagerly}, which compiles at once. Fixtures are nested public static classes; the res fixtures
 * ({@code AStatic}, {@code PojoStatic}, {@code KnowledgeHelper}, ...) are copied to {@code res/}.
 */
@Tag("mvel2-compatibility")
public class CoreMethodResolutionTests extends AbstractTest {

  /** Compiles at once (the adapter's compileExpression only creates a lazy carrier). */
  private static void compileEagerly(final String expression, final TestCompilerContext ctx) {
    compileEagerly(expression, ctx, new LinkedHashSet<>());
  }

  private static void compileEagerly(final String expression, final TestCompilerContext ctx,
                                     final Set<String> extraImports) {
    final Map<String, Type<?>> types = new LinkedHashMap<>();
    ctx.getInputs().forEach((name, type) -> types.put(name, Type.type(type)));
    final Set<String> imports = new LinkedHashSet<>(extraImports);
    ctx.getImports().forEach(type -> imports.add(type.getCanonicalName()));
    ctx.getPackageImports().forEach(packageName -> imports.add(packageName + ".*"));
    if (expression.indexOf(';') > 0) {
      new MVEL().compileMapBlock(expression, Object.class, imports, types);
    } else {
      new MVEL().compileMapExpression(expression, Object.class, imports, types);
    }
  }

  protected static Object runSingleTest(final String ex) {
    return Mvel3TestSupport.eval(ex, createTestMap());
  }

  /** The strong typing flag does not exist in MVEL3; the expression is just evaluated. */
  protected static Object compileAndExecuteWithStrongTyping(final String ex) {
    return Mvel3TestSupport.eval(ex, new HashMap());
  }

  // ---------------------------------------------------------------- fixtures

  public static class Bean {
    private Date myDate = new Date();

    public Date getToday() {
      return new Date();
    }

    public Date getNullDate() {
      return null;
    }

    public String getNullString() {
      return null;
    }

    public Date getMyDate() {
      return myDate;
    }

    public void setMyDate(Date myDate) {
      this.myDate = myDate;
    }
  }

  public static class Context {
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy");
    private Bean bean;

    public Bean getBean() {
      return bean;
    }

    public void setBean(Bean bean) {
      this.bean = bean;
    }

    public String formatDate(Date date) {
      return date == null ? null : dateFormat.format(date);
    }

    public String formatString(String str) {
      return str == null ? "<NULL>" : str;
    }
  }

  public static class Person {
    private String name;
    private int age;
    private String likes;

    public Person() {
    }

    public Person(String name) {
      this.name = name;
    }

    public Person(String name, int age) {
      this.name = name;
      this.age = age;
    }

    public Person(String name, String likes, int age) {
      this.name = name;
      this.likes = likes;
      this.age = age;
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

    public String getLikes() {
      return likes;
    }

    public void setLikes(String likes) {
      this.likes = likes;
    }
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
  }

  public static class Drools {
    public void insert(Object obj) {
    }
  }

  public static class Model {
    private List latestHeadlines;

    public List getLatestHeadlines() {
      return latestHeadlines;
    }

    public void setLatestHeadlines(List latestHeadlines) {
      this.latestHeadlines = latestHeadlines;
    }
  }

  public static class MvelContext {
    public boolean singleCalled;
    public boolean arrayCalled;
    public String[] regkeys;

    public void methodForTest(String string) {
      System.out.println("sigle param method called!");
      singleCalled = true;
    }

    public void methodForTest(String[] strings) {
      System.out.println("array param method called!");
      arrayCalled = true;
    }

    public void setRegkeys(String[] regkeys) {
      this.regkeys = regkeys;
    }

    public void setRegkeys(String regkey) {
      this.regkeys = regkey.split(",");
    }
  }

  public static class StaticMethods {
    public static <T> boolean is(List<T> arg) {
      return true;
    }

    public static boolean is(Collection arg) {
      throw new RuntimeException("Wrong method called");
    }

    public static List<Object> getList(Class<?> arg) {
      ArrayList<Object> result = new ArrayList<Object>();
      result.add(arg);
      return result;
    }

    public static String throwException() {
      throw new RuntimeException("this should throw an exception");
    }
  }

  public static class ProcessManager {
    public void startProcess(String name, Map<String, Object> variables) {
      System.out.println("Process started");
    }
  }

  public static class KnowledgeRuntimeHelper {
    public ProcessManager getProcessManager() {
      return new ProcessManager();
    }
  }

  public static class PetStore {
    public List getPets() {
      List pets = new ArrayList();
      pets.add(new Dog());
      pets.add(new Cat());
      return pets;
    }
  }

  public static class Pet {
    public void run() {
    }
  }

  public static class Dog extends Pet {
    @Override
    public void run() {
      System.out.println("dog is running");
    }
  }

  public static class Cat extends Pet {
    @Override
    public void run() {
      System.out.println("cat is running");
    }
  }

  public static class A {
    public int invoke(String s1, String s2, B... bs) {
      return bs.length;
    }

    public static int invokeSum(int start, B... bs) {
      for (B b : bs) start += b.getValue();
      return start;
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

    public boolean equals(Object other) {
      return other != null && other instanceof B && value == ((B) other).value;
    }
  }

  public static class MySet {
    private Set<String> set = new HashSet<String>();

    public MySet(String... strings) {
      add(strings);
    }

    public void add(String... strings) {
      for (String s : strings) {
        set.add(s);
      }
    }

    public boolean contains(String s) {
      return set.contains(s);
    }

    public String toString() {
      return set.toString();
    }
  }

  public static class MyObj {
    public long valueField;

    public MyObj(long value) {
      this.valueField = value;
    }

    public long getValue() {
      return valueField;
    }

    public static String doSomething(MyObj s1, String s2) {
      return s1 + s2;
    }
  }

  public interface InterfaceA {
    InterfaceB getB();
  }

  public interface InterfaceB {
  }

  public static class ImplementationA implements InterfaceA {
    public ImplementationB getB() {
      return new ImplementationB();
    }

    public void setB(InterfaceB b) {
    }
  }

  public static class ImplementationB implements InterfaceB {
    public int getValue() {
      return 42;
    }
  }

  public static class Overloaded {
    public int method(int i, int j, String s) {
      return i + j + s.length();
    }

    public int method(int i, String s, int j) {
      return i + s.length() - j;
    }

    public int method(String s, int i, int j) {
      return s.length() - i - j;
    }
  }

  public static abstract class AbstractBase<T> {
    protected T foo;

    public T getFoo() {
      return foo;
    }
  }

  public static class StringConcrete extends AbstractBase<String> {
    public StringConcrete() {
      this.foo = new String();
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

  public static class O1 {
    public O2 getObj() {
      return new O2();
    }
  }

  public static class O2 extends O3 {
  }

  public abstract static class O3 {
    public String getValue() {
      return "value";
    }
  }

  public static class Thingy implements Serializable {
    private String name;
    private String version;
    private Object[] items;

    public Thingy(String name, String version, Object... items) {
      this.name = name;
      this.version = version;
      this.items = items;
    }

    public Thingy(String name) {
      this.name = name;
      this.version = null;
      this.items = null;
    }

    public void print() {
      System.out.println("Printing rule " + name);
    }

    public String getName() {
      return name;
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

  public static class StaticClassWithStaticMethod {
    public static String getString() {
      return "hello";
    }
  }

  private static class MyPrivatePrintStream extends PrintStream {
    public MyPrivatePrintStream(OutputStream os) {
      super(os);
    }

    public void println(String s) {
      super.println(s);
    }
  }

  public static class MyPublicPrintStream extends PrintStream {
    public MyPublicPrintStream(OutputStream os) {
      super(os);
    }

    public void println(String s) {
      super.println(s);
    }

    public void myPrintln(String s) {
      super.println(s);
    }
  }

  public static class Triangle {
    private List<Map<String, Object>> deliveries = new ArrayList<Map<String, Object>>();

    public List<Map<String, Object>> getDeliveries() {
      return deliveries;
    }
  }

  // ------------------------------------------------------------------- tests

  // Fails with "cannot find symbol: variable bean". With a named input `ctx.formatDate(ctx.bean.today)`, `ctx.formatDate(ctx.bean.nullDate)`,
  // `ctx.formatString(ctx.bean.nullString)` and the null assignment all work.
  @Test
  @KnownCompatibility("this-reference")
  public void testMethodResolutionWithNullParameter() {
    Context ctx = new Context();
    ctx.setBean(new Bean());
    Map<String, Object> vars = new HashMap<String, Object>();
    System.out.println("bean.today: " + eval("bean.today",
        ctx,
        vars));
    System.out.println("formatDate(bean.today): " + eval("formatDate(bean.today)",
        ctx,
        vars));
    //calling method with string param with null parameter works
    System.out.println("formatString(bean.nullString): " + eval("formatString(bean.nullString)",
        ctx,
        vars));
    System.out.println("bean.myDate = bean.nullDate: " + eval("bean.myDate = bean.nullDate; return bean.nullDate;",
        ctx,
        vars));
    //calling method with Date param with null parameter fails
    System.out.println("formatDate(bean.myDate): " + eval("formatDate(bean.myDate)",
        ctx,
        vars));
    //same here
    System.out.println(eval("formatDate(bean.nullDate)",
        ctx,
        vars));
  }

  // Category not isolated: fails with a parse error ("missing ';' at '}'"): the trailing `foo.bar.testList == empty` has no `return`. With `return` it fails with
  // "Unsolved symbol in empty" (empty-operator), and `testList.add(new String())` on the `List<Integer>` fails at run time with
  // NumberFormatException (the empty string is coerced to Integer).
  @Test
  @KnownCompatibility("block-syntax")
  public void testDeepMethod() {
    assertEquals(false,
        test("foo.bar.testList.add(new String()); foo.bar.testList == empty"));
  }

  // Fails with "missing return statement". With `return null;` added the chain `drools.workingMemory.ruleBase.removeRule(..)` works.
  @Test
  @KnownCompatibility("block-syntax")
  public void testsequentialAccessorsThenMethodCall() {
    String expr = "System.out.println(drools.workingMemory); " +
        "drools.workingMemory.ruleBase.removeRule(\"org.drools.examples\", \"some rule\"); ";

    TestCompilerContext context = new TestCompilerContext();
    context.addInput("drools",
        KnowledgeHelper.class);

    RuleBase ruleBase = new RuleBaseImpl();
    WorkingMemory wm = new WorkingMemoryImpl(ruleBase);
    KnowledgeHelper drools = new DefaultKnowledgeHelper(wm);

    Map vars = new HashMap();
    vars.put("drools",
        drools);
    ExpressionCompiler compiler = new ExpressionCompiler(expr, context);
    executeExpression(compiler.compile(),
        vars);
  }

  // Fails with "missing return statement". With `return null;` added the expression compiles.
  @Test
  @KnownCompatibility("block-syntax")
  public void testCompileWithNewInsideMethodCall() {
    String expr = "     p.name = \"goober\";\n" + "     System.out.println(p.name);\n"
        + "     drools.insert(new Address(\"Latona\"));\n";

    TestCompilerContext context = new TestCompilerContext();

    context.addImport(Person.class);
    context.addImport(Address.class);

    context.addInput("p",
        Person.class);
    context.addInput("drools",
        Drools.class);

    // The original only compiles the expression; compile at once so that the result is checked.
    compileEagerly(expr, context);
  }

  // Fails with a parse error at `Process('10')`. With `Process("10")` it works.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testStaticWithExplicitParam() {
    PojoStatic pojo = new PojoStatic("10");
    eval("org.mvel3.compat.mvel2.res.AStatic.Process('10')",
        pojo,
        new HashMap());
  }

  // Fails with "Unsolved symbol in value". With a named input `pojo.value.getClass().getName().toString()` works.
  @Test
  @KnownCompatibility("this-reference")
  public void testStaticWithExpressionParam() {
    PojoStatic pojo = new PojoStatic("10");
    assertEquals("java.lang.String",
        eval("org.mvel3.compat.mvel2.res.AStatic.Process(value.getClass().getName().toString())",
            pojo));
  }

  // Category not isolated: fails with a parse error at `{` of `{'1','2'}`. The single-quoted strings are a second cause (single-quote-string); with
  // `new String[] {"1","2"}` the array/single overloads are both selected.
  @Test
  @KnownCompatibility("inline-array-literal")
  public void testMethodResolutionOrder() {
    MvelContext mvelContext = new MvelContext();
    eval("methodForTest({'1','2'})",
        mvelContext);
    eval("methodForTest('1')",
        mvelContext);

    assertTrue(mvelContext.arrayCalled && mvelContext.singleCalled);
  }

  // Category not isolated: fails with "No method candidates were found for method call 'getList'": a static-imported method called with arguments is not found
  // (`static ...StaticMethods.*` or `.getList`; a no-argument `getString()` is found). The bare class `java.util.Formatter` as an argument
  // (class-literal; qualified with `.class` it still fails with the static import) is a second cause. `StaticMethods.is(StaticMethods.getList(
  // java.util.Formatter.class))` through the imported class works.
  @Test
  @KnownCompatibility("static-import-method-call")
  public void testMethodScoring() {
    // pconf.addImport(name, Method) for every static method of StaticMethods becomes a static import of the class.
    Set<String> imports = new LinkedHashSet<>();
    imports.add("static " + StaticMethods.class.getCanonicalName() + ".*");
    imports.add(TestCase.class.getCanonicalName());

    // this is successful
    TestCase.assertTrue(StaticMethods.is(StaticMethods.getList(java.util.Formatter.class)));

    // this also should be fine
    new MVEL().executeExpression("TestCase.assertTrue( is( getList( java.util.Formatter ) ) )", imports,
        new HashMap<String, Object>());
  }

  // Fails with "No method candidates were found for method call 'ord'" (the method belongs to the root `Base`). With a named input
  // `base.ord(true,-1)` returns true.
  @Test
  @KnownCompatibility("this-reference")
  public void testMethodWithNegativeIntParamMVEL313() {
    assertTrue((Boolean) runSingleTest("ord(true,-1)"));
  }

  // Category not isolated: fails with "Unsolved symbol in variables" (undeclared variable). With `var variables = [..]` it fails differently: the literal is
  // inferred as `Map<String,String>`, which javac rejects for the `Map<String,Object>` parameter; `Map<String, Object> variables = [..]` works.
  @Test
  @KnownCompatibility("block-syntax")
  public void testDeepMethodNameResolution() {
    String expression = "variables = [ \"symbol\" : \"RHT\" ]; \n" +
        "drools.getProcessManager().startProcess(\"id\", variables );";

    TestCompilerContext ctx = new TestCompilerContext();
    ctx.addInput("drools", KnowledgeRuntimeHelper.class);
    Map vars = new HashMap();
    vars.put("drools", new KnowledgeRuntimeHelper());
    Serializable expr = compileExpression(expression, ctx);
    executeExpression(expr, vars);
  }

  // Category not isolated: fails with a parse error at `pet:` (untyped `for (pet : list)`, not the `foreach` keyword). The unqualified `getPets()` (this-reference) is a
  // second cause, and a typed loop variable over the raw `List` needs a cast (see raw-collection-element-type); `for (Object pet : store.getPets())`
  // with a cast works.
  @Test
  @KnownCompatibility("foreach-syntax")
  public void testMethodCaching() {
    eval("for (pet: getPets()) pet.run();",
        new PetStore());
  }

  @Test
  public void testAmbiguousGetName() {
    Map<String, Object> vars = createTestMap();
    vars.put("Foo244", Foo.class);

    Serializable s = compileExpression("foo.getClass().getName()");

    System.out.println(executeExpression(s, vars));

    s = compileExpression("Foo244.getName()");

    System.out.println(executeExpression(s, vars));
  }

  // Fails with "missing return statement" (the trailing `;` makes it a block). `String.format("")` works.
  @Test
  @KnownCompatibility("block-syntax")
  public void testVarArgs() {
    // The original analyzes the expression; compile at once so that the result is checked.
    compileEagerly("String.format(\"\");", new TestCompilerContext());
  }

  // Category not isolated: fails with "Unsolved symbol in tm" (undeclared variable, block-syntax). `System.currentTimeMillis` without parentheses also fails when
  // the variable is declared ("Unsolved symbol: currentTimeMillis"), while the naked instance call `foo.happy` works.
  @Test
  @KnownCompatibility("naked-static-method-call")
  public void testNakedMethodCall() {
    // MVEL.COMPILER_OPT_ALLOW_NAKED_METH_CALL = true is removed (MVEL2 compiler option).

    Serializable c = compileExpression("tm = System.currentTimeMillis");
    assertTrue(((Long) executeExpression(c,
        new HashMap())) > 0);

    assertTrue(((Long) executeExpression(c,
        new HashMap())) > 0);

    Map map = new HashMap();
    map.put("foo",
        new Foo());
    c = compileExpression("foo.happy");
    assertEquals("happyBar",
        executeExpression(c,
            map));

    c = compileExpression("foo.happy");

    assertEquals("happyBar",
        executeExpression(c,
            map));
  }

  // Fails with "void cannot be converted to Object" (`drools.retract (cheese)` returns void). `drools.retract (cheese); return null;`
  // works, including the space before `(`.
  @Test
  @KnownCompatibility("void-method-expression")
  @SuppressWarnings({"unchecked"})
  public void testInterfaceMethodCallWithSpace() {
    Map map = new HashMap();
    DefaultKnowledgeHelper helper = new DefaultKnowledgeHelper();
    map.put("drools",
        helper);
    Cheese cheese = new Cheese("stilton",
        15);
    map.put("cheese",
        cheese);

    executeExpression(compileExpression("drools.retract (cheese)"),
        map);
    assertSame(cheese,
        helper.retracted.get(0));
  }

  @Test
  public void testChainedMethodCallsWithParams() {
    assertEquals(true,
        test("foo.toUC(\"abcd\").equals(\"ABCD\")"));
  }

  // Fails for all eight variants with "missing ';' at '}'": the trailing `f.isBoolTest()` has no `return`. With `return f.isBoolTest();`
  // every variant (spaces around `.` and before `(`) works.
  @Test
  @KnownCompatibility("block-syntax")
  public void testMethodCallWithSpaces() {
    String[] str = new String[]{
        "Foo f = new Foo(); f.setBoolTest( true )   ; f.isBoolTest()",
        "Foo f = new Foo(); f . setBoolTest( true ) ; f.isBoolTest()",
        "Foo f = new Foo(); f. setBoolTest( true )  ; f.isBoolTest()",
        "Foo f = new Foo(); f .setBoolTest( true )  ; f.isBoolTest()",
        "Foo f = new Foo(); f.boolTest = true   ; f.isBoolTest()",
        "Foo f = new Foo(); f . boolTest = true ; f.isBoolTest()",
        "Foo f = new Foo(); f. boolTest = true  ; f.isBoolTest()",
        "Foo f = new Foo(); f .boolTest = true  ; f.isBoolTest()"
    };

    TestCompilerContext pctx = new TestCompilerContext();
    pctx.addInput("this", Bar.class);
    pctx.addImport(Foo.class);
    List<String> errors = new ArrayList<String>();
    for (String s : str) {
      try {
        Serializable stmt = compileExpression(s, pctx);
        Bar ctx = new Bar();
        Boolean result = (Boolean) executeExpression(stmt, ctx, new HashMap());
        assertTrue(result);
      }
      catch (Exception e) {
        e.printStackTrace();
        errors.add("**** Error on expression: " + s + "\n" + e.getMessage());
      }
    }
    assertTrue(errors.toString(), errors.isEmpty());
  }

  // Same as testMethodCallWithSpaces (the trailing `f.isBoolTest()` has no `return`).
  @Test
  @KnownCompatibility("block-syntax")
  public void testMethodCallWithSpacesASM() {
    // OptimizerFactory.setDefaultOptimizer("ASM") is removed (MVEL2 optimizer selection).
    testMethodCallWithSpaces();
  }

  // Category not isolated: fails with a parse error at `import`. The same expressions have other causes: `a = null` / `a = 123` (undeclared variables, and
  // `var a = null` cannot be inferred: block-syntax) and the inline array `{a, b}` (inline-array-literal, parse error). The other
  // calls (`AStatic.process(..)`, `String.format(..)` with 0..2 arguments, an `Object[]` argument) work when imported through the API.
  @Test
  @KnownCompatibility("inline-import")
  public void testVarArgsParams() {

    assertEquals(String.format("null"),
        runSingleTest("import org.mvel3.compat.mvel2.res.AStatic; a = null; AStatic.process(a)"));
    assertEquals(String.format("hello,world,"),
        runSingleTest("import org.mvel3.compat.mvel2.res.AStatic; AStatic.process(\"hello\",\"world\")"));

    assertEquals(String.format(""),
        runSingleTest("import org.mvel3.compat.mvel2.res.AStatic; AStatic.process()"));
    assertEquals(String.format("null"),
        runSingleTest("import org.mvel3.compat.mvel2.res.AStatic; AStatic.process(null)"));

    assertEquals(String.format("xxx"),
        runSingleTest("String.format(\"xxx\")"));

    assertEquals(String.format("%010d", 123),
        runSingleTest("String.format(\"%010d\", 123)"));

    assertEquals(String.format("%010d", 123),
        runSingleTest("a = new Object[1]; a[0] = 123; String.format(\"%010d\", a)"));

    assertEquals(String.format("%010d", 123),
        runSingleTest("a = 123; String.format(\"%010d\", a)"));

    assertEquals(String.format("%010d -- %010d", 123, 456),
        runSingleTest("a = 123; b = 456; String.format(\"%010d -- %010d\", {a, b})"));

    assertEquals(String.format("%010d -- %010d", 123, 456),
        runSingleTest("a = new Object[2]; a[0] = 123; a[1] = 456; String.format(\"%010d -- %010d\", a)"));

    assertEquals(String.format("%010d -- %010d", 123, 456),
        runSingleTest("String.format(\"%010d -- %010d\", 123, 456)"));

    assertEquals(String.format("%010d -- %010d", 123, 456),
        runSingleTest("a = 123; b = 456; String.format(\"%010d -- %010d\", a, b)"));
  }

  // Fails with a parse error at `import`. With the classes imported through the API, all four varargs calls (`new A().invoke(..)`,
  // `A.invokeSum(3)`, with and without `B` arguments) return the expected values.
  @Test
  @KnownCompatibility("inline-import")
  public void testTypedVarArgsParams() {
    String imports = "import org.mvel3.compat.mvel2.CoreMethodResolutionTests.A;\nimport org.mvel3.compat.mvel2.CoreMethodResolutionTests.B;\n";

    String invoke0 = imports + "new A().invoke(\"s1\", \"s2\")";
    int result = (Integer) compileAndExecuteWithStrongTyping(invoke0);
    assertEquals(0, result);
    assertEquals(0, runSingleTest(invoke0));

    String invokeSum0 = imports + "A.invokeSum(3)";
    result = (Integer) compileAndExecuteWithStrongTyping(invokeSum0);
    assertEquals(3, result);
    assertEquals(3, runSingleTest(invokeSum0));

    String invoke3 = imports + "new A().invoke(\"s1\", \"s2\", new B(), new B(), new B())";
    result = (Integer) compileAndExecuteWithStrongTyping(invoke3);
    assertEquals(3, result);
    assertEquals(3, runSingleTest(invoke3));

    String invokeSum2 = imports + "A.invokeSum(3, new B(4), new B(5))";
    result = (Integer) compileAndExecuteWithStrongTyping(invokeSum2);
    assertEquals(12, result);
    assertEquals(12, runSingleTest(invokeSum2));
  }

  // Fails with a parse error at `import`. With the class imported through the API, `new MySet("s1", "s2")` works.
  @Test
  @KnownCompatibility("inline-import")
  public void testTypedVarArgsConstructor() {
    String imports = "import org.mvel3.compat.mvel2.CoreMethodResolutionTests.MySet;\n";
    String constructor = imports + "new MySet(\"s1\", \"s2\")";
    MySet result = (MySet) compileAndExecuteWithStrongTyping(constructor);
    assertTrue(result.contains("s1"));
    assertTrue(result.contains("s2"));
  }

  // Fails with a parse error at `import`. With the classes imported through the API, `new MySet("s1", "s2")` works.
  @Test
  @KnownCompatibility("inline-import")
  public void testTypedVarArgsConstructorASM() {
    // OptimizerFactory.setDefaultOptimizer("ASM") is removed (MVEL2 optimizer selection).
    testTypedVarArgsConstructor();
  }

  // Fails with "cannot find symbol: class CoreMethodResolutionTests$MyObj". With the dotted name `...CoreMethodResolutionTests.MyObj.doSomething(null, "abc")`
  // it returns "nullabc".
  @Test
  @KnownCompatibility("inner-class-binary-name")
  public void testStaticMethodsInvocationWithNullArg() {
    String str = "org.mvel3.compat.mvel2.CoreMethodResolutionTests$MyObj.doSomething(null, \"abc\")";

    assertEquals(null + "abc", executeExpression(compileExpression(str)));
  }

  // Same as testStaticMethodsInvocationWithNullArg.
  @Test
  @KnownCompatibility("inner-class-binary-name")
  public void testStaticMethodsInvocationWithNullArgASM() {
    // OptimizerFactory.setDefaultOptimizer("ASM") is removed (MVEL2 optimizer selection).
    testStaticMethodsInvocationWithNullArg();
  }

  // Fails with "cannot find symbol: variable b". With a named input `a.b.value` returns 42.
  @Test
  @KnownCompatibility("this-reference")
  public void testCovariance() {
    TestCompilerContext parserContext = new TestCompilerContext();
    parserContext.addInput("this", ImplementationA.class);
    // assertEquals(int.class, MVEL.analyze("b.value", parserContext)) is removed (MVEL2 analysis API).
    assertEquals(42, executeExpression(compileExpression("b.value", parserContext), new ImplementationA()));
  }

  // Fails with "No method candidates were found for method call 'method'": the unqualified call is not resolved against the root.
  // With a named input `o.method(..)` selects the right overload in all three calls.
  @Test
  @KnownCompatibility("this-reference")
  public void testOverloading() {
    TestCompilerContext parserContext = new TestCompilerContext();
    parserContext.addInput("this", Overloaded.class);
    Overloaded overloaded = new Overloaded();

    assertEquals(15, executeExpression(compileExpression("method(5, 9, \"x\")", parserContext), overloaded));
    assertEquals(-3, executeExpression(compileExpression("method(5, \"x\", 9)", parserContext), overloaded));
    assertEquals(-13, executeExpression(compileExpression("method(\"x\", 5, 9)", parserContext), overloaded));
  }

  @Test
  public void testPippo() {
    // assertEquals(String.class, MVEL.analyze(..)) is removed (MVEL2 analysis API); the evaluated value is checked.
    assertEquals("b)ar", eval("new String(\"b)ar\")"));
  }

  // Fails with "Unsolved symbol in foo". With a named input `s.foo.concat("bar")` and `s.getFoo().concat("bar")` work.
  @Test
  @KnownCompatibility("this-reference")
  public void testReturnTypeExtendingGeneric() {
    TestCompilerContext parserContext = new TestCompilerContext();
    parserContext.addInput("this", StringConcrete.class);
    // MVEL.analyze is removed (MVEL2 analysis API); the evaluated values are checked.
    assertEquals("bar", executeExpression(compileExpression("foo.concat(\"bar\")", parserContext), new StringConcrete()));
    assertEquals("bar", executeExpression(compileExpression("getFoo().concat(\"bar\")", parserContext), new StringConcrete()));
  }

  @Test
  public void testStaticMethodInvocation() {
    TestCompilerContext pctx = new TestCompilerContext();
    pctx.addImport(ARef.class);
    pctx.addImport(BRef.class);
    pctx.addInput("value", String.class);
    Map vars = new HashMap() {{ put("value", "1234"); }};
    assertEquals(0, executeExpression(compileExpression("ARef.getSize(value)", pctx), vars));
    assertEquals(4, executeExpression(compileExpression("BRef.getSize(value)", pctx), vars));
  }

  @Test
  public void testInvokeMethodInAbstractClass() {
    TestCompilerContext parserContext = new TestCompilerContext();
    parserContext.addInput("a", O1.class);
    // assertEquals(String.class, MVEL.analyze(..)) is removed (MVEL2 analysis API).

    Map vars = new HashMap() {{ put("a", new O1()); }};
    assertEquals("value", executeExpression(compileExpression("a.getObj().getValue()", parserContext), vars));
  }

  @Test
  public void testInvokeVarargConstructor() {
    TestCompilerContext pctx = new TestCompilerContext();
    pctx.addImport(Thingy.class);
    pctx.addInput("name", String.class);
    Map vars = new HashMap() {{ put("name", "test"); }};
    Thingy result = (Thingy) executeExpression(compileExpression("new Thingy(name)", pctx), vars);
    assertEquals("test", result.getName());
  }

  // Fails with "cannot find symbol: class CoreMethodResolutionTests$MySet". With the dotted name `new ...CoreMethodResolutionTests.MySet()` it works.
  @Test
  @KnownCompatibility("inner-class-binary-name")
  public void testEmptyVarargConstructor() {
    String clsName = MySet.class.getName();
    // OptimizerFactory.setDefaultOptimizer("ASM") is removed (MVEL2 optimizer selection).
    Serializable s = compileExpression("new " + clsName + "()");
    assertNotNull(executeExpression(s));
  }

  // Fails with "void cannot be converted to Object" (`m.add()` returns void). `m.add(); return null;` works, so the empty varargs call
  // itself is fine.
  @Test
  @KnownCompatibility("void-method-expression")
  public void testEmptyVarargMethod() {
    // OptimizerFactory.setDefaultOptimizer("ASM") is removed (MVEL2 optimizer selection).
    Serializable s = compileExpression("m.add()");
    Map<String, MySet> inputs = Collections.singletonMap("m", new MySet());
    executeExpression(s, inputs);
  }

  // Passes for a different reason than in MVEL2: the single-quoted strings are a parse error, which the catch accepts. With double quotes the
  // missing five-argument Person constructor is reported by javac ("no suitable constructor found").
  @Test
  public void testConstructor() {
    String ex = " TestHelper.method(new Person('bob', 30), new Person('mark', 40, 999, 55, 10));\n";
    TestCompilerContext ctx = new TestCompilerContext();
    ctx.addImport(TestHelper.class);
    ctx.addImport(Person.class);

    // un-comment the following line to see how MVEL is converting the int argument 40 into a
    // string and then executing the wrong constructor on the Person class
    try {
      compileEagerly(ex, ctx);
      fail("Constructor should not have been found.");
    }
    catch (Exception e) {
      // yay.
    }
    // fail( "The Person constructor used in the expression does not exist, so an error should have been raised during compilation." );
  }

  @Test
  public void testEmptyConstructorWithSpace() {
    String text = "getString( )";

    // pconf.addImport("getString", Method) becomes a static import of the class.
    Set<String> imports = new LinkedHashSet<>();
    imports.add("static " + StaticClassWithStaticMethod.class.getCanonicalName() + ".getString");

    compileEagerly(text, new TestCompilerContext(), imports);
  }

  // Category not isolated: fails with a parse error at `.{` (the inline form on `new Cheese()`; Compatibility.md describes a silent no-op for the inline
  // form on other receivers). The undeclared variable `cheese` and the missing `return` (block-syntax) were not tested separately.
  // `Cheese cheese = new Cheese(); with (cheese) { type = $c.type; } return cheese;` works.
  @Test
  @KnownCompatibility("with-block-syntax")
  public void testInlineConstructor() {
    String str = "cheese = new Cheese().{ type = $c.type };";
    TestCompilerContext pctx = new TestCompilerContext();
    pctx.addInput("$c", Cheese.class);
    pctx.addImport(Cheese.class);
    Serializable stmt = compileExpression(str, pctx);
    Cheese $c = new Cheese();
    $c.setType("stilton");
    Map<String, Object> vars = new HashMap<String, Object>();
    vars.put("$c", $c);
    Cheese cheese = (Cheese) executeExpression(stmt, vars);
    assertEquals("stilton", cheese.getType());
  }

  // Fails with "Method 'getName' cannot be resolved" (unqualified call against the root). With a named input `bar.getName()` works.
  @Test
  @KnownCompatibility("this-reference")
  public void testContextObjMethodCall() {
    String str = "getName() == \"bob\"";

    TestCompilerContext pctx = new TestCompilerContext();
    pctx.addInput("this", Bar.class);
    Serializable stmt = compileExpression(str, pctx);
    Bar ctx = new Bar();
    ctx.setName("bob");
    Boolean result = (Boolean) executeExpression(stmt, ctx);
    assertTrue(result);
  }

  // Fails with "missing return statement". With `return null;` added the same call compiles and runs with a private PrintStream
  // subclass installed.
  @Test
  @KnownCompatibility("block-syntax")
  public void testSystemOutOnPrivateClass() {
    PrintStream originalSystemOut = System.out;
    System.setOut(new MyPrivatePrintStream(System.out));
    try {
      String expression = "System.out.println(\"Hello World\");";
      runSingleTest(expression);
    }
    finally {
      System.setOut(originalSystemOut);
    }
  }

  // Fails with "cannot find symbol: method myPrintln(String) in PrintStream": MVEL3 uses the declared type of `System.out`, MVEL2 the runtime class.
  // A cast `((MyPublicPrintStream) System.out).myPrintln(..)` was not usable here because the test runner wraps System.out.
  @Test
  @KnownCompatibility("runtime-type-method-resolution")
  public void testSystemOutWithActualInstanceMethod() {
    PrintStream originalSystemOut = System.out;
    System.setOut(new MyPublicPrintStream(System.out));
    try {
      String expression = "System.out.myPrintln(\"Hello World\");";
      runSingleTest(expression);
    }
    finally {
      System.setOut(originalSystemOut);
    }
  }

  // Category not isolated: fails with a JavaParser NPE ("HasParentNode.setParentNode") on `'This is a test'`. The undeclared `a` and the missing `return`
  // (block-syntax) were not tested on the original expression; with `int a = 0;`, double quotes and `return null;` it works.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testSystemOutPrint() {
    test("a = 0;\r\nSystem.out.println('This is a test');");
  }

  @Test
  public void testPrimitiveSubtyping() {
    BigDecimal result = (BigDecimal) executeExpression(compileExpression("java.math.BigDecimal.valueOf(100)"), new HashMap());
    assertEquals("100", result.toString());
  }

  @Test
  public void testSetAccessorOverloadedEqualsStrictMode() {
    TestCompilerContext ctx = new TestCompilerContext();
    ctx.addInput("foo",
        Foo.class);

    try {
      compileEagerly("foo.bar = 0", ctx);
    }
    catch (Exception e) {
      // should fail.

      e.printStackTrace();
      return;
    }

    assertTrue(false);
  }

  @Test
  public void testIssue286() {
    Serializable s = compileExpression("java.lang.Character.toLowerCase(name.charAt(0)) == 'a'");
    Map<String, Object> map = new HashMap<>();
    map.put("name", "Adam");
    assertEquals(true, executeExpression(s, map));
  }

  // Category not isolated: fails with a parse error on `'stilton'.equals`. With double quotes and no trailing `;` it works; the trailing `;` (block-syntax: missing
  // return) and the unqualified `type` (this-reference) were not tested on the original expression.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testContextFieldNotFound() {
    String str = "'stilton'.equals( type );";

    TestCompilerContext pctx = new TestCompilerContext();
    pctx.addInput("this", Cheese.class);

    Serializable stmt = compileExpression(str, pctx);
    executeExpression(stmt, new Cheese(), new HashMap());
  }

  // Fails with a parse error on `'foo'. hashCode()`. With `"foo". hashCode()` it works.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testLiteralToStringWithSpace() {
    String expr = "'foo'. hashCode()";
    int hashCode = "foo". hashCode();
    Serializable s = compileExpression(expr);
    assertEquals(Integer.valueOf(hashCode), executeExpression(s));
  }

  // Same as testLiteralToStringWithSpace.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testLiteralToStringWithSpaceASM() {
    // OptimizerFactory.setDefaultOptimizer("ASM") is removed (MVEL2 optimizer selection).
    testLiteralToStringWithSpace();
  }

  // Fails with "missing return statement". With `return null;` added the expression compiles and runs.
  @Test
  @KnownCompatibility("block-syntax")
  public void testPropertyVerfierShoudldNotLoopIndefinately() {
    String expr = "\t\tmodel.latestHeadlines = $list;\n"
        + "model.latestHeadlines.add( 0, (model.latestHeadlines[2]) );";

    TestCompilerContext pCtx = new TestCompilerContext();
    pCtx.addInput("$list",
        List.class);
    pCtx.addInput("model",
        Model.class);

    // compiler.setVerifying(true) is removed (MVEL2 compiler option); the expression is compiled at once.
    compileEagerly(expr, pCtx);
  }

  // Category not isolated: fails with "Method 'getStatus' cannot be resolved in context m.getStatus()": `m` is declared as `Object`, and MVEL3 resolves methods on
  // the declared type (MVEL2 does not check them in non-strict mode). With `m` typed as the runtime class and `return null;` added the
  // expression compiles; the missing trailing `return` (block-syntax) was not tested on its own. The `System` input declared as
  // `void` was removed.
  @Test
  @KnownCompatibility("runtime-type-method-resolution")
  public void testCompileParserContextShouldNotLoopIndefinitelyOnValidJavaExpression() {
    String expr = "		System.out.println( message );\n" + //
        "m.setMessage( \"Goodbye cruel world\" );\n" + //
        "System.out.println(m.getStatus());\n" + //
        "m.setStatus( Message.GOODBYE );\n";

    TestCompilerContext context = new TestCompilerContext();

    context.addImport(Message.class);
    // context.addInput("System", void.class) is removed (a void input cannot be declared in MVEL3).
    context.addInput("message",
        Object.class);
    context.addInput("m",
        Object.class);

    compileEagerly(expr, context);
  }

  // Category not isolated: fails with "Object cannot be converted to String": the input is declared as a raw `List`, so `for (String s : strings)` gets `Object`
  // elements. With generics given (`Type.type(List.class, "<String>")`) it works. The `for` body returns and nothing follows it, so a
  // missing-return error (block-syntax) is also possible.
  @Test
  @KnownCompatibility("raw-collection-element-type")
  public void testForLoopWithSpaces() {
    Map<String, Object> vars = new HashMap<String, Object>();
    vars.put("strings", Arrays.asList("test"));

    TestCompilerContext pctx = new TestCompilerContext();
    pctx.addInput("strings", List.class);

    String expression =
        "for (   String   s : strings ) {\n" +
        "  return s;\n" +
        "}";

    Serializable compiledExpr = compileExpression(expression, pctx);
    assertEquals("test", executeExpression(compiledExpr, vars));
  }

  // Fails with "cannot find symbol: variable deliveries". With a named input `t.deliveries.size` and `t.deliveries.size == 0` work, so the
  // naked method call itself is supported.
  @Test
  @KnownCompatibility("this-reference")
  public void testNestedClassWithNestedGenericsOnNakedMethod() {
    String str = "deliveries.size";

    // MVEL.COMPILER_OPT_ALLOW_NAKED_METH_CALL = true is removed (MVEL2 compiler option).
    TestCompilerContext pctx = new TestCompilerContext();
    pctx.addInput("this", Triangle.class);

    Serializable stmt = compileExpression(str, pctx);
    assertEquals(Integer.valueOf(0), (Integer) executeExpression(stmt, new Triangle(), new HashMap()));

    str = "deliveries.size == 0";

    stmt = compileExpression(str, pctx);
    assertTrue((Boolean) executeExpression(stmt, new Triangle(), new HashMap()));
  }
}
