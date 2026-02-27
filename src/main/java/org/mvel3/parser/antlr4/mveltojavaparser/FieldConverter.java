package org.mvel3.parser.antlr4.mveltojavaparser;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.type.Type;
import org.mvel3.parser.antlr4.ModifiersAnnotations;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.Mvel3ToJavaParserVisitor;

public final class FieldConverter {

    private FieldConverter() {
    }

    public static Node convertFieldDeclaration(
            final Mvel3Parser.FieldDeclarationContext ctx,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        Type fieldType = (Type) TypeConverter.convertTypeType(ctx.typeType(), mvel3toJavaParserVisitor);

        NodeList<VariableDeclarator> declarators = new NodeList<>();
        for (Mvel3Parser.VariableDeclaratorContext declaratorCtx : ctx.variableDeclarators().variableDeclarator()) {
            String varName = declaratorCtx.variableDeclaratorId().identifier().getText();

            // Apply C-style array dimensions from variableDeclaratorId
            Type declType = ArrayConverter.applyArrayDimensions(fieldType, declaratorCtx.variableDeclaratorId());

            VariableDeclarator varDeclarator = new VariableDeclarator(declType, varName);
            varDeclarator.setTokenRange(TokenRangeConverter.createTokenRange(declaratorCtx));

            if (declaratorCtx.variableInitializer() != null) {
                Expression initializer = (Expression) VariableConverter.convertVariableInitializer(declaratorCtx.variableInitializer(), mvel3toJavaParserVisitor);
                varDeclarator.setInitializer(initializer);
            }

            declarators.add(varDeclarator);
        }

        FieldDeclaration fieldDecl = new FieldDeclaration(new NodeList<>(), declarators);
        fieldDecl.setTokenRange(TokenRangeConverter.createTokenRange(ctx));

        // Handle modifiers from parent context
        ModifiersAnnotations fieldModifiers = ModifiersParser.resolveModifiersFromParent(ctx);
        fieldDecl.setModifiers(fieldModifiers.modifiers());
        fieldDecl.setAnnotations(fieldModifiers.annotations());

        return fieldDecl;
    }
}
