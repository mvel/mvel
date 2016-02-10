package org.mvel2.bugs;
import java.io.Serializable;

import org.mvel2.MVEL;

import junit.framework.TestCase;

public class NoContextErrorTest extends TestCase {
  
  private static final String TEST_CODE = 
            "for (int i=0; i < 1; i++) {System.err.println(\"for with i\");} "+
            "for (int j=0; j < 1; j++) {System.err.println(\"for with j\");}";

  // this test fails with exception and message "no context"
  public void testEval() throws Exception {
    MVEL.eval(TEST_CODE);
  }

  // this test is ok and in console there will be 
  // for with i
  // for with j
  public void testCompile() throws Exception {
    Serializable compile = MVEL.compileExpression(TEST_CODE);
    MVEL.executeExpression(compile);
  }
}
