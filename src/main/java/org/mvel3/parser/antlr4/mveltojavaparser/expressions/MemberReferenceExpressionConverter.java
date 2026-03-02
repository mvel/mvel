package org.mvel3.parser.antlr4.mveltojavaparser.expressions;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.Mvel3ToJavaParserVisitor;
import org.mvel3.parser.antlr4.mveltojavaparser.ArgumentsConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.TokenRangeConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.TypeConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.tolerant.TolerantMemberReferenceExpressionConverter;

public final class MemberReferenceExpressionConverter {

    private MemberReferenceExpressionConverter() {
    }

    public static Node convertMemberReferenceExpression(
            final Mvel3Parser.MemberReferenceExpressionContext ctx,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        if (mvel3toJavaParserVisitor.isTolerantMode()) {
            return TolerantMemberReferenceExpressionConverter.convertMemberReferenceExpression(ctx, mvel3toJavaParserVisitor);
        } else {
            Expression scope = (Expression) mvel3toJavaParserVisitor.visit(ctx.expression());

            if (ctx.identifier() != null) {
                return handleSimpleFieldAccess(ctx, scope, mvel3toJavaParserVisitor);
            } else if (ctx.methodCall() != null) {
                return handleMethodCall(ctx, scope, mvel3toJavaParserVisitor);
            } else if (ctx.THIS() != null) {
                return handleThisReference(ctx, scope);
            } else if (ctx.SUPER() != null && ctx.superSuffix() != null) {
                return handleSuperReference(ctx, scope, mvel3toJavaParserVisitor);
            } else if (ctx.NEW() != null && ctx.innerCreator() != null) {
                return handleInnerCreator(ctx, scope, mvel3toJavaParserVisitor);
            } else if (ctx.explicitGenericInvocation() != null) {
                return handleExplicitGenericInvocation(ctx, scope, mvel3toJavaParserVisitor);
            }

            throw new IllegalArgumentException("Unsupported member reference: " + ctx.getText());
        }
    }

    private static Node handleSimpleFieldAccess(
            final Mvel3Parser.MemberReferenceExpressionContext ctx,
            final Expression scope,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        // Simple field access: expression.identifier
        String fieldName = ctx.identifier().getText();
        FieldAccessExpr fieldAccess = new FieldAccessExpr(scope, fieldName);
        fieldAccess.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        // TODO fix this hack for Tolerant visitor to work.
        ((Mvel3ToJavaParserVisitor) mvel3toJavaParserVisitor).associateAntlrTokenWithJPNode(ctx.identifier(), fieldAccess);
        return fieldAccess;
    }

    private static Node handleMethodCall(
            final Mvel3Parser.MemberReferenceExpressionContext ctx,
            final Expression scope,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        // Method call: expression.methodCall()
        String methodName = ctx.methodCall().identifier().getText();
        NodeList<Expression> args = ArgumentsConverter.convertArguments(
                ctx.methodCall().arguments(), mvel3toJavaParserVisitor);

        MethodCallExpr methodCall = new MethodCallExpr(scope, methodName);
        methodCall.setArguments(args);
        methodCall.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return methodCall;
    }

    private static Node handleThisReference(
            final Mvel3Parser.MemberReferenceExpressionContext ctx,
            final Expression scope) {
        // expression.this
        FieldAccessExpr fieldAccess = new FieldAccessExpr(scope, "this");
        fieldAccess.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return fieldAccess;
    }

    private static Node handleSuperReference(
            final Mvel3Parser.MemberReferenceExpressionContext ctx,
            final Expression scope,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        // expression.super(args) or expression.super.method(args) or expression.super.field
        Mvel3Parser.SuperSuffixContext suffixCtx = ctx.superSuffix();
        com.github.javaparser.ast.expr.SuperExpr superExpr = createSuperExpr(scope);

        if (isSuperConstructorInvocation(suffixCtx)) {
            return createSuperConstructorCall(ctx, superExpr, suffixCtx, mvel3toJavaParserVisitor);
        } else if (suffixCtx.identifier() != null) {
            return handleSuperMemberAccess(ctx, superExpr, suffixCtx, mvel3toJavaParserVisitor);
        }

        throw new IllegalArgumentException("Unsupported super reference: " + ctx.getText());
    }

    private static com.github.javaparser.ast.expr.SuperExpr createSuperExpr(final Expression scope) {
        // Build SuperExpr with the scope as type name (e.g., Outer.super)
        // The scope expression should be a name (e.g., NameExpr("Outer"))
        Name typeName = new Name(scope.toString());
        return new com.github.javaparser.ast.expr.SuperExpr(typeName);
    }

    private static boolean isSuperConstructorInvocation(final Mvel3Parser.SuperSuffixContext suffixCtx) {
        return suffixCtx.arguments() != null && suffixCtx.identifier() == null;
    }

