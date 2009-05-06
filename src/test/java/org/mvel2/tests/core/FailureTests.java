package org.mvel2.tests.core;

import org.mvel2.MVEL;
import org.mvel2.ParserContext;

import java.util.HashMap;

/**
 * Tests to ensure MVEL fails when it should.
 */
public class FailureTests extends AbstractTest {
    public void testBadParserConstruct() {
        try {
            MVEL.eval("a = 0; a =+++ 5;");
        }
        catch (RuntimeException e) {
            return;
        }

        assertTrue(false);
    }

    public void testShouldFail() {
        try {
            MVEL.eval("i = 0; i < 99 dksadlka", new HashMap());
        }
        catch (Exception e) {
            return;
        }
        assertTrue(false);
    }

    public void testShouldFail2() {
        try {
            MVEL.compileExpression("i = 0; i < 99 dksadlka");
        }
        catch (Exception e) {
            return;
        }
        assertTrue(false);
    }

    public void testShouldFail3() {
        try {
            MVEL.compileExpression("def foo() { 'bar' }; foo(123);");
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
        assertTrue(false);
    }

    public void testShouldFail4() {
        try {
            MVEL.eval("hour zzz", createTestMap());
        }
        catch (Exception e) {
            return;
        }
        assertTrue(false);
    }

    public void testShouldFail5() {
        try {
            MVEL.eval("[");
        }
        catch (Exception e) {
            return;
        }
        assertTrue(false);
    }

    public void testShouldFail6() {
        try {
            ParserContext pctx = new ParserContext();
            pctx.setStrongTyping(true);
            MVEL.compileExpression("new int[] {1.5}", pctx);
        }
        catch (Exception e) {
            return;
        }
        assertTrue(false);
    }

    public void testShouldFail7() {
        try {
            ParserContext pctx = new ParserContext();
            pctx.setStrongTyping(true);
            MVEL.compileExpression("String x = 'foo'; int y = 2; new int[] { x, y }", pctx);
        }
        catch (Exception e) {
//            e.printStackTrace();
            return;
        }
        assertTrue(false);
    }

    public void testShouldFail8() {
        try {
            ParserContext pCtx = new ParserContext();
            pCtx.setStrongTyping(true);

            MVEL.compileExpression("for (String s : new java.util.HashMap()) { }", pCtx);
        }
        catch (Exception e) {
            //        e.printStackTrace();
            return;
        }

        assertTrue(false);
    }
}
