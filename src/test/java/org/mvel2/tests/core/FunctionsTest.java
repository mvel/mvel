package org.mvel2.tests.core;

import org.mvel2.MVEL;
import static org.mvel2.MVEL.executeExpression;
import org.mvel2.ast.Function;
import org.mvel2.compiler.CompiledExpression;
import org.mvel2.compiler.ExpressionCompiler;
import org.mvel2.optimizers.OptimizerFactory;
import org.mvel2.util.CompilerTools;
import static org.mvel2.util.CompilerTools.extractAllDeclaredFunctions;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Dhanji R. Prasanna (dhanji@gmail com)
 */
public class FunctionsTest extends AbstractTest {

    public final void testThatFunctionsCloseOverArguments() {
        final Object o = MVEL.eval(
                "def fun(x) { ($ in [1, 2, 3] if $ > x) }" +
                        "" +
                        "fun(0)",
                new HashMap<String, Object>()
        );

        assertTrue(o instanceof List);
        assertEquals(Arrays.asList(1, 2, 3), o);
    }

    public void testFunctionDefAndCall() {
        assertEquals("FoobarFoobar",
                test("function heyFoo() { return 'Foobar'; };\n" +
                        "return heyFoo() + heyFoo();"));
    }

    public void testFunctionDefAndCall2() {
        ExpressionCompiler compiler = new ExpressionCompiler("function heyFoo() { return 'Foobar'; };\n" +
                "return heyFoo() + heyFoo();");

        Serializable s = compiler.compile();

        Map<String, Function> m = extractAllDeclaredFunctions((CompiledExpression) s);

        assertTrue(m.containsKey("heyFoo"));

        OptimizerFactory.setDefaultOptimizer("reflective");

        assertEquals("FoobarFoobar", executeExpression(s, new HashMap()));
        assertEquals("FoobarFoobar", executeExpression(s, new HashMap()));

        OptimizerFactory.setDefaultOptimizer("dynamic");

    }

    public void testFunctionDefAndCall3() {
        assertEquals("FOOBAR", test("function testFunction() { a = 'foo'; b = 'bar'; a + b; }; testFunction().toUpperCase();  "));
    }

    public void testFunctionDefAndCall4() {
        assertEquals("barfoo", test("function testFunction(input) { return input; }; testFunction('barfoo');"));
    }

    public void testFunctionDefAndCall5() {
        assertEquals(10, test("function testFunction(x, y) { return x + y; }; testFunction(7, 3);"));
    }

    public void testFunctionDefAndCall6() {
        assertEquals("foo", MVEL.eval("def fooFunction(x) x; fooFunction('foo')", new HashMap()));
    }

    public void testAnonymousFunction() {
        assertEquals("foobar", test("a = function { 'foobar' }; a();"));
    }

    public void testJIRA207() {
        String ex = "x = 0; y = 0;" +
                "def foo() { x = 1; System.out.println('Word up'); }\n" +
                "def bar() { y = 1;  System.out.println('Peace out'); }\n" +
                "def doMany(fps) {\n" +
                "foreach(f : fps) { System.out.println(f); f(); }\n" +
                "}\n" +
                "doMany([foo,bar]);" +
                "x == 1 && y == 1;";
        Boolean bool = (Boolean) MVEL.eval(ex, new HashMap());
        assertTrue(bool);

        OptimizerFactory.setDefaultOptimizer("ASM");
        Serializable s = MVEL.compileExpression(ex);

        bool = (Boolean) MVEL.executeExpression(s, new HashMap());
        assertTrue(bool);

        OptimizerFactory.setDefaultOptimizer("dynamic");
    }

}
