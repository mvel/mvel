package org.mvel3.parser.antlr4.mveltojavaparser.type;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VoidType;
import org.mvel3.parser.antlr4.ModifiersAnnotations;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.Mvel3ParserBaseVisitor;
import org.mvel3.parser.antlr4.mveltojavaparser.BlockConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.ModifiersParser;
import org.mvel3.parser.antlr4.mveltojavaparser.ParametersConverter;

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
}
