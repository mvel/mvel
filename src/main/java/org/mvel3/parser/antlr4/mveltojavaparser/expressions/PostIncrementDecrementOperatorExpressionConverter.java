package org.mvel3.parser.antlr4.mveltojavaparser.expressions;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.UnaryExpr;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.Mvel3ToJavaParserVisitor;
import org.mvel3.parser.antlr4.mveltojavaparser.TokenRangeConverter;

public final class PostIncrementDecrementOperatorExpressionConverter {

    private PostIncrementDecrementOperatorExpressionConverter() {
    }

    public static Node convertPostIncrementDecrementOperatorExpression(
            final Mvel3Parser.PostIncrementDecrementOperatorExpressionContext ctx,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        // Handle post-increment and post-decrement: expression++ or expression--
        Expression operand = (Expression) mvel3toJavaParserVisitor.visit(ctx.expression());
        String operator = ctx.postfix.getText();

        UnaryExpr.Operator unaryOp;
        if ("++".equals(operator)) {
            unaryOp = UnaryExpr.Operator.POSTFIX_INCREMENT;
        } else if ("--".equals(operator)) {
            unaryOp = UnaryExpr.Operator.POSTFIX_DECREMENT;
        } else {
            throw new IllegalArgumentException("Unknown post-increment/decrement operator: " + operator);
        }

        UnaryExpr unaryExpr = new UnaryExpr(operand, unaryOp);
        unaryExpr.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return unaryExpr;
    }
}
