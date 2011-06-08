package org.mvel2.tests.core;

import org.mvel2.CompileException;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;

import java.util.HashMap;

/**
 * Tests to ensure MVEL fails when it should.
 */
public class FailureTests extends AbstractTest {
  public void shouldThrowException() {
    fail("should have thrown exception, but didn't");
  }

  public void testBadParserConstruct() {
    try {
      MVEL.eval("a = 0; a =+++ 5;");
    }
    catch (RuntimeException e) {
      return;
    }

    shouldThrowException();
  }

  public void testShouldFail() {
    try {
      MVEL.eval("i = 0; i < 99 dksadlka", new HashMap());
    }
    catch (Exception e) {
      return;
    }

    shouldThrowException();
  }

  public void testShouldFail2() {
    try {
      MVEL.compileExpression("i = 0; i < 99 dksadlka");
    }
    catch (Exception e) {
      return;
    }

    shouldThrowException();
  }

  public void testShouldFail3() {
    try {
      MVEL.compileExpression("def foo() { 'bar' }; foo(123);");
    }
    catch (Exception e) {
      e.printStackTrace();
      return;
    }

    shouldThrowException();
  }

  public void testShouldFail4() {
    try {
      MVEL.eval("hour zzz", createTestMap());
    }
    catch (Exception e) {
      return;
    }

    shouldThrowException();
  }

  public void testShouldFail5() {
    try {
      MVEL.eval("[");
    }
    catch (Exception e) {
      return;
    }

    shouldThrowException();
  }

  public void testShouldFail6() {
    try {
      ParserContext pctx = new ParserContext();
      pctx.setStrongTyping(true);
      MVEL.compileExpression("new int[] {1.5}", pctx);
    }
    catch (Exception e) {
      return;
    }

    shouldThrowException();
  }

  public void testShouldFail7() {
    try {
      ParserContext pctx = new ParserContext();
      pctx.setStrongTyping(true);
      MVEL.compileExpression("String x = 'foo'; int y = 2; new int[] { x, y }", pctx);
    }
    catch (Exception e) {
//            e.printStackTrace();
      return;
    }

    shouldThrowException();
  }

  public void testShouldFail8() {
    try {
      ParserContext pCtx = new ParserContext();
      pCtx.setStrongTyping(true);

      MVEL.compileExpression("for (String s : new java.util.HashMap()) { }", pCtx);
    }
    catch (Exception e) {
      //   e.printStackTrace();
      return;
    }

    shouldThrowException();
  }

  public void testShouldFail9() {
    try {

      MVEL.compileExpression("foo = ", new HashMap());
    }
    catch (Exception e) {
      e.printStackTrace();
      return;
    }

    shouldThrowException();
  }

  public void testShouldFail10() {
    try {
      MVEL.compileExpression("foo = [1,1,qq,zz]", ParserContext.create().stronglyTyped());
    }
    catch (CompileException e) {
      e.printStackTrace();
      return;
    }

    shouldThrowException();
  }


  public void testShouldFailCleanly() {
    try {
      MVEL.eval("6:f-\t\n" +
          "r:wsGbL%X5&wb<C.8n(\n" +
          "1X-;zUX-L<%<TG)S#z1fh69:tn`#eH9XneL8XFIB94-z*XzQ-RPhX{&bbp{fLDe@`3<-\tZp9_k*Xo\tDy36t15XX>-75EArR]6`**<kw}P<FpD-+XA-<+K!\n" +
          "Rb9n)zA-<L9pIIbHb?!b-bO%n<zLFqzbmm-n#~-zL`5Fq_PYD+9`NPt4Tnt\tvT-L[d\n" +
          "<b<9l9XV9-9 X9h7#9Ln?FnnLXLkg5<V-Z%bb-n&Et<B-X[n\"jbvg&@b{X0?*9eC{%zU\n" +
          "L\t{RPX\tbwhY&L`z<`Oh`<8pH\n" +
          "b:y:#H-;&,PzXw\ttHicFbs");
    }
    catch (CompileException e) {
      return;
    }
    catch (Exception e) {
      e.printStackTrace();
      fail("Unclean failure");
    }
  }

  public void testShouldFail11() {
    try {
      MVEL.eval("for (int i = 0; i--; i++) {}");
    }
    catch (Exception e) {
      e.printStackTrace();
      return;
    }

    shouldThrowException();
  }

  public void testShouldFail12() {
    try {
      MVEL.eval("def x(_");
    }
    catch (Exception e) {
      e.printStackTrace();
      return;
    }

  }

}
