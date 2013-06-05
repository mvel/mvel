package org.mvel2.tests.core;

import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.tests.core.res.Foo;
import org.mvel2.tests.core.res.KnowledgeHelperFixer;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;

import static org.mvel2.MVEL.compileExpression;
import static org.mvel2.MVEL.executeExpression;

/**
 * @author Mike Brock .
 */
public class CommentParsingTests extends AbstractTest {
  public void testOKQuoteComment() throws Exception {
    // ' in comments outside of blocks seem OK
    compileExpression("// ' this is OK!");
    compileExpression("// ' this is OK!\n");
    compileExpression("// ' this is OK!\nif(1==1) {};");
  }

  public void testOKDblQuoteComment() throws Exception {
    // " in comments outside of blocks seem OK
    compileExpression("// \" this is OK!");
    compileExpression("// \" this is OK!\n");
    compileExpression("// \" this is OK!\nif(1==1) {};");
  }

  public void testIfComment() throws Exception {
    // No quote?  OK!
    compileExpression("if(1 == 1) {\n" + "  // Quote & Double-quote seem to break this expression\n" + "}");
  }

  public void testIfQuoteCommentBug() throws Exception {
    // Comments in an if seem to fail if they contain a '
    compileExpression("if(1 == 1) {\n" + "  // ' seems to break this expression\n" + "}");
  }

  public void testIfDblQuoteCommentBug() throws Exception {
    // Comments in a foreach seem to fail if they contain a '
    compileExpression("if(1 == 1) {\n" + "  // ' seems to break this expression\n" + "}");
  }

  public void testForEachQuoteCommentBug() throws Exception {
    // Comments in a foreach seem to fail if they contain a '
    compileExpression("foreach ( item : 10 ) {\n" + "  // The ' character causes issues\n" + "}");
  }

  public void testForEachDblQuoteCommentBug() throws Exception {
    // Comments in a foreach seem to fail if they contain a '
    compileExpression("foreach ( item : 10 ) {\n" + "  // The \" character causes issues\n" + "}");
  }

  public void testForEachCommentOK() throws Exception {
    // No quote?  OK!
    compileExpression("foreach ( item : 10 ) {\n" + "  // The quote & double quote characters cause issues\n" + "}");
  }

  public void testElseIfCommentBugPreCompiled() throws Exception {
    // Comments can't appear before else if() - compilation works, but evaluation fails
    executeExpression(compileExpression("// This is never true\n" + "if (1==0) {\n"
        + "  // Never reached\n" + "}\n" + "// This is always true...\n" + "else if (1==1) {"
        + "  System.out.println('Got here!');" + "}\n"));
  }

  public void testElseIfCommentBugEvaluated() throws Exception {
    // Comments can't appear before else if()
    MVEL.eval("// This is never true\n" + "if (1==0) {\n" + "  // Never reached\n" + "}\n"
        + "// This is always true...\n" + "else if (1==1) {" + "  System.out.println('Got here!');" + "}\n");
  }


  private static final KnowledgeHelperFixer fixer = new KnowledgeHelperFixer();

  public void testSingleLineCommentSlash() {
    String result = fixer.fix("        //System.out.println( \"help\" );\r\n      " +
        "  System.out.println( \"help\" );  \r\n     list.add( $person );");
    assertEquals("        //System.out.println( \"help\" );\r\n        System.out.println( \"help\" );  \r\n   " +
        "  list.add( $person );",
        result);
  }

  public void testSingleLineCommentHash() {
    String result = fixer.fix("        #System.out.println( \"help\" );\r\n    " +
        "    System.out.println( \"help\" );  \r\n     list.add( $person );");
    assertEquals("        #System.out.println( \"help\" );\r\n        System.out.println( \"help\" );  \r\n    " +
        " list.add( $person );",
        result);
  }

  public void testMultiLineComment() {
    String result = fixer.fix("        /*System.out.println( \"help\" );\r\n*/    " +
        "   System.out.println( \"help\" );  \r\n     list.add( $person );");
    assertEquals("        /*System.out.println( \"help\" );\r\n*/       System.out.println( \"help\" );  \r\n    " +
        " list.add( $person );",
        result);
  }

  public void testComments() {
    assertEquals(10,
        test("// This is a comment\n5 + 5"));
  }

  public void testComments2() {
    assertEquals(20,
        test("10 + 10; // This is a comment"));
  }

  public void testComments3() {
    assertEquals(30,
        test("/* This is a test of\r\n" + "MVEL's support for\r\n" + "multi-line comments\r\n" + "*/\r\n 15 + 15"));
  }

  public void testComments4() {
    assertEquals(((10 + 20) * 2) - 10,
        test("/** This is a fun test script **/\r\n" + "a = 10;\r\n" + "/**\r\n"
            + "* Here is a useful variable\r\n" + "*/\r\n" + "b = 20; // set b to '20'\r\n"
            + "return ((a + b) * 2) - 10;\r\n" + "// last comment\n"));
  }

  public void testComments5() {
    assertEquals("dog",
        test("foo./*Hey!*/name"));
  }

  public void testMultiLineCommentInList() {
    assertEquals(Arrays.asList(new Integer[]{10, 20}),
        test("import " + Foo.class.getName() + ";\n [ 10, 20 /* ... */ ]"));

//    assertEquals(Arrays.asList(new Integer[]{10, 20}),
//        test("import " + Foo.class.getName() + ";\n [ 10, 20           ]"));
  }

  public void testInExpressionComment() {

    Serializable s1 = MVEL.compileExpression("new String /*XXX*/(\"foo\")",

        ParserContext.create().stronglyTyped());

    MVEL.executeExpression(s1);


    Serializable s2 = MVEL.compileExpression("new String/*XXX*/(\"foo\")",

        ParserContext.create().stronglyTyped());

    MVEL.executeExpression(s2);

  }

  public void testComments6() {
    String ex = "//This is an array\n" +
        "long[] arr = [ //start of array\n" +
        "\t1,2, // one and two\n" +
        "\t3, /*three*/\n" +
        "\t4, /*four*/ 5,\n" +
        "\t6, /*six*/ 7,/*seven*/ //six & seven\n" +
        "\t8/*eight*/  \n" +
        "\t,9,\n" +
        "\t10 //ten\n" +
        "]; //end of array\n" +
        "java.util.Arrays.toString(arr)";

    final Object o = MVEL.eval(ex, new HashMap<String, Object>());

    assertEquals("[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]", o);
  }
}
