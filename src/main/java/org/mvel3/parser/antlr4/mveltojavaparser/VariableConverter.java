package org.mvel3.parser.antlr4.mveltojavaparser;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VarType;
import org.mvel3.parser.antlr4.ModifiersAnnotations;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.Mvel3ParserBaseVisitor;
import org.mvel3.parser.antlr4.mveltojavaparser.type.TypeConverter;

public final class VariableConverter {

    private VariableConverter() {
    }

    public static Node convertLocalVariableDeclaration(final Mvel3Parser.LocalVariableDeclarationContext ctx,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        // Handle both: var x = expression; and Type name = expression;
        ModifiersAnnotations varModifiers = VariableParser.parseVariableModifiers(ctx.variableModifier());

        if (ctx.VAR() != null) {
            // Handle: var x = expression;
            Type varType = new VarType();
            varType.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            String varName = ctx.identifier().getText();

            VariableDeclarator varDeclarator = new VariableDeclarator(varType, varName);
            varDeclarator.setTokenRange(TokenRangeConverter.createTokenRange(ctx));

            // Handle initializer for var declaration
            if (ctx.expression() != null) {
                Expression initializer = (Expression) mvel3toJavaParserVisitor.visit(ctx.expression());
                varDeclarator.setInitializer(initializer);
            }

            NodeList<VariableDeclarator> declarators = new NodeList<>();
            declarators.add(varDeclarator);
            VariableDeclarationExpr varDecl = new VariableDeclarationExpr(varModifiers.modifiers(), varModifiers.annotations(), declarators);
            varDecl.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return varDecl;
        } else if (ctx.typeType() != null && ctx.variableDeclarators() != null) {
            // Handle: Type name = expression;
            Type varType = (Type) TypeConverter.convertTypeType(ctx.typeType(), mvel3toJavaParserVisitor);

            // Create NodeList for multiple declarators (though we usually have just one)
            NodeList<VariableDeclarator> declarators = new NodeList<>();

            for (Mvel3Parser.VariableDeclaratorContext declaratorCtx : ctx.variableDeclarators().variableDeclarator()) {
                // Get variable name
                String varName = declaratorCtx.variableDeclaratorId().identifier().getText();

                // Apply C-style array dimensions from variableDeclaratorId
                Type declType = ArrayConverter.applyArrayDimensions(varType, declaratorCtx.variableDeclaratorId());

                // Create variable declarator
                VariableDeclarator varDeclarator = new VariableDeclarator(declType, varName);
                varDeclarator.setTokenRange(TokenRangeConverter.createTokenRange(declaratorCtx));

                // Handle initializer if present
                if (declaratorCtx.variableInitializer() != null) {
                    Expression initializer = (Expression) convertVariableInitializer(declaratorCtx.variableInitializer(), mvel3toJavaParserVisitor);
                    varDeclarator.setInitializer(initializer);
                }

                declarators.add(varDeclarator);
            }

            // Create the variable declaration expression with all declarators
            VariableDeclarationExpr varDecl = new VariableDeclarationExpr(varModifiers.modifiers(), varModifiers.annotations(), declarators);
            varDecl.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return varDecl;
        } else {
            throw new IllegalArgumentException("Unsupported local variable declaration: " + ctx.getText());
        }
    }

    public static Node convertVariableInitializer(Mvel3Parser.VariableInitializerContext ctx,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        if (ctx.arrayInitializer() != null) {
            return ArrayConverter.convertArrayInitializer(ctx.arrayInitializer(), mvel3toJavaParserVisitor);
        } else if (ctx.expression() != null) {
            return mvel3toJavaParserVisitor.visit(ctx.expression());
        }
        return null;
    }
}
