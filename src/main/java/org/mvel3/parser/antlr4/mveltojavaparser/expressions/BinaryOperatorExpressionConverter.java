package org.mvel3.parser.antlr4.mveltojavaparser.expressions;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.Mvel3ToJavaParserVisitor;
import org.mvel3.parser.antlr4.mveltojavaparser.TokenRangeConverter;

import static org.mvel3.parser.util.AstUtils.getBinaryExprOperator;

public final class BinaryOperatorExpressionConverter {

    private BinaryOperatorExpressionConverter() {
    }

    public static Node convertBinaryOperatorExpression(
            final Mvel3Parser.BinaryOperatorExpressionContext ctx,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        Expression left = (Expression) mvel3toJavaParserVisitor.visit(ctx.expression(0));
        Expression right = (Expression) mvel3toJavaParserVisitor.visit(ctx.expression(1));

        String operatorText = resolveOperatorText(ctx);

        // Handle assignment operators separately
        AssignExpr.Operator assignOp = getAssignOperator(operatorText);
        if (assignOp != null) {
            return new AssignExpr(TokenRangeConverter.createTokenRange(ctx), left, right, assignOp);
        }

        // Handle other binary operators
        BinaryExpr.Operator operator = getBinaryExprOperator(operatorText);
        return new BinaryExpr(TokenRangeConverter.createTokenRange(ctx), left, right, operator);
    }

    /**
     * Map operator text to AssignExpr.Operator, return null if not an assignment operator
     */
    private static AssignExpr.Operator getAssignOperator(String operatorText) {
        switch (operatorText) {
            case "=": return AssignExpr.Operator.ASSIGN;
            case "+=": return AssignExpr.Operator.PLUS;
            case "-=": return AssignExpr.Operator.MINUS;
            case "*=": return AssignExpr.Operator.MULTIPLY;
            case "/=": return AssignExpr.Operator.DIVIDE;
            case "&=": return AssignExpr.Operator.BINARY_AND;
            case "|=": return AssignExpr.Operator.BINARY_OR;
            case "^=": return AssignExpr.Operator.XOR;
            case "%=": return AssignExpr.Operator.REMAINDER;
            case "<<=": return AssignExpr.Operator.LEFT_SHIFT;
            case ">>=": return AssignExpr.Operator.SIGNED_RIGHT_SHIFT;
            case ">>>=": return AssignExpr.Operator.UNSIGNED_RIGHT_SHIFT;
            default: return null;
        }
    }

    /**
     * The Java grammar we inherit emits shift operators as separate '<' and '>' tokens, so
     * {@code ctx.bop} remains {@code null}. JavaCC still produces "<<", ">>", ">>>", so we
     * synthesise that text here to keep the generated AST identical to the legacy pipeline.
     */
    private static String resolveOperatorText(Mvel3Parser.BinaryOperatorExpressionContext ctx) {
        if (ctx.bop != null) {
            return ctx.bop.getText();
        }

        // This looks odd, but indeed it's expected by JavaParser.g4
        int ltCount = ctx.LT().size();
        int gtCount = ctx.GT().size();

        if (ltCount == 2) {
            return "<<";
        }
        if (gtCount == 3) {
            return ">>>";
        }
        if (gtCount == 2) {
            return ">>";
        }

        throw new IllegalArgumentException("Unknown binary operator: " + ctx.getText());
    }
}
