package org.mvel2.tests.core;

import org.mvel2.MVEL;

import java.util.HashMap;

/**
 * Tests to ensure MVEL fails when it should.
 */
public class FailureTests extends AbstractTest {
    public void testBadParserConstruct() {
        try {
            test("a = 0; a =+++ 5;");
        }
        catch (Exception e) {
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
            MVEL.eval("new int[] {1.5}");
        }
        catch (Exception e) {
            return;
        }
        assertTrue(false);
    }
}
