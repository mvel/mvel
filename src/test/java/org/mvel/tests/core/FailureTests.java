package org.mvel.tests.core;

/**
 * Tests to ensure MVEL fails when it should.
 */
public class FailureTests extends AbstractTest {
//    public void testUnknownToken() {
//        testForFailure("someUnknownToken");
//    }
//
//    public void testIncompleteStatement() {
//        testForFailure("1 +");
//    }
//
//
//    private void testForFailure(String ex) {
//        try {
//            test(ex);
//
//        }
//        catch (Throwable e) {
//            e.printStackTrace();
//            return;
//        }
//
//        throw new AssertionError();
//
//    }


    public void testBadParserConstruct() {
       try {
          test("a = 0; a =+++ 5;");
       }
       catch (Exception e) {
           System.out.println(e);
       }

    }
}
