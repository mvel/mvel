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
import org.mvel3.parser.antlr4.Mvel3ParserBaseVisitor;
import org.mvel3.parser.antlr4.mveltojavaparser.ArgumentsConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.TokenRangeConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.TypeConverter;

public final class MemberReferenceExpressionConverter {

    private MemberReferenceExpressionConverter() {
    }

    public static Node convertMemberReferenceExpression(
            final Mvel3Parser.MemberReferenceExpressionContext ctx,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        // Handle member reference like "java.math.MathContext.DECIMAL128"
        Expression scope = (Expression) mvel3toJavaParserVisitor.visit(ctx.expression());

        if (ctx.identifier() != null) {
            // Simple field access: expression.identifier
            String fieldName = ctx.identifier().getText();
            FieldAccessExpr fieldAccess = new FieldAccessExpr(scope, fieldName);
            fieldAccess.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            // TODO - fix Tolerant visitor - it needs this to work properly.
//            associateAntlrTokenWithJPNode(ctx.identifier(), fieldAccess);
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
            // expression.super(args) or expression.super.method(args) or expression.super.field
            Mvel3Parser.SuperSuffixContext suffixCtx = ctx.superSuffix();

            // Build SuperExpr with the scope as type name (e.g., Outer.super)
            // The scope expression should be a name (e.g., NameExpr("Outer"))
            Name typeName = new Name(scope.toString());
            com.github.javaparser.ast.expr.SuperExpr superExpr = new com.github.javaparser.ast.expr.SuperExpr(typeName);

            if (suffixCtx.arguments() != null && suffixCtx.identifier() == null) {
                // expression.super(args) — super constructor invocation
                // Represented as MethodCallExpr with super as scope
                NodeList<Expression> args = ArgumentsConverter.convertArguments(suffixCtx.arguments(), mvel3toJavaParserVisitor);
                MethodCallExpr methodCall = new MethodCallExpr(superExpr, "super");
                methodCall.setArguments(args);
                methodCall.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
                return methodCall;
            } else if (suffixCtx.identifier() != null) {
                String memberName = suffixCtx.identifier().getText();
                if (suffixCtx.arguments() != null) {
                    // expression.super.method(args)
                    NodeList<Expression> args = ArgumentsConverter.convertArguments(suffixCtx.arguments(), mvel3toJavaParserVisitor);
                    MethodCallExpr methodCall = new MethodCallExpr(superExpr, memberName);
                    methodCall.setArguments(args);
                    // Handle type arguments if present
                    if (suffixCtx.typeArguments() != null) {
                        methodCall.setTypeArguments(ArgumentsConverter.convertTypeArguments(suffixCtx.typeArguments(), mvel3toJavaParserVisitor));
                    }
                    methodCall.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
                    return methodCall;
                } else {
                    // expression.super.field
                    FieldAccessExpr fieldAccess = new FieldAccessExpr(superExpr, memberName);
                    fieldAccess.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
                    return fieldAccess;
                }
            }
        } else if (ctx.NEW() != null && ctx.innerCreator() != null) {
            // expression.new InnerClass(args) [classBody]
            Mvel3Parser.InnerCreatorContext innerCtx = ctx.innerCreator();
            String className = innerCtx.identifier().getText();
            ClassOrInterfaceType type = new ClassOrInterfaceType(null, className);

            // Handle type arguments if present (e.g., expr.new Inner<String>())
            if (innerCtx.nonWildcardTypeArgumentsOrDiamond() != null) {
                Mvel3Parser.NonWildcardTypeArgumentsOrDiamondContext diamondCtx = innerCtx.nonWildcardTypeArgumentsOrDiamond();
                if (diamondCtx.nonWildcardTypeArguments() != null) {
                    type.setTypeArguments(ArgumentsConverter.convertNonWildcardTypeArguments(diamondCtx.nonWildcardTypeArguments(), mvel3toJavaParserVisitor));
                } else {
                    // Diamond operator <>
                    type.setTypeArguments(new NodeList<>());
                }
            }

            // Parse constructor arguments
            NodeList<Expression> arguments = new NodeList<>();
            if (innerCtx.classCreatorRest().arguments() != null &&
                    innerCtx.classCreatorRest().arguments().expressionList() != null) {
                for (Mvel3Parser.ExpressionContext exprCtx : innerCtx.classCreatorRest().arguments().expressionList().expression()) {
                    arguments.add((Expression) mvel3toJavaParserVisitor.visit(exprCtx));
                }
            }

            // Handle anonymous class body if present
            NodeList<BodyDeclaration<?>> anonymousClassBody = null;
            if (innerCtx.classCreatorRest().classBody() != null) {
                anonymousClassBody = TypeConverter.convertAnonymousClassBody(innerCtx.classCreatorRest().classBody(), mvel3toJavaParserVisitor);
            }

            ObjectCreationExpr objectCreation = new ObjectCreationExpr(scope, type, null, arguments, anonymousClassBody);
            objectCreation.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return objectCreation;
        } else if (ctx.explicitGenericInvocation() != null) {
            // expression.<Type>method(args) — explicit generic invocation
            Mvel3Parser.ExplicitGenericInvocationContext egiCtx = ctx.explicitGenericInvocation();

            // Parse type arguments from nonWildcardTypeArguments: '<' typeList '>'
            NodeList<Type> typeArgs = ArgumentsConverter.convertNonWildcardTypeArguments(egiCtx.nonWildcardTypeArguments(), mvel3toJavaParserVisitor);

            Mvel3Parser.ExplicitGenericInvocationSuffixContext suffixCtx = egiCtx.explicitGenericInvocationSuffix();
            if (suffixCtx.identifier() != null) {
                // <Type>method(args)
                String methodName = suffixCtx.identifier().getText();
                NodeList<Expression> args = ArgumentsConverter.convertArguments(suffixCtx.arguments(), mvel3toJavaParserVisitor);
                MethodCallExpr methodCall = new MethodCallExpr(scope, typeArgs, methodName, args);
                methodCall.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
                return methodCall;
            } else if (suffixCtx.SUPER() != null) {
                // expression.<Type>super(...) or expression.<Type>super.method(...)
                Mvel3Parser.SuperSuffixContext superSuffix = suffixCtx.superSuffix();
                Name typeName = new Name(scope.toString());
                com.github.javaparser.ast.expr.SuperExpr superExpr = new com.github.javaparser.ast.expr.SuperExpr(typeName);

                if (superSuffix.arguments() != null && superSuffix.identifier() == null) {
                    // expression.<Type>super(args) — generic super constructor call
                    NodeList<Expression> args = ArgumentsConverter.convertArguments(superSuffix.arguments(), mvel3toJavaParserVisitor);
                    MethodCallExpr methodCall = new MethodCallExpr(superExpr, typeArgs, "super", args);
                    methodCall.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
                    return methodCall;
                } else if (superSuffix.identifier() != null) {
                    // expression.<Type>super.method(args)
                    String memberName = superSuffix.identifier().getText();
                    NodeList<Expression> args = superSuffix.arguments() != null
                            ? ArgumentsConverter.convertArguments(superSuffix.arguments(), mvel3toJavaParserVisitor) : new NodeList<>();
                    MethodCallExpr methodCall = new MethodCallExpr(superExpr, typeArgs, memberName, args);
                    methodCall.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
                    return methodCall;
                }
            }
        }

        throw new IllegalArgumentException("Unsupported member reference: " + ctx.getText());
    }
}
