/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mvel3.compat.mvel2;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mvel3.MVEL;
import org.mvel3.compat.mvel2.res.KnowledgeHelperFixer;
import org.mvel3.compat.mvel2.res.Foo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.mvel3.compat.mvel2.Mvel3TestSupport.compileExpression;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.eval;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.executeExpression;

/**
 * Imported from org.mvel2.tests.core.CommentParsingTests (mvel2 branch).
 *
 * <p>Expressions, input values, expected values, and assertions are retained
 * unchanged. Failures are intentionally visible; known behavioural differences
 * are annotated with {@link KnownCompatibility}.
 *
 * <p>{@code KnowledgeHelperFixer} is re-implemented without {@code MVEL.parseMacros}
 * (see {@code res/KnowledgeHelperFixer.java}).  {@code testInExpressionComment} uses
 * {@code ParserContext.create().stronglyTyped()} which has no MVEL3 equivalent and
 * is annotated accordingly.
 */
@Tag("mvel2-compatibility")
public class CommentParsingTests extends AbstractTest {

    // -------------------------------------------------------------------------
    // Comment parsing — eager block compilation (no evaluation or value assertion)
    // -------------------------------------------------------------------------

    private static void compileBlock(final String expression) {
        new MVEL().compileMapBlock(expression, Object.class, Set.of(), Map.of());
    }

    @Test
    @KnownCompatibility("block-syntax")
    public void testOKQuoteComment() throws Exception {
        compileBlock("// ' this is OK!");
        compileBlock("// ' this is OK!\n");
        compileBlock("// ' this is OK!\nif(1==1) {};");
    }

    @Test
    @KnownCompatibility("block-syntax")
    public void testOKDblQuoteComment() throws Exception {
        compileBlock("// \" this is OK!");
        compileBlock("// \" this is OK!\n");
        compileBlock("// \" this is OK!\nif(1==1) {};");
    }

    @Test
    @KnownCompatibility("block-syntax")
    public void testIfComment() throws Exception {
        compileBlock("if(1 == 1) {\n" + "  // Quote & Double-quote seem to break this expression\n" + "}");
    }

    @Test
    @KnownCompatibility("block-syntax")
    public void testIfQuoteCommentBug() throws Exception {
        compileBlock("if(1 == 1) {\n" + "  // ' seems to break this expression\n" + "}");
    }

    @Test
    @KnownCompatibility("block-syntax")
    public void testIfDblQuoteCommentBug() throws Exception {
        compileBlock("if(1 == 1) {\n" + "  // ' seems to break this expression\n" + "}");
    }

    // -------------------------------------------------------------------------
    // foreach — MVEL3 does not yet support MVEL2's foreach syntax
    // -------------------------------------------------------------------------

    @Test
    @KnownCompatibility("foreach-syntax")
    public void testForEachQuoteCommentBug() throws Exception {
        compileBlock("foreach ( item : 10 ) {\n" + "  // The ' character causes issues\n" + "}");
    }

    @Test
    @KnownCompatibility("foreach-syntax")
    public void testForEachDblQuoteCommentBug() throws Exception {
        compileBlock("foreach ( item : 10 ) {\n" + "  // The \" character causes issues\n" + "}");
    }

    @Test
    @KnownCompatibility("foreach-syntax")
    public void testForEachCommentOK() throws Exception {
        compileBlock("foreach ( item : 10 ) {\n" + "  // The quote & double quote characters cause issues\n" + "}");
    }

    // -------------------------------------------------------------------------
    // else-if with comments — root cause: 'Got here!' is a multi-char
    // single-quoted literal. MVEL3 treats it as a char, which corrupts the
    // JavaParser AST and produces a null-node NPE during transpilation.
    // -------------------------------------------------------------------------

