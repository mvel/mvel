package org.mvel3.parser.antlr4.mveltojavaparser;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import org.mvel3.parser.antlr4.ModifiersAnnotations;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.Mvel3ToJavaParserVisitor;

public final class EnumConverter {

    private EnumConverter() {
    }

    public static Node convertEnumDeclaration(final Mvel3Parser.EnumDeclarationContext ctx,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        String enumName = ctx.identifier().getText();
        EnumDeclaration enumDecl = new EnumDeclaration(new NodeList<>(), enumName);

        // Handle modifiers
        ModifiersAnnotations enumModifiers = ModifiersParser.resolveModifiersFromParent(ctx);
        enumDecl.setModifiers(enumModifiers.modifiers());
        enumDecl.setAnnotations(enumModifiers.annotations());

        // Handle implements
        if (ctx.IMPLEMENTS() != null && ctx.typeList() != null) {
            enumDecl.setImplementedTypes(TypeConverter.convertTypeList(ctx.typeList(), mvel3toJavaParserVisitor));
        }

        // Handle enum constants
        if (ctx.enumConstants() != null) {
            NodeList<EnumConstantDeclaration> entries = new NodeList<>();
            for (Mvel3Parser.EnumConstantContext constCtx : ctx.enumConstants().enumConstant()) {
                String constName = constCtx.identifier().getText();
                EnumConstantDeclaration constDecl = new EnumConstantDeclaration(constName);
                constDecl.setTokenRange(TokenRangeConverter.createTokenRange(constCtx));

                // Handle annotations
                if (constCtx.annotation() != null && !constCtx.annotation().isEmpty()) {
                    NodeList<AnnotationExpr> annotations = new NodeList<>();
                    for (Mvel3Parser.AnnotationContext annCtx : constCtx.annotation()) {
                        annotations.add(ModifiersConverter.convertAnnotationExpr(annCtx));
                    }
                    constDecl.setAnnotations(annotations);
                }

                // Handle arguments
                if (constCtx.arguments() != null) {
                    constDecl.setArguments(ArgumentsConverter.convertArguments(constCtx.arguments(), mvel3toJavaParserVisitor));
                }

                // Handle class body on enum constant
                if (constCtx.classBody() != null) {
                    constDecl.setClassBody(TypeConverter.convertAnonymousClassBody(constCtx.classBody(),  mvel3toJavaParserVisitor));
                }

                entries.add(constDecl);
            }
            enumDecl.setEntries(entries);
        }

        // Handle enum body declarations (methods, fields after the semicolon)
        if (ctx.enumBodyDeclarations() != null) {
            enumDecl.getMembers().addAll(TypeConverter.convertClassBodyDeclarations(ctx.enumBodyDeclarations().classBodyDeclaration(), mvel3toJavaParserVisitor));
        }

        enumDecl.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return enumDecl;
    }
}
