package org.mvel3.parser.antlr4.mveltojavaparser.expressions;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.PatternExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.type.ReferenceType;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.Mvel3ToJavaParserVisitor;
import org.mvel3.parser.antlr4.mveltojavaparser.TokenRangeConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.TypeConverter;

public final class InstanceOfOperatorExpressionConverter {

    private InstanceOfOperatorExpressionConverter() {
    }

    public static Node convertInstanceOfOperatorExpression(
            final Mvel3Parser.InstanceOfOperatorExpressionContext ctx,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        Expression expression = (Expression) mvel3toJavaParserVisitor.visit(ctx.expression());

        if (ctx.pattern() != null) {
            // Java 14+ pattern matching: expr instanceof Type varName
            Mvel3Parser.PatternContext patternCtx = ctx.pattern();
            ReferenceType type = (ReferenceType) TypeConverter.convertTypeType(patternCtx.typeType(), mvel3toJavaParserVisitor);
            SimpleName name = new SimpleName(patternCtx.identifier().getText());

            NodeList<Modifier> modifiers = new NodeList<>();
            if (patternCtx.variableModifier() != null) {
                for (Mvel3Parser.VariableModifierContext modCtx : patternCtx.variableModifier()) {
                    if (modCtx.FINAL() != null) {
                        modifiers.add(Modifier.finalModifier());
                    }
                }
            }

            PatternExpr patternExpr = new PatternExpr(modifiers, type, name);
            patternExpr.setTokenRange(TokenRangeConverter.createTokenRange(patternCtx));

            InstanceOfExpr instanceOfExpr = new InstanceOfExpr(expression, type, patternExpr);
            instanceOfExpr.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return instanceOfExpr;
        } else {
            // Classic instanceof: expr instanceof Type
            ReferenceType type = (ReferenceType) TypeConverter.convertTypeType(ctx.typeType(), mvel3toJavaParserVisitor);
            InstanceOfExpr instanceOfExpr = new InstanceOfExpr(expression, type);
            instanceOfExpr.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return instanceOfExpr;
        }
    }
}
