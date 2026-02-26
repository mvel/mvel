package org.mvel3.parser.antlr4.mveltojavaparser.expressions;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.type.Type;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.Mvel3ParserBaseVisitor;
import org.mvel3.parser.antlr4.mveltojavaparser.ArgumentsConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.TokenRangeConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.TypeConverter;

public final class MethodReferenceExpressionConverter {

    private MethodReferenceExpressionConverter() {
    }

    public static Node convertMethodReferenceExpression(
            final Mvel3Parser.MethodReferenceExpressionContext ctx,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        Expression scope;
        if (ctx.expression() != null) {
            // expression '::' typeArguments? identifier
            scope = (Expression) mvel3toJavaParserVisitor.visit(ctx.expression());
        } else if (ctx.classType() != null) {
            // classType '::' typeArguments? NEW
            Type type = (Type) TypeConverter.convertClassType(ctx.classType(), mvel3toJavaParserVisitor);
            scope = new TypeExpr(type);
        } else if (ctx.typeType() != null) {
            // typeType '::' (typeArguments? identifier | NEW)
            Type type = (Type) TypeConverter.convertTypeType(ctx.typeType(), mvel3toJavaParserVisitor);
            scope = new TypeExpr(type);
        } else {
            throw new IllegalArgumentException("Unsupported method reference: " + ctx.getText());
        }

        // Identifier is the method name, or "new" for constructor references
        String identifier = ctx.NEW() != null ? "new" : ctx.identifier().getText();

        // Handle type arguments if present
        NodeList<Type> typeArguments = ctx.typeArguments() != null ?
                ArgumentsConverter.convertTypeArguments(ctx.typeArguments(), mvel3toJavaParserVisitor) :
                null;

        MethodReferenceExpr methodRef = new MethodReferenceExpr(scope, typeArguments, identifier);
        methodRef.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return methodRef;
    }
}
