package org.mvel3.parser.antlr4.mveltojavaparser.expressions;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.Expression;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.Mvel3ParserBaseVisitor;
import org.mvel3.parser.antlr4.mveltojavaparser.TokenRangeConverter;

public final class TernaryExpressionConverter {

    private TernaryExpressionConverter() {
    }

    public static Node convertTernaryExpression(
            final Mvel3Parser.TernaryExpressionContext ctx,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        Expression condition = (Expression) mvel3toJavaParserVisitor.visit(ctx.expression(0));
        Expression thenExpr = (Expression) mvel3toJavaParserVisitor.visit(ctx.expression(1));
        Expression elseExpr = (Expression) mvel3toJavaParserVisitor.visit(ctx.expression(2));

        ConditionalExpr conditionalExpr = new ConditionalExpr(condition, thenExpr, elseExpr);
        conditionalExpr.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return conditionalExpr;
    }
}
