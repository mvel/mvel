package org.mvel3.compat.mvel2;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.mvel3.compat.mvel2.Mvel3TestSupport.compileExpression;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.eval;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.executeExpression;

/**
 * Imported from org.mvel2.tests.core.RegularExpressionTests (mvel2 branch).
 *
 * <p>Expressions, input values, expected values, and assertions are retained
 * unchanged. Failures are intentionally visible; known behavioural differences
 * are annotated with {@link KnownCompatibility}.
 */
@Tag("mvel2-compatibility")
public class RegularExpressionTests extends AbstractTest {

    // Category not isolated: observed error is a parse error near `llooUpperCase(`. With the literals converted to double
    // quotes (experiment) the result was the uppercased string (`~=` not evaluated), which supports regex-operator,
    // so the single-quoted literals (single-quote-string) contribute to the original parse error.
    @Test
    @KnownCompatibility("regex-operator")
    public void testRegExpOK() throws Exception {
        // This works OK intepreted
        assertEquals(Boolean.TRUE,
                eval("'Hello'.toUpperCase() ~= '[A-Z]{0,5}'"));
        assertEquals(Boolean.TRUE,
                eval("1 == 0 || ('Hello'.toUpperCase() ~= '[A-Z]{0,5}')"));
        // This works OK if toUpperCase() is avoided in pre-compiled
        assertEquals(Boolean.TRUE,
                executeExpression(compileExpression("'Hello' ~= '[a-zA-Z]{0,5}'")));
    }

    // Category not isolated: same as testRegExpOK. The parse error comes from the single-quoted literals; the remaining
    // failure (the uppercased string returned instead of a boolean) comes from `~=` (regex-operator).
    @Test
    @KnownCompatibility("regex-operator")
    public void testRegExpPreCompiledBug() throws Exception {
        // If toUpperCase() is used in the expression then this fails; returns null not
        // a boolean.
        Object ser = compileExpression("'Hello'.toUpperCase() ~= '[a-zA-Z]{0,5}'");
        assertEquals(Boolean.TRUE,
                executeExpression((java.io.Serializable) ser));
    }

    // Category not isolated: the observed result is `false`, not the regex result. `~=` (regex-operator) is the suspected
    // cause; the single-quoted literals were not tested separately.
    @Test
    @KnownCompatibility("regex-operator")
    public void testRegExpOrBug() throws Exception {
        // This fails during execution due to returning null, I think...
        assertEquals(Boolean.TRUE,
                executeExpression(compileExpression("1 == 0 || ('Hello'.toUpperCase() ~= '[A-Z]{0,5}')")));
    }

    @Test
    @KnownCompatibility("regex-operator")
    public void testRegExpAndBug() throws Exception {
        // This also fails due to returning null, I think...
        assertEquals(Boolean.TRUE,
                executeExpression(compileExpression("1 == 1 && ('Hello'.toUpperCase() ~= '[A-Z]{0,5}')")));
    }

    // Category not isolated: the observed result is the left operand (`foobie`), so `~=` is not evaluated as an operator
    // (regex-operator). The single-quoted pattern literal was not tested separately.
    @Test
    @KnownCompatibility("regex-operator")
    public void testRegExSurroundedByBrackets() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("x", "foobie");

        assertEquals(Boolean.TRUE, eval("x ~= ('f.*')", map));
    }

    // Category not isolated: observed error is a null-node failure. `~=` (regex-operator) is the suspected cause; the
    // single-quoted literals were not tested separately.
    @Test
    @KnownCompatibility("regex-operator")
    public void testMVEL231() {
        eval("Q8152405_A35423077=\"1\"; Q8152405_A35423077!=null && (Q8152405_A35423077~=\"^[0-9]$\");", new HashMap());
    }

    @Test
    @KnownCompatibility("regex-operator")
    public void testParsingStability4() {
        assertEquals(true,
                test("vv=\"Edson\"; !(vv ~= \"Mark\")"));
    }

    // Category not isolated: the observed ClassCastException (String to Boolean) means `~=` is not evaluated as an
    // operator (regex-operator); the single-quoted pattern literal was not tested separately.
    @Test
    @KnownCompatibility("regex-operator")
    public void testRegExOR() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("os",
                "windows");
        assertTrue((Boolean) eval("os ~= 'windows|unix'",
                map));
    }

    // Category not isolated: observed error is "Unsolved symbol" from a single-quoted literal. With the literals converted
    // (experiment) it became a ClassCastException (String to Boolean), which supports regex-operator.
    @Test
    @KnownCompatibility("regex-operator")
    public void testRegExOR2() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("os",
                "windows");
        assertTrue((Boolean) eval("'windows' ~= 'windows|unix'",
                map));
        assertFalse((Boolean) eval("time ~= 'windows|unix'",
                new java.util.Date()));
    }

    // Category not isolated: observed error is a null-node failure. With single-quoted literals converted (experiment) it
    // became a parse error at `~=`, which supports regex-operator; the `$test` / `$ex` assignments (block-syntax)
    // were not tested separately.
    @Test
    @KnownCompatibility("regex-operator")
    public void testRegExMatch() {
        assertEquals(true,
                eval("$test = 'foo'; $ex = 'f.*'; $test ~= $ex",
                        new HashMap()));
    }
}
