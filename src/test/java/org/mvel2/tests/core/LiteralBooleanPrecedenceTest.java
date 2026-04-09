package org.mvel2.tests.core;

import org.junit.Test;
import org.mvel2.MVEL;

import static org.junit.Assert.assertEquals;

public class LiteralBooleanPrecedenceTest {

    @Test
    public void testCompiledBooleanLiteralPrecedence() {
        String expr = "true && false && false || true";
        assertEquals(true, MVEL.executeExpression(MVEL.compileExpression(expr)));
    }
}
