package org.mvel3.parser.antlr4.mveltojavaparser;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.LocalClassDeclarationStmt;
import com.github.javaparser.ast.stmt.LocalRecordDeclarationStmt;
import com.github.javaparser.ast.stmt.Statement;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.Mvel3ParserBaseVisitor;

public final class BlockConverter {

    private BlockConverter() {
    }

    public static Node convertBlock(final Mvel3Parser.BlockContext ctx,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        BlockStmt blockStmt = new BlockStmt();
        blockStmt.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        NodeList<Statement> statements = new NodeList<>();

        if (ctx.blockStatement() != null) {
            for (Mvel3Parser.BlockStatementContext blockStatementCtx : ctx.blockStatement()) {
                Node node = convertBlockStatement(blockStatementCtx, mvel3toJavaParserVisitor);
                if (node instanceof Statement) {
                    statements.add((Statement) node);
                }
            }
        }

        blockStmt.setStatements(statements);
        return blockStmt;
    }

    public static Node convertBlockStatement(final Mvel3Parser.BlockStatementContext ctx,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        if (ctx.localVariableDeclaration() != null) {
            // Handle local variable declaration
            VariableDeclarationExpr varDecl = (VariableDeclarationExpr) VariableConverter.convertLocalVariableDeclaration(ctx.localVariableDeclaration(), mvel3toJavaParserVisitor);
            ExpressionStmt exprStmt = new ExpressionStmt(varDecl);
            exprStmt.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return exprStmt;
        } else if (ctx.statement() != null) {
            return StatementConverter.convertStatement(ctx.statement(), mvel3toJavaParserVisitor);
        } else if (ctx.localTypeDeclaration() != null) {
            Mvel3Parser.LocalTypeDeclarationContext localCtx = ctx.localTypeDeclaration();
            if (localCtx.classDeclaration() != null) {
                ClassOrInterfaceDeclaration classDecl = (ClassOrInterfaceDeclaration) TypeConverter.convertClassDeclaration(localCtx.classDeclaration(), mvel3toJavaParserVisitor);
                LocalClassDeclarationStmt stmt = new LocalClassDeclarationStmt(classDecl);
                stmt.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
                return stmt;
            } else if (localCtx.interfaceDeclaration() != null) {
                ClassOrInterfaceDeclaration interfaceDecl = (ClassOrInterfaceDeclaration) InterfaceConverter.convertInterfaceDeclaration(localCtx.interfaceDeclaration(), mvel3toJavaParserVisitor);
                LocalClassDeclarationStmt stmt = new LocalClassDeclarationStmt(interfaceDecl);
                stmt.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
                return stmt;
            } else if (localCtx.recordDeclaration() != null) {
                RecordDeclaration recordDecl = (RecordDeclaration) RecordConverter.convertRecordDeclaration(localCtx.recordDeclaration(), mvel3toJavaParserVisitor);
                LocalRecordDeclarationStmt stmt = new LocalRecordDeclarationStmt(recordDecl);
                stmt.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
                return stmt;
            }
        }
        return null;
    }
}
