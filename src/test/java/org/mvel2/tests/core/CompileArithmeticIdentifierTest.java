package org.mvel2.tests.core;

import org.junit.Test;
import org.mvel2.MVEL;
import org.mvel2.PropertyAccessException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Regression tests for issue #422: compileExpression must not attempt to
 * resolve identifiers at compile time during arithmetic constant folding.
 *
 * Before the fix, expressions of the form {@code <lit> + <lit> * <lit> * <ident>}
 * threw PropertyAccessException at compile time because the same-precedence
 * branch of arithmeticFunctionReduction reduced the trailing identifier
 * eagerly. Sibling forms with at most one literal multiplication before the
 * identifier compiled cleanly, exposing the asymmetric handling.
 */
public class CompileArithmeticIdentifierTest {

    @Test
    public void compilesLiteralPlusTwoLiteralMulsThenIdentifier() {
        // The exact case from issue #422 — must not throw at compile time.
        MVEL.compileExpression("1 + 1 * 1 * D");
    }

    @Test
    public void compilesLiteralPlusTwoNonOneLiteralMulsThenIdentifier() {
        MVEL.compileExpression("1 + 2 * 3 * D");
    }

    @Test
    public void compilesLiteralPlusThreeLiteralMulsThenIdentifier() {
        MVEL.compileExpression("1 + 1 * 1 * 1 * D");
    }

    @Test
    public void compilesPreviouslyWorkingVariants() {
        // Cases the issue reporter confirmed already compiled — must stay green.
        MVEL.compileExpression("1 * 1 * D");
        MVEL.compileExpression("1 + 1 * D");
        MVEL.compileExpression("1 + D * 1 * 1");
        MVEL.compileExpression("1 + 1 + 1 * D");
    }

    @Test
    public void executesWithIdentifierBound() {
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("D", 10);

        assertEquals(11, MVEL.executeExpression(MVEL.compileExpression("1 + 1 * 1 * D"), vars));
        assertEquals(61, MVEL.executeExpression(MVEL.compileExpression("1 + 2 * 3 * D"), vars));
        assertEquals(11, MVEL.executeExpression(MVEL.compileExpression("1 + 1 * D"), vars));
        assertEquals(10, MVEL.executeExpression(MVEL.compileExpression("1 * 1 * D"), vars));
    }

    public static class Bag {
        public int bar = 7;
    }

    @Test
    public void continuesAfterDeferredIdentifierWithLowerPrecedenceOp() {
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("D", 10);
        // After the deferred `* D`, a lower-precedence `+ 4` must still parse cleanly.
        assertEquals(65, MVEL.executeExpression(MVEL.compileExpression("1 + 2 * 3 * D + 4"), vars));
    }

    @Test
    public void continuesAfterDeferredIdentifierWithSamePrecedenceOp() {
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("D", 10);
        // After the deferred `* D`, a same-precedence `* 4` must still parse cleanly.
        assertEquals(241, MVEL.executeExpression(MVEL.compileExpression("1 + 2 * 3 * D * 4"), vars));
    }

    @Test
    public void deferredNonLiteralMayBeAPropertyAccessor() {
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("foo", new Bag());
        assertEquals(43, MVEL.executeExpression(MVEL.compileExpression("1 + 2 * 3 * foo.bar"), vars));
    }

    @Test
    public void compileSucceedsButRuntimeStillFailsWhenIdentifierUnbound() {
        // Compile must not eagerly resolve `D`; runtime must still report it missing.
        Object compiled = MVEL.compileExpression("1 + 1 * 1 * D");
        try {
            MVEL.executeExpression(compiled);
            fail("Expected PropertyAccessException at runtime for unbound identifier");
        } catch (PropertyAccessException expected) {
            // expected
        }
    }

    @Test
    public void literalOnlyConstantFoldingStillWorks() {
        // Ensure constant folding for purely literal chains is not regressed.
        assertEquals(2, MVEL.executeExpression(MVEL.compileExpression("1 + 1 * 1 * 1")));
        assertEquals(7, MVEL.executeExpression(MVEL.compileExpression("1 + 2 * 3")));
        assertEquals(13, MVEL.executeExpression(MVEL.compileExpression("1 + 2 * 2 * 3")));
    }
}
