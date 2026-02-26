package org.mvel3.parser.antlr4.mveltojavaparser.expressions;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.type.IntersectionType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.Mvel3ParserBaseVisitor;
import org.mvel3.parser.antlr4.mveltojavaparser.TokenRangeConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.TypeConverter;

public final class CastExpressionConverter {

    private CastExpressionConverter() {
    }

    public static Node convertCastExpression(
            final Mvel3Parser.CastExpressionContext ctx,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        NodeList<Type> parsedTypes = new NodeList<>();
        for (Mvel3Parser.TypeTypeContext typeCtx : ctx.typeType()) {
            parsedTypes.add((Type) TypeConverter.convertTypeType(typeCtx, mvel3toJavaParserVisitor));
        }

        Type targetType;
        if (parsedTypes.size() == 1) {
            targetType = parsedTypes.get(0);
        } else {
            NodeList<ReferenceType> referenceTypes = new NodeList<>();
            for (Type type : parsedTypes) {
                if (!(type instanceof ReferenceType)) {
                    throw new IllegalArgumentException("Intersection casts require reference types: " + ctx.getText());
                }
                referenceTypes.add((ReferenceType) type);
            }
            IntersectionType intersectionType = new IntersectionType(referenceTypes);
            intersectionType.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            targetType = intersectionType;
        }

        Expression expression = (Expression) mvel3toJavaParserVisitor.visit(ctx.expression());

        CastExpr castExpr = new CastExpr(targetType, expression);
        castExpr.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return castExpr;
    }
}
