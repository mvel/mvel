package org.mvel2.tests.core;

import junit.framework.TestCase;
import org.mvel2.*;
import org.mvel2.ast.ASTNode;
import org.mvel2.ast.BinaryOperation;
import org.mvel2.compiler.CompiledExpression;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.compiler.ExpressionCompiler;
import org.mvel2.integration.Interceptor;
import org.mvel2.integration.PropertyHandlerFactory;
import org.mvel2.integration.ResolverTools;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.ClassImportResolverFactory;
import org.mvel2.integration.impl.DefaultLocalVariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;
import org.mvel2.integration.impl.StaticMethodImportResolverFactory;
import org.mvel2.optimizers.OptimizerFactory;
import org.mvel2.templates.TemplateRuntime;
import org.mvel2.tests.core.res.*;
import org.mvel2.tests.core.res.res2.ClassProvider;
import org.mvel2.tests.core.res.res2.Outer;
import org.mvel2.tests.core.res.res2.PublicClass;
import org.mvel2.util.MethodStub;
import org.mvel2.util.ReflectionUtil;

import java.awt.*;
import java.io.BufferedReader;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

import static java.util.Collections.unmodifiableCollection;
import static org.mvel2.MVEL.*;

@SuppressWarnings({"ALL"})
public class CoreConfidenceTests extends AbstractTest {
    public void testWhileUsingImports() {
        Map<String, Object> imports = new HashMap<String, Object>();
        imports.put("ArrayList",
                java.util.ArrayList.class);
        imports.put("List",
                java.util.List.class);

        ParserContext context = new ParserContext(imports, null, "testfile");
        ExpressionCompiler compiler = new ExpressionCompiler("List list = new ArrayList(); return (list == empty)");
        assertTrue((Boolean) executeExpression(compiler.compile(context),
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

        //    HashMap[] maps = (HashMap[]) test("import java.util.*; HashMap[] maps = new HashMap[10]; maps");
        assertEquals(10,
                maps.length);
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

    public void testAnalyzer() {
        ParserContext ctx = new ParserContext();
        MVEL.compileExpression("order.id == 10", ctx);

        for (String input : ctx.getInputs().keySet()) {
            System.out.println("input>" + input);
        }

        assertEquals(1, ctx.getInputs().size());
        assertTrue(ctx.getInputs().containsKey("order"));
    }

    public void testClassImportViaFactory() {
        MapVariableResolverFactory mvf = new MapVariableResolverFactory(createTestMap());
        ClassImportResolverFactory classes = new ClassImportResolverFactory();
        classes.addClass(HashMap.class);

        ResolverTools.appendFactory(mvf, classes);

        assertTrue(executeExpression(compileExpression("HashMap map = new HashMap()",
                classes.getImportedClasses()),
                mvf) instanceof HashMap);
    }

    public void testSataticClassImportViaFactory() {
        MapVariableResolverFactory mvf = new MapVariableResolverFactory(createTestMap());
        ClassImportResolverFactory classes = new ClassImportResolverFactory();
        classes.addClass(Person.class);

        ResolverTools.appendFactory(mvf,
                classes);

        assertEquals("tom",
                executeExpression(compileExpression("p = new Person('tom'); return p.name;",
                        classes.getImportedClasses()),
                        mvf));
    }

    public void testSataticClassImportViaFactoryAndWithModification() {
        OptimizerFactory.setDefaultOptimizer("ASM");
        MapVariableResolverFactory mvf = new MapVariableResolverFactory(createTestMap());
        ClassImportResolverFactory classes = new ClassImportResolverFactory();
        classes.addClass(Person.class);

        ResolverTools.appendFactory(mvf,
                classes);

        assertEquals(21,
                executeExpression(
                        compileExpression("p = new Person('tom'); p.age = 20; " +
                                "with( p ) { age = p.age + 1 }; return p.age;",
                                classes.getImportedClasses()),
                        mvf));
    }

    public void testCheeseConstructor() {
        MapVariableResolverFactory mvf = new MapVariableResolverFactory(createTestMap());
        ClassImportResolverFactory classes = new ClassImportResolverFactory();
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

    public void testExecuteCoercionTwice() {
        OptimizerFactory.setDefaultOptimizer("reflective");

        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("foo",
                new Foo());
        vars.put("$value",
                new Long(5));

        ExpressionCompiler compiler = new ExpressionCompiler("with (foo) { countTest = $value };");

        ParserContext ctx = new ParserContext();
        ctx.setSourceFile("test.mv");
        ctx.setDebugSymbols(true);

        CompiledExpression compiled = compiler.compile(ctx);

        executeExpression(compiled, null, vars);
        executeExpression(compiled, null, vars);
    }

    public void testExecuteCoercionTwice2() {
        OptimizerFactory.setDefaultOptimizer("ASM");

        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("foo",
                new Foo());
        vars.put("$value",
                new Long(5));

        ExpressionCompiler compiler = new ExpressionCompiler("with (foo) { countTest = $value };");

        ParserContext ctx = new ParserContext();
        ctx.setSourceFile("test.mv");
        ctx.setDebugSymbols(true);

        CompiledExpression compiled = compiler.compile(ctx);

        executeExpression(compiled,
                null,
                vars);
        executeExpression(compiled,
                null,
                vars);
    }

    public void testComments() {
        assertEquals(10,
                test("// This is a comment\n5 + 5"));
    }

    public void testComments2() {
        assertEquals(20,
                test("10 + 10; // This is a comment"));
    }

    public void testComments3() {
        assertEquals(30,
                test("/* This is a test of\r\n" + "MVEL's support for\r\n" + "multi-line comments\r\n" + "*/\r\n 15 + 15"));
    }

    public void testComments4() {
        assertEquals(((10 + 20) * 2) - 10,
                test("/** This is a fun test script **/\r\n" + "a = 10;\r\n" + "/**\r\n"
                        + "* Here is a useful variable\r\n" + "*/\r\n" + "b = 20; // set b to '20'\r\n"
                        + "return ((a + b) * 2) - 10;\r\n" + "// last comment\n"));
    }

    public void testComments5() {
        assertEquals("dog",
                test("foo./*Hey!*/name"));
    }

    public void testSubtractNoSpace1() {
        assertEquals(59,
                test("hour-1"));
    }

    public void testStrictTypingCompilation() {
        ExpressionCompiler compiler = new ExpressionCompiler("a.foo;\nb.foo;\n x = 5");
        ParserContext ctx = new ParserContext();
        ctx.setStrictTypeEnforcement(true);

        try {
            compiler.compile(ctx);
        }
        catch (CompileException e) {
            e.printStackTrace();
            assertEquals(2,
                    e.getErrors().size());
            return;
        }
        assertTrue(false);
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

    public void testStrictTypingCompilation4() throws NoSuchMethodException {
        ParserContext ctx = new ParserContext();

        ctx.addImport(Foo.class);
        ctx.setStrictTypeEnforcement(true);

        ExpressionCompiler compiler = new ExpressionCompiler("x_a = new Foo()");

        compiler.compile(ctx);

        assertEquals(Foo.class,
                ctx.getVariables().get("x_a"));
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
            fail("This should not compile");
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
            MVEL.compileExpression("x_a = new Foo( $ba ); x_a.equals($ba);", ctx);
            fail("This should not compile");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testDetermineRequiredInputsInConstructor() throws Exception {
        ParserContext ctx = new ParserContext();
        ctx.setStrictTypeEnforcement(false);
        ctx.setStrongTyping(false);
        ctx.addImport(Foo.class);

        ExpressionCompiler compiler = new ExpressionCompiler("new Foo( $bar,  $bar.age );");

        Serializable compiled = compiler.compile(ctx);

        Set<String> requiredInputs = compiler.getParserContextState().getInputs().keySet();
        assertEquals(1, requiredInputs.size());
        assertTrue(requiredInputs.contains("$bar"));

    }

    public void testProvidedExternalTypes() {
        ExpressionCompiler compiler = new ExpressionCompiler("foo.bar");
        ParserContext ctx = new ParserContext();
        ctx.setStrictTypeEnforcement(true);
        ctx.addInput("foo",
                Foo.class);

        compiler.compile(ctx);
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

    public void testAssignmentRegression() {
        ExpressionCompiler compiler = new ExpressionCompiler("total = total + $cheese.price");
        compiler.compile();
    }

    public void testTypeRegression() {
        ExpressionCompiler compiler = new ExpressionCompiler("total = 0");
        ParserContext ctx = new ParserContext();
        ctx.setStrictTypeEnforcement(true);
        compiler.compile(ctx);
        assertEquals(Integer.class,
                compiler.getParserContextState().getVarOrInputType("total"));
    }

    public void testTestIntToLong() {
        //System.out.println( int.class.isAssignableFrom( Integer.class ) );
        //Number n = new Integer ( 3 )

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

    public void _testBinaryOperatorWidening() {
        String s = "1f+a";

        ParserContext pc = new ParserContext();
        pc.addInput("a", Byte.class);

        ExpressionCompiler compiler = new ExpressionCompiler(s, pc);
        CompiledExpression expr = compiler.compile();

        BinaryOperation binNode = (BinaryOperation) expr.getFirstNode();
        ASTNode left = binNode.getLeft();
        assertEquals(Float.class, left.getEgressType());
        ASTNode right = binNode.getRight();
        assertEquals(Float.class, left.getEgressType());
        assertEquals(Byte.class, right.getEgressType());
        assertEquals(Float.class, binNode.getEgressType());
    }

    public void testMapPropertyCreateCondensed() {
        assertEquals("foo",
                test("map = new java.util.HashMap(); map['test'] = 'foo'; map['test'];"));
    }

    public void testClassLiteral() {
        assertEquals(String.class,
                test("java.lang.String"));
    }

    public void testDeepMethod() {
        assertEquals(false,
                test("foo.bar.testList.add(new String()); foo.bar.testList == empty"));
    }

    public void testArrayAccessorAssign() {
        assertEquals("foo",
                test("a = {'f00', 'bar'}; a[0] = 'foo'; a[0]"));
    }

    public void testListAccessorAssign() {
        assertEquals("bar",
                test("a = new java.util.ArrayList(); a.add('foo'); a.add('BAR'); a[1] = 'bar'; a[1]"));
    }

    public void testBracketInString() {
        test("System.out.println('1)your guess was:');");
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
        assertEquals(0,
                test("map = new java.util.HashMap(); map.put('doggie', new java.util.ArrayList());" +
                        " ((java.util.ArrayList) map['doggie']).size()"));
    }

    public void testTypeCast3() {
        Map map = new HashMap();
        map.put("foo",
                new Foo());

        ParserContext pCtx = new ParserContext();
        pCtx.setStrongTyping(true);
        pCtx.addInput("foo",
                Foo.class);

        Serializable s = MVEL.compileExpression("((org.mvel2.tests.core.res.Bar) foo.getBar()).name != null",
                pCtx);

        assertEquals(true,
                executeExpression(s,
                        map));

        assertEquals(1,
                pCtx.getInputs().size());
        assertEquals(true,
                pCtx.getInputs().containsKey("foo"));
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
                        "\n'bobba' : new User('Bobba', 'Feta') ]; [ users.get('darth'), users.get('bobba') ]");
        //    Serializable s = compiler.compile(ctx);
        List list = (List) executeExpression(compiler.compile(ctx),
                new HashMap());
        User user = (User) list.get(0);
        assertEquals("Darth",
                user.getFirstName());
        user = (User) list.get(1);
        assertEquals("Bobba",
                user.getFirstName());

        compiler =
                new ExpressionCompiler("users = [ 'darth'  : new User('Darth', 'Vadar')," +
                        "\n'bobba' : new User('Bobba', 'Feta') ]; [ users['darth'], users['bobba'] ]");
        list = (List) executeExpression(compiler.compile(ctx),
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
                        "new User('Bobba', 'Feta') ]; [ users.get( 0 ), users.get( 1 ) ]");
        List list = (List) executeExpression(compiler.compile(ctx),
                new HashMap());
        User user = (User) list.get(0);
        assertEquals("Darth",
                user.getFirstName());
        user = (User) list.get(1);
        assertEquals("Bobba",
                user.getFirstName());

        compiler = new ExpressionCompiler("users = [ new User('Darth', 'Vadar'), " +
                "new User('Bobba', 'Feta') ]; [ users[0], users[1] ]");
        list = (List) executeExpression(compiler.compile(ctx),
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

        ExpressionCompiler compiler = new ExpressionCompiler("HashMap");
        Serializable s = compiler.compile(ctx);

        assertEquals(HashMap.class,
                executeExpression(s));

        compiler = new ExpressionCompiler("map = new HashMap(); map.size()");
        s = compiler.compile(ctx);

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

        ExpressionCompiler compiler = new ExpressionCompiler("[ new User('Bobba', 'Feta') ]");
        List list = (List) executeExpression(compiler.compile(ctx));
        User user = (User) list.get(0);
        assertEquals("Bobba",
                user.getFirstName());
    }

    public void testDynamicImportsInMap() {
        ParserContext ctx = new ParserContext();
        ctx.addPackageImport("org.mvel2.tests.core.res");

        ExpressionCompiler compiler = new ExpressionCompiler("[ 'bobba' : new User('Bobba', 'Feta') ]");
        Map map = (Map) executeExpression(compiler.compile(ctx));
        User user = (User) map.get("bobba");
        assertEquals("Bobba",
                user.getFirstName());
    }

    public void testDynamicImportsOnNestedExpressions() {
        ParserContext ctx = new ParserContext();
        ctx.addPackageImport("org.mvel2.tests.core.res");

        ExpressionCompiler compiler = new ExpressionCompiler("new Cheesery(\"bobbo\", new Cheese(\"cheddar\", 15))");

        Cheesery p1 = new Cheesery("bobbo",
                new Cheese("cheddar",
                        15));
        Cheesery p2 = (Cheesery) executeExpression(compiler.compile(ctx),
                new DefaultLocalVariableResolverFactory());

        assertEquals(p1,
                p2);
    }

    public void testDynamicImportsWithNullConstructorParam() {
        ParserContext ctx = new ParserContext();
        ctx.addPackageImport("org.mvel2.tests.core.res");

        ExpressionCompiler compiler = new ExpressionCompiler("new Cheesery(\"bobbo\", null)");

        Cheesery p1 = new Cheesery("bobbo",
                null);
        Cheesery p2 = (Cheesery) executeExpression(compiler.compile(ctx),
                new DefaultLocalVariableResolverFactory());

        assertEquals(p1,
                p2);
    }

    public void testDynamicImportsWithIdentifierSameAsClassWithDiffCase() {
        ParserContext ctx = new ParserContext();
        ctx.addPackageImport("org.mvel2.tests.core.res");
        ctx.setStrictTypeEnforcement(false);

        ExpressionCompiler compiler = new ExpressionCompiler("bar.add(\"hello\")");
        compiler.compile(ctx);
    }

    public void testTypedAssignment() {
        assertEquals("foobar",
                test("java.util.Map map = new java.util.HashMap(); map.put('conan', 'foobar'); map['conan'];"));
    }

    public void testFQCNwithStaticInList() {
        assertEquals(Integer.MIN_VALUE,
                test("list = [java.lang.Integer.MIN_VALUE]; list[0]"));
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

    @SuppressWarnings({"UnnecessaryBoxing"})
    public void testToList() {
        String text = "misc.toList(foo.bar.name, 'hello', 42, ['key1' : 'value1'," +
                " c : [ foo.bar.age, 'car', 42 ]], [42, [c : 'value1']] )";

        List list = (List) test(text);

        assertSame("dog",
                list.get(0));
        assertEquals("hello",
                list.get(1));
        assertEquals(new Integer(42),
                list.get(2));
        Map map = (Map) list.get(3);
        assertEquals("value1",
                map.get("key1"));

        List nestedList = (List) map.get("cat");
        assertEquals(14,
                nestedList.get(0));
        assertEquals("car",
                nestedList.get(1));
        assertEquals(42,
                nestedList.get(2));

        nestedList = (List) list.get(4);
        assertEquals(42,
                nestedList.get(0));
        map = (Map) nestedList.get(1);
        assertEquals("value1",
                map.get("cat"));
    }

    @SuppressWarnings({"UnnecessaryBoxing"})
    public void testToListStrictMode() {
        String text = "misc.toList(foo.bar.name, 'hello', 42, ['key1' : 'value1'," +
                " c : [ foo.bar.age, 'car', 42 ]], [42, [c : 'value1']] )";

        ParserContext ctx = new ParserContext();
        ctx.addInput("misc",
                MiscTestClass.class);
        ctx.addInput("foo",
                Foo.class);
        ctx.addInput("c",
                String.class);

        ctx.setStrictTypeEnforcement(true);
        ExpressionCompiler compiler = new ExpressionCompiler(text);

        List list = (List) executeExpression(compiler.compile(ctx),
                createTestMap());

        assertSame("dog",
                list.get(0));
        assertEquals("hello",
                list.get(1));
        assertEquals(new Integer(42),
                list.get(2));
        Map map = (Map) list.get(3);
        assertEquals("value1",
                map.get("key1"));

        List nestedList = (List) map.get("cat");
        assertEquals(14,
                nestedList.get(0));
        assertEquals("car",
                nestedList.get(1));
        assertEquals(42,
                nestedList.get(2));

        nestedList = (List) list.get(4);
        assertEquals(42,
                nestedList.get(0));
        map = (Map) nestedList.get(1);
        assertEquals("value1",
                map.get("cat"));
    }

    public void testParsingStability1() {
        assertEquals(true,
                test("( order.number == 1 || order.number == ( 1+1) || order.number == $id )"));
    }

    public void testParsingStability2() {
        ExpressionCompiler compiler =
                new ExpressionCompiler("( dim.height == 1 || dim.height == ( 1+1) || dim.height == x )");

        Map<String, Object> imports = new HashMap<String, Object>();
        imports.put("java.awt.Dimension",
                Dimension.class);

        final ParserContext parserContext = new ParserContext(imports,
                null,
                "sourceFile");

        parserContext.setStrictTypeEnforcement(false);
        compiler.compile(parserContext);
    }

    public void testParsingStability3() {
        assertEquals(false,
                test("!( [\"X\", \"Y\"] contains \"Y\" )"));
    }

    public void testParsingStability4() {
        assertEquals(true,
                test("vv=\"Edson\"; !(vv ~= \"Mark\")"));
    }

    public void testConcatWithLineBreaks() {
        ExpressionCompiler parser = new ExpressionCompiler("\"foo\"+\n\"bar\"");

        ParserContext ctx = new ParserContext();
        ctx.setDebugSymbols(true);
        ctx.setSourceFile("source.mv");

        assertEquals("foobar",
                executeExpression(parser.compile(ctx)));
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

    /**
     * Community provided test cases
     */
    @SuppressWarnings({"unchecked"})
    public void testCalculateAge() {
        Calendar c1 = Calendar.getInstance();
        c1.set(1999,
                0,
                10); // 1999 jan 20
        Map objectMap = new HashMap(1);
        Map propertyMap = new HashMap(1);
        propertyMap.put("GEBDAT",
                c1.getTime());
        objectMap.put("EV_VI_ANT1",
                propertyMap);
        assertEquals("N",
                testCompiledSimple(
                        "new org.mvel2.tests.core.res.PDFFieldUtil().calculateAge(EV_VI_ANT1.GEBDAT) >= 25 ? 'Y' : 'N'",
                        null,
                        objectMap));
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

        ExpressionCompiler compiler = new ExpressionCompiler(expr);

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

        compiler.compile(context);
    }

    public void testStaticNested() {
        assertEquals(1,
                eval("org.mvel2.tests.core.AbstractTest$Message.GOODBYE",
                        new HashMap()));
    }

    public void testStaticNestedWithImport() {
        String expr = "Message.GOODBYE;\n";

        ExpressionCompiler compiler = new ExpressionCompiler(expr);

        ParserContext context = new ParserContext();
        context.setStrictTypeEnforcement(false);

        context.addImport("Message",
                Message.class);

        assertEquals(1,
                executeExpression(compiler.compile(context)));
    }

    public void testStaticNestedWithMethodCall() {
        String expr = "item = new Item( \"Some Item\"); $msg.addItem( item ); return $msg";

        ExpressionCompiler compiler = new ExpressionCompiler(expr);

        ParserContext context = new ParserContext();
        context.setStrictTypeEnforcement(false);

        context.addImport("Message",
                Message.class);
        context.addImport("Item",
                Item.class);
        //   Serializable compiledExpression = compiler.compile(context);

        Map vars = new HashMap();
        vars.put("$msg",
                new Message());
        Message msg = (Message) executeExpression(compiler.compile(context),
                vars);
        Item item = (Item) msg.getItems().get(0);
        assertEquals("Some Item",
                item.getName());
    }

    public void testsequentialAccessorsThenMethodCall() {
        String expr = "System.out.println(drools.workingMemory); " +
                "drools.workingMemory.ruleBase.removeRule(\"org.drools.examples\", \"some rule\"); ";

        ExpressionCompiler compiler = new ExpressionCompiler(expr);

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
        executeExpression(compiler.compile(context),
                vars);
    }

    /**
     * Provided by: Aadi Deshpande
     */
    public void testPropertyVerfierShoudldNotLoopIndefinately() {
        String expr = "\t\tmodel.latestHeadlines = $list;\n"
                + "model.latestHeadlines.add( 0, (model.latestHeadlines[2]) );";

        ExpressionCompiler compiler = new ExpressionCompiler(expr);
        compiler.setVerifying(true);

        ParserContext pCtx = new ParserContext();
        pCtx.addInput("$list",
                List.class);
        pCtx.addInput("model",
                Model.class);

        compiler.compile(pCtx);
    }

    public void testCompileWithNewInsideMethodCall() {
        String expr = "     p.name = \"goober\";\n" + "     System.out.println(p.name);\n"
                + "     drools.insert(new Address(\"Latona\"));\n";

        ExpressionCompiler compiler = new ExpressionCompiler(expr);

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

        compiler.compile(context);
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

    /**
     * Submitted by: Michael Neale
     */
    public void testInlineCollectionParser1() {
        assertEquals("q",
                ((Map) test("['Person.age' : [1, 2, 3, 4],'Person.rating' : 'q']")).get("Person.rating"));
        assertEquals("q",
                ((Map) test("['Person.age' : [1, 2, 3, 4], 'Person.rating' : 'q']")).get("Person.rating"));
    }

    public void testIndexer() {
        assertEquals("foobar",
                testCompiledSimple("import java.util.LinkedHashMap; LinkedHashMap map = new LinkedHashMap();"
                        + " map.put('a', 'foo'); map.put('b', 'bar'); s = ''; " +
                        "foreach (key : map.keySet()) { System.out.println(map[key]); s += map[key]; }; return s;",
                        createTestMap()));
    }

    public void testLateResolveOfClass() {
        ExpressionCompiler compiler = new ExpressionCompiler("System.out.println(new Foo());");
        ParserContext ctx = new ParserContext();
        ctx.addImport(Foo.class);

        compiler.removeParserContext();

        System.out.println(executeExpression(compiler.compile(ctx)));
    }

    public void testClassAliasing() {
        assertEquals("foobar",
                test("Foo = String; new Foo('foobar')"));
    }

    public void testRandomExpression1() {
        assertEquals("HelloWorld",
                test("if ((x15 = foo.bar) == foo.bar && x15 == foo.bar) { return 'HelloWorld'; } " +
                        "else { return 'GoodbyeWorld' } "));
    }

    public void testRandomExpression2() {
        assertEquals(11,
                test("counterX = 0; foreach (item:{1,2,3,4,5,6,7,8,9,10}) { counterX++; }; return counterX + 1;"));
    }

    public void testRandomExpression3() {
        assertEquals(0,
                test("counterX = 10; foreach (item:{1,1,1,1,1,1,1,1,1,1}) { counterX -= item; } return counterX;"));
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

    public static class TargetClass {
        private short _targetValue = 5;

        public short getTargetValue() {
            return _targetValue;
        }
    }

    public void testNestedMethodCall() {
        List elements = new ArrayList();
        elements.add(new TargetClass());
        Map variableMap = new HashMap();
        variableMap.put("elements",
                elements);
        eval("results = new java.util.ArrayList(); foreach (element : elements) { " +
                "if( {5} contains element.targetValue.intValue()) { results.add(element); } }; results",
                variableMap);
    }

    public void testBooleanEvaluation() {
        assertEquals(true,
                test("true||false||false"));
    }

    public void testBooleanEvaluation2() {
        assertEquals(true,
                test("equalityCheck(1,1)||fun||ackbar"));
    }

    /**
     * Submitted by: Dimitar Dimitrov
     */
    public void testFailing() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("os",
                "windows");
        assertTrue((Boolean) eval("os ~= 'windows|unix'",
                map));
    }

    public void testSuccess() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("os",
                "windows");
        assertTrue((Boolean) eval("'windows' ~= 'windows|unix'",
                map));
        assertFalse((Boolean) eval("time ~= 'windows|unix'",
                new java.util.Date()));
    }

    public void testStaticWithExplicitParam() {
        PojoStatic pojo = new PojoStatic("10");
        eval("org.mvel2.tests.core.res.AStatic.Process('10')",
                pojo,
                new HashMap());
    }

    public void testSimpleExpression() {
        PojoStatic pojo = new PojoStatic("10");
        eval("value!= null",
                pojo,
                new HashMap());
    }

    public void testStaticWithExpressionParam() {
        PojoStatic pojo = new PojoStatic("10");
        assertEquals("java.lang.String",
                eval("org.mvel2.tests.core.res.AStatic.Process(value.getClass().getName().toString())",
                        pojo));
    }

    public void testStringIndex() {
        assertEquals(true,
                test("a = 'foobar'; a[4] == 'a'"));
    }

    public void testArrayConstructionSupport1() {
        assertTrue(test("new String[5]") instanceof String[]);
    }

    public void testArrayConstructionSupport2() {
        assertTrue((Boolean) test("xStr = new String[5]; xStr.size() == 5"));
    }

    public void testArrayConstructionSupport3() {
        assertEquals("foo",
                test("xStr = new String[5][5]; xStr[4][0] = 'foo'; xStr[4][0]"));
    }

    public void testArrayConstructionSupport4() {
        assertEquals(10,
                test("xStr = new String[5][10]; xStr[4][0] = 'foo'; xStr[4].length"));
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
        map.put("foo",
                foo);

        String expression = "foo.?bar.name == null";
        Serializable compiled = compileExpression(expression);

        OptimizerFactory.setDefaultOptimizer("reflective");
        assertEquals(false,
                executeExpression(compiled,
                        map));
        foo.setBar(null);
        assertEquals(true,
                executeExpression(compiled,
                        map)); // execute a second time (to search for optimizer problems)

        OptimizerFactory.setDefaultOptimizer("ASM");
        compiled = compileExpression(expression);
        foo.setBar(new Bar());
        assertEquals(false,
                executeExpression(compiled,
                        map));
        foo.setBar(null);
        assertEquals(true,
                executeExpression(compiled,
                        map)); // execute a second time (to search for optimizer problems)

        assertEquals(true,
                eval(expression,
                        map));
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

    public void testNestedWithInList() {
        Recipient recipient1 = new Recipient();
        recipient1.setName("userName1");
        recipient1.setEmail("user1@domain.com");

        Recipient recipient2 = new Recipient();
        recipient2.setName("userName2");
        recipient2.setEmail("user2@domain.com");

        List list = new ArrayList();
        list.add(recipient1);
        list.add(recipient2);

        String text = "array = [" + "(with ( new Recipient() ) {name = 'userName1', email = 'user1@domain.com' }),"
                + "(with ( new Recipient() ) {name = 'userName2', email = 'user2@domain.com' })];\n";

        ParserContext context = new ParserContext();
        context.addImport(Recipient.class);

        ExpressionCompiler compiler = new ExpressionCompiler(text);
        Serializable execution = compiler.compile(context);
        List result = (List) executeExpression(execution,
                new HashMap());
        assertEquals(list,
                result);
    }

    public void testNestedWithInComplexGraph3() {
        Recipients recipients = new Recipients();

        Recipient recipient1 = new Recipient();
        recipient1.setName("user1");
        recipient1.setEmail("user1@domain.com");
        recipients.addRecipient(recipient1);

        Recipient recipient2 = new Recipient();
        recipient2.setName("user2");
        recipient2.setEmail("user2@domain.com");
        recipients.addRecipient(recipient2);

        EmailMessage msg = new EmailMessage();
        msg.setRecipients(recipients);
        msg.setFrom("from@domain.com");

        String text = "";
        text += "new EmailMessage().{ ";
        text += "     recipients = new Recipients().{ ";
        text += "         recipients = [ new Recipient().{ name = 'user1', email = 'user1@domain.com' }, ";
        text += "                        new Recipient().{ name = 'user2', email = 'user2@domain.com' } ] ";
        text += "     }, ";
        text += "     from = 'from@domain.com' }";
        ParserContext context;
        context = new ParserContext();
        context.addImport(Recipient.class);
        context.addImport(Recipients.class);
        context.addImport(EmailMessage.class);

        OptimizerFactory.setDefaultOptimizer("ASM");

        ExpressionCompiler compiler = new ExpressionCompiler(text);
        Serializable execution = compiler.compile(context);

        assertEquals(msg,
                executeExpression(execution));
        assertEquals(msg,
                executeExpression(execution));
        assertEquals(msg,
                executeExpression(execution));

        OptimizerFactory.setDefaultOptimizer("reflective");

        context = new ParserContext(context.getParserConfiguration());
        compiler = new ExpressionCompiler(text);
        execution = compiler.compile(context);

        assertEquals(msg,
                executeExpression(execution));
        assertEquals(msg,
                executeExpression(execution));
        assertEquals(msg,
                executeExpression(execution));
    }

    public static class Recipient {
        private String name;
        private String email;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((email == null) ? 0 : email.hashCode());
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            final Recipient other = (Recipient) obj;
            if (email == null) {
                if (other.email != null) return false;
            }
            else if (!email.equals(other.email)) return false;
            if (name == null) {
                if (other.name != null) return false;
            }
            else if (!name.equals(other.name)) return false;
            return true;
        }
    }

    public static class Recipients {
        private List<Recipient> list = Collections.EMPTY_LIST;

        public void setRecipients(List<Recipient> recipients) {
            this.list = recipients;
        }

        public boolean addRecipient(Recipient recipient) {
            if (list == Collections.EMPTY_LIST) {
                this.list = new ArrayList<Recipient>();
            }

            if (!this.list.contains(recipient)) {
                this.list.add(recipient);
                return true;
            }
            return false;
        }

        public boolean removeRecipient(Recipient recipient) {
            return this.list.remove(recipient);
        }

        public List<Recipient> getRecipients() {
            return this.list;
        }

        public Recipient[] toArray() {
            return list.toArray(new Recipient[list.size()]);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((list == null) ? 0 : list.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            final Recipients other = (Recipients) obj;
            if (list == null) {
                if (other.list != null) return false;
            }

            return list.equals(other.list);
        }
    }

    public static class EmailMessage {
        private Recipients recipients;
        private String from;

        public EmailMessage() {
        }

        public Recipients getRecipients() {
            return recipients;
        }

        public void setRecipients(Recipients recipients) {
            this.recipients = recipients;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((from == null) ? 0 : from.hashCode());
            result = prime * result + ((recipients == null) ? 0 : recipients.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {

            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            final EmailMessage other = (EmailMessage) obj;
            if (from == null) {
                if (other.from != null) return false;
            }
            else if (!from.equals(other.from)) return false;
            if (recipients == null) {
                if (other.recipients != null) return false;
            }
            else if (!recipients.equals(other.recipients)) return false;
            return true;
        }
    }

    public class POJO {
        private Set<Date> dates = new HashSet<Date>();

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
    }

    public void testSubEvaluation() {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("EV_BER_BER_NR",
                "12345");
        map.put("EV_BER_BER_PRIV",
                Boolean.FALSE);

        assertEquals("12345",
                testCompiledSimple("EV_BER_BER_NR + ((EV_BER_BER_PRIV != empty && EV_BER_BER_PRIV == true) ? \"/PRIVAT\" : '')",
                        null,
                        map));

        map.put("EV_BER_BER_PRIV",
                Boolean.TRUE);
        assertEquals("12345/PRIVAT",
                testCompiledSimple("EV_BER_BER_NR + ((EV_BER_BER_PRIV != empty && EV_BER_BER_PRIV == true) ? \"/PRIVAT\" : '')",
                        null,
                        map));
    }

    public void testNestedMethod1() {
        Vector vectorA = new Vector();
        Vector vectorB = new Vector();

        vectorA.add("Foo");

        Map map = new HashMap();
        map.put("vecA",
                vectorA);
        map.put("vecB",
                vectorB);

        testCompiledSimple("vecB.add(vecA.remove(0)); vecA.add('Foo');",
                null,
                map);

        assertEquals("Foo",
                vectorB.get(0));
    }

    public void testNegativeArraySizeBug() throws Exception {
        String expressionString1 = "results = new java.util.ArrayList(); foreach (element : elements) { " +
                "if( ( {30, 214, 158, 31, 95, 223, 213, 86, 159, 34, 32, 96, 224, 160, 85, 201, 29, 157, 100, 146," +
                " 82, 203, 194, 145, 140, 81, 27, 166, 212, 38, 28, 94, 168, 23, 87, 150, 35, 149, 193, 33, 132," +
                " 206, 93, 196, 24, 88, 195, 36, 26, 154, 167, 108, 204, 74, 46, 25, 153, 202, 79, 207, 143, 43, " +
                "16, 80, 198, 208, 144, 41, 97, 142, 83, 18, 162, 103, 155, 98, 44, 17, 205, 77, 156, 141, 165," +
                " 102, 84, 37, 101, 222, 40, 104, 99, 177, 182, 22, 180, 21, 137, 221, 179, 78, 42, 178, 19, 183," +
                " 139, 218, 219, 39, 220, 20, 184, 217, 138, 62, 190, 171, 123, 113, 59, 118, 225, 124, 169, 60, " +
                "117, 1} contains element.attribute ) ) { results.add(element); } }; results";

        String expressionString2 = "results = new java.util.ArrayList(); foreach (element : elements) { " +
                "if( ( {30, 214, 158, 31, 95, 223, 213, 86, 159, 34, 32, 96, 224, 160, 85, 201, 29, 157, 100, 146," +
                " 82, 203, 194, 145, 140, 81, 27, 166, 212, 38, 28, 94, 168, 23, 87, 150, 35, 149, 193, 33, 132, " +
                "206, 93, 196, 24, 88, 195, 36, 26, 154, 167, 108, 204, 74, 46, 25, 153, 202, 79, 207, 143, 43," +
                " 16, 80, 198, 208, 144, 41, 97, 142, 83, 18, 162, 103, 155, 98, 44, 17, 205, 77, 156, 141, 165," +
                " 102, 84, 37, 101, 222, 40, 104, 99, 177, 182, 22, 180, 21, 137, 221, 179, 78, 42, 178, 19, 183," +
                " 139, 218, 219, 39, 220, 20, 184, 217, 138, 62, 190, 171, 123, 113, 59, 118, 225, 124, 169, 60," +
                " 117, 1, 61, 189, 122, 68, 58, 119, 63, 226, 3, 172}" +
                " contains element.attribute ) ) { results.add(element); } }; results";

        List<Target> targets = new ArrayList<Target>();
        targets.add(new Target(1));
        targets.add(new Target(999));

        Map vars = new HashMap();
        vars.put("elements",
                targets);

        assertEquals(1,
                ((List) testCompiledSimple(expressionString1,
                        null,
                        vars)).size());
        assertEquals(1,
                ((List) testCompiledSimple(expressionString2,
                        null,
                        vars)).size());
    }

    public static final class Target {
        private int _attribute;

        public Target(int attribute_) {
            _attribute = attribute_;
        }

        public int getAttribute() {
            return _attribute;
        }
    }

    public void testDynamicImports2() {
        assertEquals(BufferedReader.class,
                test("import java.io.*; BufferedReader"));
    }

    public void testStringWithTernaryIf() {
        test("System.out.print(\"Hello : \" + (foo != null ? \"FOO!\" : \"NO FOO\") + \". Bye.\");");
    }

    public void testCompactIfElse() {
        assertEquals("foo",
                test("if (false) 'bar'; else 'foo';"));
    }

    public void testAndOpLiteral() {
        assertEquals(true,
                test("true && true"));
    }

    public void testAnonymousFunctionDecl() {
        assertEquals(3,
                test("anonFunc = function (a,b) { return a + b; }; anonFunc(1,2)"));
    }

    public void testFunctionSemantics() {
        assertEquals(true,
                test("function fooFunction(a) { return a; }; x__0 = ''; 'boob' == fooFunction(x__0 = 'boob') " +
                        "&& x__0 == 'boob';"));
    }

    public void testUseOfVarKeyword() {
        assertEquals("FOO_BAR",
                test("var barfoo = 'FOO_BAR'; return barfoo;"));
    }

    public void testAssignment5() {
        assertEquals(15,
                test("x = (10) + (5); x"));
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

    public void testEgressType() {
        ExpressionCompiler compiler = new ExpressionCompiler("( $cheese )");
        ParserContext context = new ParserContext();
        context.addInput("$cheese",
                Cheese.class);

        assertEquals(Cheese.class,
                compiler.compile(context).getKnownEgressType());
    }

    public void testDuplicateVariableDeclaration() {
        ExpressionCompiler compiler = new ExpressionCompiler("String x = \"abc\"; Integer x = new Integer( 10 );");
        ParserContext context = new ParserContext();

        try {
            compiler.compile(context);
            fail("Compilation must fail with duplicate variable declaration exception.");
        }
        catch (CompileException ce) {
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

    public void testRegExMatch() {
        assertEquals(true,
                MVEL.eval("$test = 'foo'; $ex = 'f.*'; $test ~= $ex",
                        new HashMap()));
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

        ExpressionCompiler compiler = new ExpressionCompiler("fooString[0].toUpperCase()");
        compiler.compile(ctx);
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

    public void testStringToArrayCast() {
        Object o = test("(char[]) 'abcd'");

        assertTrue(o instanceof char[]);
    }

    public void testStringToArrayCast2() {
        assertTrue((Boolean) test("_xyxy = (char[]) 'abcd'; _xyxy[0] == 'a'"));
    }

    public void testStaticallyTypedArrayVar() {
        assertTrue((Boolean) test("char[] _c___ = new char[10]; _c___ instanceof char[]"));
    }

    public void testParserErrorHandling() {
        final ParserContext ctx = new ParserContext();
        ExpressionCompiler compiler = new ExpressionCompiler("a[");
        try {
            compiler.compile(ctx);
        }
        catch (Exception e) {
            return;
        }
        assertTrue(false);
    }

    public void testJIRA99_Interpreted() {
        Map map = new HashMap();
        map.put("x",
                20);
        map.put("y",
                10);
        map.put("z",
                5);

        assertEquals(20 - 10 - 5,
                MVEL.eval("x - y - z",
                        map));
    }

    public void testJIRA99_Compiled() {
        Map map = new HashMap();
        map.put("x",
                20);
        map.put("y",
                10);
        map.put("z",
                5);

        assertEquals(20 - 10 - 5,
                testCompiledSimple("x - y - z",
                        map));
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

    public void testParameterizedTypeInStrictMode() {
        ParserContext ctx = new ParserContext();
        ctx.setStrongTyping(true);
        ctx.addInput("foo",
                HashMap.class,
                new Class[]{String.class, String.class});
        ExpressionCompiler compiler = new ExpressionCompiler("foo.get('bar').toUpperCase()");
        compiler.compile(ctx);
    }

    public void testParameterizedTypeInStrictMode2() {
        ParserContext ctx = new ParserContext();
        ctx.setStrongTyping(true);
        ctx.addInput("ctx",
                Object.class);

        ExpressionCompiler compiler =
                new ExpressionCompiler("org.mvel2.DataConversion.convert(ctx, String).toUpperCase()");
        assertEquals(String.class,
                compiler.compile(ctx).getKnownEgressType());
    }

    public void testParameterizedTypeInStrictMode3() {
        ParserContext ctx = new ParserContext();
        ctx.setStrongTyping(true);
        ctx.addInput("base",
                Base.class);

        ExpressionCompiler compiler = new ExpressionCompiler("base.list");

        assertTrue(compiler.compile(ctx).getParserContext().getLastTypeParameters()[0].equals(String.class));
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

    public void testOKQuoteComment() throws Exception {
        // ' in comments outside of blocks seem OK
        compileExpression("// ' this is OK!");
        compileExpression("// ' this is OK!\n");
        compileExpression("// ' this is OK!\nif(1==1) {};");
    }

    public void testOKDblQuoteComment() throws Exception {
        // " in comments outside of blocks seem OK
        compileExpression("// \" this is OK!");
        compileExpression("// \" this is OK!\n");
        compileExpression("// \" this is OK!\nif(1==1) {};");
    }

    public void testIfComment() throws Exception {
        // No quote?  OK!
        compileExpression("if(1 == 1) {\n" + "  // Quote & Double-quote seem to break this expression\n" + "}");
    }

    public void testIfQuoteCommentBug() throws Exception {
        // Comments in an if seem to fail if they contain a '
        compileExpression("if(1 == 1) {\n" + "  // ' seems to break this expression\n" + "}");
    }

    public void testIfDblQuoteCommentBug() throws Exception {
        // Comments in a foreach seem to fail if they contain a '
        compileExpression("if(1 == 1) {\n" + "  // ' seems to break this expression\n" + "}");
    }

    public void testForEachQuoteCommentBug() throws Exception {
        // Comments in a foreach seem to fail if they contain a '
        compileExpression("foreach ( item : 10 ) {\n" + "  // The ' character causes issues\n" + "}");
    }

    public void testForEachDblQuoteCommentBug() throws Exception {
        // Comments in a foreach seem to fail if they contain a '
        compileExpression("foreach ( item : 10 ) {\n" + "  // The \" character causes issues\n" + "}");
    }

    public void testForEachCommentOK() throws Exception {
        // No quote?  OK!
        compileExpression("foreach ( item : 10 ) {\n" + "  // The quote & double quote characters cause issues\n" + "}");
    }

    public void testElseIfCommentBugPreCompiled() throws Exception {
        // Comments can't appear before else if() - compilation works, but evaluation fails
        executeExpression(compileExpression("// This is never true\n" + "if (1==0) {\n"
                + "  // Never reached\n" + "}\n" + "// This is always true...\n" + "else if (1==1) {"
                + "  System.out.println('Got here!');" + "}\n"));
    }

    public void testElseIfCommentBugEvaluated() throws Exception {
        // Comments can't appear before else if()
        MVEL.eval("// This is never true\n" + "if (1==0) {\n" + "  // Never reached\n" + "}\n"
                + "// This is always true...\n" + "else if (1==1) {" + "  System.out.println('Got here!');" + "}\n");
    }

    public void testRegExpOK() throws Exception {
        // This works OK intepreted
        assertEquals(Boolean.TRUE,
                MVEL.eval("'Hello'.toUpperCase() ~= '[A-Z]{0,5}'"));
        assertEquals(Boolean.TRUE,
                MVEL.eval("1 == 0 || ('Hello'.toUpperCase() ~= '[A-Z]{0,5}')"));
        // This works OK if toUpperCase() is avoided in pre-compiled
        assertEquals(Boolean.TRUE,
                executeExpression(compileExpression("'Hello' ~= '[a-zA-Z]{0,5}'")));
    }

    public void testRegExpPreCompiledBug() throws Exception {
        // If toUpperCase() is used in the expression then this fails; returns null not
        // a boolean.
        Object ser = compileExpression("'Hello'.toUpperCase() ~= '[a-zA-Z]{0,5}'");
        assertEquals(Boolean.TRUE,
                executeExpression(ser));
    }

    public void testRegExpOrBug() throws Exception {
        // This fails during execution due to returning null, I think...
        assertEquals(Boolean.TRUE,
                executeExpression(compileExpression("1 == 0 || ('Hello'.toUpperCase() ~= '[A-Z]{0,5}')")));
    }

    public void testRegExpAndBug() throws Exception {
        // This also fails due to returning null, I think...
        //  Object ser = MVEL.compileExpression("1 == 1 && ('Hello'.toUpperCase() ~= '[A-Z]{0,5}')");
        assertEquals(Boolean.TRUE,
                executeExpression(compileExpression("1 == 1 && ('Hello'.toUpperCase() ~= '[A-Z]{0,5}')")));
    }

    public void testLiteralUnionWithComparison() {
        assertEquals(Boolean.TRUE,
                executeExpression(compileExpression("1 == 1 && ('Hello'.toUpperCase() ~= '[A-Z]{0,5}')")));
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

        final Type[] typeParameters = expr.getParserContext().getLastTypeParameters();
        assertTrue(typeParameters != null);
        assertTrue(String.class.equals(typeParameters[0]));
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

        final Type[] typeParameters = expr.getParserContext().getLastTypeParameters();

        assertTrue(null != typeParameters);
        assertTrue(String.class.equals(typeParameters[0]));

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
            CompiledExpression expr = new ExpressionCompiler("foo.bar = 0").compile(ctx);
        }
        catch (CompileException e) {
            // should fail.

            e.printStackTrace();
            return;
        }

        assertTrue(false);
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

    public static class MVELDateCoercion
            implements
            ConversionHandler {
        public boolean canConvertFrom(Class cls) {
            if (cls == String.class || cls.isAssignableFrom(Date.class)) {
                return true;
            }
            else {
                return false;
            }
        }

        public Object convertFrom(Object o) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");
                if (o instanceof String) {
                    return sdf.parse((String) o);
                }
                else {
                    return o;
                }
            }
            catch (Exception e) {
                throw new RuntimeException("Exception was thrown",
                        e);
            }
        }
    }

    private static final KnowledgeHelperFixer fixer = new KnowledgeHelperFixer();

    public void testSingleLineCommentSlash() {
        String result = fixer.fix("        //System.out.println( \"help\" );\r\n      " +
                "  System.out.println( \"help\" );  \r\n     list.add( $person );");
        assertEquals("        //System.out.println( \"help\" );\r\n        System.out.println( \"help\" );  \r\n   " +
                "  list.add( $person );",
                result);
    }

    public void testSingleLineCommentHash() {
        String result = fixer.fix("        #System.out.println( \"help\" );\r\n    " +
                "    System.out.println( \"help\" );  \r\n     list.add( $person );");
        assertEquals("        #System.out.println( \"help\" );\r\n        System.out.println( \"help\" );  \r\n    " +
                " list.add( $person );",
                result);
    }

    public void testMultiLineComment() {
        String result = fixer.fix("        /*System.out.println( \"help\" );\r\n*/    " +
                "   System.out.println( \"help\" );  \r\n     list.add( $person );");
        assertEquals("        /*System.out.println( \"help\" );\r\n*/       System.out.println( \"help\" );  \r\n    " +
                " list.add( $person );",
                result);
    }

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

    public void testIncrementInBooleanStatement() {
        assertEquals(true,
                test("hour++ < 61 && hour == 61"));
    }

    public void testIncrementInBooleanStatement2() {
        assertEquals(true,
                test("++hour == 61"));
    }

    public void testDeepNestedLoopsInFunction() {
        assertEquals(10,
                test("def increment(i) { i + 1 }; def ff(i) { x = 0; while (i < 1) { " + "x++; " +
                        "while (i < 10) { i = increment(i); } }; if (x == 1) return i; else -1; }; i = 0; ff(i);"));
    }

    public void testArrayDefinitionWithInitializer() {
        String[] compareTo = new String[]{"foo", "bar"};
        String[] results = (String[]) test("new String[] { 'foo', 'bar' }");

        for (int i = 0; i < compareTo.length; i++) {
            if (!compareTo[i].equals(results[i])) throw new AssertionError("arrays do not match.");
        }
    }

    public void testStaticallyTypedItemInForEach() {
        assertEquals("1234",
                test("StringBuffer sbuf = new StringBuffer(); foreach (int i : new int[] { 1,2,3,4 })" +
                        " { sbuf.append(i); }; sbuf.toString()"));
    }

    public void testArrayDefinitionWithCoercion() {
        Double[] d = (Double[]) test("new double[] { 1,2,3,4 }");
        assertEquals(2d,
                d[1]);
    }

    public void testArrayDefinitionWithCoercion2() {
        Float[] d = (Float[]) test("new float[] { 1,2,3,4 }");
        assertEquals(2f,
                d[1]);
    }

    public void testStaticallyTypedLong() {
        assertEquals(10l,
                test("10l"));
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

    public void testHexCharacter() {
        assertEquals(0x0A,
                MVEL.eval("0x0A"));
    }

    public void testOctalEscapes() {
        assertEquals("\344",
                MVEL.eval("'\\344'"));
    }

    public void testOctalEscapes2() {
        assertEquals("\7",
                MVEL.eval("'\\7'"));
    }

    public void testOctalEscapes3() {
        assertEquals("\777",
                MVEL.eval("'\\777'"));
    }

    public void testUniHex1() {
        assertEquals("\uFFFF::",
                MVEL.eval("'\\uFFFF::'"));
    }

    public void testNumLiterals() {
        assertEquals(1e1f,
                MVEL.eval("1e1f"));
    }

    public void testNumLiterals2() {
        assertEquals(2.f,
                MVEL.eval("2.f"));
    }

    public void testNumLiterals3() {
        assertEquals(.3f,
                MVEL.eval(".3f"));
    }

    public void testNumLiterals4() {
        assertEquals(3.14f,
                MVEL.eval("3.14f"));
    }

    public void testNumLiterals5() {
        Object o = MVEL.eval("1e1");

        assertEquals(1e1,
                MVEL.eval("1e1"));
    }

    public void testNumLiterals6() {
        assertEquals(2.,
                MVEL.eval("2."));
    }

    public void testNumLiterals7() {
        assertEquals(.3,
                MVEL.eval(".3"));
    }

    public void testNumLiterals8() {
        assertEquals(1e-9d,
                MVEL.eval("1e-9d"));
    }

    public void testNumLiterals9() {
        assertEquals(0x400921FB54442D18L,
                MVEL.eval("0x400921FB54442D18L"));
    }

    public void testArrayCreation2() {
        String[][] s = (String[][]) test("new String[][] {{\"2008-04-01\", \"2008-05-10\"}," +
                " {\"2007-03-01\", \"2007-02-12\"}}");
        assertEquals("2007-03-01",
                s[1][0]);
    }

    public void testArrayCreation3() {
        OptimizerFactory.setDefaultOptimizer("ASM");

        Serializable ce = compileExpression("new String[][] {{\"2008-04-01\", \"2008-05-10\"}," +
                " {\"2007-03-01\", \"2007-02-12\"}}");

        String[][] s = (String[][]) executeExpression(ce);

        assertEquals("2007-03-01",
                s[1][0]);
    }

    public void testArrayCreation4() {
        String[][] s = (String[][]) test("new String[][]{{\"2008-04-01\", \"2008-05-10\"}," +
                " {\"2007-03-01\", \"2007-02-12\"}}");
        assertEquals("2007-03-01",
                s[1][0]);
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

    public void testArrayCoercion1() {
        ParserContext ctx = new ParserContext();
        ctx.setStrongTyping(true);
        ctx.addInput("bar",
                Bar.class);

        Serializable s = compileSetExpression("bar.intarray[0]",
                ctx);

        Foo foo = new Foo();

        executeSetExpression(s,
                foo,
                "12");

        assertEquals(12,
                foo.getBar().getIntarray()[0].intValue());

        foo = new Foo();

        executeSetExpression(s,
                foo,
                "13");

        assertEquals(13,
                foo.getBar().getIntarray()[0].intValue());

        OptimizerFactory.setDefaultOptimizer("ASM");

        ctx = new ParserContext();
        ctx.setStrongTyping(true);
        ctx.addInput("bar",
                Bar.class);

        s = compileSetExpression("bar.intarray[0]",
                ctx);

        foo = new Foo();

        executeSetExpression(s,
                foo,
                "12");

        assertEquals(12,
                foo.getBar().getIntarray()[0].intValue());

        executeSetExpression(s,
                foo,
                "13");

        assertEquals(13,
                foo.getBar().getIntarray()[0].intValue());
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

    public void testJIRA115() {
        String exp = "results = new java.util.ArrayList(); foreach (element : elements) { " +
                "if( {1,32769,32767} contains element ) { results.add(element);  } }; results";
        Map map = new HashMap();
        map.put("elements",
                new int[]{1, 32769, 32767});
        ArrayList result = (ArrayList) MVEL.eval(exp,
                map);

        assertEquals(3,
                result.size());
    }

    public void testStaticTyping2() {
        String exp = "int x = 5; int y = 2; new int[] { x, y }";
        Integer[] res = (Integer[]) MVEL.eval(exp,
                new HashMap());

        assertEquals(5,
                res[0].intValue());
        assertEquals(2,
                res[1].intValue());
    }

    public void testFunctions5() {
        String exp = "def foo(a,b) { a + b }; foo(1.5,5.25)";
        System.out.println(MVEL.eval(exp,
                new HashMap()));
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

    public void testNewUsingWith() {
        ParserContext ctx = new ParserContext();
        ctx.setStrongTyping(true);
        ctx.addImport(Foo.class);
        ctx.addImport(Bar.class);

        Serializable s = compileExpression("[ 'foo' : (with ( new Foo() )" +
                " { bar = with ( new Bar() ) { name = 'ziggy' } }) ]",
                ctx);

        OptimizerFactory.setDefaultOptimizer("reflective");
        assertEquals("ziggy",
                (((Foo) ((Map) executeExpression(s)).get("foo")).getBar().getName()));
    }

    private static Map<String, Boolean> JIRA124_CTX = Collections.singletonMap("testValue",
            true);

    public void testJIRA124() throws Exception {
        assertEquals("A",
                testTernary(1,
                        "testValue == true ? 'A' :  'B' + 'C'"));
        assertEquals("AB",
                testTernary(2,
                        "testValue ? 'A' +  'B' : 'C'"));
        assertEquals("A",
                testTernary(3,
                        "(testValue ? 'A' :  'B' + 'C')"));
        assertEquals("AB",
                testTernary(4,
                        "(testValue ? 'A' +  'B' : 'C')"));
        assertEquals("A",
                testTernary(5,
                        "(testValue ? 'A' :  ('B' + 'C'))"));
        assertEquals("AB",
                testTernary(6,
                        "(testValue ? ('A' + 'B') : 'C')"));

        JIRA124_CTX = Collections.singletonMap("testValue",
                false);

        assertEquals("BC",
                testTernary(1,
                        "testValue ? 'A' :  'B' + 'C'"));
        assertEquals("C",
                testTernary(2,
                        "testValue ? 'A' +  'B' : 'C'"));
        assertEquals("BC",
                testTernary(3,
                        "(testValue ? 'A' :  'B' + 'C')"));
        assertEquals("C",
                testTernary(4,
                        "(testValue ? 'A' +  'B' : 'C')"));
        assertEquals("BC",
                testTernary(5,
                        "(testValue ? 'A' :  ('B' + 'C'))"));
        assertEquals("C",
                testTernary(6,
                        "(testValue ? ('A' + 'B') : 'C')"));
    }

    private static Object testTernary(int i,
                                      String expression) throws Exception {
        Object val;
        Object val2;
        try {
            val = executeExpression(compileExpression(expression),
                    JIRA124_CTX);
        }
        catch (Exception e) {
            System.out.println("FailedCompiled[" + i + "]:" + expression);
            throw e;
        }

        try {
            val2 = MVEL.eval(expression,
                    JIRA124_CTX);
        }
        catch (Exception e) {
            System.out.println("FailedEval[" + i + "]:" + expression);
            throw e;
        }

        if (((val == null || val2 == null) && val != val2) || (val != null && !val.equals(val2))) {
            throw new AssertionError("results do not match (" + String.valueOf(val)
                    + " != " + String.valueOf(val2) + ")");
        }

        return val;
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
        final Serializable fooExpr = compileSetExpression("collectionTest");
        executeSetExpression(fooExpr,
                foo,
                col);
        assertEquals(col,
                foo.getCollectionTest());
    }

    public class Fruit {
        public class Apple {

        }
    }

    public void testInnerClassReference() {
        assertEquals(Fruit.Apple.class,
                test("import " + CoreConfidenceTests.class.getName() + "; CoreConfidenceTests.Fruit.Apple"));
    }

    public void testEdson() {
        assertEquals("foo",
                test("list = new java.util.ArrayList(); list.add(new String('foo')); list[0]"));
    }

    public void testEnumSupport() {
        MyInterface myInterface = new MyClass();
        myInterface.setType(MyInterface.MY_ENUM.TWO,
                true);
        boolean isType = MVEL.eval("isType(org.mvel2.tests.core.res.MyInterface$MY_ENUM.ONE)",
                myInterface,
                Boolean.class);
        System.out.println(isType);

    }

    public void testOperatorPrecedenceOrder() {
        Serializable compiled =
                compileExpression("bean1.successful && bean2.failed || bean1.failed && bean2.successful");
        Map context = new HashMap();

        BeanB bean1 = new BeanB(true);
        BeanB bean2 = new BeanB(false);

        context.put("bean1",
                bean1);
        context.put("bean2",
                bean2);

        System.out.println("interpreted: "
                + MVEL.eval("bean1.successful && bean2.failed || bean1.failed && bean2.successful",
                context));

        assertEquals(bean1.isSuccessful() && bean2.isFailed() || bean1.isFailed() && bean2.isSuccessful(),
                (boolean) executeExpression(compiled,
                        context,
                        Boolean.class));
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
        ParserContext ctx = new ParserContext();
        ctx.addImport("ReflectionUtil",
                ReflectionUtil.class);
        Serializable s = compileExpression("ReflectionUtil.getGetter('foo')",
                ctx);
        assertEquals(ReflectionUtil.getGetter("foo"),
                executeExpression(s));
    }

    public void testJIRA140() {
        ParserContext ctx = new ParserContext();
        Serializable s = compileExpression("import org.mvel2.tests.core.res.*;"
                + "cols = new Column[] { new Column('name', 20), new Column('age', 2) };"
                + "grid = new Grid(new Model(cols));",
                ctx);

        Grid g = (Grid) executeExpression(s,
                new HashMap());

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
        ParserContext ctx = new ParserContext();
        ctx.setStrictTypeEnforcement(true);
        ctx.addInput("base",
                Base.class);

        Serializable s = compileExpression("base.fooMap['foo'].setName('coffee')",
                ctx);

        Map vars = new HashMap();
        vars.put("base",
                new Base());

        executeExpression(s,
                vars);

        assertEquals("coffee",
                ((Base) vars.get("base")).fooMap.get("foo").getName());
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
        OptimizerFactory.setDefaultOptimizer("ASM");

        ParserContext ctx = new ParserContext();
        ctx.setStrongTyping(true);
        ctx.addImport(HashMap.class);
        ctx.addImport(ArrayList.class);
        ctx.addInput("list",
                List.class);

        String expression = "m = new HashMap();\n" + "l = new ArrayList();\n" + "l.add(\"first\");\n" +
                "m.put(\"content\", l);\n" + "list.add(((ArrayList)m[\"content\"])[0]);";

        Serializable s = compileExpression(expression,
                ctx);

        Map vars = new HashMap();
        List list = new ArrayList();
        vars.put("list",
                list);

        Boolean result = (Boolean) executeExpression(s,
                vars);

        assertNotNull(result);
        assertTrue(result);
        assertEquals(1,
                list.size());
        assertEquals("first",
                list.get(0));
    }

    public void testMapsAndLists2() {
        OptimizerFactory.setDefaultOptimizer("reflective");

        ParserContext ctx = new ParserContext();
        ctx.setStrongTyping(true);
        ctx.addImport(HashMap.class);
        ctx.addImport(ArrayList.class);
        ctx.addInput("list",
                List.class);

        String expression = "m = new HashMap();\n" + "l = new ArrayList();\n" + "l.add(\"first\");\n" +
                "m.put(\"content\", l);\n" + "list.add(((ArrayList)m[\"content\"])[0]);";

        Serializable s = compileExpression(expression,
                ctx);

        Map vars = new HashMap();
        List list = new ArrayList();
        vars.put("list",
                list);

        Boolean result = (Boolean) executeExpression(s,
                vars);

        assertNotNull(result);
        assertTrue(result);
        assertEquals(1,
                list.size());
        assertEquals("first",
                list.get(0));
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

    public void testInlineListSensitivenessToSpaces() {
        String ex = "([\"a\",\"b\", \"c\"])";

        ParserContext ctx = new ParserContext();
        Serializable s = compileExpression(ex,
                ctx);

        List result = (List) executeExpression(s,
                new HashMap());
        assertNotNull(result);
        assertEquals("a",
                result.get(0));
        assertEquals("b",
                result.get(1));
        assertEquals("c",
                result.get(2));
    }

    public void testComaProblemStrikesBack() {
        String ex = "a.explanation = \"There is a coma, in here\"";

        ParserContext ctx = new ParserContext();
        ExpressionCompiler compiler = new ExpressionCompiler(ex);
        Serializable s = compiler.compile(ctx);

        Base a = new Base();
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("a",
                a);

        executeExpression(s,
                variables);
        assertEquals("There is a coma, in here",
                a.data);
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
            assertEquals(Integer.class,
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
        String ex = "services.log( \"Drop +5%: \"+$sb+\" avg: $\"+$av+\" price: $\"+$pr );";
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
            ExpressionCompiler compiler = new ExpressionCompiler(ex);
            compiler.compile(ctx);
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
            ExpressionCompiler compiler = new ExpressionCompiler(ex);
            compiler.compile(ctx);
        }
        catch (Throwable e) {
            e.printStackTrace();
            fail("Should not raise exception: " + e.getMessage());
        }
    }

    public void testMapsWithVariableAsKey() {
        String ex = "aMap[aKey] == 'aValue'";
        ParserContext ctx = new ParserContext();
        ctx.setStrongTyping(false);

        ExpressionCompiler compiler = new ExpressionCompiler(ex);
        compiler.setVerifyOnly(true);
        compiler.compile(ctx);

        Set<String> requiredInputs = compiler.getParserContextState().getInputs().keySet();
        assertTrue(requiredInputs.contains("aMap"));
        assertTrue(requiredInputs.contains("aKey"));
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
        public void foo(String s) {
        }
    }

    public class Bz extends Az {
    }

    public void testJIRA151() {
        OptimizerFactory.setDefaultOptimizer(OptimizerFactory.SAFE_REFLECTIVE);
        Bz b = new Bz();
        ParserContext context = new ParserContext();
        Object expression = MVEL.compileExpression("a.foo(value)",
                context);
        Map<String, Object> variables = new HashMap<String, Object>();

        variables.put("a",
                b);
        variables.put("value",
                123);
        for (int i = 0; i < 100; i++) {
            System.out.println("i: " + i);
            System.out.flush();
            executeExpression(expression,
                    variables);
        }
    }

    public void testJIRA151b() {
        OptimizerFactory.setDefaultOptimizer("ASM");
        Bz b = new Bz();
        ParserContext context = new ParserContext();
        Object expression = MVEL.compileExpression("a.foo(value)",
                context);
        Map<String, Object> variables = new HashMap<String, Object>();

        variables.put("a",
                b);
        variables.put("value",
                123);
        for (int i = 0; i < 100; i++) {
            System.out.println("i: " + i);
            System.out.flush();
            executeExpression(expression,
                    variables);
        }
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

    public void testJIRA153() {
        assertEquals(false,
                MVEL.eval("!(true)"));
        assertEquals(false,
                executeExpression(MVEL.compileExpression("!(true)")));
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
        provider.getPrivate().foo();

        PublicClass.class.getMethod("foo").invoke(provider.getPrivate());

        String script = "provider.getPrivate().foo()";
        HashMap<String, Object> vars = new HashMap<String, Object>();
        vars.put("provider",
                provider);
        MVEL.eval(script,
                vars);
    }

    public void testJIRA156b() throws Throwable {
        ClassProvider provider = new ClassProvider();
        provider.getPrivate().foo();

        PublicClass.class.getMethod("foo").invoke(provider.getPrivate());

        String script = "provider.getPrivate().foo()";

        Serializable s = MVEL.compileExpression(script);

        HashMap<String, Object> vars = new HashMap<String, Object>();
        vars.put("provider",
                provider);

        OptimizerFactory.setDefaultOptimizer("reflective");
        executeExpression(s,
                vars);
        OptimizerFactory.setDefaultOptimizer("ASM");
        executeExpression(s,
                vars);
    }

    public void testJIRA156c() throws Throwable {
        ClassProvider provider = new ClassProvider();
        provider.getPublic().foo();

        PublicClass.class.getMethod("foo").invoke(provider.getPublic());

        String script = "provider.getPublic().foo()";

        Serializable s = MVEL.compileExpression(script);

        HashMap<String, Object> vars = new HashMap<String, Object>();
        vars.put("provider",
                provider);

        MVEL.eval(script,
                vars);
        OptimizerFactory.setDefaultOptimizer("reflective");
        executeExpression(s,
                vars);
        OptimizerFactory.setDefaultOptimizer("ASM");
        executeExpression(s,
                vars);
    }

    public static boolean returnTrue() {
        return true;
    }

    public static class TestHelper {
        public static void method(int id,
                                  Object[] arr) {
            System.out.println(id + " -> " + arr.length);
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
        ParserContext ctx = new ParserContext();
        ctx.setStrongTyping(true);
        ctx.addImport(TestHelper.class);
        ctx.addImport(Fooz.class);
        ExpressionCompiler compiler = new ExpressionCompiler(ex);

        OptimizerFactory.setDefaultOptimizer("ASM");
        CompiledExpression expr = compiler.compile(ctx);
        executeExpression(expr);

        OptimizerFactory.setDefaultOptimizer("reflective");
        expr = compiler.compile(ctx);
        executeExpression(expr);
    }

    public void testArray2() {
        String ex = " TestHelper.method(1, {\"a\", \"b\"});\n"
                + " TestHelper.method(2, {new String(\"a\"), new String(\"b\")});\n"
                + " TestHelper.method(3, {new Fooz(\"a\"), new Fooz(\"b\")});";
        ParserContext ctx = new ParserContext();
        ctx.setStrongTyping(true);
        ctx.addImport(TestHelper.class);
        ctx.addImport(Fooz.class);
        ExpressionCompiler compiler = new ExpressionCompiler(ex);

        OptimizerFactory.setDefaultOptimizer("ASM");
        CompiledExpression expr = compiler.compile(ctx);
        executeExpression(expr);

        OptimizerFactory.setDefaultOptimizer("reflective");
        expr = compiler.compile(ctx);
        executeExpression(expr);
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
            CompiledExpression expr = new ExpressionCompiler(expressionNaked).compile(ctx);

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

    public void testJIRA174() {

        Serializable s = MVEL.compileExpression("def test(a1) { java.util.Collection a = a1; a.clear(); a.add(1); a.add(2); a.add(3); a.remove((Object) 2); a; }\n" +
                "a = test(new java.util.ArrayList());\n" +
                "b = test(new java.util.HashSet());");

        Map vars = new HashMap();
        executeExpression(s, vars);

        assertEquals(false, ((Collection) vars.get("a")).contains(2));
        assertEquals(2, ((Collection) vars.get("a")).size());

        assertEquals(false, ((Collection) vars.get("b")).contains(2));
        assertEquals(2, ((Collection) vars.get("b")).size());
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

    public void testMVEL190a() {
        Serializable compiled = MVEL.compileExpression("a.toString()", ParserContext.create().stronglyTyped().withInput("a", String.class));
    }

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

    public void testRegExSurroundedByBrackets() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("x", "foobie");

        assertEquals(Boolean.TRUE, MVEL.eval("x ~= ('f.*')", map));
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
            //       System.out.println("'" + expr + " ' = " + ret.toString());
            assertNotNull(val1);
            Serializable compiled = MVEL.compileExpression(expr);
            val2 = executeExpression(compiled, vars);
            //     System.out.println("'" + expr + " ' = " + ret.toString());
            assertNotNull(val2);
            assertEquals("expression did not evaluate correctly: " + expr, val1, val2);
        }
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

    public void testEmptyLoopSemantics() {
        Serializable s = MVEL.compileExpression("for (i = 0; i < 100000000000; i++) { }");
        MVEL.executeExpression(s, new HashMap());
    }

    public void testLoopWithEscape() {
        Serializable s = MVEL.compileExpression("x = 0; for (; x < 10000; x++) {}");
        Map<String, Object> vars = new HashMap<String, Object>();
        MVEL.executeExpression(s, vars);

        assertEquals(10000, vars.get("x"));

        vars.remove("x");

        MVEL.eval("x = 0; for (; x < 10000; x++) {}", vars);

        assertEquals(10000, vars.get("x"));
    }

    String[] testCasesMVEL219 = {
            "map['foo']==map['foo']", // ok
            "(map['one'] > 0)", // ok
            "(map['one'] > 0) && (map['foo'] == map['foo'])", // ok
            "(map['one'] > 0) && (map['foo']==map['foo'])", // broken
    };
    String[] templateTestCasesMVEL219 = {
            "@{map['foo']==map['foo']}", // ok
            "@(map['one'] > 0)}", // ok
            "@{(map['one'] > 0) && (map['foo'] == map['foo'])}", // ok
            "@{(map['one'] > 0) && (map['foo']==map['foo'])}" // broken
    };

    public void testEvalMVEL219() {
        Map<String, Object> vars = setupVarsMVEL219();

        for (String expr : testCasesMVEL219) {
            System.out.println("Evaluating '" + expr + "': ......");
            Object ret = MVEL.eval(expr, vars);
            System.out.println("'" + expr + " ' = " + ret.toString());
            assertNotNull(ret);
        }
    }

    public void testCompiledMVEL219() {
        Map<String, Object> vars = setupVarsMVEL219();

        for (String expr : testCasesMVEL219) {
            System.out.println("Compiling '" + expr + "': ......");
            Serializable compiled = MVEL.compileExpression(expr);
            Boolean ret = (Boolean) MVEL.executeExpression(compiled, vars);
            System.out.println("'" + expr + " ' = " + ret.toString());
            assertNotNull(ret);
        }
    }

    public void testTemplateMVEL219() {
        Map<String, Object> vars = setupVarsMVEL219();

        for (String expr : templateTestCasesMVEL219) {
            System.out.println("Templating '" + expr + "': ......");
            Object ret = TemplateRuntime.eval(expr, vars);
            System.out.println("'" + expr + " ' = " + ret.toString());
            assertNotNull(ret);
        }
    }

    private Map<String, Object> setupVarsMVEL219() {
        Map<String, Object> vars = new LinkedHashMap<String, Object>();
        vars.put("bal", new BigDecimal("999.99"));
        vars.put("word", "ball");
        vars.put("object", new Dog());
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("foo", "bar");
        map.put("fu", new Dog());
        map.put("trueValue", true);
        map.put("falseValue", false);
        map.put("one", 1);
        map.put("zero", 0);
        vars.put("map", map);

        return vars;
    }


    String[] testCasesMVEL220 = {
            //        "map[\"foundIt\"] = !(map['list']).contains(\"john\")",
            "map[\"foundIt\"] = !(map['list'].contains(\"john\"))",
    };
    String[] templateTestCasesMVEL220 = {
            "@{map[\"foundIt\"] = !(map['list']).contains(\"john\")}",
            "@{map[\"foundIt\"] = !(map['list'].contains(\"john\"))}"
    };

    public void testEvalMVEL220() {
        Map<String, Object> vars = setupVarsMVEL220();

        System.out.println("Evaluation=====================");

        for (String expr : testCasesMVEL220) {
            System.out.println("Evaluating '" + expr + "': ......");
            Object ret = MVEL.eval(expr, vars);
            System.out.println("'" + expr + " ' = " + ret.toString());
            assertNotNull(ret);
        }

        System.out.println("Evaluation=====================");
    }

    public void testCompiledMVEL220() {
        Map<String, Object> vars = setupVarsMVEL220();

        System.out.println("Compilation=====================");

        for (String expr : testCasesMVEL220) {
            System.out.println("Compiling '" + expr + "': ......");
            Serializable compiled = MVEL.compileExpression(expr);
            Boolean ret = (Boolean) MVEL.executeExpression(compiled, vars);
            System.out.println("'" + expr + " ' = " + ret.toString());
            assertNotNull(ret);
        }
        System.out.println("Compilation=====================");
    }

    public void testTemplateMVEL220() {
        Map<String, Object> vars = setupVarsMVEL220();

        System.out.println("Templates=====================");

        for (String expr : templateTestCasesMVEL220) {
            System.out.println("Templating '" + expr + "': ......");
            Object ret = TemplateRuntime.eval(expr, vars);
            System.out.println("'" + expr + " ' = " + ret.toString());
            assertNotNull(ret);
        }

        System.out.println("Templates=====================");
    }


    private Map<String, Object> setupVarsMVEL220() {
        Map<String, Object> vars = new LinkedHashMap<String, Object>();
        vars.put("word", "ball");
        vars.put("object", new Dog());
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("foo", "bar");
        map.put("fu", new Dog());
        map.put("trueValue", true);
        map.put("falseValue", false);
        map.put("one", 1);
        map.put("zero", 0);
        map.put("list", "john,paul,ringo,george");
        vars.put("map", map);

        return vars;
    }


    public void testAmbiguousGetName() {
        Map<String, Object> vars = createTestMap();
        vars.put("Foo", Foo.class);

        Serializable s = MVEL.compileExpression("foo.getClass().getName()");

        System.out.println(MVEL.executeExpression(s, vars));

        s = MVEL.compileExpression("Foo.getName()");

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

}
