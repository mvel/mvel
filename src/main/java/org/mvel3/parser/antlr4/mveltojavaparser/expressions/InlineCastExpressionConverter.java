package org.mvel3.parser.antlr4.mveltojavaparser.expressions;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.type.Type;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.Mvel3ParserBaseVisitor;
import org.mvel3.parser.antlr4.Mvel3ToJavaParserVisitor;
import org.mvel3.parser.antlr4.mveltojavaparser.ArgumentsConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.TokenRangeConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.TypeConverter;
import org.mvel3.parser.ast.expr.InlineCastExpr;

public final class InlineCastExpressionConverter {

    private InlineCastExpressionConverter() {
    }

    public static Node convertInlineCastExpression(
            final Mvel3Parser.InlineCastExpressionContext ctx,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        // Handle inline cast: expr#Type#[member]
        Expression scope = (Expression) mvel3toJavaParserVisitor.visit(ctx.expression(0));
        Type type = (Type) TypeConverter.convertTypeType(ctx.typeType(), mvel3toJavaParserVisitor);

        InlineCastExpr inlineCastExpr = new InlineCastExpr(type, scope);
        inlineCastExpr.setTokenRange(TokenRangeConverter.createTokenRange(ctx));

        // TODO fix this hack for Tolerant visitor to work.
        ((Mvel3ToJavaParserVisitor) mvel3toJavaParserVisitor).associateAntlrTokenWithJPNode(ctx.identifier(), inlineCastExpr);

        if (ctx.identifier() != null) {
            String name = ctx.identifier().getText();
            if (ctx.arguments() != null) {
                MethodCallExpr methodCall = new MethodCallExpr(inlineCastExpr, name);
                methodCall.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
                methodCall.setArguments(ArgumentsConverter.convertArguments(ctx.arguments(), mvel3toJavaParserVisitor));
                return methodCall;
            }

            FieldAccessExpr fieldAccess = new FieldAccessExpr(inlineCastExpr, name);
            fieldAccess.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return fieldAccess;
        }

        if (ctx.LBRACK() != null) {
            Expression indexExpr = (Expression) mvel3toJavaParserVisitor.visit(ctx.expression(1));
            MethodCallExpr methodCall = new MethodCallExpr(inlineCastExpr, "get");
            methodCall.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            methodCall.addArgument(indexExpr);
            return methodCall;
        }

        return inlineCastExpr;
    }
}
