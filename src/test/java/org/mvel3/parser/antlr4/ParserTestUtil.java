package org.mvel3.parser.antlr4;

import org.antlr.v4.runtime.tree.ParseTree;

import static org.assertj.core.api.Assertions.assertThat;

public class ParserTestUtil {

    static Mvel3Parser.BinaryOperatorExpressionContext getBinaryOperatorExpressionContext(Mvel3Parser.MvelStartContext tree) {
        Mvel3Parser.MvelExpressionContext exprCtx = tree.mvelExpression();
        Mvel3Parser.ExpressionContext expression = exprCtx.expression();
        if (expression instanceof Mvel3Parser.BinaryOperatorExpressionContext) {
            return (Mvel3Parser.BinaryOperatorExpressionContext) expression;
        } else {
            throw new IllegalStateException("Expected BinaryOperatorExpressionContext, but got: " + expression.getClass().getSimpleName());
        }
    }

    static void assertParsedExpressionRoundTrip(String expr) {
        ParseTree tree = Antlr4MvelParser.parseExpressionAsAntlrAST(expr);
        Mvel3Parser.MvelStartContext startCtx = (Mvel3Parser.MvelStartContext) tree;
        assertThat(startCtx).isNotNull();
        assertThat(startCtx.mvelExpression().getText()).isEqualToIgnoringWhitespace(expr);
    }
}