package org.mvel3.compat.mvel2;

import java.io.BufferedReader;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mvel3.ClassManager;
import org.mvel3.MVEL;
import org.mvel3.Type;
import org.mvel3.compat.mvel2.res.Cheese;
import org.mvel3.compat.mvel2.res.Cheesery;
import org.mvel3.compat.mvel2.res.Foo;
import org.mvel3.compat.mvel2.res.MyClass;
import org.mvel3.compat.mvel2.res.MyInterface;
import org.mvel3.compat.mvel2.res.Status;
import org.mvel3.compat.mvel2.res.User;

import static org.mvel3.compat.mvel2.Mvel3TestSupport.ExpressionCompiler;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.compileExpression;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.eval;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.executeExpression;

/**
 * Imported from the import-related tests of org.mvel2.tests.core.CoreConfidenceTests (mvel2 branch).
 *
 * <p>Expressions, input values, expected values, and assertions are retained
 * unchanged where MVEL3 has an equivalent. Failures are intentionally visible;
 * known behavioural differences are annotated with {@link KnownCompatibility}.
 *
 * <p>Not imported, because they use MVEL2-only APIs: {@code testUnwantedImport}
 * ({@code MVEL.analysisCompile} and {@code ParserConfiguration.getImports()}). The commented-out
 * {@code testClassImportViaFactory} and {@code testSataticClassImportViaFactory} of the original are not imported.
 *
 * <p>Adaptations: {@code ParserContext}/{@code ParserConfiguration} become {@link TestCompilerContext}
 * ({@code addImport}, {@code addPackageImport}); {@code ClassImportResolverFactory} is replaced by
 * {@code addImport}; the {@code DefaultLocalVariableResolverFactory} argument is replaced by an empty
 * variable map; strong typing / strict type enforcement flags and optimizer selection are removed.
 * The adapter's {@code compileExpression} is lazy and does not compile, so the tests that only compile an
 * expression use {@link #compileEagerly}, which compiles at once. {@code Triangle} keeps only the nested
 * {@code Foo} enum used by {@code testNestedEnum}.
 */
@Tag("mvel2-compatibility")
public class CoreImportsTests extends AbstractTest {

  /** Compiles at once (the adapter's compileExpression only creates a lazy carrier). */
  private static void compileEagerly(final String expression, final TestCompilerContext ctx) {
    final Map<String, Type<?>> types = new LinkedHashMap<>();
    ctx.getInputs().forEach((name, type) -> types.put(name, Type.type(type)));
    final Set<String> imports = new LinkedHashSet<>();
    ctx.getImports().forEach(type -> imports.add(type.getCanonicalName()));
    ctx.getPackageImports().forEach(packageName -> imports.add(packageName + ".*"));
    if (expression.indexOf(';') > 0) {
      new MVEL().compileMapBlock(expression, Object.class, imports, types);
    } else {
      new MVEL().compileMapExpression(expression, Object.class, imports, types);
    }
  }

  // Category not isolated: fails with a parse error ("missing ';' at '}'") because `return (list == empty)` has no
  // trailing semicolon. With the semicolon added it fails with "Unsolved symbol in empty" (empty-operator), so the `empty`
  // operator is a second cause.
  @Test
  @KnownCompatibility("block-syntax")
  public void testWhileUsingImports() {
    TestCompilerContext context = new TestCompilerContext();
    context.addImport(java.util.ArrayList.class);
    context.addImport(java.util.List.class);

    ExpressionCompiler compiler = new ExpressionCompiler("List list = new ArrayList(); return (list == empty)", context);
    // DefaultLocalVariableResolverFactory is replaced by an empty variable map.
    assertTrue((Boolean) executeExpression(compiler.compile(),
        new HashMap()));
  }

  // Category not isolated: fails with a parse error at `import`. A bare class name used as the value
  // (`HashMap;`) is a second cause: with the class imported through the API, `HashMap` as an expression fails with
  // "cannot find symbol: variable HashMap" and `HashMap;` with "not a statement" (class-literal); `HashMap.class` works.
  @Test
  @KnownCompatibility("inline-import")
  public void testImport() {
    assertEquals(HashMap.class,
        test("import java.util.HashMap; HashMap;"));
  }

  // Category not isolated: fails with a parse error at `import` (MVEL3 does not accept it inside an expression). With the
  // package imported through the API, the trailing `maps` without `return` still fails to parse (block-syntax).
  @Test
  @KnownCompatibility("inline-import")
  public void testImport2() {
    HashMap[] maps = (HashMap[]) eval("import java.util.*; HashMap[] maps = new HashMap[10]; maps",
        new HashMap());

    assertEquals(10,
        maps.length);
  }

