package org.mvel3.parser.antlr4.mveltojavaparser.expressions;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.Mvel3ParserBaseVisitor;
import org.mvel3.parser.antlr4.mveltojavaparser.TokenRangeConverter;
import org.mvel3.parser.ast.expr.MapCreationLiteralExpression;
import org.mvel3.parser.ast.expr.MapCreationLiteralExpressionKeyValuePair;

public final class MapCreationLiteralConverter {

    private MapCreationLiteralConverter() {
    }

    public static Node convertMapCreationLiteral(
            final Mvel3Parser.MapCreationLiteralContext ctx,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        NodeList<Expression> entries = new NodeList<>();

        // Check for empty map syntax [:]
        if (ctx.COLON() != null && ctx.mapEntry().isEmpty()) {
            // Empty map
            MapCreationLiteralExpression mapExpr = new MapCreationLiteralExpression(entries);
            mapExpr.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return mapExpr;
        }

        // Process each map entry
        if (ctx.mapEntry() != null) {
            for (Mvel3Parser.MapEntryContext entryCtx : ctx.mapEntry()) {
                Expression key = (Expression) mvel3toJavaParserVisitor.visit(entryCtx.expression(0));
                Expression value = (Expression) mvel3toJavaParserVisitor.visit(entryCtx.expression(1));

                // Wrap in MapCreationLiteralExpressionKeyValuePair as per mvel.jj
                MapCreationLiteralExpressionKeyValuePair pair =
                        new MapCreationLiteralExpressionKeyValuePair(key, value);
                pair.setTokenRange(TokenRangeConverter.createTokenRange(entryCtx));
                entries.add(pair);
            }
        }

        MapCreationLiteralExpression mapExpr = new MapCreationLiteralExpression(entries);
        mapExpr.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return mapExpr;
    }
}
