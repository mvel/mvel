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

    @Test
    @KnownCompatibility("regex-operator")
    public void testRegExpPreCompiledBug() throws Exception {
        // If toUpperCase() is used in the expression then this fails; returns null not
        // a boolean.
        Object ser = compileExpression("'Hello'.toUpperCase() ~= '[a-zA-Z]{0,5}'");
        assertEquals(Boolean.TRUE,
                executeExpression((java.io.Serializable) ser));
    }

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

    @Test
    @KnownCompatibility("regex-operator")
    public void testRegExSurroundedByBrackets() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("x", "foobie");

        assertEquals(Boolean.TRUE, eval("x ~= ('f.*')", map));
    }

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

    @Test
    @KnownCompatibility("regex-operator")
    public void testRegExOR() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("os",
                "windows");
        assertTrue((Boolean) eval("os ~= 'windows|unix'",
                map));
    }

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

    @Test
    @KnownCompatibility("regex-operator")
    public void testRegExMatch() {
        assertEquals(true,
                eval("$test = 'foo'; $ex = 'f.*'; $test ~= $ex",
                        new HashMap()));
    }
}
