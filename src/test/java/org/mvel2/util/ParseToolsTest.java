package org.mvel2.util;

import junit.framework.TestCase;

/**
 * @author Ken Scoggins
 */
public class ParseToolsTest extends TestCase {

  /**
   * Test a bug that appears to have been introduced in 2.1.0 (commit 86b3331547a20ec28250d02a4f2ed0d679aed664)
   * where the full expression was being passed to Integer.decode, not the literal token, causing a
   * NumberFormatException for expressions with Oct or Hex literals.
   */
  public void testHandleNumericConversionBug() {
    String[] testLiterals = {"0x20","020",};
    String baseExpression = "int foo = ";

    for( String literal : testLiterals ) {
      char[] decExpr = ( baseExpression + literal ).toCharArray();
      assertEquals( Integer.decode( literal ),
                    ParseTools.handleNumericConversion( decExpr, baseExpression.length(), literal.length() ) );
    }
  }
}