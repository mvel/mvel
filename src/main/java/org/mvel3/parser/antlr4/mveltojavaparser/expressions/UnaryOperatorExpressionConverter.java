package org.mvel3.parser.antlr4.mveltojavaparser.expressions;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.UnaryExpr;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.Mvel3ParserBaseVisitor;
import org.mvel3.parser.antlr4.mveltojavaparser.TokenRangeConverter;

public final class UnaryOperatorExpressionConverter {

    private UnaryOperatorExpressionConverter() {
    }

    public static Node convertUnaryOperatorExpression(
            final Mvel3Parser.UnaryOperatorExpressionContext ctx,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        // Handle unary operators: +expr, -expr, ++expr, --expr, ~expr, !expr
        Expression operand = (Expression) mvel3toJavaParserVisitor.visit(ctx.expression());
        String operator = ctx.prefix.getText();

        UnaryExpr.Operator unaryOp = switch (operator) {
            case "+" -> UnaryExpr.Operator.PLUS;
            case "-" -> UnaryExpr.Operator.MINUS;
            case "++" -> UnaryExpr.Operator.PREFIX_INCREMENT;
            case "--" -> UnaryExpr.Operator.PREFIX_DECREMENT;
            case "~" -> UnaryExpr.Operator.BITWISE_COMPLEMENT;
            case "!" -> UnaryExpr.Operator.LOGICAL_COMPLEMENT;
            default -> throw new IllegalArgumentException("Unknown unary operator: " + operator);
        };

        UnaryExpr unaryExpr = new UnaryExpr(operand, unaryOp);
        unaryExpr.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return unaryExpr;
    }
}
