/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates.
 *
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

package org.mvel3.parser.antlr4;

import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Ignore;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mvel3.parser.antlr4.ParserTestUtil.assertParsedExpressionRoundTrip;
import static org.mvel3.parser.antlr4.ParserTestUtil.getBinaryOperatorExpressionContext;

/**
 * Tests for the MVEL parser using ANTLR4.
 * Assertions are based on the generated Antlr AST.
 */
public class Antlr4MvelParserTest {

    @Test
    public void testParseSimpleExpr() {
        String expr = "name == \"Mark\"";
        ParseTree tree = Antlr4MvelParser.parseExpressionAsAntlrAST(expr);

        Mvel3Parser.BinaryOperatorExpressionContext binary = getBinaryOperatorExpressionContext((Mvel3Parser.MvelStartContext) tree);

        // Assert on the actual AST nodes
        assertThat(binary.EQUAL()).isNotNull();
        assertThat(binary.EQUAL().getText()).isEqualTo("==");

        // Should have relational expression on the right (for "Mark")
        assertThat(binary.expression(0).getText()).isEqualTo("name");
        assertThat(binary.expression(1).getText()).isEqualTo("\"Mark\"");

        // Verify the complete expression text
        assertThat(binary.getText()).isEqualTo("name==\"Mark\"");
    }

    @Test
    public void testBinaryWithNewLine() {
        String orExpr = "(addresses == 2 ||\n" +
                "                   addresses == 3  )";
        assertParsedExpressionRoundTrip(orExpr);

        String andExpr = "(addresses == 2 &&\n addresses == 3  )";
        assertParsedExpressionRoundTrip(andExpr);
    }

    @Test
    public void testBinaryWithWindowsNewLine() {
        String orExpr = "(addresses == 2 ||\r\n" +
                "                   addresses == 3  )";
        assertParsedExpressionRoundTrip(orExpr);

        String andExpr = "(addresses == 2 &&\r\n addresses == 3  )";
        assertParsedExpressionRoundTrip(andExpr);
    }

    @Test
    public void testBinaryWithNewLineBeginning() {
        String orExpr = "(" + System.lineSeparator() + "addresses == 2 || addresses == 3  )";
        assertParsedExpressionRoundTrip(orExpr);

        String andExpr = "(" + System.lineSeparator() + "addresses == 2 && addresses == 3  )";
        assertParsedExpressionRoundTrip(andExpr);
    }

    @Test
    public void testBinaryWithNewLineEnd() {
        String orExpr = "(addresses == 2 || addresses == 3 " + System.lineSeparator() + ")";
        assertParsedExpressionRoundTrip(orExpr);

        String andExpr = "(addresses == 2 && addresses == 3 " + System.lineSeparator() + ")";
        assertParsedExpressionRoundTrip(andExpr);
    }

    @Test
    public void testBinaryWithNewLineBeforeOperator() {
        String andExpr = "(addresses == 2" + System.lineSeparator() + "&& addresses == 3  )";
        assertParsedExpressionRoundTrip(andExpr);

        String orExpr = "(addresses == 2" + System.lineSeparator() + "|| addresses == 3  )";
        assertParsedExpressionRoundTrip(andExpr);
    }

    @Test
    public void testParseSafeCastExpr() {
        String expr = "this instanceof Person && ((Person) this).name == \"Mark\"";
        assertParsedExpressionRoundTrip(expr);
    }

    @Ignore("Inline Cast : TBD")
    @Test
    public void testParseInlineCastExpr() {
        String expr = "this#Person.name == \"Mark\"";
        assertParsedExpressionRoundTrip(expr);
    }

    @Ignore("Inline Cast : TBD")
    @Test
    public void testParseInlineCastExpr2() {
        String expr = "address#com.pkg.InternationalAddress.state.length == 5";
        assertParsedExpressionRoundTrip(expr);
    }

    @Ignore("Inline Cast : TBD")
    @Test
    public void testParseInlineCastExpr3() {
        String expr = "address#org.mvel3.compiler.LongAddress.country.substring(1)";
        assertParsedExpressionRoundTrip(expr);
    }

    @Ignore("Inline Cast : TBD")
    @Test
    public void testParseInlineCastExpr4() {
        String expr = "address#com.pkg.InternationalAddress.getState().length == 5";
        assertParsedExpressionRoundTrip(expr);
    }

    @Ignore("`!.` Null Safe Dereferencing : TBD. Mvel2 has `.?` syntax, but skipping for now")
    @Test
    public void testParseNullSafeFieldAccessExpr() {
        String expr = "person!.name == \"Mark\"";
        assertParsedExpressionRoundTrip(expr);
    }

    @Ignore("Custom Operator : TBD")
    @Test
    public void testDotFreeExpr() {
        String expr = "this after $a";
        assertParsedExpressionRoundTrip(expr);
    }

    @Ignore("Custom Operator : TBD")
    @Test
    public void testDotFreeEnclosed() {
        String expr = "(this after $a)";
        assertParsedExpressionRoundTrip(expr);
    }

    @Ignore("Custom Operator : TBD")
    @Test
    public void testDotFreeEnclosedWithNameExpr() {
        String expr = "(something after $a)";
        assertParsedExpressionRoundTrip(expr);
    }

    @Test
    public void testLiteral() {
        String bigDecimalLiteral = "bigDecimal < 50B";
        assertParsedExpressionRoundTrip(bigDecimalLiteral);

        String bigIntegerLiteral = "bigInteger == 50I";
        assertParsedExpressionRoundTrip(bigIntegerLiteral);
    }

    @Test
    public void testBigDecimalLiteral() {
        String expr = "12.111B";
        assertParsedExpressionRoundTrip(expr);
    }
}