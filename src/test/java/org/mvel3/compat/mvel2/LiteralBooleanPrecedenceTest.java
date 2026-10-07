package org.mvel3.compat.mvel2;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.compileExpression;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.eval;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.executeExpression;

/**
 * Imported from org.mvel2.tests.core.LiteralBooleanPrecedenceTest (mvel2 branch).
 *
 * <p>Expressions, input values, expected values, and assertions are retained
 * unchanged. Failures are intentionally visible; known behavioural differences
 * are annotated with {@link KnownCompatibility}.
 */
@Tag("mvel2-compatibility")
public class LiteralBooleanPrecedenceTest {

    // --- Cases from issue #417 ---

    @Test
    public void testCompiledBooleanLiteralPrecedence() {
        // ((true && false && false) || true) == true
        String expr = "true && false && false || true";
        assertEquals(true, executeExpression(compileExpression(expr)));
    }

    @Test
    public void testTrueOrTrueAndFalse() {
        // (true || (true && false)) == true
        String expr = "true || true && false";
        assertEquals(true, executeExpression(compileExpression(expr)));
    }

    @Test
    public void testTrueOrFalseAndFalse() {
        // (true || (false && false)) == true
        String expr = "true || false && false";
        assertEquals(true, executeExpression(compileExpression(expr)));
    }

    // --- Additional precedence cases ---

    @Test
    public void testFalseOrTrueAndTrue() {
        // (false || (true && true)) == true
        String expr = "false || true && true";
        assertEquals(true, executeExpression(compileExpression(expr)));
    }

    @Test
    public void testFalseAndTrueOrTrue() {
        // ((false && true) || true) == true
        String expr = "false && true || true";
        assertEquals(true, executeExpression(compileExpression(expr)));
    }

    @Test
    public void testFalseOrFalseAndFalseOrTrue() {
        // (false || (false && false) || true) == true
        String expr = "false || false && false || true";
        assertEquals(true, executeExpression(compileExpression(expr)));
    }

    @Test
    public void testTrueAndTrueOrFalseAndFalse() {
        // ((true && true) || (false && false)) == true
        String expr = "true && true || false && false";
        assertEquals(true, executeExpression(compileExpression(expr)));
    }

    @Test
    public void testFalseAndFalseOrFalseAndFalse() {
        // ((false && false) || (false && false)) == false
        String expr = "false && false || false && false";
        assertEquals(false, executeExpression(compileExpression(expr)));
    }

    @Test
    public void testAllAndShouldBeTrue() {
        // (true && true && true) == true
        String expr = "true && true && true";
        assertEquals(true, executeExpression(compileExpression(expr)));
    }

    @Test
    public void testAllOrShouldBeFalse() {
        // (false || false || false) == false
        String expr = "false || false || false";
        assertEquals(false, executeExpression(compileExpression(expr)));
    }

    // --- Verify compiled literals match interpreted eval ---

    @Test
    public void testCompiledMatchesInterpreted() {
        String[] expressions = {
            "true && false && false || true",
            "true || true && false",
            "true || false && false",
            "false || true && true",
            "false && true || true",
            "false || false && false || true",
            "true && true || false && false",
            "false && false || false && false"
        };
        for (String expr : expressions) {
            Object interpreted = eval(expr);
            Object compiled = executeExpression(compileExpression(expr));
            assertEquals("Mismatch for: " + expr, interpreted, compiled);
        }
    }

    // --- Verify compiled literals match variable-based evaluation ---

    @Test
    public void testCompiledLiteralsMatchVariables() {
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("T", true);
        vars.put("F", false);

        String[][] pairs = {
            {"true && false && false || true",    "T && F && F || T"},
            {"true || true && false",             "T || T && F"},
            {"true || false && false",            "T || F && F"},
            {"false || true && true",             "F || T && T"},
            {"false && true || true",             "F && T || T"},
            {"true && true || false && false",    "T && T || F && F"},
        };
        for (String[] pair : pairs) {
            Object literal = executeExpression(compileExpression(pair[0]));
            Object variable = executeExpression(compileExpression(pair[1]), vars);
            assertEquals("Mismatch for: " + pair[0], variable, literal);
        }
    }
}
