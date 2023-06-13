package org.mvel2.tests.core;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;
import org.junit.Assert;
import org.mvel2.CompileException;
import org.mvel2.DataConversion;
import org.mvel2.MVEL;
import org.mvel2.Macro;
import org.mvel2.ParserConfiguration;
import org.mvel2.ParserContext;
import org.mvel2.PropertyAccessException;
import org.mvel2.PropertyAccessor;
import org.mvel2.ast.ASTNode;
import org.mvel2.compiler.CompiledExpression;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.compiler.ExpressionCompiler;
import org.mvel2.integration.Interceptor;
import org.mvel2.integration.PropertyHandlerFactory;
import org.mvel2.integration.ResolverTools;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.ClassImportResolverFactory;
import org.mvel2.integration.impl.DefaultLocalVariableResolverFactory;
import org.mvel2.integration.impl.IndexedVariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;
import org.mvel2.optimizers.OptimizerFactory;
import org.mvel2.tests.core.res.AStatic;
import org.mvel2.tests.core.res.Bar;
import org.mvel2.tests.core.res.Base;
import org.mvel2.tests.core.res.Cheese;
import org.mvel2.tests.core.res.Cheesery;
import org.mvel2.tests.core.res.Column;
import org.mvel2.tests.core.res.DefaultKnowledgeHelper;
import org.mvel2.tests.core.res.Foo;
import org.mvel2.tests.core.res.Grid;
import org.mvel2.tests.core.res.KnowledgeHelper;
import org.mvel2.tests.core.res.KnowledgeHelperFixer;
import org.mvel2.tests.core.res.MapObject;
import org.mvel2.tests.core.res.MyClass;
import org.mvel2.tests.core.res.MyInterface;
import org.mvel2.tests.core.res.OverloadedInterface;
import org.mvel2.tests.core.res.PojoStatic;
import org.mvel2.tests.core.res.RuleBase;
import org.mvel2.tests.core.res.RuleBaseImpl;
import org.mvel2.tests.core.res.SampleBean;
import org.mvel2.tests.core.res.SampleBeanAccessor;
import org.mvel2.tests.core.res.Ship;
import org.mvel2.tests.core.res.Status;
import org.mvel2.tests.core.res.TestClass;
import org.mvel2.tests.core.res.User;
import org.mvel2.tests.core.res.WorkingMemory;
import org.mvel2.tests.core.res.WorkingMemoryImpl;
import org.mvel2.tests.core.res.res2.ClassProvider;
import org.mvel2.tests.core.res.res2.Outer;
import org.mvel2.tests.core.res.res2.OverloadedClass;
import org.mvel2.tests.core.res.res2.PublicClass;
import org.mvel2.util.ParseTools;
import org.mvel2.util.ReflectionUtil;

import static java.util.Collections.unmodifiableCollection;
import static org.mvel2.MVEL.compileExpression;
import static org.mvel2.MVEL.compileSetExpression;
import static org.mvel2.MVEL.evalToBoolean;
import static org.mvel2.MVEL.executeExpression;
import static org.mvel2.MVEL.executeSetExpression;
import static org.mvel2.MVEL.parseMacros;
import static org.mvel2.MVEL.setProperty;
import static org.mvel2.util.ParseTools.loadFromFile;

@SuppressWarnings({"ALL"})
public class CoreConfidenceTests extends AbstractTest {
  public void testWhileUsingImports() {
    Map<String, Object> imports = new HashMap<String, Object>();
    imports.put("ArrayList",
        java.util.ArrayList.class);
    imports.put("List",
        java.util.List.class);

    ParserContext context = new ParserContext(imports, null, "testfile");
    ExpressionCompiler compiler = new ExpressionCompiler("List list = new ArrayList(); return (list == empty)", context);
    assertTrue((Boolean) executeExpression(compiler.compile(),
        new DefaultLocalVariableResolverFactory()));
  }

  public void testBooleanModeOnly2() {
    assertEquals(false, (Object) DataConversion.convert(test("BWAH"), Boolean.class));
  }

  public void testBooleanModeOnly4() {
    assertEquals(true, test("hour == (hour + 0)"));
  }

  // interpreted

  public void testThisReferenceMapVirtualObjects() {
    Map<String, String> map = new HashMap<String, String>();
    map.put("foo",
        "bar");

    VariableResolverFactory factory = new MapVariableResolverFactory(new HashMap<String, Object>());
    factory.createVariable("this", map);

    assertEquals(true,
        eval("this.foo == 'bar'", map, factory));
  }


  // compiled - reflective

  public void testThisReferenceMapVirtualObjects1() {
    // Create our root Map object
    Map<String, String> map = new HashMap<String, String>();
    map.put("foo", "bar");

    VariableResolverFactory factory = new MapVariableResolverFactory(new HashMap<String, Object>());
    factory.createVariable("this", map);

    OptimizerFactory.setDefaultOptimizer("reflective");

    // Run test
    assertEquals(true,
        executeExpression(compileExpression("this.foo == 'bar'"),
            map,
            factory));
  }

  // compiled - asm

  public void testThisReferenceMapVirtualObjects2() {
    // Create our root Map object
    Map<String, String> map = new HashMap<String, String>();
    map.put("foo",
        "bar");

    VariableResolverFactory factory = new MapVariableResolverFactory(new HashMap<String, Object>());
    factory.createVariable("this",
        map);

    // I think we can all figure this one out.

    if (!Boolean.getBoolean("mvel2.disable.jit")) OptimizerFactory.setDefaultOptimizer("ASM");

    // Run test
    assertEquals(true,
        executeExpression(compileExpression("this.foo == 'bar'"),
            map,
            factory));
  }

  public void testEvalToBoolean() {
    assertEquals(true,
        (boolean) evalToBoolean("true ",
            "true"));
    assertEquals(true,
        (boolean) evalToBoolean("true ",
            "true"));
  }

  public void testImport() {
    assertEquals(HashMap.class,
        test("import java.util.HashMap; HashMap;"));
  }

