package org.mvel3.parser.antlr4.mveltojavaparser;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import org.mvel3.parser.antlr4.ModifiersAnnotations;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.Mvel3ToJavaParserVisitor;

public final class ConstructorConverter {

    private ConstructorConverter() {
    }

    public static Node convertConstructorDeclaration(
            final Mvel3Parser.ConstructorDeclarationContext ctx,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        String name = ctx.identifier().getText();

        ConstructorDeclaration constructorDecl = new ConstructorDeclaration(name);

        // Handle parameters
        if (ctx.formalParameters() != null) {
            constructorDecl.setParameters(ParametersConverter.convertFormalParameters(ctx.formalParameters(), mvel3toJavaParserVisitor));
            ParametersConverter.convertReceiverParameter(ctx.formalParameters(), constructorDecl, mvel3toJavaParserVisitor);
        }

        // Handle throws clause
        if (ctx.THROWS() != null && ctx.qualifiedNameList() != null) {
            constructorDecl.setThrownExceptions(TypeConverter.convertQualifiedNameListAsTypes(ctx.qualifiedNameList()));
        }

        // Handle modifiers from parent context
        ModifiersAnnotations constructorModifiers = ModifiersParser.resolveModifiersFromParent(ctx);
        constructorDecl.setModifiers(constructorModifiers.modifiers());
        constructorDecl.setAnnotations(constructorModifiers.annotations());

        // Handle constructor body
        if (ctx.block() != null) {
            BlockStmt body = (BlockStmt) BlockConverter.convertBlock(ctx.block(), mvel3toJavaParserVisitor);
            constructorDecl.setBody(body);
        }

        constructorDecl.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return constructorDecl;
    }

    public static Node convertGenericConstructorDeclaration(
            final Mvel3Parser.GenericConstructorDeclarationContext ctx,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        ConstructorDeclaration constructorDecl = (ConstructorDeclaration) convertConstructorDeclaration(ctx.constructorDeclaration(), mvel3toJavaParserVisitor);
        if (ctx.typeParameters() != null) {
            constructorDecl.setTypeParameters(TypeConverter.convertTypeParameters(ctx.typeParameters(), mvel3toJavaParserVisitor));
        }
        return constructorDecl;
    }
}
