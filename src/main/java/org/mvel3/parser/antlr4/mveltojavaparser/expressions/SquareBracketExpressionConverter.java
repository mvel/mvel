package org.mvel3.parser.antlr4.mveltojavaparser.expressions;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.Expression;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.Mvel3ParserBaseVisitor;
import org.mvel3.parser.antlr4.mveltojavaparser.TokenRangeConverter;

public final class SquareBracketExpressionConverter {

    private SquareBracketExpressionConverter() {
    }

    public static Node convertSquareBracketExpression(
            final Mvel3Parser.SquareBracketExpressionContext ctx,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        // Handle array/list access: expression[index]
        Expression array = (Expression) mvel3toJavaParserVisitor.visit(ctx.expression(0)); // The array/list expression
        Expression index = (Expression) mvel3toJavaParserVisitor.visit(ctx.expression(1)); // The index expression

        // Create ArrayAccessExpr like mvel.jj does
        // The transformation to .get() method calls is handled by MVELToJavaRewriter
        ArrayAccessExpr arrayAccess = new ArrayAccessExpr(array, index);
        arrayAccess.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return arrayAccess;
    }
}
