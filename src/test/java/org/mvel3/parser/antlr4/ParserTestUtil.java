package org.mvel3.parser.antlr4;

import org.antlr.v4.runtime.tree.ParseTree;

import static org.assertj.core.api.Assertions.assertThat;

public class ParserTestUtil {

    // Navigate to equality expression from root for simple expressions
    static Mvel3Parser.EqualityExpressionContext getEqualityExpressionContext(Mvel3Parser.MvelStartContext tree) {
        Mvel3Parser.MvelExpressionContext exprCtx = tree.mvelExpression();
        Mvel3Parser.ConditionalExpressionContext condCtx = exprCtx.conditionalExpression();
        Mvel3Parser.ConditionalOrExpressionContext orCtx = condCtx.conditionalOrExpression();
        Mvel3Parser.ConditionalAndExpressionContext andCtx = orCtx.conditionalAndExpression();
        Mvel3Parser.InclusiveOrExpressionContext incOrCtx = andCtx.inclusiveOrExpression();
        Mvel3Parser.ExclusiveOrExpressionContext excOrCtx = incOrCtx.exclusiveOrExpression();
        Mvel3Parser.AndExpressionContext andExprCtx = excOrCtx.andExpression();
        return andExprCtx.equalityExpression();
    }

    static void assertParsedExpressionRoundTrip(String expr) {
        ParseTree tree = Antlr4MvelParser.parseExpression(expr);
        Mvel3Parser.MvelStartContext startCtx = (Mvel3Parser.MvelStartContext) tree;
        assertThat(startCtx).isNotNull();
        assertThat(startCtx.mvelExpression().getText()).isEqualToIgnoringWhitespace(expr);
    }
}