    private static Node createSuperConstructorCall(
            final Mvel3Parser.MemberReferenceExpressionContext ctx,
            final com.github.javaparser.ast.expr.SuperExpr superExpr,
            final Mvel3Parser.SuperSuffixContext suffixCtx,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        // expression.super(args) — super constructor invocation
        // Represented as MethodCallExpr with super as scope
        NodeList<Expression> args = ArgumentsConverter.convertArguments(
                suffixCtx.arguments(), mvel3toJavaParserVisitor);
        MethodCallExpr methodCall = new MethodCallExpr(superExpr, "super");
        methodCall.setArguments(args);
        methodCall.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return methodCall;
    }

    private static Node handleSuperMemberAccess(
            final Mvel3Parser.MemberReferenceExpressionContext ctx,
            final com.github.javaparser.ast.expr.SuperExpr superExpr,
            final Mvel3Parser.SuperSuffixContext suffixCtx,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        String memberName = suffixCtx.identifier().getText();

        if (suffixCtx.arguments() != null) {
            return createSuperMethodCall(ctx, superExpr, memberName, suffixCtx, mvel3toJavaParserVisitor);
        } else {
            return createSuperFieldAccess(ctx, superExpr, memberName);
        }
    }

    private static Node createSuperMethodCall(
            final Mvel3Parser.MemberReferenceExpressionContext ctx,
            final com.github.javaparser.ast.expr.SuperExpr superExpr,
            final String memberName,
            final Mvel3Parser.SuperSuffixContext suffixCtx,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        // expression.super.method(args)
        NodeList<Expression> args = ArgumentsConverter.convertArguments(
                suffixCtx.arguments(), mvel3toJavaParserVisitor);
        MethodCallExpr methodCall = new MethodCallExpr(superExpr, memberName);
        methodCall.setArguments(args);

        // Handle type arguments if present
        if (suffixCtx.typeArguments() != null) {
            methodCall.setTypeArguments(ArgumentsConverter.convertTypeArguments(
                    suffixCtx.typeArguments(), mvel3toJavaParserVisitor));
        }

        methodCall.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return methodCall;
    }

    private static Node createSuperFieldAccess(
            final Mvel3Parser.MemberReferenceExpressionContext ctx,
            final com.github.javaparser.ast.expr.SuperExpr superExpr,
            final String memberName) {
        // expression.super.field
        FieldAccessExpr fieldAccess = new FieldAccessExpr(superExpr, memberName);
        fieldAccess.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return fieldAccess;
    }

    private static Node handleInnerCreator(
            final Mvel3Parser.MemberReferenceExpressionContext ctx,
            final Expression scope,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        // expression.new InnerClass(args) [classBody]
        Mvel3Parser.InnerCreatorContext innerCtx = ctx.innerCreator();
        ClassOrInterfaceType type = createInnerClassType(innerCtx, mvel3toJavaParserVisitor);
        NodeList<Expression> arguments = extractConstructorArguments(innerCtx, mvel3toJavaParserVisitor);
        NodeList<BodyDeclaration<?>> anonymousClassBody = extractAnonymousClassBody(innerCtx, mvel3toJavaParserVisitor);

        ObjectCreationExpr objectCreation = new ObjectCreationExpr(scope, type, null, arguments, anonymousClassBody);
        objectCreation.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return objectCreation;
    }

    private static ClassOrInterfaceType createInnerClassType(
            final Mvel3Parser.InnerCreatorContext innerCtx,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        String className = innerCtx.identifier().getText();
        ClassOrInterfaceType type = new ClassOrInterfaceType(null, className);

        // Handle type arguments if present (e.g., expr.new Inner<String>())
        if (innerCtx.nonWildcardTypeArgumentsOrDiamond() != null) {
            Mvel3Parser.NonWildcardTypeArgumentsOrDiamondContext diamondCtx =
                    innerCtx.nonWildcardTypeArgumentsOrDiamond();
            if (diamondCtx.nonWildcardTypeArguments() != null) {
                type.setTypeArguments(ArgumentsConverter.convertNonWildcardTypeArguments(
                        diamondCtx.nonWildcardTypeArguments(), mvel3toJavaParserVisitor));
            } else {
                // Diamond operator <>
                type.setTypeArguments(new NodeList<>());
            }
        }

        return type;
    }

    private static NodeList<Expression> extractConstructorArguments(
            final Mvel3Parser.InnerCreatorContext innerCtx,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        NodeList<Expression> arguments = new NodeList<>();

        if (innerCtx.classCreatorRest().arguments() != null &&
                innerCtx.classCreatorRest().arguments().expressionList() != null) {
            for (Mvel3Parser.ExpressionContext exprCtx :
                    innerCtx.classCreatorRest().arguments().expressionList().expression()) {
                arguments.add((Expression) mvel3toJavaParserVisitor.visit(exprCtx));
            }
        }

        return arguments;
    }

