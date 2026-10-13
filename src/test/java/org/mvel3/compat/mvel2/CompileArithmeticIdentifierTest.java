package org.mvel3.compat.mvel2;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.compileExpression;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.executeExpression;

/**
 * Imported from org.mvel2.tests.core.CompileArithmeticIdentifierTest (mvel2 branch).
 *
 * <p>Expressions, input values, expected values, and assertions are retained
 * unchanged. Failures are intentionally visible; known behavioural differences
 * are annotated with {@link KnownCompatibility}.
 */
@Tag("mvel2-compatibility")
public class CompileArithmeticIdentifierTest extends AbstractTest {

    @Test
    public void compilesLiteralPlusTwoLiteralMulsThenIdentifier() {
        // The exact case from issue #422 — must not throw at compile time.
        compileExpression("1 + 1 * 1 * D");
    }

    @Test
    public void compilesLiteralPlusTwoNonOneLiteralMulsThenIdentifier() {
        compileExpression("1 + 2 * 3 * D");
    }

    @Test
    public void compilesLiteralPlusThreeLiteralMulsThenIdentifier() {
        compileExpression("1 + 1 * 1 * 1 * D");
    }

    @Test
    public void compilesPreviouslyWorkingVariants() {
        // Cases the issue reporter confirmed already compiled — must stay green.
        compileExpression("1 * 1 * D");
        compileExpression("1 + 1 * D");
        compileExpression("1 + D * 1 * 1");
        compileExpression("1 + 1 + 1 * D");
    }

    @Test
    public void executesWithIdentifierBound() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("D", 10);

        assertEquals(11, executeExpression(compileExpression("1 + 1 * 1 * D"), vars));
        assertEquals(61, executeExpression(compileExpression("1 + 2 * 3 * D"), vars));
        assertEquals(11, executeExpression(compileExpression("1 + 1 * D"), vars));
        assertEquals(10, executeExpression(compileExpression("1 * 1 * D"), vars));
    }

    public static class Bag {
        public int bar = 7;
    }

    @Test
    public void continuesAfterDeferredIdentifierWithLowerPrecedenceOp() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("D", 10);
        // After the deferred `* D`, a lower-precedence `+ 4` must still parse cleanly.
        assertEquals(65, executeExpression(compileExpression("1 + 2 * 3 * D + 4"), vars));
    }

    @Test
    public void continuesAfterDeferredIdentifierWithSamePrecedenceOp() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("D", 10);
        // After the deferred `* D`, a same-precedence `* 4` must still parse cleanly.
        assertEquals(241, executeExpression(compileExpression("1 + 2 * 3 * D * 4"), vars));
    }

    @Test
    public void deferredNonLiteralMayBeAPropertyAccessor() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("foo", new Bag());
        assertEquals(43, executeExpression(compileExpression("1 + 2 * 3 * foo.bar"), vars));
    }

    @Test
    public void compileSucceedsButRuntimeStillFailsWhenIdentifierUnbound() {
        // Compile must not eagerly resolve `D`; runtime must still report it missing.
        Object compiled = compileExpression("1 + 1 * 1 * D");
        try {
            executeExpression((java.io.Serializable) compiled);
            fail("Expected PropertyAccessException at runtime for unbound identifier");
        } catch (Exception expected) {
            // expected (MVEL2 throws PropertyAccessException, MVEL3 runtime fails when identifier unbound)
        }
    }

    @Test
    public void literalOnlyConstantFoldingStillWorks() {
        // Ensure constant folding for purely literal chains is not regressed.
        assertEquals(2, executeExpression(compileExpression("1 + 1 * 1 * 1")));
        assertEquals(7, executeExpression(compileExpression("1 + 2 * 3")));
        assertEquals(13, executeExpression(compileExpression("1 + 2 * 2 * 3")));
    }
}
