package org.mvel2.tests.core;

import org.mvel2.MVEL;

import static org.mvel2.MVEL.compileExpression;
import static org.mvel2.MVEL.executeExpression;

/**
 * @author Mike Brock .
 */
public class LiteralParsingTests extends AbstractTest {

  public void testClassLiteral() {
    assertEquals(String.class,
        test("java.lang.String"));
  }

  public void testAndOpLiteral() {
    assertEquals(true,
        test("true && true"));
  }


  public void testLiteralUnionWithComparison() {
    assertEquals(Boolean.TRUE,
        executeExpression(compileExpression("1 == 1 && ('Hello'.toUpperCase() ~= '[A-Z]{0,5}')")));
  }

  public void testHexCharacter() {
    assertEquals(0x0A,
        MVEL.eval("0x0A"));
  }

  public void testOctalEscapes() {
    assertEquals("\344",
        MVEL.eval("'\\344'"));
  }

  public void testOctalEscapes2() {
    assertEquals("\7",
        MVEL.eval("'\\7'"));
  }

  public void testOctalEscapes3() {
    assertEquals("\777",
        MVEL.eval("'\\777'"));
  }

  public void testUniHex1() {
    assertEquals("\uFFFF::",
        MVEL.eval("'\\uFFFF::'"));
  }

  public void testNumLiterals() {
    assertEquals(1e1f,
        MVEL.eval("1e1f"));
  }

  public void testNumLiterals2() {
    assertEquals(2.f,
        MVEL.eval("2.f"));
  }

  public void testNumLiterals3() {
    assertEquals(.3f,
        MVEL.eval(".3f"));
  }

  public void testNumLiterals4() {
    assertEquals(3.14f,
        MVEL.eval("3.14f"));
  }

  public void testNumLiterals5() {
    assertEquals(1e1,
        MVEL.eval("1e1"));
  }

  public void testNumLiterals6() {
    assertEquals(2.,
        MVEL.eval("2."));
  }

  public void testNumLiterals7() {
    assertEquals(.3,
        MVEL.eval(".3"));
  }

  public void testNumLiterals8() {
    assertEquals(1e-9d,
        MVEL.eval("1e-9d"));
  }

  public void testNumLiterals9() {
    assertEquals(0x400921FB54442D18L,
        MVEL.eval("0x400921FB54442D18L"));
  }


}