  public static class MyPerson {
    private String name;

    public MyPerson(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }

  // Category not isolated: fails with a parse error at `import`. A second cause is the root object: with `name` declared as
  // an input the expression `name == "Mario"` works, but with a POJO root it fails with "Unsolved symbol in name"
  // (this-reference).
  @Test
  @KnownCompatibility("inline-import")
  public void testUnusedPackageImport() {
    String expression =
        "import java.util.*; " +
            "name == \"Mario\" ";
    Serializable compiledExpression = compileExpression(expression);
    boolean result = (Boolean) executeExpression(compiledExpression, new MyPerson("Mario"));
    assertTrue(result);
  }

  // Category not isolated: fails with a parse error at `import_static` (the inline form of `inline-import`). The static
  // import through the API imports set was not tested here; `Math.sqrt(4)` itself works.
  @Test
  @KnownCompatibility("inline-import")
  public void testStaticImport() {
    assertEquals(2.0,
        test("import_static java.lang.Math.sqrt; sqrt(4)"));
  }

  @Test
  @KnownCompatibility("block-syntax")
  public void testCheeseConstructor() {
    // ClassImportResolverFactory / ResolverTools / MapVariableResolverFactory are replaced by addImport.
    TestCompilerContext ctx = new TestCompilerContext();
    ctx.addImport(Cheese.class);

    assertTrue(executeExpression(compileExpression("cheese = new Cheese(\"cheddar\", 15);",
        ctx),
        createTestMap()) instanceof Cheese);
  }

  // Category not isolated: the first compiled expression `HashMap` (a bare class name as the value) fails with "cannot find
  // symbol: variable HashMap", also when the package is imported through the API (`HashMap.class` works). The second
  // expression `map = new HashMap(); map.size()` was observed separately to fail with a parse error ("missing ';' at '}'",
  // block-syntax).
  @Test
  @KnownCompatibility("class-literal")
  public void testDynamicImports() {
    TestCompilerContext ctx = new TestCompilerContext();
    ctx.addPackageImport("java.util");

    ExpressionCompiler compiler = new ExpressionCompiler("HashMap", ctx);
    Serializable s = compiler.compile();

    assertEquals(HashMap.class,
        executeExpression(s));

    compiler = new ExpressionCompiler("map = new HashMap(); map.size()", ctx);
    s = compiler.compile();

    assertEquals(0,
        executeExpression(s,
            new HashMap()));
  }

  // Category not isolated: fails with a parse error at `import`. With the package imported through the API, the trailing
  // `map.size()` without `return` still fails to parse ("missing ';' at '}'", block-syntax).
  @Test
  @KnownCompatibility("inline-import")
  public void testDynamicImports3() {
    String expression = "import java.util.*; HashMap map = new HashMap(); map.size()";

    ExpressionCompiler compiler = new ExpressionCompiler(expression);
    Serializable s = compiler.compile();

    assertEquals(0,
        executeExpression(s,
            new HashMap()));

    assertEquals(0,
        eval(expression,
            new HashMap()));
  }

  @Test
  @KnownCompatibility("single-quote-string")
  public void testDynamicImportsInList() {
    TestCompilerContext ctx = new TestCompilerContext();
    ctx.addPackageImport("org.mvel3.compat.mvel2.res");

    ExpressionCompiler compiler = new ExpressionCompiler("[ new User('Bobba', 'Feta') ]", ctx);
    List list = (List) executeExpression(compiler.compile());
    User user = (User) list.get(0);
    assertEquals("Bobba",
        user.getFirstName());
  }

  @Test
  @KnownCompatibility("single-quote-string")
  public void testDynamicImportsInMap() {
    TestCompilerContext ctx = new TestCompilerContext();
    ctx.addPackageImport("org.mvel3.compat.mvel2.res");

    ExpressionCompiler compiler = new ExpressionCompiler("[ 'bobba' : new User('Bobba', 'Feta') ]", ctx);
    Map map = (Map) executeExpression(compiler.compile());
    User user = (User) map.get("bobba");
    assertEquals("Bobba",
        user.getFirstName());
  }

