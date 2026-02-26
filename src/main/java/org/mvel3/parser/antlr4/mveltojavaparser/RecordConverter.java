package org.mvel3.parser.antlr4.mveltojavaparser;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.CompactConstructorDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.Type;
import org.mvel3.parser.antlr4.ModifiersAnnotations;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.Mvel3ParserBaseVisitor;

public final class RecordConverter {

    private RecordConverter() {
    }

    public static Node convertRecordDeclaration(final Mvel3Parser.RecordDeclarationContext ctx,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        String recordName = ctx.identifier().getText();
        RecordDeclaration recordDecl = new RecordDeclaration(new NodeList<>(), recordName);

        // Handle modifiers from parent context
        ModifiersAnnotations recordModifiers = ModifiersParser.resolveModifiersFromParent(ctx);
        recordDecl.setModifiers(recordModifiers.modifiers());
        recordDecl.setAnnotations(recordModifiers.annotations());

        // Handle type parameters
        if (ctx.typeParameters() != null) {
            recordDecl.setTypeParameters(TypeConverter.convertTypeParameters(ctx.typeParameters(), mvel3toJavaParserVisitor));
        }

        // Handle record header (parameters)
        if (ctx.recordHeader() != null && ctx.recordHeader().recordComponentList() != null) {
            NodeList<Parameter> parameters = new NodeList<>();
            for (Mvel3Parser.RecordComponentContext compCtx : ctx.recordHeader().recordComponentList().recordComponent()) {
                Type compType = (Type) TypeConverter.convertTypeType(compCtx.typeType(), mvel3toJavaParserVisitor);
                String compName = compCtx.identifier().getText();
                Parameter param = new Parameter(compType, compName);
                param.setTokenRange(TokenRangeConverter.createTokenRange(compCtx));
                parameters.add(param);
            }
            recordDecl.setParameters(parameters);
        }

        // Handle implements
        if (ctx.IMPLEMENTS() != null && ctx.typeList() != null) {
            recordDecl.setImplementedTypes(TypeConverter.convertTypeList(ctx.typeList(), mvel3toJavaParserVisitor));
        }

        // Handle record body
        if (ctx.recordBody() != null) {
            convertRecordBody(ctx.recordBody(), recordDecl, mvel3toJavaParserVisitor);
        }

        recordDecl.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return recordDecl;
    }

    private static void convertRecordBody(final Mvel3Parser.RecordBodyContext ctx, final RecordDeclaration recordDecl,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        // Handle classBodyDeclaration members (methods, fields, constructors, etc.)
        recordDecl.getMembers().addAll(TypeConverter.convertClassBodyDeclarations(ctx.classBodyDeclaration(), mvel3toJavaParserVisitor));

        // Handle compact constructor declarations
        if (ctx.compactConstructorDeclaration() != null) {
            for (Mvel3Parser.CompactConstructorDeclarationContext compactCtx : ctx.compactConstructorDeclaration()) {
                String name = compactCtx.identifier().getText();
                CompactConstructorDeclaration compactDecl = new CompactConstructorDeclaration(name);

                // Handle body
                if (compactCtx.block() != null) {
                    BlockStmt body = (BlockStmt) BlockConverter.convertBlock(compactCtx.block(), mvel3toJavaParserVisitor);
                    compactDecl.setBody(body);
                }

                // Handle modifiers
                if (compactCtx.modifier() != null) {
                    ModifiersAnnotations ma = ModifiersParser.parseModifiers(compactCtx.modifier());
                    compactDecl.setModifiers(ma.modifiers());
                    compactDecl.setAnnotations(ma.annotations());
                }

                compactDecl.setTokenRange(TokenRangeConverter.createTokenRange(compactCtx));
                recordDecl.addMember(compactDecl);
            }
        }
    }
}
