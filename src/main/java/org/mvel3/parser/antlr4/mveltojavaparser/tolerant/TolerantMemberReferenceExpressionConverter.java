package org.mvel3.parser.antlr4.mveltojavaparser.tolerant;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import org.antlr.v4.runtime.tree.ParseTree;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.Mvel3ToJavaParserVisitor;
import org.mvel3.parser.antlr4.mveltojavaparser.ArgumentsConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.TokenRangeConverter;

public final class TolerantMemberReferenceExpressionConverter {

    private TolerantMemberReferenceExpressionConverter() {
    }

    public static Node convertMemberReferenceExpression(
            final Mvel3Parser.MemberReferenceExpressionContext ctx,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        // Handle member reference like "java.math.MathContext.DECIMAL128"
        Expression scope = (Expression) mvel3toJavaParserVisitor.visit(ctx.expression());

        if (ctx.identifier() != null) {
            // Simple field access: expression.identifier
            String fieldName = ctx.identifier().getText();
            FieldAccessExpr fieldAccess = new FieldAccessExpr(scope, fieldName);
            fieldAccess.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            // TODO fix this hack for Tolerant visitor to work.
            ((Mvel3ToJavaParserVisitor) mvel3toJavaParserVisitor).associateAntlrTokenWithJPNode(ctx.identifier(), fieldAccess);
            return fieldAccess;
        } else if (ctx.methodCall() != null) {
            // Method call: expression.methodCall()
            String methodName = ctx.methodCall().identifier().getText();
            NodeList<Expression> args = ArgumentsConverter.convertArguments(ctx.methodCall().arguments(), mvel3toJavaParserVisitor);

            MethodCallExpr methodCall = new MethodCallExpr(scope, methodName);
            methodCall.setArguments(args);
            methodCall.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return methodCall;
        } else if (ctx.THIS() != null) {
            // expression.this
            FieldAccessExpr fieldAccess = new FieldAccessExpr(scope, "this");
            fieldAccess.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return fieldAccess;
        } else if (ctx.SUPER() != null && ctx.superSuffix() != null) {
            // expression.super.something
            // TODO: Implement super handling
            throw new UnsupportedOperationException("Super references not yet implemented");
        } else if (ctx.NEW() != null && ctx.innerCreator() != null) {
            // expression.new InnerClass()
            // TODO: Implement inner class creation
            throw new UnsupportedOperationException("Inner class creation not yet implemented");
        }

        //===== Tolerant mode handling =====

        // Check if this is an incomplete member access (e.g., list#ArrayList#.)
        // In tolerant mode, we want to add a COMPLETION_FIELD marker for code completion
        if (hasTrailingDot(ctx)) {
            FieldAccessExpr completionField = new FieldAccessExpr(scope, TolerantModeConstants.COMPLETION_FIELD);
            scope.setParentNode(completionField);
            completionField.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return completionField;
        }

        throw new IllegalArgumentException("Unsupported member reference: " + ctx.getText());
    }

    private static boolean hasTrailingDot(Mvel3Parser.MemberReferenceExpressionContext ctx) {
        // Check if the last child is a DOT token (incomplete member access)
        if (ctx.children == null || ctx.children.isEmpty()) {
            return false;
        }

        ParseTree lastChild = ctx.children.get(ctx.children.size() - 1);
        return ".".equals(lastChild.getText());
    }
}