  @Test
  public void testDynamicImportsOnNestedExpressions() {
    TestCompilerContext ctx = new TestCompilerContext();
    ctx.addPackageImport("org.mvel3.compat.mvel2.res");

    ExpressionCompiler compiler = new ExpressionCompiler("new Cheesery(\"bobbo\", new Cheese(\"cheddar\", 15))", ctx);

    Cheesery p1 = new Cheesery("bobbo",
        new Cheese("cheddar",
            15));
    Cheesery p2 = (Cheesery) executeExpression(compiler.compile(),
        new HashMap());

    assertEquals(p1,
        p2);
  }

  @Test
  public void testDynamicImportsWithNullConstructorParam() {
    TestCompilerContext ctx = new TestCompilerContext();
    ctx.addPackageImport("org.mvel3.compat.mvel2.res");

    ExpressionCompiler compiler = new ExpressionCompiler("new Cheesery(\"bobbo\", null)", ctx);

    Cheesery p1 = new Cheesery("bobbo",
        null);
    Cheesery p2 = (Cheesery) executeExpression(compiler.compile(),
        new HashMap());

    assertEquals(p1,
        p2);
  }

  @Test
  public void testDynamicImportsWithNullConstructorParamWithStrongType() {
    TestCompilerContext ctx = new TestCompilerContext();
    // setStrongTyping(true) is removed (MVEL3 has no such mode).
    ctx.addPackageImport("org.mvel3.compat.mvel2.res");

    ExpressionCompiler compiler = new ExpressionCompiler("new Cheesery(\"bobbo\", null)", ctx);

    Cheesery p1 = new Cheesery("bobbo",
        null);
    Cheesery p2 = (Cheesery) executeExpression(compiler.compile(),
        new HashMap());

    assertEquals(p1,
        p2);
  }

  @Test
  @KnownCompatibility("undeclared-input-variable")
  public void testDynamicImportsWithIdentifierSameAsClassWithDiffCase() {
    TestCompilerContext ctx = new TestCompilerContext();
    ctx.addPackageImport("org.mvel3.compat.mvel2.res");
    // setStrictTypeEnforcement(false) is removed (MVEL3 has no such mode).

    // The original only compiles the expression; compile at once so that the result is checked.
    compileEagerly("bar.add(\"hello\")", ctx);
  }

  // Category not isolated: fails with a null-reference error that goes away with double quotes. With double quotes the
  // trailing `map["conan"];` without `return` fails with "missing return statement" (block-syntax); with `return` it works.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testTypedAssignment() {
    assertEquals("foobar",
        test("java.util.Map map = new java.util.HashMap(); map.put('conan', 'foobar'); map['conan'];"));
  }

  @Test
  @KnownCompatibility("block-syntax")
  public void testFQCNwithStaticInList() {
    assertEquals(Integer.MIN_VALUE,
        test("list = [java.lang.Integer.MIN_VALUE]; list[0]"));
  }

  @Test
  @KnownCompatibility("inner-class-binary-name")
  public void testStaticNested() {
    assertEquals(1,
        eval("org.mvel3.compat.mvel2.AbstractTest$Message.GOODBYE",
            new HashMap()));
  }

  @Test
  @KnownCompatibility("block-syntax")
  public void testStaticNestedWithImport() {
    String expr = "Message.GOODBYE;\n";

    TestCompilerContext context = new TestCompilerContext();

    context.addImport(Message.class);

    ExpressionCompiler compiler = new ExpressionCompiler(expr, context);
    assertEquals(1,
        executeExpression(compiler.compile()));
  }

  @Test
  @KnownCompatibility("block-syntax")
  public void testStaticNestedWithMethodCall() {
    String expr = "item = new Item( \"Some Item\"); $msg.addItem( item ); return $msg";

    TestCompilerContext context = new TestCompilerContext();

    context.addImport(Message.class);
    context.addImport(Item.class);

    Map vars = new HashMap();
    vars.put("$msg",
        new Message());
    ExpressionCompiler compiler = new ExpressionCompiler(expr, context);

    Message msg = (Message) executeExpression(compiler.compile(),
        vars);
    Item item = (Item) msg.getItems().get(0);
    assertEquals("Some Item",
        item.getName());
  }

  @Test
  @KnownCompatibility("block-syntax")
  public void testLateResolveOfClass() {
    TestCompilerContext ctx = new TestCompilerContext();
    ctx.addImport(Foo.class);

    ExpressionCompiler compiler = new ExpressionCompiler("System.out.println(new Foo());", ctx);
    System.out.println(executeExpression(compiler.compile()));
  }