    private static NodeList<BodyDeclaration<?>> extractAnonymousClassBody(
            final Mvel3Parser.InnerCreatorContext innerCtx,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        if (innerCtx.classCreatorRest().classBody() != null) {
            return TypeConverter.convertAnonymousClassBody(
                    innerCtx.classCreatorRest().classBody(), mvel3toJavaParserVisitor);
        }
        return null;
    }

    private static Node handleExplicitGenericInvocation(
            final Mvel3Parser.MemberReferenceExpressionContext ctx,
            final Expression scope,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        // expression.<Type>method(args) — explicit generic invocation
        Mvel3Parser.ExplicitGenericInvocationContext egiCtx = ctx.explicitGenericInvocation();
        NodeList<Type> typeArgs = ArgumentsConverter.convertNonWildcardTypeArguments(
                egiCtx.nonWildcardTypeArguments(), mvel3toJavaParserVisitor);

        Mvel3Parser.ExplicitGenericInvocationSuffixContext suffixCtx = egiCtx.explicitGenericInvocationSuffix();

        if (suffixCtx.identifier() != null) {
            return createGenericMethodCall(ctx, scope, typeArgs, suffixCtx, mvel3toJavaParserVisitor);
        } else if (suffixCtx.SUPER() != null) {
            return handleGenericSuperInvocation(ctx, scope, typeArgs, suffixCtx, mvel3toJavaParserVisitor);
        }

        throw new IllegalArgumentException("Unsupported explicit generic invocation: " + ctx.getText());
    }

    private static Node createGenericMethodCall(
            final Mvel3Parser.MemberReferenceExpressionContext ctx,
            final Expression scope,
            final NodeList<Type> typeArgs,
            final Mvel3Parser.ExplicitGenericInvocationSuffixContext suffixCtx,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        // <Type>method(args)
        String methodName = suffixCtx.identifier().getText();
        NodeList<Expression> args = ArgumentsConverter.convertArguments(
                suffixCtx.arguments(), mvel3toJavaParserVisitor);
        MethodCallExpr methodCall = new MethodCallExpr(scope, typeArgs, methodName, args);
        methodCall.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return methodCall;
    }

    private static Node handleGenericSuperInvocation(
            final Mvel3Parser.MemberReferenceExpressionContext ctx,
            final Expression scope,
            final NodeList<Type> typeArgs,
            final Mvel3Parser.ExplicitGenericInvocationSuffixContext suffixCtx,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        // expression.<Type>super(...) or expression.<Type>super.method(...)
        Mvel3Parser.SuperSuffixContext superSuffix = suffixCtx.superSuffix();
        com.github.javaparser.ast.expr.SuperExpr superExpr = createSuperExpr(scope);

        if (isSuperConstructorInvocation(superSuffix)) {
            return createGenericSuperConstructorCall(ctx, superExpr, typeArgs, superSuffix, mvel3toJavaParserVisitor);
        } else if (superSuffix.identifier() != null) {
            return createGenericSuperMethodCall(ctx, superExpr, typeArgs, superSuffix, mvel3toJavaParserVisitor);
        }

        throw new IllegalArgumentException("Unsupported generic super invocation: " + ctx.getText());
    }

    private static Node createGenericSuperConstructorCall(
            final Mvel3Parser.MemberReferenceExpressionContext ctx,
            final com.github.javaparser.ast.expr.SuperExpr superExpr,
            final NodeList<Type> typeArgs,
            final Mvel3Parser.SuperSuffixContext superSuffix,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        // expression.<Type>super(args) — generic super constructor call
        NodeList<Expression> args = ArgumentsConverter.convertArguments(
                superSuffix.arguments(), mvel3toJavaParserVisitor);
        MethodCallExpr methodCall = new MethodCallExpr(superExpr, typeArgs, "super", args);
        methodCall.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return methodCall;
    }

    private static Node createGenericSuperMethodCall(
            final Mvel3Parser.MemberReferenceExpressionContext ctx,
            final com.github.javaparser.ast.expr.SuperExpr superExpr,
            final NodeList<Type> typeArgs,
            final Mvel3Parser.SuperSuffixContext superSuffix,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        // expression.<Type>super.method(args)
        String memberName = superSuffix.identifier().getText();
        NodeList<Expression> args = superSuffix.arguments() != null
                ? ArgumentsConverter.convertArguments(superSuffix.arguments(), mvel3toJavaParserVisitor)
                : new NodeList<>();
        MethodCallExpr methodCall = new MethodCallExpr(superExpr, typeArgs, memberName, args);
        methodCall.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return methodCall;
    }
}
