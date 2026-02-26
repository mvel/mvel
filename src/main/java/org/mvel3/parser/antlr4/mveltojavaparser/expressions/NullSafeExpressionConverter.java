package org.mvel3.parser.antlr4.mveltojavaparser.expressions;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.type.Type;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.Mvel3ParserBaseVisitor;
import org.mvel3.parser.antlr4.mveltojavaparser.ArgumentsConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.TokenRangeConverter;
import org.mvel3.parser.ast.expr.NullSafeFieldAccessExpr;
import org.mvel3.parser.ast.expr.NullSafeMethodCallExpr;

public final class NullSafeExpressionConverter {

    private NullSafeExpressionConverter() {
    }

    public static Node convertNullSafeExpression(
            final Mvel3Parser.NullSafeExpressionContext ctx,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        // Extract the scope (left side of !.)
        Expression scope = (Expression) mvel3toJavaParserVisitor.visit(ctx.expression());

        // Extract the identifier name
        String name = ctx.identifier().getText();

        // Check if there are arguments (method call) or not (field access)
        if (ctx.arguments() != null) {
            // Method call: $p!.getName()
            NodeList<Expression> arguments = new NodeList<>();
            if (ctx.arguments().expressionList() != null) {
                for (Mvel3Parser.ExpressionContext argCtx : ctx.arguments().expressionList().expression()) {
                    arguments.add((Expression) mvel3toJavaParserVisitor.visit(argCtx));
                }
            }

            // Extract type arguments if present
            NodeList<Type> typeArguments = ctx.typeArguments() != null ? ArgumentsConverter.convertTypeArguments(ctx.typeArguments(), mvel3toJavaParserVisitor) : null;

            NullSafeMethodCallExpr methodCall = new NullSafeMethodCallExpr(
                    scope,
                    typeArguments,
                    name,
                    arguments
            );
            methodCall.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return methodCall;
        } else {
            // Field access: $p!.name
            NullSafeFieldAccessExpr fieldAccess = new NullSafeFieldAccessExpr(scope, name);
            fieldAccess.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return fieldAccess;
        }
    }
}