  // Category not isolated: fails with a parse error at the end of the input. Single-quoted `'foobar'`
  // (single-quote-string) and the undeclared assignment `Foo244 = ...` (block-syntax) are further suspected causes that
  // were not tested separately; `var Foo244 = String;` fails with "cannot find symbol: variable String", and a Java
  // `new` cannot be applied to a variable.
  @Test
  @KnownCompatibility("class-alias")
  public void testClassAliasing() {
    assertEquals("foobar",
        test("Foo244 = String; new Foo244('foobar')"));
  }

  // Category not isolated: fails with a parse error at `import`. With the package imported through the API, the bare class
  // name `BufferedReader` as the value still fails (class-literal).
  @Test
  @KnownCompatibility("inline-import")
  public void testDynamicImports2() {
    assertEquals(BufferedReader.class,
        test("import java.io.*; BufferedReader"));
  }

  // Category not isolated: fails with "char cannot be converted to java.lang.Integer" because `'1'` is a `char`. With
  // `"1"` the error becomes "String cannot be converted to java.lang.Integer" (string-coercion, MVEL2 converts in the cast),
  // and the statement has no trailing `return` (block-syntax, not tested separately).
  @Test
  @KnownCompatibility("single-quote-string")
  public void testFullyQualifiedTypeAndCast() {
    assertEquals(1,
        test("java.lang.Integer number = (java.lang.Integer) '1';"));
  }

  public class Fruit {
    public class Apple {

    }
  }

  // Category not isolated: fails with a parse error at `import`. With the class imported through the API, the bare
  // `CoreImportsTests.Fruit.Apple` still fails ("cannot find symbol: variable Apple", class-literal); `.Apple.class` works.
  @Test
  @KnownCompatibility("inline-import")
  public void testInnerClassReference() {
    assertEquals(Fruit.Apple.class,
        test("import " + CoreImportsTests.class.getName() + "; CoreImportsTests.Fruit.Apple"));
  }

  // Category not isolated: fails with "No method candidates were found for method call 'isType'" because the root object's
  // method is called unqualified. The `$` binary name `MyInterface$MY_ENUM` (inner-class-binary-name) is a second suspected
  // cause; the dotted name fails the same way, and `m.isType(..)` with the root passed as an input works.
  @Test
  @KnownCompatibility("this-reference")
  public void testEnumSupport() {
    MyInterface myInterface = new MyClass();
    myInterface.setType(MyInterface.MY_ENUM.TWO,
        true);
    boolean isType = (Boolean) eval("isType(org.mvel3.compat.mvel2.res.MyInterface$MY_ENUM.ONE)",
        myInterface);
    System.out.println(isType);

  }

  @Test
  @KnownCompatibility("block-syntax")
  public void testJavaLangImport() throws Exception {
    String s = "Exception e = null;";
    TestCompilerContext pctx = new TestCompilerContext();
    // The original only compiles the expression; compile at once so that the result is checked.
    compileEagerly(s, pctx);
  }

  public static class Triangle {
    public static enum Foo {
      INCOMPLETE, UNCLASSIFIED,
      EQUILATERAL, ISOSCELES, RECTANGLED, ISOSCELES_RECTANGLED, ACUTE, OBTUSE;
    }
  }

  @Test
  @KnownCompatibility("inline-import")
  public void testNestedEnum() throws Exception {
    eval("import " + Triangle.class.getCanonicalName() + "; Triangle.Foo.OBTUSE", new HashMap());
  }

  // Category not isolated: with the original jar fails with "package org.drools.examples.eventing does not exist". With a
  // repacked copy that has directory entries, the package is found and the expression fails with "non-static method
  // getStatus() cannot be referenced from a static context" (`EventRequest.Status` is read as the `status` property).
  // Both are MVEL3 limitations; only the first has a category.
  @Test
  @KnownCompatibility("external-jar-classloader")
  public void testNestedEnumFromJar() throws ClassNotFoundException,
      SecurityException,
      NoSuchFieldException {
    String expr = "EventRequest.Status.ACTIVE";

    // creating a classloader for the jar
    URL resource = getClass().getResource("/eventing-example.jar");
    assertNotNull(resource);
    URLClassLoader loader = new URLClassLoader(new URL[]{resource},
        getClass().getClassLoader());

    // loading the class to prove it works
    Class<?> er = loader.loadClass("org.drools.examples.eventing.EventRequest");
    assertNotNull(er);
    assertEquals("org.drools.examples.eventing.EventRequest",
        er.getCanonicalName());

    // getting the value of the enum to prove it works:
    Class<?> st = er.getDeclaredClasses()[0];
    assertNotNull(st);
    Field active = st.getField("ACTIVE");
    assertNotNull(active);

    // now, trying with MVEL
    // ParserConfiguration.setClassLoader/addImport are replaced by the MVEL3 compile call that takes a class loader.
    Set<String> imports = new LinkedHashSet<>();
    imports.add(er.getCanonicalName());

    Object result = new MVEL().compileMapExpression(expr, Object.class, imports, new LinkedHashMap<>(),
        new ClassManager(), loader).eval(new HashMap());

    assertNotNull(result);
  }

