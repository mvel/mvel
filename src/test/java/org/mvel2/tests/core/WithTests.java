package org.mvel2.tests.core;

import org.mvel2.MVEL;
import static org.mvel2.MVEL.executeExpression;
import org.mvel2.ParserContext;
import org.mvel2.compiler.CompiledExpression;
import org.mvel2.compiler.ExpressionCompiler;
import org.mvel2.optimizers.OptimizerFactory;
import org.mvel2.tests.core.res.Base;
import org.mvel2.tests.core.res.Foo;

import java.util.HashMap;
import java.util.Map;

public class WithTests extends AbstractTest {
    public void testWith() {
        assertEquals("OneTwo", test("with (foo) {aValue = 'One',bValue='Two'}; foo.aValue + foo.bValue;"));
    }

    public void testWith2() {
        assertEquals("OneTwoOne", test(
                "var y; with (foo) { \n" +
                        "aValue = (y = 'One'), // this is a comment \n" +
                        "bValue='Two'  // this is also a comment \n" +
                        "}; \n" +
                        "foo.aValue + foo.bValue + y;"));
    }

    public void testWith3() {
        assertEquals("OneOneTwoTwo", test("with (foo) {aValue = 'One',bValue='Two'}; with (foo) {aValue += 'One', bValue += 'Two'}; foo.aValue + foo.bValue;"));
    }


    public void testWith4() {
        assertEquals(10, test("with (foo) {countTest += 5 }; with (foo) { countTest *= 2 }; foo.countTest"));
    }

    public void testWith5() {
        Foo foo = (Foo) test("with (foo) { countTest += 5, \n" +
                "// foobar!\n" +
                "aValue = 'Hello',\n" +
                "/** Comment! **/\n" +
                "bValue = 'Goodbye'\n }; with (foo) { countTest *= 2 }; foo");

        assertEquals(10, foo.getCountTest());
        assertEquals("Hello", foo.aValue);
        assertEquals("Goodbye", foo.bValue);
    }

    public void testInlineWith() {
        CompiledExpression expr = new ExpressionCompiler("foo.{name='poopy', aValue='bar'}").compile();
        Foo f = (Foo) executeExpression(expr, createTestMap());
        assertEquals("poopy", f.getName());
        assertEquals("bar", f.aValue);
    }

    public void testInlineWith2() {
        CompiledExpression expr = new ExpressionCompiler("foo.{name = 'poopy', aValue = 'bar', bar.{name = 'foobie'}}").compile();

        Foo f = (Foo) executeExpression(expr, createTestMap());

        assertEquals("poopy", f.getName());
        assertEquals("bar", f.aValue);
        assertEquals("foobie", f.getBar().getName());
    }

    public void testInlineWith3() {
        CompiledExpression expr = new ExpressionCompiler("foo.{name = 'poopy', aValue = 'bar', bar.{name = 'foobie'}, toUC('doopy')}").compile();

        Foo f = (Foo) executeExpression(expr, createTestMap());

        assertEquals("poopy", f.getName());
        assertEquals("bar", f.aValue);
        assertEquals("foobie", f.getBar().getName());
        assertEquals("doopy", f.register);
    }

    public void testInlineWith3a() {
        CompiledExpression expr = new ExpressionCompiler("foo.{name='poopy',aValue='bar',bar.{name='foobie'},toUC('doopy')}").compile();

        Foo f = (Foo) executeExpression(expr, createTestMap());

        assertEquals("poopy", f.getName());
        assertEquals("bar", f.aValue);
        assertEquals("foobie", f.getBar().getName());
        assertEquals("doopy", f.register);
    }

    public void testInlineWith4() {
        OptimizerFactory.setDefaultOptimizer("ASM");
        ExpressionCompiler expr = new ExpressionCompiler("new Foo().{ name = 'bar' }");
        ParserContext pCtx = new ParserContext();
        pCtx.addImport(Foo.class);

        CompiledExpression c = expr.compile(pCtx);

        Foo f = (Foo) executeExpression(c);

        assertEquals("bar", f.getName());

        f = (Foo) executeExpression(c);

        assertEquals("bar", f.getName());
    }

    public void testInlineWith5() {
        OptimizerFactory.setDefaultOptimizer("ASM");

        ParserContext pCtx = new ParserContext();
        pCtx.setStrongTyping(true);

        pCtx.addInput("foo", Foo.class);

        CompiledExpression expr = new ExpressionCompiler("foo.{name='poopy', aValue='bar'}").compile(pCtx);
        Foo f = (Foo) executeExpression(expr, createTestMap());
        assertEquals("poopy", f.getName());
        assertEquals("bar", f.aValue);
    }

    public void testInlineWithImpliedThis() {
        Base b = new Base();
        ExpressionCompiler expr = new ExpressionCompiler(".{ data = 'foo' }");
        CompiledExpression compiled = expr.compile();

        executeExpression(compiled, b);

        assertEquals(b.data, "foo");
    }

    public void testSingleMethodCall() {
        Base b = new Base();

        Map map = new HashMap();
        map.put("base", b);

        MVEL.eval("base.{ populate() }", map);

        assertEquals("sarah", b.barfoo);
    }
}
