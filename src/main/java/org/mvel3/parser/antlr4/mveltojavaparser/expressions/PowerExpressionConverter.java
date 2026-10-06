package org.mvel3.parser.antlr4.mveltojavaparser.expressions;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.Mvel3ToJavaParserVisitor;
import org.mvel3.parser.antlr4.mveltojavaparser.TokenRangeConverter;

/**
 * Converts the MVEL power expression {@code a ** b} to {@code Math.pow(a, b)}.
 */
public final class PowerExpressionConverter {

    private PowerExpressionConverter() {
    }

    public static Node convertPowerExpression(
            final Mvel3Parser.PowerExpressionContext ctx,
            final Mvel3ToJavaParserVisitor visitor) {
        Expression base = (Expression) visitor.visit(ctx.expression(0));
        Expression exponent = (Expression) visitor.visit(ctx.expression(1));

        // a ** b  →  Math.pow(a, b)
        return new MethodCallExpr(
                TokenRangeConverter.createTokenRange(ctx),
                new NameExpr("Math"),
                null,
                new com.github.javaparser.ast.expr.SimpleName("pow"),
                new NodeList<>(base, exponent));
    }
}