    @Test
    @KnownCompatibility("single-quote-string")
    public void testElseIfCommentBugPreCompiled() throws Exception {
        executeExpression(compileExpression("// This is never true\n" + "if (1==0) {\n"
            + "  // Never reached\n" + "}\n" + "// This is always true...\n" + "else if (1==1) {"
            + "  System.out.println('Got here!');" + "}\n"));
    }

    @Test
    @KnownCompatibility("single-quote-string")
    public void testElseIfCommentBugEvaluated() throws Exception {
        eval("// This is never true\n" + "if (1==0) {\n" + "  // Never reached\n" + "}\n"
            + "// This is always true...\n" + "else if (1==1) {" + "  System.out.println('Got here!');" + "}\n");
    }

    // -------------------------------------------------------------------------
    // KnowledgeHelperFixer — string manipulation, no MVEL evaluation
    // -------------------------------------------------------------------------

    private static final KnowledgeHelperFixer fixer = new KnowledgeHelperFixer();

    @Test
    public void testSingleLineCommentSlash() {
        String result = fixer.fix("        //System.out.println( \"help\" );\r\n      " +
            "  System.out.println( \"help\" );  \r\n     list.add( $person );");
        assertEquals("        //System.out.println( \"help\" );\r\n        System.out.println( \"help\" );  \r\n   " +
            "  list.add( $person );",
            result);
    }

    @Test
    public void testSingleLineCommentHash() {
        String result = fixer.fix("        #System.out.println( \"help\" );\r\n    " +
            "    System.out.println( \"help\" );  \r\n     list.add( $person );");
        assertEquals("        #System.out.println( \"help\" );\r\n        System.out.println( \"help\" );  \r\n    " +
            " list.add( $person );",
            result);
    }

    @Test
    public void testMultiLineComment() {
        String result = fixer.fix("        /*System.out.println( \"help\" );\r\n*/    " +
            "   System.out.println( \"help\" );  \r\n     list.add( $person );");
        assertEquals("        /*System.out.println( \"help\" );\r\n*/       System.out.println( \"help\" );  \r\n    " +
            " list.add( $person );",
            result);
    }

    // -------------------------------------------------------------------------
    // Comment parsing with value assertions
    // -------------------------------------------------------------------------

    @Test
    public void testComments() {
        assertEquals(10,
            test("// This is a comment\n5 + 5"));
    }

    @Test
    @KnownCompatibility("block-syntax")
    public void testComments2() {
        assertEquals(20,
            test("10 + 10; // This is a comment"));
    }

    @Test
    public void testComments3() {
        assertEquals(30,
            test("/* This is a test of\r\n" + "MVEL's support for\r\n" + "multi-line comments\r\n" + "*/\r\n 15 + 15"));
    }

    @Test
    @KnownCompatibility("block-syntax")
    public void testComments4() {
        assertEquals(((10 + 20) * 2) - 10,
            test("/** This is a fun test script **/\r\n" + "a = 10;\r\n" + "/**\r\n"
                + "* Here is a useful variable\r\n" + "*/\r\n" + "b = 20; // set b to '20'\r\n"
                + "return ((a + b) * 2) - 10;\r\n" + "// last comment\n"));
    }

    @Test
    public void testComments5() {
        assertEquals("dog",
            test("foo./*Hey!*/name"));
    }

    @Test
    @KnownCompatibility("inline-import")
    public void testMultiLineCommentInList() {
        assertEquals(Arrays.asList(new Integer[]{10, 20}),
            test("import " + Foo.class.getName() + ";\n [ 10, 20 /* ... */ ]"));
    }

    @Test
    public void testInExpressionComment() {
        // ParserContext.create().stronglyTyped() not reproduced; expression compiles in MVEL3 regardless
        executeExpression(compileExpression("new String /*XXX*/(\"foo\")"));
        executeExpression(compileExpression("new String/*XXX*/(\"foo\")"));
    }

    @Test
    @KnownCompatibility("block-syntax")
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

        final Object o = eval(ex, new HashMap<String, Object>());
        assertEquals("[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]", o);
    }
}
