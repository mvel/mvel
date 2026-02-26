package org.mvel3.parser.antlr4.mveltojavaparser.expressions;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.Mvel3ParserBaseVisitor;
import org.mvel3.parser.antlr4.mveltojavaparser.TokenRangeConverter;
import org.mvel3.parser.ast.expr.ListCreationLiteralExpression;
import org.mvel3.parser.ast.expr.ListCreationLiteralExpressionElement;

public final class ListCreationLiteralConverter {

    private ListCreationLiteralConverter() {
    }

    public static Node convertListCreationLiteral(
            final Mvel3Parser.ListCreationLiteralContext ctx,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        NodeList<Expression> elements = new NodeList<>();

        // Process each list element
        if (ctx.listElement() != null) {
            for (Mvel3Parser.ListElementContext elementCtx : ctx.listElement()) {
                Expression expr = (Expression) mvel3toJavaParserVisitor.visit(elementCtx.expression());
                // Wrap in ListCreationLiteralExpressionElement as per mvel.jj
                ListCreationLiteralExpressionElement element =
                        new ListCreationLiteralExpressionElement(expr);
                element.setTokenRange(TokenRangeConverter.createTokenRange(elementCtx));
                elements.add(element);
            }
        }

        ListCreationLiteralExpression listExpr = new ListCreationLiteralExpression(elements);
        listExpr.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return listExpr;
    }
}