  // Category not isolated: fails with "cannot find symbol: class MyInterface$STATIC_ENUM" for the `$` binary name; the
  // dotted names, the imported `STATIC_ENUM`/`RoundingMode` and `java.math.RoundingMode` compile. The block ends with
  // `println(..)` and no `return`; whether that also fails (block-syntax) was not tested separately.
  @Test
  @KnownCompatibility("inner-class-binary-name")
  public void testFullyQualifiedEnums() {
    String str = "System.out.println( STATIC_ENUM.FOO ); \n" +
        "System.out.println( org.mvel3.compat.mvel2.res.MyInterface$STATIC_ENUM.BAR );\n" +
        "System.out.println( org.mvel3.compat.mvel2.res.MyInterface.MyInnerInterface.INNER_STATIC_ENUM.BAR );\n" +
        "System.out.println( RoundingMode.UP );\n" +
        "System.out.println( java.math.RoundingMode.DOWN );";

    TestCompilerContext pctx = new TestCompilerContext();
    pctx.addInput("this", MyInterface.class);
    pctx.addImport(MyInterface.STATIC_ENUM.class);
    pctx.addImport(java.math.RoundingMode.class);
    // The original only compiles the expression; compile at once so that the result is checked.
    compileEagerly(str, pctx);
  }

  // Category not isolated: fails with "cannot find symbol: variable START". With `Status.START` and the package imported,
  // the expression works. Whether importing the class `Status` through the API (instead of its package) makes `START`
  // visible was observed to fail as well.
  @Test
  @KnownCompatibility("static-member-unqualified")
  public void testPackageImportEnum() {
    String str = "new Status( START )";
    TestCompilerContext context = new TestCompilerContext();
    context.addPackageImport("org.mvel3.compat.mvel2.res");
    context.addPackageImport("org.mvel3.compat.mvel2.res.Status");

    Serializable s = compileExpression(str.trim(), context);
    assertEquals(new Status(Status.START), executeExpression(s));
    assertFalse(new Status(Status.STOP).equals(executeExpression(s)));
  }

  public static class ARef {
    public static int getSize(String s) {
      return 0;
    }
  }

  @Test
  public void testInstanceofOnInnerClass() {
    TestCompilerContext pctx = new TestCompilerContext();
    pctx.addImport(ARef.class);
    pctx.addInput("value", Object.class);
    Map vars = new HashMap() {{ put("value", new ARef()); }};
    assertEquals(true, executeExpression(compileExpression("value instanceof ARef", pctx), vars));
    assertEquals(true, executeExpression(compileExpression("value instanceof " + ARef.class.getCanonicalName(), pctx), vars));
  }

  @Test
  public void testInstanceofWithPackageImport() {
    TestCompilerContext pctx = new TestCompilerContext();
    pctx.addPackageImport("org.mvel3.compat.mvel2");
    pctx.addInput("value", Object.class);
    Map vars = new HashMap() {{ put("value", new CoreImportsTests()); }};
    assertEquals(true, executeExpression(compileExpression("value instanceof CoreImportsTests", pctx), vars));
    assertEquals(true, executeExpression(compileExpression("value instanceof " + CoreImportsTests.class.getCanonicalName(), pctx), vars));
  }

  @Test
  public void testInstanceofWithPackageImportAndInnerClass() {
    TestCompilerContext pctx = new TestCompilerContext();
    pctx.addPackageImport("org.mvel3.compat.mvel2.CoreImportsTests");
    pctx.addInput("value", Object.class);
    Map vars = new HashMap() {{ put("value", new ARef()); }};
    assertEquals(true, executeExpression(compileExpression("value instanceof ARef", pctx), vars));
    assertEquals(true, executeExpression(compileExpression("value instanceof " + ARef.class.getCanonicalName(), pctx), vars));
  }
}
