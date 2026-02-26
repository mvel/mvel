package org.mvel3.parser.antlr4.mveltojavaparser;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VoidType;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.Mvel3ParserBaseVisitor;
import org.mvel3.parser.antlr4.mveltojavaparser.expressions.LiteralConverter;
import org.mvel3.parser.ast.expr.DrlNameExpr;

public final class PrimaryConverter {

    private PrimaryConverter() {
    }

    public static Node convertPrimary(
            final Mvel3Parser.PrimaryContext ctx,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        if (ctx.literal() != null) {
            return LiteralConverter.convertLiteral(ctx.literal());
        } else if (ctx.identifier() != null) {
            // Always use DrlNameExpr for identifiers to match JavaCC behavior
            // backReferencesCount defaults to 0 for normal identifiers
            DrlNameExpr nameExpr = new DrlNameExpr(ctx.identifier().getText());
            nameExpr.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            // TODO - fix Tolerant visitor - it needs this to work properly.
            // associateAntlrTokenWithJPNode(ctx.identifier(), nameExpr);
            return nameExpr;
        } else if (ctx.LPAREN() != null && ctx.expression() != null && ctx.RPAREN() != null) {
            // Parenthesized expression
            EnclosedExpr enclosedExpr = new EnclosedExpr((Expression) mvel3toJavaParserVisitor.visit(ctx.expression()));
            enclosedExpr.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return enclosedExpr;
        } else if (ctx.THIS() != null) {
            ThisExpr thisExpr = new ThisExpr();
            thisExpr.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return thisExpr;
        } else if (ctx.SUPER() != null) {
            // super as primary expression
            return new com.github.javaparser.ast.expr.SuperExpr();
        } else if (ctx.typeTypeOrVoid() != null && ctx.CLASS() != null) {
            // Class literal: typeTypeOrVoid '.' CLASS  (e.g. String.class, int.class, void.class)
            Type type;
            if (ctx.typeTypeOrVoid().VOID() != null) {
                type = new VoidType();
            } else {
                type = (Type) TypeConverter.convertTypeType(ctx.typeTypeOrVoid().typeType(), mvel3toJavaParserVisitor);
            }
            ClassExpr classExpr = new ClassExpr(type);
            classExpr.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return classExpr;
        }

        if (ctx.nonWildcardTypeArguments() != null) {
            // nonWildcardTypeArguments (explicitGenericInvocationSuffix | THIS arguments)
            NodeList<Type> typeArgs = ArgumentsConverter.convertNonWildcardTypeArguments(ctx.nonWildcardTypeArguments(), mvel3toJavaParserVisitor);

            if (ctx.explicitGenericInvocationSuffix() != null) {
                Mvel3Parser.ExplicitGenericInvocationSuffixContext suffixCtx = ctx.explicitGenericInvocationSuffix();
                if (suffixCtx.identifier() != null) {
                    // <Type>method(args) — generic method call without scope
                    String methodName = suffixCtx.identifier().getText();
                    NodeList<Expression> args = ArgumentsConverter.convertArguments(suffixCtx.arguments(), mvel3toJavaParserVisitor);
                    MethodCallExpr methodCall = new MethodCallExpr(null, typeArgs, methodName, args);
                    methodCall.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
                    return methodCall;
                }
                // <Type>super(...) — handled as explicit constructor invocation
                // Falls through to visitChildren for now
            } else if (ctx.THIS() != null && ctx.arguments() != null) {
                // <Type>this(args) — explicit constructor invocation with type arguments
                NodeList<Expression> args = ArgumentsConverter.convertArguments(ctx.arguments(), mvel3toJavaParserVisitor);
                MethodCallExpr thisCall = new MethodCallExpr(null, typeArgs, "this", args);
                thisCall.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
                return thisCall;
            }
        }

        // Handle other primary cases that might be needed
        return mvel3toJavaParserVisitor.visitChildren(ctx);
    }
}