  public void testImport2() {
    HashMap[] maps = (HashMap[]) MVEL.eval("import java.util.*; HashMap[] maps = new HashMap[10]; maps",
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

  public void testUnusedPackageImport() {
    String expression =
        "import java.util.*; " +
            "name == \"Mario\" ";
    Serializable compiledExpression = MVEL.compileExpression(expression);
    boolean result = (Boolean) MVEL.executeExpression(compiledExpression, new MyPerson("Mario"));
    assertTrue(result);
  }

  public void testStaticImport() {
    assertEquals(2.0,
        test("import_static java.lang.Math.sqrt; sqrt(4)"));
  }

  /**
   * Start collections framework based compliance tests
   */
  public void testCreationOfSet() {
    assertEquals("foo bar foo bar",
        test("set = new java.util.LinkedHashSet(); "
            + "set.add('foo');" + "set.add('bar');"
            + "output = '';" + "foreach (item : set) {"
            + "output = output + item + ' ';"
            + "} "
            + "foreach (item : set) {"
            + "output = output + item + ' ';"
            + "} " + "output = output.trim();"
            + "if (set.size() == 2) { return output; }"));

  }

  public void testCreationOfList() {
    assertEquals(5,
        test("l = new java.util.LinkedList(); l.add('fun'); l.add('happy'); l.add('fun'); l.add('slide');"
            + "l.add('crap'); poo = new java.util.ArrayList(l); poo.size();"));
  }

  public void testMapOperations() {
    assertEquals("poo5",
        test("l = new java.util.ArrayList(); l.add('plop'); l.add('poo'); m = new java.util.HashMap();"
            + "m.put('foo', l); m.put('cah', 'mah'); m.put('bar', 'foo'); m.put('sarah', 'mike');"
            + "m.put('edgar', 'poe'); if (m.edgar == 'poe') { return m.foo[1] + m.size(); }"));
  }

  public void testStackOperations() {
    assertEquals(10,
        test("stk = new java.util.Stack();" + "stk.push(5);" + "stk.push(5);" + "stk.pop() + stk.pop();"));
  }

  public void testSystemOutPrint() {
    test("a = 0;\r\nSystem.out.println('This is a test');");
  }


//    public void testClassImportViaFactory() {
//        MapVariableResolverFactory mvf = new MapVariableResolverFactory(createTestMap());
//        ClassImportResolverFactory classes = new ClassImportResolverFactory();
//        classes.addClass(HashMap.class);
//
//        ResolverTools.appendFactory(mvf, classes);
//
//        assertTrue(executeExpression(compileExpression("HashMap map = new HashMap()",
//                classes.getImportedClasses()),
//                mvf) instanceof HashMap);
//    }
//
//    public void testSataticClassImportViaFactory() {
//        MapVariableResolverFactory mvf = new MapVariableResolverFactory(createTestMap());
//        ClassImportResolverFactory classes = new ClassImportResolverFactory();
//        classes.addClass(Person.class);
//
//        ResolverTools.appendFactory(mvf,
//                classes);
//
//        assertEquals("tom",
//                executeExpression(compileExpression("p = new Person('tom'); return p.name;",
//                        classes.getImportedClasses()),
//                        mvf));
//    }


  public void testCheeseConstructor() {
    MapVariableResolverFactory mvf = new MapVariableResolverFactory(createTestMap());
    ClassImportResolverFactory classes = new ClassImportResolverFactory(null, null, false);
    classes.addClass(Cheese.class);

    ResolverTools.appendFactory(mvf,
        classes);
    assertTrue(executeExpression(compileExpression("cheese = new Cheese(\"cheddar\", 15);",
        classes.getImportedClasses()),
        mvf) instanceof Cheese);
  }

  public void testInterceptors() {
    Interceptor testInterceptor = new Interceptor() {
      public int doBefore(ASTNode node,
                          VariableResolverFactory factory) {
        System.out.println("BEFORE Node: " + node.getName());
        return 0;
      }

      public int doAfter(Object val,
                         ASTNode node,
                         VariableResolverFactory factory) {
        System.out.println("AFTER Node: " + node.getName());
        return 0;
      }
    };

    Map<String, Interceptor> interceptors = new HashMap<String, Interceptor>();
    interceptors.put("test",
        testInterceptor);

    executeExpression(compileExpression("@test System.out.println('MIDDLE');",
        null,
        interceptors));
  }


  public void testSubtractNoSpace1() {
    assertEquals(59,
        test("hour-1"));
  }

  public void testStrictTypingCompilation() {
    //  OptimizerFactory.setDefaultOptimizer(OptimizerFactory.DYNAMIC);

    ParserContext ctx = new ParserContext();
    ctx.setStrictTypeEnforcement(true);
    ExpressionCompiler compiler = new ExpressionCompiler("a.foo;\nb.foo;\n x = 5", ctx);

    try {
      compiler.compile();
    }
    catch (CompileException e) {
      e.printStackTrace();
      return;
    }
    assertTrue(false);

    // OptimizerFactory.setDefaultOptimizer(OptimizerFactory.DYNAMIC);
  }


  public void testEqualityRegression() {
    ExpressionCompiler compiler = new ExpressionCompiler("price == (new Integer( 5 ) + 5 ) ");
    compiler.compile();
  }

  public void testEvaluationRegression() {
    ExpressionCompiler compiler = new ExpressionCompiler("(p.age * 2)");
    compiler.compile();
    assertTrue(compiler.getParserContextState().getInputs().containsKey("p"));
  }

  public void testIncrementAndAssignWithInputs() {
    ExpressionCompiler compiler = new ExpressionCompiler("total += cheese");
    compiler.compile();
    assertTrue(compiler.getParserContextState().getInputs().containsKey("total"));
    assertTrue(compiler.getParserContextState().getInputs().containsKey("cheese"));
  }


  public void testAssignmentRegression() {
    ExpressionCompiler compiler = new ExpressionCompiler("total = total + $cheese.price");
    compiler.compile();
  }

  public void testTypeRegression() {
    ParserContext ctx = new ParserContext();
    ctx.setStrictTypeEnforcement(true);
    ExpressionCompiler compiler = new ExpressionCompiler("total = 0", ctx);
    compiler.compile();
    assertEquals(Integer.class,
        compiler.getParserContextState().getVarOrInputType("total"));
  }

  public void testTestIntToLong() {
    String s = "1+(long)a";

    ParserContext pc = new ParserContext();
    pc.addInput("a", Integer.class);

    ExpressionCompiler compiler = new ExpressionCompiler(s, pc);
    CompiledExpression expr = compiler.compile();

    Map vars = new HashMap();
    vars.put("a", 1);

    Object r = ((ExecutableStatement) expr).getValue(null, new MapVariableResolverFactory(vars));
    assertEquals(new Long(2), r);
  }

  public void testMapPropertyCreateCondensed() {
    assertEquals("foo",
        test("map = new java.util.HashMap(); map['test'] = 'foo'; map['test'];"));
  }


  public void testDeepMethod() {
    assertEquals(false,
        test("foo.bar.testList.add(new String()); foo.bar.testList == empty"));
  }


  public void testListAccessorAssign() {
    String ex = "a = new java.util.ArrayList(); a.add('foo'); a.add('BAR'); a[1] = 'bar'; a[1]";

    OptimizerFactory.setDefaultOptimizer("ASM");

    Serializable s = MVEL.compileExpression(ex);
    assertEquals("bar", MVEL.executeExpression(s, new HashMap()));

    OptimizerFactory.setDefaultOptimizer(OptimizerFactory.DYNAMIC);

    assertEquals("bar",
        test("a = new java.util.ArrayList(); a.add('foo'); a.add('BAR'); a[1] = 'bar'; a[1]"));
  }

  public void testBracketInString() {
    assertEquals("1)your guess was:", test("System.out.println(\"1)your guess was:\"); return \"1)your guess was:\";"));
  }

  public void testNesting() {
    assertEquals("foo",
        test("new String(new String(new String(\"foo\")));"));
  }

  public void testTypeCast() {
    assertEquals("10",
        test("(String) 10"));
  }

  public void testTypeCast2() {
    String ex = "var map = new java.util.HashMap(); map.put(\"doggie\", new java.util.ArrayList());" +
        "return ((java.util.ArrayList) map[\"doggie\"]).size();";

    Map map = createTestMap();

    assertEquals(0, eval(ex, null, map));

    assertEquals(0,
        test(ex));
  }


  public void testMapAccessSemantics() {
    Map<String, Object> outermap = new HashMap<String, Object>();
    Map<String, Object> innermap = new HashMap<String, Object>();

    innermap.put("test",
        "foo");
    outermap.put("innermap",
        innermap);

    assertEquals("foo",
        testCompiledSimple("innermap['test']",
            outermap,
            null));
  }

  public void testMapBindingSemantics() {
    Map<String, Object> outermap = new HashMap<String, Object>();
    Map<String, Object> innermap = new HashMap<String, Object>();

    innermap.put("test",
        "foo");
    outermap.put("innermap",
        innermap);

    setProperty(outermap,
        "innermap['test']",
        "bar");

    assertEquals("bar",
        testCompiledSimple("innermap['test']",
            outermap,
            null));
  }

  public void testMapNestedInsideList() {
    ParserContext ctx = new ParserContext();
    ctx.addImport("User",
        User.class);

    ExpressionCompiler compiler =
        new ExpressionCompiler("users = [ 'darth'  : new User('Darth', 'Vadar')," +
            "\n'bobba' : new User('Bobba', 'Feta') ]; [ users.get('darth'), users.get('bobba') ]", ctx);
    //    Serializable s = compiler.compileShared(ctx);
    List list = (List) executeExpression(compiler.compile(),
        new HashMap());
    User user = (User) list.get(0);
    assertEquals("Darth",
        user.getFirstName());
    user = (User) list.get(1);
    assertEquals("Bobba",
        user.getFirstName());

    compiler =
        new ExpressionCompiler("users = [ 'darth'  : new User('Darth', 'Vadar')," +
            "\n'bobba' : new User('Bobba', 'Feta') ]; [ users['darth'], users['bobba'] ]", ctx);
    list = (List) executeExpression(compiler.compile(),
        new HashMap());
    user = (User) list.get(0);
    assertEquals("Darth",
        user.getFirstName());
    user = (User) list.get(1);
    assertEquals("Bobba",
        user.getFirstName());
  }

  public void testListNestedInsideList() {
    ParserContext ctx = new ParserContext();
    ctx.addImport("User",
        User.class);

    ExpressionCompiler compiler =
        new ExpressionCompiler("users = [ new User('Darth', 'Vadar'), " +
            "new User('Bobba', 'Feta') ]; [ users.get( 0 ), users.get( 1 ) ]", ctx);
    List list = (List) executeExpression(compiler.compile(),
        new HashMap());
    User user = (User) list.get(0);
    assertEquals("Darth",
        user.getFirstName());
    user = (User) list.get(1);
    assertEquals("Bobba",
        user.getFirstName());

    compiler = new ExpressionCompiler("users = [ new User('Darth', 'Vadar'), " +
        "new User('Bobba', 'Feta') ]; [ users[0], users[1] ]", ctx);
    list = (List) executeExpression(compiler.compile(),
        new HashMap());
    user = (User) list.get(0);
    assertEquals("Darth",
        user.getFirstName());
    user = (User) list.get(1);
    assertEquals("Bobba",
        user.getFirstName());
  }

  public void testSetSemantics() {
    Bar bar = new Bar();
    Foo foo = new Foo();

    assertEquals("dog",
        MVEL.getProperty("name",
            bar));
    assertEquals("dog",
        MVEL.getProperty("name",
            foo));
  }

  public void testMapBindingSemantics2() {
    OptimizerFactory.setDefaultOptimizer("ASM");
    Map<String, Object> outermap = new HashMap<String, Object>();
    Map<String, Object> innermap = new HashMap<String, Object>();

    innermap.put("test",
        "foo");
    outermap.put("innermap",
        innermap);

    executeSetExpression(compileSetExpression("innermap['test']"),
        outermap,
        "bar");

    assertEquals("bar",
        testCompiledSimple("innermap['test']",
            outermap,
            null));
  }

  public void testDynamicImports() {
    ParserContext ctx = new ParserContext();
    ctx.addPackageImport("java.util");

    ExpressionCompiler compiler = new ExpressionCompiler("HashMap", ctx);
    Serializable s = compiler.compile();

    assertEquals(HashMap.class,
        executeExpression(s));

    compiler = new ExpressionCompiler("map = new HashMap(); map.size()", ctx);
    s = compiler.compile();

    assertEquals(0,
        executeExpression(s,
            new DefaultLocalVariableResolverFactory()));
  }

  public void testDynamicImports3() {
    String expression = "import java.util.*; HashMap map = new HashMap(); map.size()";

    ExpressionCompiler compiler = new ExpressionCompiler(expression);
    Serializable s = compiler.compile();

    assertEquals(0,
        executeExpression(s,
            new DefaultLocalVariableResolverFactory()));

    assertEquals(0,
        MVEL.eval(expression,
            new HashMap()));
  }

  public void testDynamicImportsInList() {
    ParserContext ctx = new ParserContext();
    ctx.addPackageImport("org.mvel2.tests.core.res");

    ExpressionCompiler compiler = new ExpressionCompiler("[ new User('Bobba', 'Feta') ]", ctx);
    List list = (List) executeExpression(compiler.compile());
    User user = (User) list.get(0);
    assertEquals("Bobba",
        user.getFirstName());
  }

  public void testDynamicImportsInMap() {
    ParserContext ctx = new ParserContext();
    ctx.addPackageImport("org.mvel2.tests.core.res");

    ExpressionCompiler compiler = new ExpressionCompiler("[ 'bobba' : new User('Bobba', 'Feta') ]", ctx);
    Map map = (Map) executeExpression(compiler.compile());
    User user = (User) map.get("bobba");
    assertEquals("Bobba",
        user.getFirstName());
  }

  public void testDynamicImportsOnNestedExpressions() {
    ParserContext ctx = new ParserContext();
    ctx.addPackageImport("org.mvel2.tests.core.res");

    ExpressionCompiler compiler = new ExpressionCompiler("new Cheesery(\"bobbo\", new Cheese(\"cheddar\", 15))", ctx);

    Cheesery p1 = new Cheesery("bobbo",
        new Cheese("cheddar",
            15));
    Cheesery p2 = (Cheesery) executeExpression(compiler.compile(),
        new DefaultLocalVariableResolverFactory());

    assertEquals(p1,
        p2);
  }

  public void testDynamicImportsWithNullConstructorParam() {
    ParserContext ctx = new ParserContext();
    ctx.addPackageImport("org.mvel2.tests.core.res");

    ExpressionCompiler compiler = new ExpressionCompiler("new Cheesery(\"bobbo\", null)", ctx);

    Cheesery p1 = new Cheesery("bobbo",
        null);
    Cheesery p2 = (Cheesery) executeExpression(compiler.compile(),
        new DefaultLocalVariableResolverFactory());

    assertEquals(p1,
        p2);
  }

  public void testDynamicImportsWithNullConstructorParamWithStrongType() {
    ParserContext ctx = new ParserContext();
    ctx.setStrongTyping(true);
    ctx.addPackageImport("org.mvel2.tests.core.res");

    ExpressionCompiler compiler = new ExpressionCompiler("new Cheesery(\"bobbo\", null)", ctx);

    Cheesery p1 = new Cheesery("bobbo",
        null);
    Cheesery p2 = (Cheesery) executeExpression(compiler.compile(),
        new DefaultLocalVariableResolverFactory());

    assertEquals(p1,
        p2);
  }

  public void testDynamicImportsWithIdentifierSameAsClassWithDiffCase() {
    ParserContext ctx = new ParserContext();
    ctx.addPackageImport("org.mvel2.tests.core.res");
    ctx.setStrictTypeEnforcement(false);

    ExpressionCompiler compiler = new ExpressionCompiler("bar.add(\"hello\")", ctx);
    compiler.compile();
  }

  public void testTypedAssignment() {
    assertEquals("foobar",
        test("java.util.Map map = new java.util.HashMap(); map.put(\"conan\", \"foobar\"); map[\"conan\"];"));
  }

  public void testFQCNwithStaticInList() {
    assertEquals(Integer.MIN_VALUE,
        test("var list = [java.lang.Integer.MIN_VALUE]; list[0];"));
  }

  public void testPrecedenceOrder() {
    assertTrue((Boolean) test("5 > 6 && 2 < 1 || 10 > 9"));
  }

  @SuppressWarnings({"unchecked"})
  public void testDifferentImplSameCompile() {
    Serializable compiled = compileExpression("a.funMap.hello");

    Map testMap = new HashMap();

    for (int i = 0; i < 100; i++) {
      Base b = new Base();
      b.funMap.put("hello",
          "dog");
      testMap.put("a",
          b);

      assertEquals("dog",
          executeExpression(compiled,
              testMap));

      b = new Base();
      b.funMap.put("hello",
          "cat");
      testMap.put("a",
          b);

      assertEquals("cat",
          executeExpression(compiled,
              testMap));
    }
  }

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

  @SuppressWarnings({"unchecked"})
  public void testInterfaceMethodCallWithMacro() {
    Map macros = new HashMap(1);

    macros.put("retract",
        new Macro() {
          public String doMacro() {
            return "drools.retract";
          }
        });

    Map map = new HashMap();
    DefaultKnowledgeHelper helper = new DefaultKnowledgeHelper();
    map.put("drools",
        helper);
    Cheese cheese = new Cheese("stilton",
        15);
    map.put("cheese",
        cheese);

    executeExpression(compileExpression(parseMacros("retract(cheese)",
        macros)),
        map);
    assertSame(cheese,
        helper.retracted.get(0));
  }

  public void testParsingStability1() {
    assertEquals(true,
        test("( order.number == 1 || order.number == ( 1+1) || order.number == $id )"));
  }

  public void testParsingStability2() {
    Map<String, Object> imports = new HashMap<String, Object>();
    imports.put("java.awt.Dimension",
        Dimension.class);

    final ParserContext parserContext = new ParserContext(imports, null, "sourceFile");
    parserContext.setStrictTypeEnforcement(false);

    ExpressionCompiler compiler =
      new ExpressionCompiler("( dim.height == 1 || dim.height == ( 1+1) || dim.height == x )", parserContext);
    compiler.compile();
  }

  public void testConcatWithLineBreaks() {

    ParserContext ctx = new ParserContext();
    ctx.setDebugSymbols(true);
    ctx.setSourceFile("source.mv");

    ExpressionCompiler parser = new ExpressionCompiler("\"foo\"+\n\"bar\"", ctx);
    assertEquals("foobar",
        executeExpression(parser.compile()));
  }

  /**
   * Provided by: Alex Roytman
   */
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

  /**
   * Provided by: Phillipe Ombredanne
   */
  public void testCompileParserContextShouldNotLoopIndefinitelyOnValidJavaExpression() {
    String expr = "		System.out.println( message );\n" + //
        "m.setMessage( \"Goodbye cruel world\" );\n" + //
        "System.out.println(m.getStatus());\n" + //
        "m.setStatus( Message.GOODBYE );\n";

    ParserContext context = new ParserContext();
    context.setStrictTypeEnforcement(false);

    context.addImport("Message",
        Message.class);
    context.addInput("System",
        void.class);
    context.addInput("message",
        Object.class);
    context.addInput("m",
        Object.class);

    ExpressionCompiler compiler = new ExpressionCompiler(expr, context);
    compiler.compile();
  }

  public void testStaticNested() {
    assertEquals(1,
        eval("org.mvel2.tests.core.AbstractTest$Message.GOODBYE",
            new HashMap()));
  }

  public void testStaticNestedWithImport() {
    String expr = "Message.GOODBYE;\n";

    ParserContext context = new ParserContext();
    context.setStrictTypeEnforcement(false);

    context.addImport("Message",
        Message.class);

    ExpressionCompiler compiler = new ExpressionCompiler(expr, context);
    assertEquals(1,
        executeExpression(compiler.compile()));
  }

  public void testStaticNestedWithMethodCall() {
    String expr = "item = new Item( \"Some Item\"); $msg.addItem( item ); return $msg";

    ParserContext context = new ParserContext();
    context.setStrictTypeEnforcement(false);

    context.addImport("Message",
        Message.class);
    context.addImport("Item",
        Item.class);
    //   Serializable compiledExpression = compiler.compileShared(context);

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

  public void testsequentialAccessorsThenMethodCall() {
    String expr = "System.out.println(drools.workingMemory); " +
        "drools.workingMemory.ruleBase.removeRule(\"org.drools.examples\", \"some rule\"); ";

    ParserContext context = new ParserContext();
    context.setStrictTypeEnforcement(true);
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

  /**
   * Provided by: Aadi Deshpande
   */
  public void testPropertyVerfierShoudldNotLoopIndefinately() {
    String expr = "\t\tmodel.latestHeadlines = $list;\n"
        + "model.latestHeadlines.add( 0, (model.latestHeadlines[2]) );";

    ParserContext pCtx = new ParserContext();
    pCtx.addInput("$list",
        List.class);
    pCtx.addInput("model",
        Model.class);

    ExpressionCompiler compiler = new ExpressionCompiler(expr, pCtx);
    compiler.setVerifying(true);
    compiler.compile();
  }

  public void testCompileWithNewInsideMethodCall() {
    String expr = "     p.name = \"goober\";\n" + "     System.out.println(p.name);\n"
        + "     drools.insert(new Address(\"Latona\"));\n";

    ParserContext context = new ParserContext();
    context.setStrictTypeEnforcement(false);

    context.addImport("Person",
        Person.class);
    context.addImport("Address",
        Address.class);

    context.addInput("p",
        Person.class);
    context.addInput("drools",
        Drools.class);

    ExpressionCompiler compiler = new ExpressionCompiler(expr, context);
    compiler.compile();
  }

  /**
   * Submitted by: cleverpig
   */
  public void testBug4() {
    ClassA A = new ClassA();
    ClassB B = new ClassB();
    System.out.println(MVEL.getProperty("date",
        A));
    System.out.println(MVEL.getProperty("date",
        B));
  }

  public void testIndexer() {
    assertEquals("foobar",
        testCompiledSimple("import java.util.LinkedHashMap; LinkedHashMap map = new LinkedHashMap();"
            + " map.put('a', 'foo'); map.put('b', 'bar'); s = ''; " +
            "foreach (key : map.keySet()) { System.out.println(map[key]); s += map[key]; }; return s;",
            createTestMap()));
  }

  public void testLateResolveOfClass() {
    ParserContext ctx = new ParserContext();
    ctx.addImport(Foo.class);

    ExpressionCompiler compiler = new ExpressionCompiler("System.out.println(new Foo());", ctx);
    System.out.println(executeExpression(compiler.compile()));
  }

  public void testClassAliasing() {
    assertEquals("foobar",
        test("Foo244 = String; new Foo244('foobar')"));
  }

  public void testRandomExpression1() {
    assertEquals("HelloWorld",
        test("if ((x15 = foo.bar) == foo.bar && x15 == foo.bar) { return 'HelloWorld'; } " +
            "else { return 'GoodbyeWorld' } "));
  }

  public void testRandomExpression4() {
    assertEquals(true,
        test("result = org.mvel2.MVEL.eval('10 * 3'); result == (10 * 3);"));
  }

  public void testRandomExpression5() {
    assertEquals(true,
        test("FooClassRef = foo.getClass(); fooInst = new FooClassRef();" +
            " name = org.mvel2.MVEL.eval('name', fooInst); return name == 'dog'"));
  }

  public void testRandomExpression6() {
    assertEquals(500,
        test("exprString = '250' + ' ' + '*' + ' ' + '2'; " +
            "compiledExpr = org.mvel2.MVEL.compileExpression(exprString);"
            + " return org.mvel2.MVEL.executeExpression(compiledExpr);"));
  }

  public void testRandomExpression7() {
    assertEquals("FOOBAR",
        test("'foobar'.toUpperCase();"));
  }

  public void testRandomExpression8() {
    assertEquals(true,
        test("'someString'.intern(); 'someString'.hashCode() == 'someString'.hashCode();"));
  }

  public void testRandomExpression9() {
    assertEquals(false,
        test("_abc = 'someString'.hashCode(); _xyz = _abc + 1; _abc == _xyz"));
  }

  public void testRandomExpression10() {
    assertEquals(false,
        test("(_abc = (_xyz = 'someString'.hashCode()) + 1); _abc == _xyz"));
  }

  /**
   * Submitted by: Guerry Semones
   */
  private Map<Object, Object> outerMap;
  private Map<Object, Object> innerMap;

  public void testAddIntToMapWithMapSyntax() throws Throwable {
    outerMap = new HashMap<Object, Object>();
    innerMap = new HashMap<Object, Object>();
    outerMap.put("innerMap",
        innerMap);

    // fails because mvel2 checks for 'tak' in the outerMap,
    // rather than inside innerMap in outerMap
    PropertyAccessor.set(outerMap,
        "innerMap['foo']",
        42);

    // instead of here
    assertEquals(42,
        innerMap.get("foo"));
  }

  public void testUpdateIntInMapWithMapSyntax() throws Throwable {
    outerMap = new HashMap<Object, Object>();
    innerMap = new HashMap<Object, Object>();
    outerMap.put("innerMap",
        innerMap);

    // fails because mvel2 checks for 'tak' in the outerMap,
    // rather than inside innerMap in outerMap
    innerMap.put("foo",
        21);
    PropertyAccessor.set(outerMap,
        "innerMap['foo']",
        42);

    // instead of updating it here
    assertEquals(42,
        innerMap.get("foo"));
  }

  private HashMap<String, Object> context = new HashMap<String, Object>();

  public void before() {
    HashMap<String, Object> map = new HashMap<String, Object>();

    MyBean bean = new MyBean();
    bean.setVar(4);

    map.put("bean",
        bean);
    context.put("map",
        map);
  }

  public void testDeepProperty() {
    before();

    Object obj = executeExpression(compileExpression("map.bean.var"),
        context);
    assertEquals(4,
        obj);
  }

  public void testDeepProperty2() {
    before();

    Object obj = executeExpression(compileExpression("map.bean.getVar()"),
        context);
    assertEquals(4,
        obj);
  }

  public class MyBean {
    int var;

    public int getVar() {
      return var;
    }

    public void setVar(int var) {
      this.var = var;
    }
  }


  public void testBooleanEvaluation() {
    assertEquals(true,
        test("true||false||false"));
  }

  public void testBooleanEvaluation2() {
    assertEquals(true,
        test("equalityCheck(1,1)||fun||ackbar"));
  }


  public void testStaticWithExplicitParam() {
    PojoStatic pojo = new PojoStatic("10");
    Map<String, Object> vars = new HashMap<>();
    vars.put("pojo", pojo);

    assertEquals("10", testCompiledSimple("org.mvel2.tests.core.res.AStatic.Process(\"10\");"));
    assertEquals(pojo.getValue(), testCompiledSimple("org.mvel2.tests.core.res.AStatic.Process(pojo.value);", vars));
  }

  public void testSimpleExpression() {
    PojoStatic pojo = new PojoStatic("10");
    assertEquals( true, eval("value != null", pojo));
  }

  public void testStaticWithExpressionParam() {
    PojoStatic pojo = new PojoStatic("10");
    assertEquals("java.lang.String", eval("org.mvel2.tests.core.res.AStatic.Process(value.getClass().getName().toString())",pojo));
  }

  public void testStringIndex() {
    assertEquals(true,
        test("a = 'foobar'; a[4] == 'a'"));
  }


  public void testAssertKeyword() {
    ExpressionCompiler compiler = new ExpressionCompiler("assert 1 == 2;");
    Serializable s = compiler.compile();

    try {
      executeExpression(s);
    }
    catch (AssertionError e) {
      return;
    }

    assertTrue(false);
  }

  public void testNullSafe() {
    Foo foo = new Foo();

    Map map = new HashMap();
    map.put("foo", foo);

    String expression = "foo.?bar.name == null";
    Serializable compiled = compileExpression(expression);

    OptimizerFactory.setDefaultOptimizer("reflective");
    assertEquals(false, executeExpression(compiled, map));
    foo.setBar(null);
    assertEquals(true, executeExpression(compiled, map)); // execute a second time (to search for optimizer problems)

    OptimizerFactory.setDefaultOptimizer("ASM");
    compiled = compileExpression(expression);
    foo.setBar(new Bar());
    assertEquals(false, executeExpression(compiled, map));
    foo.setBar(null);
    assertEquals(true, executeExpression(compiled, map)); // execute a second time (to search for optimizer problems)

    assertEquals(true, eval(expression, map));
  }

  public void testNullSafe2() {
    Foo foo = new Foo();
    Bar bar = new Bar();
    foo.setBar(bar);
    bar.setName(null);

    Map map = new HashMap();
    map.put("foo", foo);

    String expression = "x = foo.bar.?name.length == null";
    Serializable compiled = compileExpression(expression);

    OptimizerFactory.setDefaultOptimizer("reflective");
    assertEquals(true, executeExpression(compiled, map));
    bar.setName("x");
    assertEquals(false, executeExpression(compiled, map)); // execute a second time (to search for optimizer problems)

    OptimizerFactory.setDefaultOptimizer("ASM");
    compiled = compileExpression(expression);
    bar.setName(null);
    assertEquals(true, executeExpression(compiled, map));
    bar.setName("x");
    assertEquals(false, executeExpression(compiled, map)); // execute a second time (to search for optimizer problems)

    assertEquals(false, eval(expression, map));
  }

  /**
   * MVEL-57 (Submitted by: Rognvald Eaversen) -- Slightly modified by cbrock to include a positive testcase.
   */
  public void testMethodInvocationWithCollectionElement() {
    context = new HashMap();
    context.put("pojo",
        new POJO());
    context.put("number",
        "1192800637980");

    Object result = MVEL.eval("pojo.function(pojo.dates[0].time)",
        context);
    assertEquals(String.valueOf(((POJO) context.get("pojo")).getDates().iterator().next().getTime()),
        result);
  }


  public class POJO {
    private Set<Date> dates = new HashSet<Date>();
    private Map<String, Object> map = new HashMap<String, Object>();

    public POJO() {
      dates.add(new Date());
    }

    public Set<Date> getDates() {
      return dates;
    }

    public void setDates(Set<Date> dates) {
      this.dates = dates;
    }

    public String function(long num) {
      return String.valueOf(num);
    }

    public String aMethod(long num) {
      return String.valueOf(num);
    }

    public Map<String, Object> getMap() {
      return map;
    }

    public void setMap(Map<String, Object> map) {
      this.map = map;
    }

    public String getKey() {
      return "1";
    }
  }

  public void testNestedMethod1() {
    Vector vectorA = new Vector();
    Vector vectorB = new Vector();

    vectorA.add("Foo244");

    Map map = new HashMap();
    map.put("vecA",
        vectorA);
    map.put("vecB",
        vectorB);

    testCompiledSimple("vecB.add(vecA.remove(0)); vecA.add('Foo244');",
        map);

    assertEquals("Foo244",
        vectorB.get(0));
  }

  public void testDynamicImports2() {
    assertEquals(BufferedReader.class,
        test("import java.io.*; BufferedReader"));
  }

  public void testUseOfVarKeyword() {
    assertEquals("FOO_BAR",
        test("var barfoo = \"FOO_BAR\"; return barfoo;"));
  }

  public void testAssignment5() {
    assertEquals(15,
        test("var x = (10) + (5); x;"));
  }

  public void testSetExpressions1() {
    Map<String, Object> myMap = new HashMap<String, Object>();

    final Serializable fooExpr = compileSetExpression("foo");
    executeSetExpression(fooExpr,
        myMap,
        "blah");
    assertEquals("blah",
        myMap.get("foo"));

    executeSetExpression(fooExpr,
        myMap,
        "baz");
    assertEquals("baz",
        myMap.get("foo"));

  }

  public void testDuplicateVariableDeclaration() {
    ParserContext context = new ParserContext();
    ExpressionCompiler compiler = new ExpressionCompiler("String x = \"abc\"; Integer x = new Integer( 10 );", context);

    try {
      compiler.compile();
      fail("Compilation must fail with duplicate variable declaration exception.");
    }
    catch (RuntimeException ce) {
      // success
    }
  }

  public void testFullyQualifiedTypeAndCast() {
    assertEquals(1,
        test("java.lang.Integer number = (java.lang.Integer) '1';"));
  }

  public void testThreadSafetyInterpreter1() {
    //First evaluation
    System.out.println("First evaluation: " + MVEL.eval("true"));

    new Thread(new Runnable() {
      public void run() {
        // Second evaluation - this succeeds only if the first evaluation is not commented out
        System.out.println("Second evaluation: " + MVEL.eval("true"));
      }
    }).start();
  }

  public void testArrayList() throws SecurityException,
      NoSuchMethodException {
    Collection<String> collection = new ArrayList<String>();
    collection.add("I CAN HAS CHEEZBURGER");
    assertEquals(collection.size(),
        MVEL.eval("size()",
            collection));
  }

  public void testUnmodifiableCollection() throws SecurityException,
      NoSuchMethodException {
    Collection<String> collection = new ArrayList<String>();
    collection.add("I CAN HAS CHEEZBURGER");
    collection = unmodifiableCollection(collection);
    assertEquals(collection.size(),
        MVEL.eval("size()",
            collection));
  }

  public void testSingleton() throws SecurityException,
      NoSuchMethodException {
    Collection<String> collection = Collections.singleton("I CAN HAS CHEEZBURGER");
    assertEquals(collection.size(),
        MVEL.eval("size()",
            collection));
  }

  public static class TestClass2 {
    public void addEqualAuthorizationConstraint(Foo leg,
                                                Bar ctrlClass,
                                                Integer authorization) {
    }
  }

  public void testJIRA93() {
    Map testMap = createTestMap();
    testMap.put("testClass2",
        new TestClass2());

    Serializable s = compileExpression("testClass2.addEqualAuthorizationConstraint(foo, foo.bar, 5)");

    for (int i = 0; i < 5; i++) {
      executeExpression(s,
          testMap);
    }
  }

  public void testJIRA96() {
    ParserContext ctx = new ParserContext();
    ctx.setStrictTypeEnforcement(true);

    ctx.addInput("fooString",
        String[].class);

    ExpressionCompiler compiler = new ExpressionCompiler("fooString[0].toUpperCase()", ctx);
    compiler.compile();
  }

  public void testStringToArrayCast() {
    Object o = test("(char[]) 'abcd'");

    assertTrue(o instanceof char[]);
  }

  public void testStringToArrayCast2() {
    assertTrue((Boolean) test("_xyxy = (char[]) 'abcd'; _xyxy[0] == 'a'"));
  }

  public void testStaticallyTypedArrayVar() {
    String ex = "char[] _c___ = new char[10]; _c___ instanceof char[]";
    assertTrue((Boolean) test(ex));
  }

  public void testParserErrorHandling() {
    final ParserContext ctx = new ParserContext();
    ExpressionCompiler compiler = new ExpressionCompiler("a[", ctx);
    try {
      compiler.compile();
    }
    catch (Exception e) {
      return;
    }
    assertTrue(false);
  }

  public void testJIRA100() {
    assertEquals(new BigDecimal(20),
        test("java.math.BigDecimal axx = new java.math.BigDecimal( 10.0 ); java.math.BigDecimal bxx = " +
            "new java.math.BigDecimal( 10.0 ); java.math.BigDecimal cxx = axx + bxx; return cxx; "));
  }

  public void testJIRA100b() {
    Serializable s = MVEL.compileExpression("java.math.BigDecimal axx = new java.math.BigDecimal( 10.0 ); java.math.BigDecimal bxx = " +
        "new java.math.BigDecimal( 10.0 ); java.math.BigDecimal cxx = axx + bxx; return cxx; ");

    assertEquals(new BigDecimal(20), executeExpression(s, new HashMap()));

  }

  public void testAssignToBean() {
    Person person = new Person();
    MVEL.eval("this.name = 'foo'",
        person);

    assertEquals("foo",
        person.getName());

    executeExpression(compileExpression("this.name = 'bar'"),
        person);

    assertEquals("bar",
        person.getName());
  }


  public void testMapAssignmentNestedExpression() {
    Map map = new HashMap();
    map.put("map",
        new HashMap());

    String ex = "map[java.lang.Integer.MAX_VALUE] = 'bar'; map[java.lang.Integer.MAX_VALUE];";

    assertEquals("bar",
        executeExpression(compileExpression(ex),
            map));
    assertEquals("bar",
        MVEL.eval(ex,
            map));
  }

  public void testMapAssignmentNestedExpression2() {
    Map map = new HashMap();
    map.put("x",
        "bar");
    map.put("map",
        new HashMap());

    String ex = "map[x] = 'foo'; map['bar'];";
    assertEquals("foo",
        executeExpression(compileExpression(ex),
            map));
    assertEquals("foo",
        MVEL.eval(ex,
            map));
  }

  /**
   * MVEL-103
   */
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

  public void testMethodResolutionOrder() {
    MvelContext mvelContext = new MvelContext();
    MVEL.eval("methodForTest({'1','2'})",
        mvelContext);
    MVEL.eval("methodForTest('1')",
        mvelContext);

    assertTrue(mvelContext.arrayCalled && mvelContext.singleCalled);
  }


  public void testCustomPropertyHandler() {
    MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = true;
    PropertyHandlerFactory.registerPropertyHandler(SampleBean.class,
        new SampleBeanAccessor());
    assertEquals("dog",
        test("foo.sampleBean.bar.name"));
    PropertyHandlerFactory.unregisterPropertyHandler(SampleBean.class);
    MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = false;

  }

  public void testSetAccessorOverloadedEqualsStrictMode() {
    ParserContext ctx = new ParserContext();
    ctx.setStrongTyping(true);
    ctx.addInput("foo",
        Foo.class);

    try {
      CompiledExpression expr = new ExpressionCompiler("foo.bar = 0", ctx).compile();
    }
    catch (CompileException e) {
      // should fail.

      e.printStackTrace();
      return;
    }

    assertTrue(false);
  }


  private static final KnowledgeHelperFixer fixer = new KnowledgeHelperFixer();

  public void testAdd__Handle__Simple() {
    String result = fixer.fix("update(myObject );");
    assertEqualsIgnoreWhitespace("drools.update(myObject );",
        result);

    result = fixer.fix("update ( myObject );");
    assertEqualsIgnoreWhitespace("drools.update( myObject );",
        result);
  }

  public void testAdd__Handle__withNewLines() {
    final String result = fixer.fix("\n\t\n\tupdate( myObject );");
    assertEqualsIgnoreWhitespace("\n\t\n\tdrools.update( myObject );",
        result);
  }

  public void testAdd__Handle__rComplex() {
    String result = fixer.fix("something update( myObject); other");
    assertEqualsIgnoreWhitespace("something drools.update( myObject); other",
        result);

    result = fixer.fix("something update ( myObject );");
    assertEqualsIgnoreWhitespace("something drools.update( myObject );",
        result);

    result = fixer.fix(" update( myObject ); x");
    assertEqualsIgnoreWhitespace(" drools.update( myObject ); x",
        result);

    //should not touch, as it is not a stand alone word
    result = fixer.fix("xxupdate(myObject ) x");
    assertEqualsIgnoreWhitespace("xxupdate(myObject ) x",
        result);
  }

  public void testMultipleMatches() {
    String result = fixer.fix("update(myObject); update(myObject );");
    assertEqualsIgnoreWhitespace("drools.update(myObject); drools.update(myObject );",
        result);

    result = fixer.fix("xxx update(myObject ); update( myObject ); update( yourObject ); yyy");
    assertEqualsIgnoreWhitespace("xxx drools.update(myObject ); " +
        "drools.update( myObject ); drools.update( yourObject ); yyy",
        result);
  }

  public void testAssert1() {
    final String raw = "insert( foo );";
    final String result = "drools.insert( foo );";
    assertEqualsIgnoreWhitespace(result,
        fixer.fix(raw));
  }

  public void testAssert2() {
    final String raw = "some code; insert( new String(\"foo\") );\n More();";
    final String result = "some code; drools.insert( new String(\"foo\") );\n More();";
    assertEqualsIgnoreWhitespace(result,
        fixer.fix(raw));
  }

  public void testAssertLogical() {
    final String raw = "some code; insertLogical(new String(\"foo\"));\n More();";
    final String result = "some code; drools.insertLogical(new String(\"foo\"));\n More();";
    assertEqualsIgnoreWhitespace(result,
        fixer.fix(raw));
  }

  public void testModifyRetractModifyInsert() {
    final String raw = "some code; insert( bar ); modifyRetract( foo );\n More();" +
        " retract( bar ); modifyInsert( foo );";
    final String result = "some code; drools.insert( bar ); drools.modifyRetract( foo );\n More();" +
        " drools.retract( bar ); drools.modifyInsert( foo );";
    assertEqualsIgnoreWhitespace(result,
        fixer.fix(raw));
  }

  public void testAllActionsMushedTogether() {
    String result = fixer.fix("insert(myObject ); update(ourObject);\t retract(herObject);");
    assertEqualsIgnoreWhitespace("drools.insert(myObject ); drools.update(ourObject);\t drools.retract(herObject);",
        result);

    result = fixer.fix("insert( myObject ); update(ourObject);\t retract(herObject  );\n" +
        "insert(  myObject ); update(ourObject);\t retract(  herObject  );");
    assertEqualsIgnoreWhitespace("drools.insert( myObject ); drools.update(ourObject);\t " +
        "drools.retract(herObject  );\ndrools.insert(  myObject ); drools.update(ourObject);\t" +
        " drools.retract(  herObject  );",
        result);
  }

  public void testLeaveLargeAlone() {
    final String original = "yeah yeah yeah minsert( xxx ) this is a long() thing Person" +
        " (name=='drools') modify a thing";
    final String result = fixer.fix(original);
    assertEqualsIgnoreWhitespace(original,
        result);
  }

  public void testWithNull() {
    final String original = null;
    final String result = fixer.fix(original);
    assertEqualsIgnoreWhitespace(original,
        result);
  }

  public void testLeaveAssertAlone() {
    final String original = "drools.insert(foo)";
    assertEqualsIgnoreWhitespace(original,
        fixer.fix(original));
  }

  public void testLeaveAssertLogicalAlone() {
    final String original = "drools.insertLogical(foo)";
    assertEqualsIgnoreWhitespace(original,
        fixer.fix(original));
  }

  public void testWackyAssert() {
    final String raw = "System.out.println($person1.getName() + \" and \" + $person2.getName() " +
        "+\" are sisters\");\n" + "insert($person1.getName(\"foo\") + \" and \" + $person2.getName() " +
        "+\" are sisters\"); yeah();";
    final String expected = "System.out.println($person1.getName() + \" and \" + $person2.getName()" +
        " +\" are sisters\");\n" + "drools.insert($person1.getName(\"foo\") + \" and \" + $person2.getName() " +
        "+\" are sisters\"); yeah();";

    assertEqualsIgnoreWhitespace(expected,
        fixer.fix(raw));
  }

  public void testMoreAssertCraziness() {
    final String raw = "foobar(); (insert(new String(\"blah\").get()); bangBangYudoHono();)";
    assertEqualsIgnoreWhitespace("foobar(); (drools.insert(new String(\"blah\").get()); bangBangYudoHono();)",
        fixer.fix(raw));
  }

  public void testRetract() {
    final String raw = "System.out.println(\"some text\");retract(object);";
    assertEqualsIgnoreWhitespace("System.out.println(\"some text\");drools.retract(object);",
        fixer.fix(raw));
  }

  private void assertEqualsIgnoreWhitespace(final String expected,
                                            final String actual) {
    if (expected == null || actual == null) {
      assertEquals(expected,
          actual);
      return;
    }
    final String cleanExpected = expected.replaceAll("\\s+",
        "");
    final String cleanActual = actual.replaceAll("\\s+",
        "");

    assertEquals(cleanExpected,
        cleanActual);
  }

  public void testIncrementInBooleanStatement() {
    assertEquals(true,
        test("hour++ < 61 && hour == 61"));
  }

  public void testIncrementInBooleanStatement2() {
    assertEquals(true,
        test("++hour == 61"));
  }


  public void testStaticallyTypedLong() {
    assertEquals(10l,
        test("10l"));
  }


  public void testNakedMethodCall() {
    MVEL.COMPILER_OPT_ALLOW_NAKED_METH_CALL = true;

    OptimizerFactory.setDefaultOptimizer("ASM");

    Serializable c = compileExpression("tm = System.currentTimeMillis");
    assertTrue(((Long) executeExpression(c,
        new HashMap())) > 0);

    OptimizerFactory.setDefaultOptimizer("reflective");

    assertTrue(((Long) executeExpression(c,
        new HashMap())) > 0);

    Map map = new HashMap();
    map.put("foo",
        new Foo());
    c = compileExpression("foo.happy");
    assertEquals("happyBar",
        executeExpression(c,
            map));

    OptimizerFactory.setDefaultOptimizer("ASM");
    c = compileExpression("foo.happy");

    assertEquals("happyBar",
        executeExpression(c,
            map));

    MVEL.COMPILER_OPT_ALLOW_NAKED_METH_CALL = false;
  }

  public void testDecl() {
    assertEquals((char) 100,
        test("char chr; chr = 100; chr"));
  }

  public void testInlineUnion() {
    assertEquals("test",
        test("{'foo', 'test'}[1]"));
  }

  public static double minim(double[] tab) {
    double min = Float.MAX_VALUE;
    for (int i = 0; i < tab.length; i++) {
      if (min > tab[i]) {
        min = tab[i];
      }
    }
    return min;
  }

  public void testJIRA113() {
    assertEquals(true,
        test("org.mvel2.tests.core.CoreConfidenceTests.minim( new double[] {456.2, 2.3} ) == 2.3"));
  }


  public void testChainedMethodCallsWithParams() {
    assertEquals(true,
        test("foo.toUC(\"abcd\").equals(\"ABCD\")"));
  }

  public void testIsUsedInIf() {
    assertEquals(true,
        test("c = 'str'; if (c is String) { true; } else { false; } "));
  }

  public void testJIRA122() {
    Serializable s = compileExpression("System.out.println('>'+java.lang.Character.toLowerCase(name.charAt(0))); java.lang.Character.toLowerCase(name.charAt(0)) == 'a'");

    OptimizerFactory.setDefaultOptimizer("ASM");

    Map map = new HashMap();
    map.put("name",
        "Adam");

    assertEquals(true,
        executeExpression(s,
            map));
    assertEquals(true,
        executeExpression(s,
            map));
  }

  public void testJIRA122b() {
    Serializable s = compileExpression("System.out.println('>'+java.lang.Character.toLowerCase(name.charAt(0))); java.lang.Character.toLowerCase(name.charAt(0)) == 'a'");

    OptimizerFactory.setDefaultOptimizer("reflective");

    Map map = new HashMap();
    map.put("name",
        "Adam");

    assertEquals(true,
        executeExpression(s,
            map));
    assertEquals(true,
        executeExpression(s,
            map));
  }

  public void testIssue286() {
	    Serializable s = compileExpression("java.lang.Character.toLowerCase(name.charAt(0)) == 'a'");
	    Map<String, Object> map = new HashMap<>();
	    map.put("name", "Adam");
	    assertEquals(true, executeExpression(s, map));
	  }

  public void testJIRA103() {
    MvelContext mvelContext = new MvelContext();
    MVEL.setProperty(mvelContext,
        "regkeys",
        "s");
  }

  public void testJIRA103b() {
    MvelContext mvelContext = new MvelContext();
    Map map = new HashMap();
    map.put("ctx",
        mvelContext);
    Serializable c = compileExpression("ctx.regkeys = 'foo'");
    executeExpression(c,
        map);
    executeExpression(c,
        map);
  }

  public void testMethodCaching() {
    MVEL.eval("for (pet: getPets()) pet.run();",
        new PetStore());
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

  public void testSetExpressions2() {
    Foo foo = new Foo();
    Collection col = new ArrayList();
    //final Serializable fooExpr = compileSetExpression("collectionTest");
    executeSetExpression("collectionTest", foo, col);
    assertEquals(col, foo.getCollectionTest());
  }

  public class Fruit {
    public class Apple {

    }
  }

  public void testInnerClassReference() {
    Set<String> imports = new HashSet<>();
    imports.add(CoreConfidenceTests.class.getCanonicalName());

    assertEquals(Fruit.Apple.class,
        eval("CoreConfidenceTests.Fruit.Apple.class;", null, null, imports));
  }

  public void testEdson() {
    assertEquals("foo",
        test("var list = new java.util.ArrayList(); list.add(new String(\"foo\")); list[0];"));
  }

  public void testEnumSupport() {
    MyInterface myInterface = new MyClass();
    myInterface.setType(MyInterface.MY_ENUM.TWO, true);
    assertFalse((boolean) eval("isType(org.mvel2.tests.core.res.MyInterface.MY_ENUM.ONE)", myInterface));
    assertTrue((boolean) eval("isType(org.mvel2.tests.core.res.MyInterface.MY_ENUM.TWO)", myInterface));

    myInterface.setType(MyInterface.MY_ENUM.TWO, false);
    myInterface.setType(MyInterface.MY_ENUM.ONE, true);
    assertTrue((boolean) eval("isType(org.mvel2.tests.core.res.MyInterface.MY_ENUM.ONE)", myInterface));
    assertFalse((boolean) eval("isType(org.mvel2.tests.core.res.MyInterface.MY_ENUM.TWO)", myInterface));
  }

  public void testOperatorPrecedenceOrder() {
    String expression = "bean1.successful && bean2.failed || bean1.failed && bean2.successful";
    Map context = new HashMap();

    BeanB bean1 = new BeanB(true);
    BeanB bean2 = new BeanB(false);

    context.put("bean1",
        bean1);
    context.put("bean2",
        bean2);

    assertEquals(bean1.isSuccessful() && bean2.isFailed() || bean1.isFailed() && bean2.isSuccessful(),
                 (boolean) eval(expression, null, context));
  }

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

  public void testJIRA139() {
    Set<String> imports = new HashSet<>();
    imports.add(ReflectionUtil.class.getCanonicalName());

    assertEquals(ReflectionUtil.getGetter("foo"),
        eval("ReflectionUtil.getGetter(\"foo\")", null, null, imports));
  }

  public void testJIRA140() {
    Set<String> imports = new HashSet<>();
    imports.add("org.mvel2.tests.core.res.*");

    String s = "var cols = new Column[] { new Column(\"name\", 20), new Column(\"age\", 2) };"
               + "var grid = new Grid(new Model(cols));";

    Grid g = (Grid) eval(s, null, new HashMap(), imports);

    assertEquals(g.getModel().getColumns()[0].getName(),
        "name");
    assertEquals(g.getModel().getColumns()[0].getLength(),
        20);
    assertEquals(g.getModel().getColumns()[1].getName(),
        "age");
    assertEquals(g.getModel().getColumns()[1].getLength(),
        2);
  }

  public void testVerifierWithIndexedProperties() {
    Map vars = new HashMap();
    vars.put("base",
        new Base());

    testCompiledSimpleVoid("base.fooMap[\"foo\"].setName(\"coffee\");", vars);

    assertEquals("coffee",
        ((Base) vars.get("base")).fooMap.get("foo").getName());
  }


  public void testEmpty() {
    ParserContext ctx = new ParserContext();
    ctx.setStrongTyping(true);

    Serializable s = compileExpression("list = new java.util.ArrayList(); list == empty",
        ctx);

    Map vars = new HashMap();

    Boolean x = (Boolean) executeExpression(s,
        vars);

    assertNotNull(x);
    assertTrue(x.booleanValue());
  }

  public void testMapsAndLists() {
    Set<String> imports = new HashSet<>();
    imports.add(HashMap.class.getCanonicalName());
    imports.add(ArrayList.class.getCanonicalName());

    String expression = "var m = new HashMap();\n" + "var l = new ArrayList();\n" + "l.add(\"first\");\n" +
        "m.put(\"content\", l);\n" + "list.add(((ArrayList)m[\"content\"])[0]);";

    Map vars = new HashMap();
    List list = new ArrayList();
    vars.put("list", list);

    Boolean result = (Boolean) eval(expression, null, vars, imports);//executeExpression(s,

    assertNotNull(result);
    assertTrue(result);
    assertEquals(1, list.size());
    assertEquals("first", list.get(0));
  }

  public void testReturnBoolean() {
    String ex = "list = new java.util.ArrayList(); return list != null";

    ParserContext ctx = new ParserContext();
    ctx.setStrongTyping(true);
    Serializable s = compileExpression(ex,
        ctx);

    assertEquals(true,
        executeExpression(s,
            new HashMap()));
  }

  public void testComaProblemStrikesBack() {
    String ex = "a.explanation = \"There is a coma, in here\"";

    ParserContext ctx = new ParserContext();
    ExpressionCompiler compiler = new ExpressionCompiler(ex, ctx);
    Serializable s = compiler.compile();

    Base a = new Base();
    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("a",
        a);

    executeExpression(s,
        variables);
    assertEquals("There is a coma, in here",
        a.data);
  }


  public static interface Services {
    public final static String A_CONST = "Hello World";

    public void log(String text);
  }

  public void testStringConcatenation() {
    // debugging MVEL code, it seems that MVEL 'thinks' that the result of the expression:
    // "Drop +5%: "+$sb+" avg: $"+$av+" price: $"+$pr
    // is a double, and as so, he looks for a method:
    // Services.log( double );
    // but finds only:
    // Services.log( String );
    // raising the error.
    String ex = "services.log((String) \"Drop +5%: \"+$sb+\" avg: $\"+$av+\" price: $\"+$pr );";
    ParserContext ctx = new ParserContext();
    ctx.setStrongTyping(true);
    ctx.addInput("$sb",
        String.class);
    ctx.addInput("$av",
        double.class);
    ctx.addInput("$pr",
        double.class);
    ctx.addInput("services",
        Services.class);
    try {
      ExpressionCompiler compiler = new ExpressionCompiler(ex, ctx);
      compiler.compile();
    }
    catch (Throwable e) {
      e.printStackTrace();
      fail("Should not raise exception: " + e.getMessage());
    }
  }

  public void testStringConcatenation2() {
    String ex = "services.log( $cheese + \" some string \" );";
    ParserContext ctx = new ParserContext();
    ctx.setStrongTyping(true);
    ctx.addInput("$cheese",
        Cheese.class);
    ctx.addInput("services",
        Services.class);
    try {
      ExpressionCompiler compiler = new ExpressionCompiler(ex, ctx);
      compiler.compile();
    }
    catch (Throwable e) {
      e.printStackTrace();
      fail("Should not raise exception: " + e.getMessage());
    }
  }

  public void testStringConcatenation3() {
    // BUG: return type of the string concatenation is inferred as double instead of String
    String ex = "services.log($av + \"Drop +5%: \"+$sb+\" avg: $\"+percent($av)+\" price: $\"+$pr );";
    ParserContext ctx = new ParserContext();
    ctx.setStrongTyping(true);
    ctx.setStrictTypeEnforcement(true);
    ctx.addInput("$sb",
        String.class);
    ctx.addInput("$av",
        double.class);
    ctx.addInput("$pr",
        double.class);
    ctx.addInput("services",
        Services.class);
    ctx.addImport("percent", MVEL.getStaticMethod(String.class, "valueOf", new Class[]{double.class}));
    try {
      Serializable compiledExpression = MVEL.compileExpression(ex, ctx);

      Services services = new Services() {
        public void log(String text) {
        }
      };
      Map<String, Object> vars = new HashMap<String, Object>();
      vars.put("services", services);
      vars.put("$sb", "RHT");
      vars.put("$av", 15.0);
      vars.put("$pr", 10.0);

      MVEL.executeExpression(compiledExpression, vars);
    }
    catch (Throwable e) {
      e.printStackTrace();
      fail("Should not raise exception: " + e.getMessage());
    }
  }

  // @TODO maybe delete (mdp)
//  public void testMapsWithVariableAsKey() {
//    String ex = "aMap[aKey] == 'aValue'";
//    ParserContext ctx = new ParserContext();
//    ctx.setStrongTyping(false);
//
//    ExpressionCompiler compiler = new ExpressionCompiler(ex, ctx);
//    compiler.setVerifyOnly(true);
//    compiler.compile();
//
//    Set<String> requiredInputs = compiler.getParserContextState().getInputs().keySet();
//
//    assertTrue(requiredInputs.contains("aMap"));
//    assertTrue(requiredInputs.contains("aKey"));
//  }

  public void testMapsWithVariableAsKey2() {
    String ex = "objectKeyMaptributes[$aPerson] == foo";
    Foo foo = new Foo();
    Person person = new Person();
    person.setObjectKeyMaptributes(new HashMap<Object, Foo>());
    person.getObjectKeyMaptributes().put(person, foo);
    Map<String, Class> inputs = new HashMap<String, Class>();
    inputs.put("this", Person.class);
    inputs.put("foo", Foo.class);
    inputs.put("$aPerson", Person.class);

    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("foo", foo);
    variables.put("$aPerson", person);

    Boolean result = (Boolean) eval(ex, person, variables);
    assertTrue(result);
  }

  public static void testProjectionUsingThis() {
    Set records = new HashSet();
    for (int i = 0; i < 53; i++) {
      Bean2 record = new Bean2(i);
      records.add(record);
    }

    Object result = MVEL.eval("(_prop in this)",
        records);
    System.out.println("result: " + result);
  }

  public static final class Bean2 {
    public final int _prop;

    public Bean2(int prop_) {
      _prop = prop_;
    }

    public int getProp() {
      return _prop;
    }

    public String toString() {
      return Integer.toString(_prop);
    }
  }

  public void testUnaryOpNegation1() {
    assertEquals(false,
        test("!new Boolean(true)"));
  }

  public void testUnaryOpNegation2() {
    assertEquals(true,
        test("!isdef _foozy_"));
  }

  public class Az {
    public String foo(String s) {
      return s;
    }
  }

  public class Bz extends Az {
  }


  public void testJIRA151() {
    String expression = "a.foo(value)";
    Map<String, Object> variables = new HashMap<String, Object>();
    Bz b = new Bz();
    variables.put("a", b);
    variables.put("value", 123);
    for (int i = 0; i < 100; i++) {
      assertEquals("123", eval(expression,null, variables));
    }
  }


  public void testJIRA153() {
    assertEquals(false,
        test("!(true)"));
  }

  public void testMultipleNegations() {
    assertEquals(true,
        test("!false"));
    assertEquals(false,
        test("!!false"));
    assertEquals(true,
        test("!!!false"));
    assertEquals(false,
        test("!!!!false"));
  }

  public void testJIRA154() {
    Map m = createTestMap();
    m.put("returnTrue",
        MVEL.getStaticMethod(CoreConfidenceTests.class,
            "returnTrue",
            new Class[0]));

    assertEquals(false,
        MVEL.eval("!returnTrue()",
            m));
  }

  public void testJIRA154b() {
    ParserContext pctx = new ParserContext();
    pctx.addImport("returnTrue",
        MVEL.getStaticMethod(CoreConfidenceTests.class,
            "returnTrue",
            new Class[0]));

    assertEquals(false,
        executeExpression(MVEL.compileExpression("!(returnTrue())",
            pctx)));
  }

  public void testJIRA155() {
    ParserContext pctx = new ParserContext();
    pctx.addImport("returnTrue",
        MVEL.getStaticMethod(CoreConfidenceTests.class,
            "returnTrue",
            new Class[0]));

    assertEquals(true,
        executeExpression(MVEL.compileExpression("!true || returnTrue()",
            pctx)));
  }

  public void testJIRA155b() {
    ParserContext pctx = new ParserContext();
    pctx.addImport("returnTrue",
        MVEL.getStaticMethod(CoreConfidenceTests.class,
            "returnTrue",
            new Class[0]));

    assertEquals(true,
        executeExpression(MVEL.compileExpression("!(!true || !returnTrue())",
            pctx)));
  }

  public void testJIRA156() throws Throwable {
    ClassProvider provider = new ClassProvider();

    String script = "provider.getPrivate().foo()";
    HashMap<String, Object> vars = new HashMap<String, Object>();
    vars.put("provider", provider);

    assertEquals("private!", testCompiledSimple(script, vars));
  }

  public void testJIRA156b() throws Throwable {
    ClassProvider provider = new ClassProvider();
    String script = "provider.getPrivate().foo()";

    HashMap<String, Object> vars = new HashMap<String, Object>();
    vars.put("provider", provider);

    assertEquals("private!", testCompiledSimple(script, vars));
  }

  public void testJIRA156c() throws Throwable {
    ClassProvider provider = new ClassProvider();
    String script = "provider.getPublic().foo()";

    HashMap<String, Object> vars = new HashMap<String, Object>();
    vars.put("provider", provider);

    assertEquals("public!", testCompiledSimple(script, vars));
  }

  public static boolean returnTrue() {
    return true;
  }

  public static class TestHelper {
    public static String method(int id,
                              Object[] arr) {
      System.out.println(id + " -> " + arr.length);
      return id + " -> " + arr.length;
    }

    public static void method(Object obj1, Object obj2) {
      System.out.println(obj1 + "-> " + obj2);
    }

    public static Calendar minDate() {
      return Calendar.getInstance();
    }

    public static Calendar maxDate() {
      return Calendar.getInstance();
    }
  }

  public static class Fooz {
    public Fooz(String id) {
    }
  }

  public void testArray() {
    String ex = " TestHelper.method(1, new String[]{\"a\", \"b\"});\n"
        + " TestHelper.method(2, new String[]{new String(\"a\"), new String(\"b\")});\n"
        + " TestHelper.method(3, new Fooz[]{new Fooz(\"a\"), new Fooz(\"b\")});";
    Set<String> imports = new HashSet<>();
    imports.add(TestHelper.class.getCanonicalName());
    imports.add(Fooz.class.getCanonicalName());
    assertEquals("3 -> 2",
                 _test(ex, imports));
  }

  public void testArray2() {
    String ex = " TestHelper.method(1, {\"a\", \"b\"});\n"
        + " TestHelper.method(2, {new String(\"a\"), new String(\"b\")});\n"
        + " TestHelper.method(3, {new Fooz(\"a\"), new Fooz(\"b\")});";
    ParserContext ctx = new ParserContext();
    ctx.setStrongTyping(true);
    ctx.addImport(TestHelper.class);
    ctx.addImport(Fooz.class);
    ExpressionCompiler compiler = new ExpressionCompiler(ex, ctx);

    OptimizerFactory.setDefaultOptimizer("ASM");
    CompiledExpression expr = compiler.compile();
    executeExpression(expr);

    OptimizerFactory.setDefaultOptimizer("reflective");
    expr = compiler.compile();
    executeExpression(expr);
  }


  public void testJIRA166() {
    Object v = MVEL.eval("import java.util.regex.Matcher; import java.util.regex.Pattern;"
        + " if (Pattern.compile(\"hoge\").matcher(\"hogehogehoge\").find()) { 'foo' } else { 'bar' }",
        new HashMap());
    assertEquals("foo",
        v);
  }

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

  public void testJIRA167() {
    Map context = new HashMap();
    context.put("bean",
        new Beano());
    MVEL.eval("bean.property1==null?bean.isProperty2():bean.isProperty3()",
        context);
  }

  public void testJIRA168() {
    boolean before = MVEL.COMPILER_OPT_ALLOW_NAKED_METH_CALL;
    try {
      Map<String, Object> st = new HashMap<String, Object>();
      st.put("__fact__", new ArrayList());
      st.put("__expected__", 0);

      String expressionNaked = "__fact__.size == __expected__";
      String expressionNonNaked = "__fact__.size() == __expected__";
      MVEL.COMPILER_OPT_ALLOW_NAKED_METH_CALL = true;

      // the following works fine
      ParserContext ctx = new ParserContext();
      for (Map.Entry<String, Object> entry : st.entrySet()) {
        ctx.addInput(entry.getKey(),
            entry.getValue().getClass());
      }
      CompiledExpression expr = new ExpressionCompiler(expressionNaked, ctx).compile();

      Boolean result = (Boolean) executeExpression(expr,
          st);
      assertTrue(result);

      // the following works fine
      result = (Boolean) MVEL.eval(expressionNonNaked, st);
      assertTrue(result);

      // the following fails
      result = (Boolean) MVEL.eval(expressionNaked, st);
      assertTrue(result);
    }
    finally {
      MVEL.COMPILER_OPT_ALLOW_NAKED_METH_CALL = before;
    }
  }

  public void testJIRA170() {
    OptimizerFactory.setDefaultOptimizer("reflective");

    List<Integer> staticDispatch = Arrays.asList(2, 1, 0);
    List<Integer> multimethodDispatch = Arrays.asList(3, 2, 1);

    //      invokeJIRA170("Dynamic", ctxJIRA170(false, false), varsJIRA170(), multimethodDispatch);
    //      invokeJIRA170("Strict", ctxJIRA170(true, false), varsJIRA170(), multimethodDispatch);
    invokeJIRA170("Strong", ctxJIRA170(false, true), varsJIRA170(), staticDispatch);
  }

  public void testJIRA170b() {
    OptimizerFactory.setDefaultOptimizer("ASM");

    List<Integer> staticDispatch = Arrays.asList(2, 1, 0);
    List<Integer> multimethodDispatch = Arrays.asList(3, 2, 1);

    //       invokeJIRA170("Dynamic", ctxJIRA170(false, false), varsJIRA170(), multimethodDispatch);
    //       invokeJIRA170("Strict", ctxJIRA170(true, false), varsJIRA170(), multimethodDispatch);
    invokeJIRA170("Strong", ctxJIRA170(false, true), varsJIRA170(), staticDispatch);
  }

  public void invokeJIRA170(String name, ParserContext pctx, Map<String, ?> vars, Collection<Integer> expected) {
    Serializable expression = MVEL.compileExpression("x.remove((Object) y); x ", pctx);
    Object result = executeExpression(expression, vars);

    assertTrue(String.format("%s Expected %s, Got %s", name, expected, result), expected.equals(result));
    result = executeExpression(expression, vars);

    assertTrue(String.format("%s Expected %s, Got %s", name, expected, result), expected.equals(result));
  }


  private Map<String, ?> varsJIRA170() {
    Map<String, Object> vars = new HashMap<String, Object>();
    vars.put("x", new ArrayList<Integer>(Arrays.asList(3, 2, 1, 0)));
    vars.put("y", 3);
    return vars;
  }

  private ParserContext ctxJIRA170(boolean strictTypeEnforcement, boolean strongTyping) {
    ParserContext ctx = new ParserContext();
    //    ctx.setStrictTypeEnforcement(strictTypeEnforcement);
    ctx.setStrongTyping(strongTyping);
    ctx.addInput("x", Collection.class, new Class[]{Integer.class});
    ctx.addInput("y", Integer.class);
    return ctx;
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

  public void testJIRA167b() {
    Map context = new HashMap();
    context.put("current", new JIRA167Step());
    context.put("node", new JIRA167Node());
    MVEL.eval("current.parent==null?node.isServer():(node==current.parent.node)", context);
  }

  public void testJIRA167c() {
    MVEL.eval("true?true:(false)");
  }


  public void testJIRA176() {
    Map innerMap = new HashMap();
    innerMap.put("testKey[MyValue=newValue]", "test");

    Map vars = new HashMap();
    vars.put("mappo", innerMap);

    assertEquals("test", MVEL.eval("mappo['testKey[MyValue=newValue]']", vars));
  }

  public void testJIRA176b() {
    Map innerMap = new HashMap();
    innerMap.put("testKey[MyValue=newValue]", "test");

    Map vars = new HashMap();
    vars.put("mappo", innerMap);

    Serializable s = MVEL.compileExpression("mappo['testKey[MyValue=newValue]']");
    OptimizerFactory.setDefaultOptimizer("reflective");

    assertEquals("test", executeExpression(s, vars));

    s = MVEL.compileExpression("mappo['testKey[MyValue=newValue]']");
    OptimizerFactory.setDefaultOptimizer("ASM");

    assertEquals("test", executeExpression(s, vars));
  }

  public void testRandomSomething() {

    Foo foo = new Foo();
    foo.setName("foo1");

    Foo foo2 = new Foo();
    foo2.setName("foo2");

    MVEL.setProperty(foo, "name", 5);

    Serializable s = MVEL.compileExpression("name.toUpperCase()", ParserContext.create().stronglyTyped().withInput("name", String.class));

    Object _return = executeExpression(s, foo);

    System.out.println("returned value: " + String.valueOf(_return));

    _return = executeExpression(s, foo2);

    System.out.println("returned value: " + String.valueOf(_return));

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

  public void testDeepMethodNameResolution() {
    String expression = "variables = [ \"symbol\" : \"RHT\" ]; \n" +
        "drools.getProcessManager().startProcess(\"id\", variables );";

    // third pass
    ParserContext ctx = new ParserContext();
    ctx.setStrongTyping(true);
    ctx.addInput("drools", KnowledgeRuntimeHelper.class);
    Map vars = new HashMap();
    vars.put("drools", new KnowledgeRuntimeHelper());
    Serializable expr = MVEL.compileExpression(expression, ctx);
    executeExpression(expr, vars);
  }

  public void testJIRA183() {
    String exp1 = "int end = 'attribute'.indexOf('@');  if(end == -1)" +
        " { end = 'attribute'.length()} 'attribute'.substring(0, end);";
    Object val1 = MVEL.eval(exp1, new HashMap<String, Object>());

    String exp2 = "int end = 'attribute'.indexOf('@');  if(end == -1)" +
        " { end = 'attribute'.length() } 'attribute'.substring(0, end);";
    Object val2 = MVEL.eval(exp2, new HashMap<String, Object>());
  }


  public void testContextAssignments() {
    Foo foo = new Foo();
    MVEL.eval("this.name = 'bar'", foo);

    assertEquals("bar", foo.getName());
  }

  public void testMVEL187() {
    ParserContext context = new ParserContext();
    context.addPackageImport("test");
    context.addInput("outer", Outer.class);

    Object compiled = MVEL.compileExpression(
        "outer.getInner().getValue()", context);

    Map<String, Object> vars = new HashMap<String, Object>();
    vars.put("outer", new Outer());
    VariableResolverFactory varsResolver = new MapVariableResolverFactory(vars);

    assertEquals(2, executeExpression(compiled, varsResolver));
  }

  public void testMVEL190() {
    ParserContext context = new ParserContext();
    context.addImport(Ship.class);
    context.addImport(MapObject.class);

    context.addInput("obj", MapObject.class);

    Object compiled = MVEL.compileExpression(
        "((Ship) obj).getName()", context);

    Map<String, Object> vars = new HashMap<String, Object>();
    vars.put("obj", new Ship());

    VariableResolverFactory varsResolver
        = new MapVariableResolverFactory(vars);

    System.out.println(
        executeExpression(compiled, varsResolver));
  }

  public void testMethodScoring() {
    OptimizerFactory.setDefaultOptimizer("ASM");
    ParserConfiguration pconf = new ParserConfiguration();
    for (Method m : StaticMethods.class.getMethods()) {
      if (Modifier.isStatic(m.getModifiers())) {
        pconf.addImport(m.getName(), m);

      }
    }
    pconf.addImport("TestCase", TestCase.class);
    ParserContext pctx = new ParserContext(pconf);

    Map<String, Object> vars = new HashMap<String, Object>();

    // this is successful
    TestCase.assertTrue(StaticMethods.is(StaticMethods.getList(java.util.Formatter.class)));

    // this also should be fine
    Serializable expr = MVEL.compileExpression("TestCase.assertTrue( is( getList( java.util.Formatter ) ) )", pctx);
    executeExpression(expr, vars);
  }

  public void testMethodWithNegativeIntParamMVEL313() {
    assertTrue((Boolean) runSingleTest("ord(true,-1)"));
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

  public void testSetterViaDotNotation() {

    TestClass tc = new TestClass();
    tc.getExtra().put("test", "value");

    ParserContext ctx = new ParserContext();
    ctx.setStrongTyping(true);
    String expression = "extra.test";
    Serializable compiled = MVEL.compileSetExpression(expression, ctx);
    MVEL.executeSetExpression(compiled, tc, "value2");
    assertEquals("value2", tc.getExtra().get("test"));
  }

  public void testSetterViaMapNotation() {

    TestClass tc = new TestClass();
    tc.getExtra().put("test", "value");

    ParserContext ctx = new ParserContext();
    ctx.withInput("this", TestClass.class);
    ctx.setStrongTyping(true);
    String expression = "extra[\"test\"]";
    Serializable compiled = MVEL.compileSetExpression(expression, tc.getClass(), ctx);
    MVEL.executeSetExpression(compiled, tc, "value3");
    assertEquals("value3", tc.getExtra().get("test"));
  }


  public void testGetterViaDotNotation() {
    TestClass tc = new TestClass();
    tc.getExtra().put("test", "value");

    Map vars = new HashMap();
    vars.put("tc", tc);

    ParserContext ctx = new ParserContext();
    ctx.setStrongTyping(true);
    ctx.addInput("tc", tc.getClass());
    String expression = "tc.extra.test";
    Serializable compiled = MVEL.compileExpression(expression, ctx);
    String val = (String) executeExpression(compiled, vars);
    assertEquals("value", val);
  }

  public void testGetterViaMapNotation() {
    TestClass tc = new TestClass();
    tc.getExtra().put("test", "value");

    Map vars = new HashMap();
    vars.put("tc", tc);

    ParserContext ctx = new ParserContext();
    ctx.setStrongTyping(true);
    ctx.addInput("tc", tc.getClass());
    String expression = "tc.extra[\"test\"]";
    Serializable compiled = MVEL.compileExpression(expression, ctx);
    String val = (String) executeExpression(compiled, vars);
    assertEquals("value", val);
  }

  public void testGetterViaMapGetter() {
    TestClass tc = new TestClass();
    tc.getExtra().put("test", "value");

    Map vars = new HashMap();
    vars.put("tc", tc);

    ParserContext ctx = new ParserContext();
    ctx.setStrongTyping(true);
    ctx.addInput("tc", tc.getClass());
    String expression = "tc.extra.get(\"test\")";
    Serializable compiled = MVEL.compileExpression(expression, ctx);
    String val = (String) executeExpression(compiled, vars);
    assertEquals("value", val);
  }


  public void testJIRA209() {
    Map vars = new LinkedHashMap();
    vars.put("bal", new BigDecimal("999.99"));

    String[] testCases = {
        //        "bal < 100 or bal > 200",
        //        "bal < 100 || bal > 200",
        "bal > 200 or bal < 100",
        "bal > 200 || bal < 100",
        "bal < 100 and bal > 200",
        "bal < 100 && bal > 200",
        "bal > 200 and bal < 100",
        "bal > 200 && bal < 100"
    };

    Object val1, val2;
    for (String expr : testCases) {
      System.out.println("Evaluating '" + expr + "': ......");
      val1 = MVEL.eval(expr, vars);
      assertNotNull(val1);
      Serializable compiled = MVEL.compileExpression(expr);
      val2 = executeExpression(compiled, vars);
      assertNotNull(val2);
      assertEquals("expression did not evaluate correctly: " + expr, val1, val2);
    }
  }

  public void testBigDecimalOutput() {
    String str = "import java.math.BigDecimal; BigDecimal test = new BigDecimal(\"50000\"); System.out.println(test / new BigDecimal(\"1.13\"));";
    ParserConfiguration pconf = new ParserConfiguration();
    ParserContext pctx = new ParserContext(pconf);
    pctx.setStrongTyping(true);
    pctx.setStrictTypeEnforcement(true);
    ExecutableStatement stmt = (ExecutableStatement) MVEL.compileExpression(str, pctx);
    MVEL.executeExpression(stmt, new HashMap());
  }

  public void testConstructor() {
    String ex = " TestHelper.method(new Person('bob', 30), new Person('mark', 40, 999, 55, 10));\n";
    ParserContext ctx = new ParserContext();
    ctx.setStrongTyping(true);
    ctx.addImport(TestHelper.class);
    ctx.addImport(Person.class);

    // un-comment the following line to see how MVEL is converting the int argument 40 into a
    // string and then executing the wrong constructor on the Person class
    try {
      MVEL.compileExpression(ex, ctx);
      fail("Constructor should not have been found.");
    }
    catch (CompileException e) {
      // yay.
    }
    // fail( "The Person constructor used in the expression does not exist, so an error should have been raised during compilation." );
  }

  public void testAmbiguousGetName() {
    Map<String, Object> vars = createTestMap();
    vars.put("Foo244", Foo.class);

    OptimizerFactory.setDefaultOptimizer("ASM");
    Serializable s = MVEL.compileExpression("foo.getClass().getName()");

    System.out.println(MVEL.executeExpression(s, vars));

    s = MVEL.compileExpression("Foo244.getName()");

    System.out.println(MVEL.executeExpression(s, vars));
  }

  public void testBindingNullToPrimitiveTypes() {
    Map<String, Object> vars = createTestMap();
    ((Foo) vars.get("foo")).setCountTest(10);

    OptimizerFactory.setDefaultOptimizer("reflective");
    Serializable s = MVEL.compileSetExpression("foo.countTest");
    MVEL.executeSetExpression(s, vars, null);

    assertEquals(((Foo) vars.get("foo")).getCountTest(), 0);

    OptimizerFactory.setDefaultOptimizer("ASM");
    s = MVEL.compileSetExpression("foo.countTest");
    MVEL.executeSetExpression(s, vars, null);

    assertEquals(((Foo) vars.get("foo")).getCountTest(), 0);

    MVEL.executeSetExpression(s, vars, null);

    assertEquals(((Foo) vars.get("foo")).getCountTest(), 0);
  }

  public void testBindingNullToPrimitiveTypes2() {
    Map<String, Object> vars = createTestMap();
    ((Foo) vars.get("foo")).setCountTest(10);

    OptimizerFactory.setDefaultOptimizer("reflective");
    Serializable s = MVEL.compileSetExpression("foo.boolTest");
    MVEL.executeSetExpression(s, vars, null);

    assertFalse(((Foo) vars.get("foo")).isBoolTest());

    OptimizerFactory.setDefaultOptimizer("ASM");
    s = MVEL.compileSetExpression("foo.boolTest");
    MVEL.executeSetExpression(s, vars, null);

    assertFalse(((Foo) vars.get("foo")).isBoolTest());

    MVEL.executeSetExpression(s, vars, null);

    assertFalse(((Foo) vars.get("foo")).isBoolTest());
  }

  public void testBindingNullToPrimitiveTypes3() {
    Map<String, Object> vars = createTestMap();
    ((Foo) vars.get("foo")).setCharTest('a');

    OptimizerFactory.setDefaultOptimizer("reflective");
    Serializable s = MVEL.compileSetExpression("foo.charTest");
    MVEL.executeSetExpression(s, vars, null);

    assertEquals(((Foo) vars.get("foo")).getCharTest(), 0);

    OptimizerFactory.setDefaultOptimizer("ASM");
    s = MVEL.compileSetExpression("foo.charTest");
    MVEL.executeSetExpression(s, vars, null);

    assertEquals(((Foo) vars.get("foo")).getCharTest(), 0);

    MVEL.executeSetExpression(s, vars, null);

    assertEquals(((Foo) vars.get("foo")).getCharTest(), 0);
  }


  public void testBindingNullToPrimitiveTypes4() {
    Map<String, Object> vars = createTestMap();
    ((Foo) vars.get("foo")).charTestFld = 'a';

    OptimizerFactory.setDefaultOptimizer("reflective");
    Serializable s = MVEL.compileSetExpression("foo.charTestFld");
    MVEL.executeSetExpression(s, vars, null);

    assertEquals(((Foo) vars.get("foo")).charTestFld, 0);

    OptimizerFactory.setDefaultOptimizer("ASM");
    s = MVEL.compileSetExpression("foo.charTestFld");
    MVEL.executeSetExpression(s, vars, null);

    assertEquals(((Foo) vars.get("foo")).charTestFld, 0);

    MVEL.executeSetExpression(s, vars, null);

    assertEquals(((Foo) vars.get("foo")).charTestFld, 0);
  }

  public void testBindListToArray() {
    Map<String, Object> vars = createTestMap();

    ArrayList<String> list = new ArrayList<String>();
    list.add("a");
    list.add("b");
    list.add("c");

    OptimizerFactory.setDefaultOptimizer("reflective");
    Serializable s = MVEL.compileSetExpression("foo.charArray");

    MVEL.executeSetExpression(s, vars, list);

    assertEquals(((Foo) vars.get("foo")).getCharArray().length, 3);
  }

  public void testBindListToMultiArray() {
    Map<String, Object> vars = createTestMap();

    ArrayList<List<String>> list = new ArrayList<List<String>>();

    List<String> l1 = new ArrayList<String>();
    l1.add("a");
    l1.add("b");
    l1.add("c");

    List<String> l2 = new ArrayList<String>();
    l2.add("d");
    l2.add("e");
    l2.add("f");

    List<String> l3 = new ArrayList<String>();
    l3.add("g");
    l3.add("h");
    l3.add("i");

    list.add(l1);
    list.add(l2);
    list.add(l3);

    OptimizerFactory.setDefaultOptimizer("reflective");
    Serializable s = MVEL.compileSetExpression("foo.charArrayMulti");

    MVEL.executeSetExpression(s, vars, list);

    Foo foo = (Foo) vars.get("foo");

    assertEquals(foo.getCharArrayMulti().length, 3);
    assertEquals(foo.getCharArrayMulti()[2][2], 'i');
  }

  public void testMVEL224() {
    ParserContext ctx = new ParserContext();
    MVEL.compileExpression("(pin == 1)", ctx);
  }


  public static class A221 {

    public B221 b;
  }

  public static class B221 {

    public String c = "something";
  }

  public void testMVEL221() {
    A221 a1 = new A221();
    a1.b = new B221();

    A221 a2 = new A221();

    OptimizerFactory.setDefaultOptimizer("ASM");

    String expression = "this.?b.c";
    Serializable compiledExpression = MVEL.compileExpression(expression);

    assertEquals(null, MVEL.executeExpression(compiledExpression, a2, String.class));
    assertEquals("something", MVEL.executeExpression(compiledExpression, a1, String.class));

    OptimizerFactory.setDefaultOptimizer("reflective");

    compiledExpression = MVEL.compileExpression(expression);

    assertEquals(null, MVEL.executeExpression(compiledExpression, a2, String.class));
    assertEquals("something", MVEL.executeExpression(compiledExpression, a1, String.class));
  }

  public void testMVEL222() throws IOException {
    String script = "for (int i= 0; i < 10; i++ ){ values[i] = 1.0; }";
    Map<String, Object> scriptVars = new HashMap<String, Object>();
    double[] values = new double[10];
    scriptVars.put("values", values);
    Serializable expression = MVEL.compileExpression(script);
    for (int i = 0; i < 6; i++) {
      scriptVars.put("values", values);
      MVEL.executeExpression(expression, scriptVars);
    }
  }

  public void testMVEL238() throws IOException {
    String expr = new String(loadFromFile(new File("src/test/java/org/mvel2/tests/MVEL238.mvel")));

    Serializable s = MVEL.compileExpression(expr);

    System.out.println(MVEL.executeExpression(s, new HashMap()));
    System.out.println(MVEL.executeExpression(s, new HashMap()));
  }

  public void testParsingRegression() {
    String expr = "if (false) {System.out.println(\" foo\")} else {System.out.println(\" bar\")}";
    MVEL.eval(expr);
  }


  public static class StaticClassWithStaticMethod {
    public static String getString() {
      return "hello";
    }
  }

//    public void testStaticImportWithWildcard() {
//        // this isn't supported yet
//        assertEquals("hello",
//                test("import_static " + getClass().getName() + ".StaticClassWithStaticMethod.*; getString()"));
//    }

  public void testArrayLength() {
    ParserContext context = new ParserContext();
    context.setStrongTyping(true);
    context.addInput("x",
        String[].class);
    ExecutableStatement stmt = (ExecutableStatement) MVEL.compileExpression("x.length", context);
  }

  public void testEmptyConstructorWithSpace() throws Exception {
    ParserConfiguration pconf = new ParserConfiguration();
    pconf.addImport("getString", StaticClassWithStaticMethod.class.getMethod("getString", null));

    String text = "getString( )";

    ParserContext pctx = new ParserContext(pconf);
    pctx.setStrongTyping(true);
    pctx.setStrictTypeEnforcement(true);


    MVEL.compileExpression(text, pctx);
  }

  public void testJavaLangImport() throws Exception {
    String s = "Exception e = null;";
    ParserConfiguration pconf = new ParserConfiguration();
    ParserContext pctx = new ParserContext(pconf);
    MVEL.compileExpression(s, pctx);
  }


  public void testContextFieldNotFound() {
    String str = "'stilton'.equals( type );";

    ParserConfiguration pconf = new ParserConfiguration();

    ParserContext pctx = new ParserContext(pconf);
    pctx.addInput("this", Cheese.class);
    pctx.setStrictTypeEnforcement(true);
    pctx.setStrongTyping(true);

    ExecutableStatement stmt = (ExecutableStatement) MVEL.compileExpression(str, pctx);
    MVEL.executeExpression(stmt, new Cheese(), new HashMap());
  }

  public void testVarArgs() throws Exception {
    ParserContext parserContext = new ParserContext();
    parserContext.setStrictTypeEnforcement(true);
    parserContext.setStrongTyping(true);

    MVEL.analyze("String.format(\"\");", parserContext);
  }


  public void testOperatorPrecedence() throws IOException {
    String script = "list = [1, 2, 3]; x = 10; list contains x || x == 20";
    Serializable expression = MVEL.compileExpression(script);
    Object result = MVEL.executeExpression(expression, new HashMap());

    assertEquals(Boolean.FALSE, result);
  }


  public void testNestedEnum() throws Exception {
//        assertEquals(Triangle.Foo.class, MVEL.analyze("import " + Triangle.class.getCanonicalName() +"; Triangle.Foo.OBTUSE" , ParserContext.create()));

    //       Serializable o = MVEL.compileExpression( "import " + Triangle.class.getCanonicalName() +"; Triangle.Foo.OBTUSE" );

    //     assertEquals( Triangle.Foo.OBTUSE, MVEL.executeExpression(o, new HashMap()) );

    MVEL.eval("import " + Triangle.class.getCanonicalName() + "; Triangle.Foo.OBTUSE", new HashMap());
  }

  public void testNestedNumInMapKey() {
    String str = "objectKeyMaptributes[Triangle.Foo.OBTUSE]";

    ParserConfiguration pconf = new ParserConfiguration();
    pconf.addImport("Triangle", Triangle.class);
    ParserContext pctx = new ParserContext(pconf);
    pctx.addInput("this", Person.class);
    pctx.setStrongTyping(true);

    Foo foo = new Foo();
    Person p = new Person();
    Map<Object, Foo> map = new HashMap<Object, Foo>();
    map.put(Triangle.Foo.OBTUSE, foo);
    p.setObjectKeyMaptributes(map);

    ExecutableStatement stmt = (ExecutableStatement) MVEL.compileExpression(str, pctx);
    Object o = MVEL.executeExpression(stmt, p, new HashMap());
  }

  public void testNestedClassWithNestedGenericsOnNakedMethod() {
    String str = "deliveries.size";

    MVEL.COMPILER_OPT_ALLOW_NAKED_METH_CALL = true;
    ParserConfiguration pconf = new ParserConfiguration();
    ParserContext pctx = new ParserContext(pconf);
    pctx.addInput("this", Triangle.class);
    pctx.setStrongTyping(true);

    ExecutableStatement stmt = (ExecutableStatement) MVEL.compileExpression(str, pctx);
    assertEquals(Integer.valueOf(0), (Integer) MVEL.executeExpression(stmt, new Triangle(), new HashMap()));

    str = "deliveries.size == 0";

    stmt = (ExecutableStatement) MVEL.compileExpression(str, pctx);
    assertTrue((Boolean) MVEL.executeExpression(stmt, new Triangle(), new HashMap()));

    MVEL.COMPILER_OPT_ALLOW_NAKED_METH_CALL = false;
  }

  public static class Triangle {
    public static enum Foo {
      INCOMPLETE, UNCLASSIFIED,
      EQUILATERAL, ISOSCELES, RECTANGLED, ISOSCELES_RECTANGLED, ACUTE, OBTUSE;
    }

    private List<Map<String, Object>> deliveries = new ArrayList<Map<String, Object>>();

    public List<Map<String, Object>> getDeliveries() {
      return deliveries;
    }

    private Object objLabel = "Triangle";
    private String strLabel = "Triangle";
    private Double doubleVal = 29.0;

    public Object getObjLabel() {
      return objLabel;
    }

    public void setObjLabel(Object objLabel) {
      this.objLabel = objLabel;
    }

    public String getStrLabel() {
      return strLabel;
    }

    public void setStrLabel(String strLabel) {
      this.strLabel = strLabel;
    }

    public Double getDoubleVal() {
      return doubleVal;
    }

    public void setDoubleVal(Double doubleVal) {
      this.doubleVal = doubleVal;
    }
  }

  public void testStrictModeAddAll() {
    String str = "list.addAll( o );";

    ParserConfiguration pconf = new ParserConfiguration();
    ParserContext pctx = new ParserContext(pconf);
    pctx.setStrongTyping(true);
    pctx.addInput("o", Object.class);
    pctx.addInput("list", ArrayList.class);
    try {
      ExecutableStatement stmt = (ExecutableStatement) MVEL.compileExpression(str, pctx);
      fail("This should not compileShared, as o is not of a type Collection");
    }
    catch (Exception e) {

    }
  }

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
    Class<?> er = loader.loadClass("org.mvel3.examples.eventing.EventRequest");
    assertNotNull(er);
    assertEquals("org.drools.examples.eventing.EventRequest",
        er.getCanonicalName());

    // getting the value of the enum to prove it works:
    Class<?> st = er.getDeclaredClasses()[0];
    assertNotNull(st);
    Field active = st.getField("ACTIVE");
    assertNotNull(active);

    // now, trying with MVEL
    ParserConfiguration pconf = new ParserConfiguration();
    pconf.setClassLoader(loader);
    pconf.addImport(er);
    ParserContext pctx = new ParserContext(pconf);
    pctx.setStrongTyping(true);

    Serializable compiled = MVEL.compileExpression(expr, pctx);
    Object result = MVEL.executeExpression(compiled);

    assertNotNull(result);
  }

  public void testContextObjMethodCall() {
    String str = "getName() == \"bob\"";

    ParserConfiguration pconf = new ParserConfiguration();
    ParserContext pctx = new ParserContext(pconf);
    pctx.setStrongTyping(true);
    pctx.addInput("this", Bar.class);
    ExecutableStatement stmt = (ExecutableStatement) MVEL.compileExpression(str, pctx);
    Bar ctx = new Bar();
    ctx.setName("bob");
    Boolean result = (Boolean) MVEL.executeExpression(stmt, ctx);
    assertTrue(result);
  }

  public void testMapAccessWithNestedMethodCall() {
    String str = "map[aMethod(1)] == \"one\"";

    ParserConfiguration pconf = new ParserConfiguration();
    ParserContext pctx = new ParserContext(pconf);
    pctx.setStrongTyping(true);
    pctx.addInput("this", POJO.class);
    ExecutableStatement stmt = (ExecutableStatement) MVEL.compileExpression(str, pctx);

    POJO ctx = new POJO();
    ctx.getMap().put("1", "one");
    Boolean result = (Boolean) MVEL.executeExpression(stmt, ctx);
    assertTrue(result);
  }

  public void testMVEL226() {
      Map<String, String> foo = new HashMap();
      foo.put("bar", "baz");
      OptimizerFactory.setDefaultOptimizer("reflective");
      Serializable compiledExpression = MVEL.compileExpression("this.bar");
      VariableResolverFactory factory = new MapVariableResolverFactory(new HashMap<String, Object>());
      assertEquals("baz", MVEL.executeExpression(compiledExpression, foo, factory, String.class));
    }

  public void testMapAccessProperty() {
    String str = "map.key";

    ParserConfiguration pconf = new ParserConfiguration();
    ParserContext pctx = new ParserContext(pconf);
    pctx.setStrongTyping(true);
    pctx.addInput("this", POJO.class);
    ExecutableStatement stmt = (ExecutableStatement) MVEL.compileExpression(str, pctx);

    POJO ctx = new POJO();
    try {
      MVEL.executeExpression(stmt, ctx);
      fail("Expected PropertyAccessException");
    }
    catch (PropertyAccessException ex) {
      assertTrue(ex.getMessage().contains("could not access: key"));
    }
  }

  public void testMapAccessWithNestedPropertyAO() {
      boolean allowCompilerOverride = MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING;
      MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = true;
      try {
         String str = "map[key] == \"one\"";
         ParserConfiguration pconf = new ParserConfiguration();
         ParserContext pctx = new ParserContext(pconf);
         pctx.setStrongTyping(true);
         pctx.addInput("this", POJO.class);
         ExecutableStatement stmt = (ExecutableStatement) MVEL.compileExpression(str, pctx);

         POJO ctx = new POJO();
         ctx.getMap().put("1", "one");
         Boolean result = (Boolean) MVEL.executeExpression(stmt, ctx);
         assertTrue(result);
         result = (Boolean) MVEL.executeExpression(stmt, ctx);
         assertTrue(result);
      } finally {
         MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = allowCompilerOverride;
      }
    }

  public void testMapAccessWithNestedPropertyAO_ASM() {
      OptimizerFactory.setDefaultOptimizer("ASM");
      boolean allowCompilerOverride = MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING;
      MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = true;
      try {
         String str = "map[key] == \"one\"";
         ParserConfiguration pconf = new ParserConfiguration();
         ParserContext pctx = new ParserContext(pconf);
         pctx.setStrongTyping(true);
         pctx.addInput("this", POJO.class);
         ExecutableStatement stmt = (ExecutableStatement) MVEL.compileExpression(str, pctx);

         POJO ctx = new POJO();
         ctx.getMap().put("1", "one");
         Boolean result = (Boolean) MVEL.executeExpression(stmt, ctx);
         assertTrue(result);
         result = (Boolean) MVEL.executeExpression(stmt, ctx);
         assertTrue(result);
      } finally {
         MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = allowCompilerOverride;
      }
  }

  public void testMapAccessWithNestedProperty() {
    String str = "map[key] == \"one\"";

    ParserConfiguration pconf = new ParserConfiguration();
    ParserContext pctx = new ParserContext(pconf);
    pctx.setStrongTyping(true);
    pctx.addInput("this", POJO.class);
    ExecutableStatement stmt = (ExecutableStatement) MVEL.compileExpression(str, pctx);

    POJO ctx = new POJO();
    ctx.getMap().put("1", "one");
    Boolean result = (Boolean) MVEL.executeExpression(stmt, ctx);
    assertTrue(result);
  }

  public void testMapAccessWithNestedPropertyRepeated() {
    /*
     * 181 - Nested property access successful in ReflectiveAccessorOptimizer 
     *   but fails in ASMAccessorOptimizer
     */
    String str = "map[key] == \"one\"";

    ParserConfiguration pconf = new ParserConfiguration();
    ParserContext pctx = new ParserContext(pconf);
    pctx.setStrongTyping(true);
    pctx.addInput("this", POJO.class);
    ExecutableStatement stmt = (ExecutableStatement) MVEL.compileExpression(str, pctx);

    POJO ctx = new POJO();
    ctx.getMap().put("1", "one");
    for (int i = 0; i < 500; ++i) {
      try {
        Boolean result = (Boolean) MVEL.executeExpression(stmt, ctx);
        assertTrue(result);
      }
      catch (RuntimeException ex) {
        if (i == 0) {
          throw ex;
        }
        throw new IllegalStateException("Expression failed at iteration " + i, ex);
      }
    }
  }

  public void testArrays() {
    String str = "Object[] a = new Object[3]; a[0] = \"a\"; a[1] = \"b\"; a[2] = \"c\"; System.out.println(java.util.Arrays.toString(a));";
    ParserConfiguration pconf = new ParserConfiguration();
    ParserContext pctx = new ParserContext(pconf);
    pctx.setStrongTyping(true);
    ExecutableStatement stmt = (ExecutableStatement) MVEL.compileExpression(str, pctx);
    MVEL.executeExpression(stmt, new HashMap());
  }

  public void testFullyQualifiedEnums() {
    String str = "System.out.println( STATIC_ENUM.FOO ); \n" +
        "System.out.println( org.mvel2.tests.core.res.MyInterface$STATIC_ENUM.BAR );\n" +
        "System.out.println( org.mvel2.tests.core.res.MyInterface.MyInnerInterface.INNER_STATIC_ENUM.BAR );\n" +
        "System.out.println( RoundingMode.UP );\n" +
        "System.out.println( java.math.RoundingMode.DOWN );";

    ParserConfiguration pconf = new ParserConfiguration();
    ParserContext pctx = new ParserContext(pconf);
    pctx.setStrongTyping(true);
    pctx.addInput("this", MyInterface.class);
    pctx.addImport(MyInterface.STATIC_ENUM.class);
    pctx.addImport(java.math.RoundingMode.class);
    ExecutableStatement stmt = (ExecutableStatement) MVEL.compileExpression(str, pctx);
  }

  public void testGenericsMap() throws Exception {
    try {
      String str = "triangle.deliveries[0].containsKey( \"x\" )";

      ParserConfiguration pconf = new ParserConfiguration();
      ParserContext pctx = new ParserContext(pconf);
      pctx.setStrongTyping(true);
      pctx.addInput("triangle", Triangle.class);
      ExecutableStatement stmt = (ExecutableStatement) MVEL.compileExpression(str, pctx);
    }
    catch (Exception e) {
      // it should not raise CCE
      e.printStackTrace();
      throw e;
    }
  }

  public void testWithInsideBlock() {
    String str = "Foo f = new Foo(); with(f) { setBoolTest( true ) }; f.isBoolTest()";

    ParserConfiguration pconf = new ParserConfiguration();
    ParserContext pctx = new ParserContext(pconf);
    pctx.setStrongTyping(true);
    pctx.addInput("this", Bar.class);
    pctx.addImport(Foo.class);
    ExecutableStatement stmt = (ExecutableStatement) MVEL.compileExpression(str, pctx);
    Bar ctx = new Bar();
    Boolean result = (Boolean) MVEL.executeExpression(stmt, ctx, new HashMap());
    assertTrue(result);
  }

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

    ParserConfiguration pconf = new ParserConfiguration();
    ParserContext pctx = new ParserContext(pconf);
    pctx.setStrongTyping(true);
    pctx.addInput("this", Bar.class);
    pctx.addImport(Foo.class);
    List<String> errors = new ArrayList<String>();
    for (String s : str) {
      try {
        ExecutableStatement stmt = (ExecutableStatement) MVEL.compileExpression(s, pctx);
        Bar ctx = new Bar();
        Boolean result = (Boolean) MVEL.executeExpression(stmt, ctx, new HashMap());
        assertTrue(result);
      }
      catch (Exception e) {
        e.printStackTrace();
        errors.add("**** Error on expression: " + s + "\n" + e.getMessage());
      }
    }
    assertTrue(errors.toString(), errors.isEmpty());
  }

  public void testMethodCallWithSpacesASM() {
    OptimizerFactory.setDefaultOptimizer("ASM");
    try {
       testMethodCallWithSpaces();
    }
    finally {
       OptimizerFactory.setDefaultOptimizer(OptimizerFactory.DYNAMIC);
    }  
  }

  public void testInlineConstructor() {
    String str = "cheese = new Cheese().{ type = $c.type };";
    ParserConfiguration pconf = new ParserConfiguration();
    ParserContext pctx = new ParserContext(pconf);
    pctx.setStrongTyping(true);
    pctx.addInput("$c", Cheese.class);
    pctx.addImport(Cheese.class);
    ExecutableStatement stmt = (ExecutableStatement) MVEL.compileExpression(str, pctx);
    Cheese $c = new Cheese();
    $c.setType("stilton");
    Map<String, Object> vars = new HashMap<String, Object>();
    vars.put("$c", $c);
    Cheese cheese = (Cheese) MVEL.executeExpression(stmt, vars);
    assertEquals("stilton", cheese.getType());
  }

  public void testStrTriangleEqualsEquals() {

    MVEL.COMPILER_OPT_ALLOW_NAKED_METH_CALL = true;

    try {
      ParserConfiguration pconf = new ParserConfiguration();
      ParserContext pctx = new ParserContext(pconf);
      pctx.addInput("this", Triangle.class);
      pctx.setStrongTyping(true);

      String str = "this.strLabel == this";

      try {
        ExecutableStatement stmt = (ExecutableStatement) MVEL.compileExpression(str, pctx);
        fail("should have failed");
      }
      catch (CompileException e) {

        System.out.println();
        return;
      }
    }
    finally {
      MVEL.COMPILER_OPT_ALLOW_NAKED_METH_CALL = false;
    }
  }

  //public void testSysoutNullVariable() {
  //  // Create our root Map object
  //  Map<String, String> map = new HashMap<String, String>();
  //  map.put("foo", null);
  //
  //  VariableResolverFactory factory = new MapVariableResolverFactory(new HashMap<String, Object>());
  //  factory.createVariable("this", map);
  //
  //  org.mvel2.MVEL.executeExpression(org.mvel2.MVEL.compileExpression("System.out.println(foo);"), map, factory);
  //}

  public void testPackageImportEnum() {
    String str = "new Status( START )";
    ParserConfiguration pconf = new ParserConfiguration();
    pconf.addPackageImport("org.mvel2.tests.core.res");
    pconf.addPackageImport("org.mvel2.tests.core.res.Status");
    ParserContext context = new ParserContext(pconf);
    context.setStrongTyping(true);

    Serializable s = MVEL.compileExpression(str.trim(), context);
    assertEquals(new Status(Status.START), MVEL.executeExpression(s));
    assertFalse(new Status(Status.STOP).equals(MVEL.executeExpression(s)));
  }

//  public void testStrDoubleEqualsEquals() {
//
//    MVEL.COMPILER_OPT_ALLOW_NAKED_METH_CALL = true;
//    try {
//      ParserConfiguration pconf = new ParserConfiguration();
//      ParserContext pctx = new ParserContext(pconf);
//      pctx.addInput("this", Triangle.class);
//      pctx.setStrongTyping(true);
//
//      String str = "strLabel == doubleVal";
//
//      try {
//        ExecutableStatement stmt = (ExecutableStatement) MVEL.compileExpression(str, pctx);
//      }
//      catch (CompileException e) {
//        fail("should have failed");
//
//      }
//    }
//    finally {
//      MVEL.COMPILER_OPT_ALLOW_NAKED_METH_CALL = false;
//    }
//  }

  public void testNarrowToWideCompare() {
    Serializable s = MVEL.compileExpression("new String('foo') == new Object()",
        ParserContext.create().stronglyTyped());

    assertFalse((Boolean) MVEL.executeExpression(s));
  }

  public void testPrimitiveArray() {
    Map vars = new HashMap();
    vars.put("array", new boolean[]{true, false});
    String expression = "a = true; array contains a";
    Serializable compiled = MVEL.compileExpression(expression);
    boolean result = (Boolean) MVEL.executeExpression(compiled, vars);
    assertEquals(true, result);

    vars = new HashMap();
    vars.put("array", new int[]{1, 3, 4});
    expression = "a = 2; array contains a";
    compiled = MVEL.compileExpression(expression);
    result = (Boolean) MVEL.executeExpression(compiled, vars);
    assertEquals(false, result);

    expression = "a = 3; array contains a";
    compiled = MVEL.compileExpression(expression);
    result = (Boolean) MVEL.executeExpression(compiled, vars);
    assertEquals(true, result);

    expression = "a = false; array contains a";
    compiled = MVEL.compileExpression(expression);
    result = (Boolean) MVEL.executeExpression(compiled, vars);
    assertEquals(false, result);
  }

  public void testPrimitiveArrayWithStrongTyping() {
    String expression = "a = true; new boolean[] { true, false } contains a";
    boolean result = (Boolean) compileAndExecuteWithStrongTyping(expression);
    assertEquals(true, result);

    expression = "a = 2; new int[] { 1, 3 } contains a";
    result = (Boolean) compileAndExecuteWithStrongTyping(expression);
    assertEquals(false, result);

    expression = "a = true; array = new boolean[] { true, false }; array contains a";
    result = (Boolean) compileAndExecuteWithStrongTyping(expression);
    assertEquals(true, result);
  }

  public void testVarArgsParams() {
    Set<String> imports = new HashSet<>();
    imports.add(AStatic.class.getCanonicalName());

    assertEquals(String.format("null,"),
                   runSingleTest("a = null; AStatic.process(a);", imports));
    assertEquals(String.format("hello,world,"),
                   runSingleTest("AStatic.process(\"hello\",\"world\");", imports));

      assertEquals(String.format(""),
                   runSingleTest("AStatic.process();", imports));
      assertEquals(String.format("null"),
                   runSingleTest("AStatic.process(null);", imports));

    assertEquals(String.format("xxx"),
        runSingleTest("String.format(\"xxx\")", imports));

    assertEquals(String.format("%010d", 123),
        runSingleTest("String.format(\"%010d\", 123)", imports));

    assertEquals(String.format("%010d", 123),
        runSingleTest("var o = new Object[1]; o[0] = 123; String.format(\"%010d\", o);", imports));

    assertEquals(String.format("%010d", 123),
        runSingleTest("a = 123; String.format(\"%010d\", a);", imports));

    assertEquals(String.format("%010d -- %010d", 123, 456),
        runSingleTest("a = 123; b = 456; String.format(\"%010d -- %010d\", new Object[] {a, b});", imports));

    assertEquals(String.format("%010d -- %010d", 123, 456),
        runSingleTest("var o = new Object[2]; o[0] = 123; o[1] = 456; String.format(\"%010d -- %010d\", o);", imports));

    assertEquals(String.format("%010d -- %010d", 123, 456),
        runSingleTest("String.format(\"%010d -- %010d\", 123, 456);", imports));

    assertEquals(String.format("%010d -- %010d", 123, 456),
        runSingleTest("a = 123; b = 456; String.format(\"%010d -- %010d\", a, b);", imports));
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

  public void testTypedVarArgsParams() {
    Set<String> imports = new HashSet<>();
    imports.add(MySet.class.getCanonicalName());
    imports.add(A.class.getCanonicalName());
    imports.add(B.class.getCanonicalName());

    String invoke0 = "new A().invoke(\"s1\", \"s2\");";
    int result = (Integer) compileAndExecuteWithStrongTyping(invoke0, imports);
    assertEquals(0, result);
    assertEquals(0, runSingleTest(invoke0, imports));

    String invokeSum0 = "A.invokeSum(3);";
    result = (Integer) compileAndExecuteWithStrongTyping(invokeSum0, imports);
    assertEquals(3, result);
    assertEquals(3, runSingleTest(invokeSum0, imports));

    String invoke3 = "new A().invoke(\"s1\", \"s2\", new B(), new B(), new B());";
    result = (Integer) compileAndExecuteWithStrongTyping(invoke3, imports);
    assertEquals(3, result);
    assertEquals(3, runSingleTest(invoke3, imports));

    String invokeSum2 = "A.invokeSum(3, new B(4), new B(5))";
    result = (Integer) compileAndExecuteWithStrongTyping(invokeSum2, imports);
    assertEquals(12, result);
    assertEquals(12, runSingleTest(invokeSum2, imports));
  }

  public void testTypedVarArgsConstructor() {
    Set<String> imports = new HashSet<>();
    imports.add("org.mvel2.tests.core.CoreConfidenceTests.MySet\n");

    MySet result = (MySet) compileAndExecuteWithStrongTyping("new MySet(\"s1\", \"s2\");", imports);
    assertTrue(result.contains("s1"));
    assertTrue(result.contains("s2"));
  }

  public void testTypedVarArgsConstructorASM() {
    testTypedVarArgsConstructor();

//    OptimizerFactory.setDefaultOptimizer("ASM");
//    try {
//
//    }
//    finally {
//	 OptimizerFactory.setDefaultOptimizer(OptimizerFactory.DYNAMIC);
//    }
  }

  private <T> T compileAndExecuteWithStrongTyping(String expression) {
    return compileAndExecuteWithStrongTyping(expression, Collections.emptySet(), new HashMap());
  }

  private <T> T compileAndExecuteWithStrongTyping(String expression, Set<String> imports) {
    return compileAndExecuteWithStrongTyping(expression, imports, new HashMap());
  }

  private <T> T compileAndExecuteWithStrongTyping(String expression, Map vars) {
    return compileAndExecuteWithStrongTyping(expression, Collections.emptySet(), vars);
  }

  private <T> T compileAndExecuteWithStrongTyping(String expression, Set<String> imports, Map vars) {
//    ParserContext context = new ParserContext();
//    context.setStrongTyping(true);
//    context.setStrictTypeEnforcement(true);
//    Serializable compiled = MVEL.compileExpression(expression, context);
//    return (T) MVEL.executeExpression(compiled, vars);
    return (T) new org.mvel3.MVEL().executeExpression(expression, imports, vars);
  }


  public void testArrayCreation() {
    assertTrue(Arrays.deepEquals(new Object[0], (Object[]) compileAndExecuteWithStrongTyping("{}")));
    assertTrue(Arrays.deepEquals(new String[0], (String[]) compileAndExecuteWithStrongTyping("new String[] {}")));
    assertTrue(Arrays.deepEquals(new String[]{"xyz"}, (String[]) compileAndExecuteWithStrongTyping("new String[] { \"xyz\" }")));
    assertTrue(Arrays.deepEquals(new String[]{"xyz"}, (String[]) compileAndExecuteWithStrongTyping("new String[] { new String(\"xyz\") }")));
    assertTrue(Arrays.deepEquals(new String[]{"xyz", "abc"}, (String[]) compileAndExecuteWithStrongTyping("new String[] { new String(\"xyz\"), new String(\"abc\") }")));
    assertTrue(Arrays.deepEquals(new B[0], (B[]) compileAndExecuteWithStrongTyping("import org.mvel2.tests.core.CoreConfidenceTests.B;\nnew B[] { }")));
    assertTrue(Arrays.deepEquals(new B[]{new B(5)}, (B[]) compileAndExecuteWithStrongTyping("import org.mvel2.tests.core.CoreConfidenceTests.B;\nnew B[] { new B(5) }")));
    assertTrue(Arrays.deepEquals(new B[]{new B(), new B(), new B()}, (B[]) compileAndExecuteWithStrongTyping("import org.mvel2.tests.core.CoreConfidenceTests.B;\nnew B[] {new B(),new B(),new B()}")));
  }

  public static class Bean1 {
    private String Field1;
    private String FIELD2;
    private int intField;

    public String getField1() {
      return Field1;
    }

    public void setField1(String Field1) {
      this.Field1 = Field1;
    }

    public String getFIELD2() {
      return FIELD2;
    }

    public void setFIELD2(String FIELD2) {
      this.FIELD2 = FIELD2;
    }

    public int getIntField() {
      return intField;
    }

    public void setIntField(int intField) {
      this.intField = intField;
    }

    public Option<String> getField1Option() {
      return new Option(Field1);
    }
  }

  public static class Option<T> {
    public final T t;

    public Option(T t) {
      this.t = t;
    }

    public boolean isDefined() {
      return t != null;
    }

    public T get() {
      return t;
    }
  }

  public void testUppercaseField() {
    String ex = "Field1 == \"foo\" || FIELD2 == \"bar\"";
    final ParserContext parserContext2 = new ParserContext();
    parserContext2.setStrictTypeEnforcement(true);
    parserContext2.setStrongTyping(true);
    parserContext2.addInput("this", Bean1.class);
    MVEL.analyze(ex, parserContext2);
  }

  public void testExpressionReturnType() {
    assertEquals(String.class, expressionReturnType("Field1"));
    assertEquals(String.class, expressionReturnType("Field1 + FIELD2"));
    assertEquals(String.class, expressionReturnType("Field1 + 3"));
    assertEquals(String.class, expressionReturnType("Field1 + 3 + FIELD2"));
    assertEquals(int.class, expressionReturnType("intField"));
    assertEquals(Integer.class, expressionReturnType("intField = 3"));
    assertEquals(Boolean.class, expressionReturnType("intField == 3"));
    assertEquals(Boolean.class, expressionReturnType("intField == \"3\""));
    assertEquals(Boolean.class, expressionReturnType("intField == 1 || Field1 == \"xxx\""));
    assertEquals(Boolean.class, expressionReturnType("FIELD2 == \"yyy\" && intField == 1 + 2 || Field1 == \"xxx\""));
  }

  public void testConstantOnLeftExpression() {
    assertEquals(Boolean.class, expressionReturnType("3 == intField"));
    assertEquals(Boolean.class, expressionReturnType("\"xxx\" == Field1"));
    assertEquals(Boolean.class, expressionReturnType("null == Field1"));
  }

  public void testExpressionReturnTypeWithGenerics() {
    assertEquals(String.class, expressionReturnType("Field1Option.get"));
    assertEquals(String.class, expressionReturnType("Field1Option.t"));
  }

  public void testModuloReturnType() {
    assertEquals(Integer.class, expressionReturnType("3 % 2"));
  }

  public void testWrongExpressions() {
    wrongExpressionMustFail("Field1 == 3");
    wrongExpressionMustFail("Field1 - 3");
    wrongExpressionMustFail("intField == 3 || Field1");
  }

  private void wrongExpressionMustFail(String expr) {
    try {
      expressionReturnType(expr);
      fail("wrong expression '" + expr + "' must fail");
    }
    catch (Exception e) {
    }
  }

  private Class<?> expressionReturnType(String expr) {
    final ParserContext parserContext = new ParserContext();
    parserContext.setStrictTypeEnforcement(true);
    parserContext.setStrongTyping(true);
    parserContext.addInput("this", Bean1.class);
    MVEL.compileExpression(expr, parserContext);
    return MVEL.analyze(expr, parserContext);
  }

  public void testExponentialNotation() {
    assertEquals(110.0, runSingleTest("10 + 1.0E+2"));
    assertEquals(90.0, runSingleTest("1.0E+2 - 10"));
    assertEquals(10.01, runSingleTest("10 + 1.0E-2"));
  }

  public void testShiftOperator() {
    String expression = "1 << 65536L";
    Serializable compiled = MVEL.compileExpression(expression, context);
    Object result = MVEL.executeExpression(compiled, new HashMap());
    assertEquals(1 << 65536L, result);

    expression = "one << shift";
    Map map = new HashMap() {{
      put("one", 1);
      put("shift", 65536L);
    }};
    compiled = MVEL.compileExpression(expression, context);
    result = MVEL.executeExpression(compiled, map);
    assertEquals(1 << 65536L, result);
    System.out.println(result);
  }

  public void testSystemOutOnPrivateClass() {
    PrintStream originalSystemOut = System.out;
    System.setOut(new MyPrivatePrintStream(System.out));
    String expression = "System.out.println(\"Hello World\");";
    runSingleTest(expression);
    System.setOut(originalSystemOut);
  }

  private static class MyPrivatePrintStream extends PrintStream {
    public MyPrivatePrintStream(OutputStream os) {
      super(os);
    }

    public void println(String s) {
      super.println(s);
    }
  }

  public void testSystemOutWithActualInstanceMethod() {
    PrintStream originalSystemOut = System.out;
    System.setOut(new MyPublicPrintStream(System.out));
    String expression = "System.out.myPrintln(\"Hello World\");";
    runSingleTest(expression);
    System.setOut(originalSystemOut);
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

  public void testMinusOperatorWithoutSpace() {
    String str = "length == $c.length -1";

    ParserConfiguration pconf = new ParserConfiguration();
    ParserContext pctx = new ParserContext(pconf);
    ExecutableStatement stmt = (ExecutableStatement) MVEL.compileExpression(str, pctx);
    Column col1 = new Column("x", 0);
    Column col2 = new Column("x", 0);
    Map<String, Object> vars = new HashMap<String, Object>();
    vars.put("$c", col2);
    Boolean result = (Boolean) MVEL.executeExpression(stmt, col1, vars);
    assertFalse(result);
  }

  public void testPrimitiveNumberCoercion() {
    final ParserContext parserContext = new ParserContext();
    parserContext.setStrictTypeEnforcement(true);
    parserContext.setStrongTyping(true);
    parserContext.addInput("a", int.class);
    parserContext.addInput("b", double.class);
    Class<?> clazz = MVEL.analyze("a > b", parserContext);
    assertEquals(Boolean.class, clazz);
  }

  public void testClassLiteral() {
    try {
      MVEL.COMPILER_OPT_SUPPORT_JAVA_STYLE_CLASS_LITERALS = true;
      final ParserContext parserContext = new ParserContext();
      parserContext.setStrictTypeEnforcement(true);
      parserContext.setStrongTyping(true);
      parserContext.addInput("a", Class.class);
      MVEL.compileExpression("a == String", parserContext);
    }
    finally {
      MVEL.COMPILER_OPT_SUPPORT_JAVA_STYLE_CLASS_LITERALS = false;
    }
  }

  public void testBigIntegerWithZeroValue() {
    final ParserContext parserContext = new ParserContext();
    parserContext.setStrictTypeEnforcement(true);
    parserContext.setStrongTyping(true);
    parserContext.addInput("a", BigInteger.class);
    Serializable expression = MVEL.compileExpression("a == 0I", parserContext);
    boolean result = (Boolean) MVEL.executeExpression(expression, new HashMap() {{
      put("a", new BigInteger("0"));
    }});
    assertTrue(result);
  }

  public void testBigDecimalWithZeroValue() {
    final ParserContext parserContext = new ParserContext();
    parserContext.setStrictTypeEnforcement(true);
    parserContext.setStrongTyping(true);
    parserContext.addInput("a", BigDecimal.class);
    Serializable expression = MVEL.compileExpression("a == 0B", parserContext);
    boolean result = (Boolean) MVEL.executeExpression(expression, new HashMap() {{
      put("a", new BigDecimal("0.0"));
    }});
    assertTrue(result);
  }

  public void testMethodReturningPrimitiveTypeAnalysis() {
    String str = "value";

    ParserConfiguration pconf = new ParserConfiguration();
    ParserContext pctx = new ParserContext(pconf);
    pctx.addInput("this", MyObj.class);
    pctx.setStrongTyping(true);

    Class<?> returnType = MVEL.analyze(str, pctx);
    assertEquals(long.class, returnType);
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

  public void testStaticMethodsInvocationWithNullArg() {
    String str = "org.mvel2.tests.core.CoreConfidenceTests$MyObj.doSomething(null, \"abc\")";

    ParserConfiguration pconf = new ParserConfiguration();
    ParserContext pctx = new ParserContext(pconf);
    pctx.setStrongTyping(true);

    assertEquals(null + "abc", MVEL.executeExpression(MVEL.compileExpression(str, pctx)));
  }

  public void testStaticMethodsInvocationWithNullArgASM() {
      OptimizerFactory.setDefaultOptimizer("ASM");
      testStaticMethodsInvocationWithNullArg();
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

  public void testCovariance() {
    final ParserContext parserContext = new ParserContext();
    parserContext.setStrictTypeEnforcement(true);
    parserContext.setStrongTyping(true);
    parserContext.addInput("this", ImplementationA.class);
    assertEquals(int.class, MVEL.analyze("b.value", parserContext));
    assertEquals(42, MVEL.executeExpression(MVEL.compileExpression("b.value", parserContext), new ImplementationA()));
  }

  public void testDivisionType() {
    final ParserContext parserContext = new ParserContext();
    parserContext.setStrictTypeEnforcement(true);
    parserContext.setStrongTyping(true);
    parserContext.addInput("a", BigDecimal.class);
    Serializable expression = MVEL.compileExpression("(a / 3).setScale(2, java.math.BigDecimal.ROUND_HALF_UP)", parserContext);
    Object result = MVEL.executeExpression(expression, new HashMap() {{
      put("a", new BigDecimal("3.0"));
    }});
    System.out.println(result);
  }

  public void testPrimitiveNumberCoercionDuringDivisionShouldWorkOnBothSide() {
    final ParserContext parserContext = new ParserContext();
    parserContext.setStrictTypeEnforcement(true);
    parserContext.setStrongTyping(true);
    parserContext.addInput("a", int.class);
    parserContext.addInput("b", int.class);

    int a = 1;
    int b = 2;
    Object res = a / b;
    System.out.printf("Result class Java: %s\nResult value: %s\n", res.getClass(), res);
    Object resBoolean = a / b < 0.99;
    System.out.println("Result Boolean: " + resBoolean);

    Serializable constantDoubleLeft = MVEL.compileExpression("0.99 >= a / b", parserContext);
    Object resultLeft = MVEL.executeExpression(constantDoubleLeft, new HashMap() {{
      put("a", 1);
      put("b", 2);
    }});
    assertEquals(true, resultLeft);

    Serializable constantDoubleRight = MVEL.compileExpression("a / b < 0.99", parserContext);
    Object resultRight = MVEL.executeExpression(constantDoubleRight, new HashMap() {{
      put("a", 1);
      put("b", 2);
    }});
    assertEquals(true, resultRight);

    parserContext.addInput("c", double.class);
    parserContext.addInput("d", double.class);
    Serializable constantIntRight = MVEL.compileExpression("c / d > 0", parserContext);
    Object resultRightInt = MVEL.executeExpression(constantIntRight, new HashMap() {{
      put("c", 1);
      put("d", 2);
    }});
    assertEquals(true, resultRightInt);
  }

  public void testNumberCoercion() {
      final ParserContext parserContext = new ParserContext();
      parserContext.setStrictTypeEnforcement(true);
      parserContext.setStrongTyping(true);
      parserContext.addInput("a", int.class);

      // Long / Integer to Long / Long
      Serializable longDivByInt = MVEL.compileExpression("15 * Math.round( new java.math.BigDecimal(\"49.4\") ) / 100.0", parserContext);
      Object resultLongDivByInt = MVEL.executeExpression(longDivByInt, new HashMap());
      assertEquals(7.35, resultLongDivByInt);

      // Don't convert BigDecimal to int
      Serializable intDivByBigDecimal = MVEL.compileExpression("a / new java.math.BigDecimal(\"0.5\")", parserContext);
      Object resultIntDivByBigDecimal = MVEL.executeExpression(intDivByBigDecimal, new HashMap() {{
          put("a", 10);
      }});
      assertEquals(20, ((BigDecimal)resultIntDivByBigDecimal).intValue());

      // Don't convert Double to int
      Serializable intDivByDouble = MVEL.compileExpression("a / 0.5", parserContext);
      Object resultIntDivByDouble = MVEL.executeExpression(intDivByDouble, new HashMap() {{
          put("a", 10);
      }});
      assertEquals(20, ((Double)resultIntDivByDouble).intValue());
    }

  public void testUntypedClone() {
    String expression = "obj.clone();";
    ParserContext context = new ParserContext();
    context.setStrongTyping(false);
    context.setStrictTypeEnforcement(false);
    MVEL.analyze(expression, context);

    try {
      context.addInput("obj", Object.class);
      context.setStrongTyping(true);
      context.setStrictTypeEnforcement(true);
      MVEL.analyze(expression, context);
      fail("Must fail with strong typing");
    }
    catch (CompileException e) {
    }
  }

  public void testOverloading() {
    final ParserContext parserContext = new ParserContext();
    parserContext.setStrictTypeEnforcement(true);
    parserContext.setStrongTyping(true);
    parserContext.addInput("this", Overloaded.class);
    Overloaded overloaded = new Overloaded();

    assertEquals(15, MVEL.executeExpression(MVEL.compileExpression("method(5, 9, \"x\")", parserContext), overloaded));
    assertEquals(-3, MVEL.executeExpression(MVEL.compileExpression("method(5, \"x\", 9)", parserContext), overloaded));
    assertEquals(-13, MVEL.executeExpression(MVEL.compileExpression("method(\"x\", 5, 9)", parserContext), overloaded));
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


  public void testPippo() {
    final ParserContext parserContext = new ParserContext();
    parserContext.setStrongTyping(true);
    assertEquals(String.class, MVEL.analyze("new String(\"b)ar\")", parserContext));
  }

  public void testReturnTypeExtendingGeneric() {
    final ParserContext parserContext = new ParserContext();
    parserContext.setStrongTyping(true);
    parserContext.addInput("this", StringConcrete.class);
    assertEquals(String.class, MVEL.analyze("foo.concat(\"bar\")", parserContext));
    assertEquals(String.class, MVEL.analyze("getFoo().concat(\"bar\")", parserContext));
  }

  public static abstract class AbstractBase<T> {
    protected T foo;
    public T getFoo() { return foo; }
  }

  public static class StringConcrete extends AbstractBase<String> {
    public StringConcrete() { this.foo = new String(); }
  }

  public void testNullCollection() throws CompileException {
    boolean allowCompilerOverride = MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING;
    MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = true;

    String[] names  = { "missing" };
    String[] values = { null };

    try {
      MVEL.executeExpression( (CompiledExpression) MVEL.compileExpression("1; missing[3]"),
                              new IndexedVariableResolverFactory(names, values) );
      fail("Should throw a NullPointerExcption");
    } catch (Exception e) {
    } finally {
      MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = allowCompilerOverride;
    }
  }

  public void testRegExWithCast() {
    final ParserContext parserContext = new ParserContext();
    parserContext.setStrongTyping(true);
    parserContext.addInput("this", Foo.class);
    assertEquals(Boolean.class, MVEL.analyze("(String)bar.name ~= '[a-z].+'", parserContext));
  }

  public void testUnwantedImport() {
    ParserConfiguration conf = new ParserConfiguration();
    conf.addPackageImport("java.util");
    conf.addPackageImport("org.mvel2.tests.core.res");
    ParserContext pctx = new ParserContext( conf );
    MVEL.analysisCompile( "ScenarioType.Set.ADD", pctx );
    assertNull(conf.getImports().get("Set"));
  }

  public void testUnaryNegative() {
    ParserConfiguration conf = new ParserConfiguration();
    ParserContext pctx = new ParserContext( conf );
    pctx.setStrictTypeEnforcement(true);
    pctx.setStrongTyping(true);
    pctx.addInput("value", int.class);
    Map vars = new HashMap() {{ put("value", 42); }};
    assertEquals(-42, MVEL.executeExpression(MVEL.compileExpression("-value", pctx), vars));
  }

  public void testUnaryNegativeWithSpace() {
    ParserConfiguration conf = new ParserConfiguration();
    ParserContext pctx = new ParserContext( conf );
    pctx.setStrictTypeEnforcement(true);
    pctx.setStrongTyping(true);
    pctx.addInput("value", int.class);
    Map vars = new HashMap() {{ put("value", 42); }};
    assertEquals(-42, MVEL.executeExpression(MVEL.compileExpression("- value", pctx), vars));
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

  public void testStaticMethodInvocation() {
    ParserConfiguration conf = new ParserConfiguration();
    conf.addImport(ARef.class);
    conf.addImport(BRef.class);
    ParserContext pctx = new ParserContext( conf );
    pctx.setStrictTypeEnforcement(true);
    pctx.setStrongTyping(true);
    pctx.addInput("value", String.class);
    Map vars = new HashMap() {{ put("value", "1234"); }};
    assertEquals(0, MVEL.executeExpression(MVEL.compileExpression("ARef.getSize(value)", pctx), vars));
    assertEquals(4, MVEL.executeExpression(MVEL.compileExpression("BRef.getSize(value)", pctx), vars));
  }

  public void testMultiplyIntByDouble() {
    ParserConfiguration conf = new ParserConfiguration();
    ParserContext pctx = new ParserContext( conf );
    pctx.setStrictTypeEnforcement(true);
    pctx.setStrongTyping(true);
    pctx.addInput("i", Integer.class);
    pctx.addInput("d", Double.class);
    Map vars = new HashMap() {{ put("i", 10); put("d", 0.3); }};
    assertEquals(3.0, MVEL.executeExpression(MVEL.compileExpression("i*d", pctx), vars));
    assertEquals(3.0, MVEL.executeExpression(MVEL.compileExpression("i*0.3", pctx), vars));
  }

  public void testCharToStringCoercionForComparison() {
    ParserConfiguration conf = new ParserConfiguration();
    ParserContext pctx = new ParserContext( conf );
    pctx.setStrictTypeEnforcement(true);
    pctx.setStrongTyping(true);
    pctx.addInput("ch", Character.class);
    Map vars = new HashMap() {{ put("ch", 'a'); }};
    assertEquals(true, MVEL.executeExpression(MVEL.compileExpression("ch == \"a\"", pctx), vars));
    assertEquals(false, MVEL.executeExpression(MVEL.compileExpression("ch == \"b\"", pctx), vars));

//    assertEquals(true, MVEL.executeExpression(MVEL.compileExpression("\"a\" == ch", pctx), vars));
//    assertEquals(false, MVEL.executeExpression(MVEL.compileExpression("\"b\" == ch", pctx), vars));
  }

  public void testFieldNameWithUnderscore() {
      final ParserContext parserContext = new ParserContext();
      parserContext.setStrictTypeEnforcement(true);
      parserContext.setStrongTyping(true);
      parserContext.addInput("this", Underscore.class);
      Underscore underscore = new Underscore();

      assertEquals(true, MVEL.executeExpression(MVEL.compileExpression("_id == \"test\"", parserContext), underscore));
  }

    public static class Underscore {
        public String get_id() {
            return "test";
        }
    }

  public void testEmptyOperatorOnStrings() {
    ParserConfiguration conf = new ParserConfiguration();
    ParserContext pctx = new ParserContext( conf );
    pctx.setStrictTypeEnforcement(true);
    pctx.setStrongTyping(true);

    pctx.addInput("nullString", String.class);
    pctx.addInput("emptyString", String.class);
    pctx.addInput("blankString", String.class);
    pctx.addInput("nonEmptyString", String.class);
    Map vars = new HashMap() {{
        put("nullString", null);
        put("emptyString", "");
        put("blankString", "   ");
        put("nonEmptyString", "abc");
    }};

    assertEquals(true, MVEL.executeExpression(MVEL.compileExpression("nullString == empty", pctx), vars));
    assertEquals(true, MVEL.executeExpression(MVEL.compileExpression("emptyString == empty", pctx), vars));
    assertEquals(true, MVEL.executeExpression(MVEL.compileExpression("blankString == empty", pctx), vars));
    assertEquals(false, MVEL.executeExpression(MVEL.compileExpression("nonEmptyString == empty", pctx), vars));
  }

  public void testEmptyOperatorOnBoolean() {
    ParserConfiguration conf = new ParserConfiguration();
    ParserContext pctx = new ParserContext( conf );
    pctx.setStrictTypeEnforcement(true);
    pctx.setStrongTyping(true);

    pctx.addInput("bNull", Boolean.class);
    pctx.addInput("bTrue", Boolean.class);
    pctx.addInput("bFalse", Boolean.class);
    Map vars = new HashMap() {{
        put("bNull", null);
        put("bTrue", true);
        put("bFalse", false);
    }};

    assertEquals(true, MVEL.executeExpression(MVEL.compileExpression("bNull == empty", pctx), vars));
    assertEquals(false, MVEL.executeExpression(MVEL.compileExpression("bTrue == empty", pctx), vars));
    assertEquals(true, MVEL.executeExpression(MVEL.compileExpression("bFalse == empty", pctx), vars));
  }

  public void testEmptyOperatorOnInteger() {
    ParserConfiguration conf = new ParserConfiguration();
    ParserContext pctx = new ParserContext( conf );
    pctx.setStrictTypeEnforcement(true);
    pctx.setStrongTyping(true);

    pctx.addInput("nullInt", Integer.class);
    pctx.addInput("zero", Integer.class);
    pctx.addInput("nonZero", Integer.class);
    Map vars = new HashMap() {{
        put("nullInt", null);
        put("zero", 0);
        put("nonZero", 42);
    }};

    assertEquals(true, MVEL.executeExpression(MVEL.compileExpression("nullInt == empty", pctx), vars));
    assertEquals(true, MVEL.executeExpression(MVEL.compileExpression("zero == empty", pctx), vars));
    assertEquals(false, MVEL.executeExpression(MVEL.compileExpression("nonZero == empty", pctx), vars));
  }

  public void testInstanceofOnInnerClass() {
    ParserConfiguration conf = new ParserConfiguration();
    conf.addImport(ARef.class);
    ParserContext pctx = new ParserContext( conf );
    pctx.setStrictTypeEnforcement(true);
    pctx.setStrongTyping(true);
    pctx.addInput("value", Object.class);
    Map vars = new HashMap() {{ put("value", new ARef()); }};
    assertEquals(true, MVEL.executeExpression(MVEL.compileExpression("value instanceof ARef", pctx), vars));
    assertEquals(true, MVEL.executeExpression(MVEL.compileExpression("value instanceof " + ARef.class.getCanonicalName(), pctx), vars));
  }

  public void testInstanceofWithPackageImport() {
    ParserConfiguration conf = new ParserConfiguration();
    conf.addPackageImport( "org.mvel2.tests.core" );
    ParserContext pctx = new ParserContext( conf );
    pctx.setStrictTypeEnforcement(true);
    pctx.setStrongTyping(true);
    pctx.addInput("value", Object.class);
    Map vars = new HashMap() {{ put("value", new CoreConfidenceTests()); }};
    assertEquals(true, MVEL.executeExpression(MVEL.compileExpression("value instanceof CoreConfidenceTests", pctx), vars));
    assertEquals(true, MVEL.executeExpression(MVEL.compileExpression("value instanceof " + CoreConfidenceTests.class.getCanonicalName(), pctx), vars));
  }

  public void testInstanceofWithPackageImportAndInnerClass() {
    ParserConfiguration conf = new ParserConfiguration();
    conf.addPackageImport( "org.mvel2.tests.core.CoreConfidenceTests" );
    ParserContext pctx = new ParserContext( conf );
    pctx.setStrictTypeEnforcement(true);
    pctx.setStrongTyping(true);
    pctx.addInput("value", Object.class);
    Map vars = new HashMap() {{ put("value", new ARef()); }};
    assertEquals(true, MVEL.executeExpression(MVEL.compileExpression("value instanceof ARef", pctx), vars));
    assertEquals(true, MVEL.executeExpression(MVEL.compileExpression("value instanceof " + ARef.class.getCanonicalName(), pctx), vars));
  }

  public void testCompilerExceptionFormatting() throws Exception {
    try {
      Object value = test("\n2x * 3\n");
      fail("Invalid expression should fail");
    } catch (Exception e) {
      // Invalid expression should fail to compile
    }
  }

  public void testHandleNumericConversionBug() {
    String[] testLiterals = {"0x20","020",};
    String baseExpression = "int foo = ";

    for( String literal : testLiterals ) {
      char[] decExpr = ( baseExpression + literal ).toCharArray();
      assertEquals( Integer.decode( literal ),
                    ParseTools.handleNumericConversion( decExpr, baseExpression.length(), literal.length() ) );
    }
  }

  public static class Parent {
    public Object getSomething() {
      return null;
    }
  }

  public static class Child extends Parent {
    @Override
    public String getSomething() {
      return null;
    }
  }

  public void testNoArgMethodInheritance() {
    final ParserContext parserContext = new ParserContext();
    parserContext.setStrictTypeEnforcement(true);
    parserContext.setStrongTyping(true);
    parserContext.addInput("a", Parent.class);
    parserContext.addInput("b", Child.class);
    assertEquals(Object.class, MVEL.analyze("a.getSomething()", parserContext));
    assertEquals(String.class, MVEL.analyze("b.getSomething()", parserContext));
  }
  
  
  public void testMethodOverloadMatch() throws Exception {
	  OverloadedClass c = new OverloadedClass();
	  Method found  = ParseTools.getExactMatch("putXX", new Class[]{int.class, String.class}, void.class, OverloadedInterface.class);
	  Method correct = OverloadedInterface.class.getMethod("putXX", new Class[]{int.class, String.class});
	  assertEquals(correct, found);
  }

  public static class O1 {
    public O2 getObj() {
      return new O2();
    }
  }
  public static class O2 extends O3 { }
  public abstract static class O3  {
    public String getValue() {
      return "value";
    }
  }

  public void testInvokeMethodInAbstractClass() {
    final ParserContext parserContext = new ParserContext();
    parserContext.setStrictTypeEnforcement(true);
    parserContext.setStrongTyping(true);
    parserContext.addInput("a", O1.class);
    assertEquals(String.class, MVEL.analyze("a.getObj().getValue()", parserContext));

    Map vars = new HashMap() {{ put("a", new O1()); }};
    assertEquals("value", MVEL.executeExpression(MVEL.compileExpression("a.getObj().getValue()", parserContext), vars));
  }

  public class Convention {
    private final Map<String, List<String>> comms;
    public Convention( Map<String, List<String>> comms ) {
      this.comms = comms;
    }
    public Map<String, List<String>> getComms(){
      return comms;
    }
  }

  public void testParseGenericMap() {
    final ParserContext parserContext = new ParserContext();
    parserContext.setStrictTypeEnforcement(true);
    parserContext.setStrongTyping(true);
    parserContext.addInput("conv", Convention.class);
    assertEquals(List.class, MVEL.analyze("conv.getComms().get(\"test\")", parserContext));
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

  public void testInvokeVarargConstructor() {
    ParserConfiguration conf = new ParserConfiguration();
    conf.addImport( Thingy.class );
    ParserContext pctx = new ParserContext( conf );
    pctx.setStrictTypeEnforcement(true);
    pctx.setStrongTyping(true);
    pctx.addInput("name", String.class);
    Map vars = new HashMap() {{ put("name", "test"); }};
    Thingy result = (Thingy) MVEL.executeExpression(MVEL.compileExpression("new Thingy(name)", pctx), vars);
    assertEquals( "test", result.getName() );
  }

  public void testGenericsWithOr() {
    final ParserContext parserContext = new ParserContext();
    parserContext.setStrictTypeEnforcement(true);
    parserContext.setStrongTyping(true);
    parserContext.addInput("o", OrderLine.class);
    parserContext.addInput("p", Product.class);
    Class<?> clazz = MVEL.analyze("p.id == o.product.id || p.category == o.product.category", parserContext);
    assertEquals(Boolean.class, clazz);
  }

  public interface OrderLine<T extends Product> {
    T getProduct();
  }

  public interface Product {
    String getId();
    String getCategory();
  }

  public void testAnalyzeTernary() {
    final ParserContext parserContext = new ParserContext();
    parserContext.setStrictTypeEnforcement(true);
    parserContext.setStrongTyping(true);
    parserContext.addInput("value", String.class);
    parserContext.addInput("x", Integer.class);
    parserContext.addInput("y", Integer.class);
    parserContext.addInput("z", Integer.class);
    Class<?> clazz = MVEL.analyze("z = (value == \"ALU\" ? x : y);", parserContext);
    assertEquals(Integer.class, clazz);
  }

  public void testAnalyzeWrongTernary() {
    final ParserContext parserContext = new ParserContext();
    parserContext.setStrictTypeEnforcement(true);
    parserContext.setStrongTyping(true);
    parserContext.addInput("value", String.class);
    parserContext.addInput("x", Integer.class);
    parserContext.addInput("y", Integer.class);
    parserContext.addInput("z", Integer.class);
    try {
      Class<?> clazz = MVEL.analyze( "z = (value = \"ALU\" ? x : y);", parserContext );
      fail("parse of this expression should raise an error");
    } catch (Exception e) { }
  }

  public void testPrimitiveSubtyping() {
    ParserConfiguration conf = new ParserConfiguration();
    ParserContext pctx = new ParserContext( conf );
    pctx.setStrictTypeEnforcement(true);
    pctx.setStrongTyping(true);
    BigDecimal result = (BigDecimal)MVEL.executeExpression(MVEL.compileExpression("java.math.BigDecimal.valueOf(100)", pctx), new HashMap());
    assertEquals("100", result.toString());
  }

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
    AtomicInteger i = new AtomicInteger( 10 );
    VariableResolverFactory factory = new MapVariableResolverFactory(new HashMap<String, Object>());
    factory.createVariable("i", i);

    ParserConfiguration pconf = new ParserConfiguration();
    ParserContext pctx = new ParserContext(pconf);
    pctx.setStrictTypeEnforcement(true);
    pctx.setStrongTyping(true);
    pctx.addInput("i", AtomicInteger.class);

    Serializable compiledExpr = MVEL.compileExpression(expression, pctx);
    int result = (Integer)MVEL.executeExpression(compiledExpr, null, factory);
    assertEquals(expectedResult, result);
  }
  public void test_BigDecimal_ASMoptimizerSupport() {
    /* https://github.com/mvel/mvel/issues/89
     * The following case failed in attempt from the ASM optimizer to 
     *  create a numeric constant from the value 30000B.
     */
    Serializable compiled = MVEL.compileExpression("big = new java.math.BigDecimal(\"10000\"); if (big.compareTo(30000B) > 0) then ;");
    Map<String, Integer> vars = new HashMap<String, Integer>();
    for (int i = 0; i < 1000; i++) {
      try {
        MVEL.executeExpression(compiled, vars);
      } catch (CompileException e) {
        e.printStackTrace();
        fail("Failed after #executions: " + i);
      }
    }
  }

  public void testLiteralToStringWithSpaceASM() throws Throwable {
      OptimizerFactory.setDefaultOptimizer("ASM");
      testLiteralToStringWithSpace();
  }

  public void testLiteralToStringWithSpace() throws Throwable {
      String expr = "'foo'. hashCode()";
      int hashCode = "foo". hashCode();
      Serializable s = MVEL.compileExpression(expr);
      assertEquals(Integer.valueOf(hashCode), MVEL.executeExpression(s));
  }

  public void testGetBestCandidateForBigDecimalArg() {
    Class<?>[] arguments = new Class<?>[] {BigDecimal.class};
    Method method = ParseTools.getBestCandidate(arguments, "round", Math.class, Math.class.getMethods(), true);
    assertEquals(long.class, method.getReturnType());
    Assert.assertArrayEquals(new Class<?>[] {double.class}, method.getParameterTypes());

    arguments = new Class<?>[] {BigDecimal.class, BigDecimal.class};
    method = ParseTools.getBestCandidate(arguments, "max", Math.class, Math.class.getMethods(), true);
    assertEquals(double.class, method.getReturnType());
    Assert.assertArrayEquals(new Class<?>[] {double.class, double.class}, method.getParameterTypes());

    arguments = new Class<?>[] {BigDecimal.class, BigDecimal.class};
    method = ParseTools.getBestCandidate(arguments, "scalb", Math.class, Math.class.getMethods(), true);
    assertEquals(double.class, method.getReturnType());
    Assert.assertArrayEquals(new Class<?>[] {double.class, int.class}, method.getParameterTypes());
  }
  
  public void testEmptyVarargConstructor() {
      String clsName = MySet.class.getName();
      OptimizerFactory.setDefaultOptimizer("ASM");
      Serializable s = MVEL.compileExpression("new " + clsName + "()");
      assertNotNull(MVEL.executeExpression(s));
  }
  
  public void testEmptyVarargMethod() {
    OptimizerFactory.setDefaultOptimizer("ASM");
    Serializable s = MVEL.compileExpression("m.add()");
    Map<String, MySet> inputs = Collections.singletonMap("m", new MySet());
    MVEL.executeExpression(s, inputs);
  }
  
  public void testForLoopWithSpaces() {
    VariableResolverFactory factory = new MapVariableResolverFactory(new HashMap<String, Object>());
    factory.createVariable("strings", Arrays.asList( "test" ));

    ParserConfiguration pconf = new ParserConfiguration();
    ParserContext pctx = new ParserContext(pconf);
    pctx.setStrictTypeEnforcement(true);
    pctx.setStrongTyping(true);
    pctx.addInput("strings", List.class);

    String expression =
            "for (   String   s : strings ) {\n" +
            "  return s;\n" +
            "}";

    Serializable compiledExpr = MVEL.compileExpression(expression, pctx);
    assertEquals( "test", MVEL.executeExpression(compiledExpr, null, factory));
  }
  
  public void testLooseTypeConversion() {
     int [] result = MVEL.eval("3.0", int [].class);
     assertEquals(3, result[0]);
  }

  public void testGetBestConstructorCandidateOfBigDecimal() {
      Class<?>[] arguments = new Class<?>[]{BigDecimal.class}; // new BigDecimal(BigDecimal bd) doesn't exist. But want to get a constant candidate
      Constructor constructor = ParseTools.getBestConstructorCandidate(arguments, BigDecimal.class, true);
      Assert.assertArrayEquals(new Class<?>[]{double.class}, constructor.getParameterTypes());

      arguments = new Class<?>[]{BigInteger.class};
      constructor = ParseTools.getBestConstructorCandidate(arguments, BigDecimal.class, true);
      Assert.assertArrayEquals(new Class<?>[]{BigInteger.class}, constructor.getParameterTypes());

      arguments = new Class<?>[]{int.class};
      constructor = ParseTools.getBestConstructorCandidate(arguments, BigDecimal.class, true);
      Assert.assertArrayEquals(new Class<?>[]{int.class}, constructor.getParameterTypes());

      arguments = new Class<?>[]{double.class};
      constructor = ParseTools.getBestConstructorCandidate(arguments, BigDecimal.class, true);
      Assert.assertArrayEquals(new Class<?>[]{double.class}, constructor.getParameterTypes());

      arguments = new Class<?>[]{String.class};
      constructor = ParseTools.getBestConstructorCandidate(arguments, BigDecimal.class, true);
      Assert.assertArrayEquals(new Class<?>[]{String.class}, constructor.getParameterTypes());
  }

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
    ParserContext pctx = new ParserContext();
    pctx.setStrictTypeEnforcement(true);
    pctx.setStrongTyping(true);
    pctx.addInput("a", BigDecimal.class);
    pctx.addInput("b", BigDecimal.class);

    Serializable compiledExpr = MVEL.compileExpression(expression, pctx);

    AtomicInteger i = new AtomicInteger( 10 );
    VariableResolverFactory factory = new MapVariableResolverFactory(new HashMap<String, Object>());
    factory.createVariable("a", a);
    factory.createVariable("b", b);

    if (a == null || b == null) {
      try {
        MVEL.executeExpression(compiledExpr, null, factory);
        fail("should throw a NPE");
      } catch (NullPointerException npe) {
        // expected
      }
    } else {
      Object result = MVEL.executeExpression(compiledExpr, null, factory);
      assertEquals(expected, result);
    }
  }

  public void testAnalyzeMathAbs() {
    final ParserContext parserContext = new ParserContext();
    parserContext.setStrictTypeEnforcement(true);
    parserContext.setStrongTyping(true);
    parserContext.addInput("x", Integer.class);
    parserContext.addInput("y", Integer.class);
    assertEquals(int.class, MVEL.analyze( "Math.abs(x - y);", parserContext ));
  }

  public void testStringEscape() {
    String expression = "[\"a\\\"b\"]";
    Serializable compiledExpression = MVEL.compileExpression(expression);
    List result = (List) MVEL.executeExpression(compiledExpression);
    assertEquals(1, result.size());
    assertEquals("a\"b", result.get(0));
  }
}