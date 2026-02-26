package org.mvel3.parser.antlr4.mveltojavaparser;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VoidType;
import org.mvel3.parser.antlr4.ModifiersAnnotations;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.Mvel3ParserBaseVisitor;

public final class MethodConverter {

    private MethodConverter() {
    }

    public static Node convertMethodDeclaration(
            final Mvel3Parser.MethodDeclarationContext ctx,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        // Get method name
        String methodName = ctx.identifier().getText();

        // Create method declaration
        MethodDeclaration methodDecl = new MethodDeclaration();
        methodDecl.setName(methodName);

        // Handle return type
        if (ctx.typeTypeOrVoid() != null) {
            if (ctx.typeTypeOrVoid().VOID() != null) {
                methodDecl.setType(new VoidType());
            } else if (ctx.typeTypeOrVoid().typeType() != null) {
                Type returnType = (Type) TypeConverter.convertTypeType(ctx.typeTypeOrVoid().typeType(), mvel3toJavaParserVisitor);
                // Handle array dimensions after formal parameters: ('[' ']')*
                int extraDims = ctx.LBRACK() != null ? ctx.LBRACK().size() : 0;
                for (int i = 0; i < extraDims; i++) {
                    returnType = new ArrayType(returnType);
                }
                methodDecl.setType(returnType);
            }
        }

        // Handle parameters
        if (ctx.formalParameters() != null) {
            methodDecl.setParameters(ParametersConverter.convertFormalParameters(ctx.formalParameters(), mvel3toJavaParserVisitor));
            ParametersConverter.convertReceiverParameter(ctx.formalParameters(), methodDecl, mvel3toJavaParserVisitor);
        }

        // Handle throws clause
        if (ctx.THROWS() != null && ctx.qualifiedNameList() != null) {
            methodDecl.setThrownExceptions(TypeConverter.convertQualifiedNameListAsTypes(ctx.qualifiedNameList()));
        }

        // Handle modifiers (from parent context)
        ModifiersAnnotations methodModifiers = ModifiersParser.resolveModifiersFromParent(ctx);
        methodDecl.setModifiers(methodModifiers.modifiers());
        methodDecl.setAnnotations(methodModifiers.annotations());

        // Handle method body
        if (ctx.methodBody() != null) {
            if (ctx.methodBody().block() != null) {
                BlockStmt body = (BlockStmt) BlockConverter.convertBlock(ctx.methodBody().block(), mvel3toJavaParserVisitor);
                methodDecl.setBody(body);
            }
        }

        return methodDecl;
    }

    public static Node convertGenericMethodDeclaration(
            final Mvel3Parser.GenericMethodDeclarationContext ctx,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        MethodDeclaration methodDecl = (MethodDeclaration) convertMethodDeclaration(ctx.methodDeclaration(), mvel3toJavaParserVisitor);
        if (ctx.typeParameters() != null) {
            methodDecl.setTypeParameters(TypeConverter.convertTypeParameters(ctx.typeParameters(), mvel3toJavaParserVisitor));
        }
        return methodDecl;
    }

    public static Node convertMethodCallExpression(
            final Mvel3Parser.MethodCallExpressionContext ctx,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        // Handle method call without scope
        return convertMethodCallWithScope(ctx.methodCall(), null, mvel3toJavaParserVisitor);
    }

    private static MethodCallExpr convertMethodCallWithScope(
            final Mvel3Parser.MethodCallContext ctx, final Expression scope,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        String methodName;
        if (ctx.identifier() != null) {
            methodName = ctx.identifier().getText();
        } else if (ctx.THIS() != null) {
            methodName = "this";
        } else if (ctx.SUPER() != null) {
            methodName = "super";
        } else {
            throw new IllegalArgumentException("Unknown method call type: " + ctx.getText());
        }

        MethodCallExpr methodCall = new MethodCallExpr(scope, methodName);

        // Handle arguments
        if (ctx.arguments() != null && ctx.arguments().expressionList() != null) {
            methodCall.setArguments(ArgumentsConverter.convertArguments(ctx.arguments(), mvel3toJavaParserVisitor));
        }

        methodCall.setTokenRange(TokenRangeConverter.createTokenRange(ctx));

        return methodCall;
    }
}
