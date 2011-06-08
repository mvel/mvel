package org.mvel2.tests.core;

import org.mvel2.MVEL;

import java.util.HashMap;
import java.util.Map;

import static org.mvel2.MVEL.compileExpression;
import static org.mvel2.MVEL.eval;
import static org.mvel2.MVEL.executeExpression;

/**
 * @author Mike Brock .
 */
public class RegularExpressionTests extends AbstractTest {

  public void testRegExpOK() throws Exception {
    // This works OK intepreted
    assertEquals(Boolean.TRUE,
        MVEL.eval("'Hello'.toUpperCase() ~= '[A-Z]{0,5}'"));
    assertEquals(Boolean.TRUE,
        MVEL.eval("1 == 0 || ('Hello'.toUpperCase() ~= '[A-Z]{0,5}')"));
    // This works OK if toUpperCase() is avoided in pre-compiled
    assertEquals(Boolean.TRUE,
        executeExpression(compileExpression("'Hello' ~= '[a-zA-Z]{0,5}'")));
  }

  public void testRegExpPreCompiledBug() throws Exception {
    // If toUpperCase() is used in the expression then this fails; returns null not
    // a boolean.
    Object ser = compileExpression("'Hello'.toUpperCase() ~= '[a-zA-Z]{0,5}'");
    assertEquals(Boolean.TRUE,
        executeExpression(ser));
  }

  public void testRegExpOrBug() throws Exception {
    // This fails during execution due to returning null, I think...
    assertEquals(Boolean.TRUE,
        executeExpression(compileExpression("1 == 0 || ('Hello'.toUpperCase() ~= '[A-Z]{0,5}')")));
  }

  public void testRegExpAndBug() throws Exception {
    // This also fails due to returning null, I think...
    //  Object ser = MVEL.compileExpression("1 == 1 && ('Hello'.toUpperCase() ~= '[A-Z]{0,5}')");
    assertEquals(Boolean.TRUE,
        executeExpression(compileExpression("1 == 1 && ('Hello'.toUpperCase() ~= '[A-Z]{0,5}')")));
  }

  public void testRegExSurroundedByBrackets() {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("x", "foobie");

    assertEquals(Boolean.TRUE, MVEL.eval("x ~= ('f.*')", map));
  }

  public void testMVEL231() {
    System.out.println(MVEL.eval("Q8152405_A35423077=\"1\"; Q8152405_A35423077!=null && (Q8152405_A35423077~=\"^[0-9]$\");", new HashMap()));
  }

  public void testParsingStability4() {
    assertEquals(true,
        test("vv=\"Edson\"; !(vv ~= \"Mark\")"));
  }


  /**
   * Submitted by: Dimitar Dimitrov
   */
  public void testRegExOR() {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("os",
        "windows");
    assertTrue((Boolean) eval("os ~= 'windows|unix'",
        map));
  }

  public void testRegExOR2() {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("os",
        "windows");
    assertTrue((Boolean) eval("'windows' ~= 'windows|unix'",
        map));
    assertFalse((Boolean) eval("time ~= 'windows|unix'",
        new java.util.Date()));
  }


  public void testRegExMatch() {
    assertEquals(true,
        MVEL.eval("$test = 'foo'; $ex = 'f.*'; $test ~= $ex",
            new HashMap()));
  }
}
