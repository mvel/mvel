/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mvel3.parser.antlr4;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.mvel3.parser.antlr4.mveltojavaparser.AnnotationTypeConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.ArgumentsConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.ArrayConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.BlockConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.CompilationUnitConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.ConstructorConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.CreatedNameConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.EnumConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.FieldConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.ImportConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.InterfaceConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.MethodConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.ParametersConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.PrimaryConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.RecordConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.StatementConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.SwitchConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.TypeConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.VariableConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.expressions.BinaryOperatorExpressionConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.expressions.CastExpressionConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.expressions.InlineCastExpressionConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.expressions.InstanceOfOperatorExpressionConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.expressions.LambdaExpressionConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.expressions.ListCreationLiteralConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.expressions.LiteralConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.expressions.MapCreationLiteralConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.expressions.MemberReferenceExpressionConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.expressions.MethodReferenceExpressionConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.expressions.NullSafeExpressionConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.expressions.ObjectCreationExpressionConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.expressions.PostIncrementDecrementOperatorExpressionConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.expressions.SquareBracketExpressionConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.expressions.TernaryExpressionConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.expressions.UnaryOperatorExpressionConverter;

import java.util.HashMap;
import java.util.Map;

/**
 * Visitor that converts ANTLR4 parse tree to JavaParser AST nodes.
 * This implementation can return various types of nodes (Expression, Type, etc.).
 */
public class Mvel3ToJavaParserVisitor extends Mvel3ParserBaseVisitor<Node> {

    private final boolean tolerantMode;
    // Associate antlr tokenId with a JavaParser node for identifier, so it can be used for code completion.
    protected final Map<Integer, Node> tokenIdJPNodeMap = new HashMap<>();

    public Mvel3ToJavaParserVisitor(boolean tolerantMode) {
        this.tolerantMode = tolerantMode;
    }

    public Mvel3ToJavaParserVisitor() {
        this.tolerantMode = false;
    }

    public boolean isTolerantMode() {
        return tolerantMode;
    }

    public void associateAntlrTokenWithJPNode(ParserRuleContext ctx, Node jpNode) {
        // Antlr token <-> JavaParser node mapping for code completion
        ParseTree lastNode = ctx.children.get(ctx.children.size() - 1); // take the last terminal node
        if (lastNode instanceof TerminalNode terminalNode) {
            tokenIdJPNodeMap.put(terminalNode.getSymbol().getTokenIndex(), jpNode);
        }
    }

    public void updateTokenIdJPNodeMapForMerge(NameExpr orphanedNode, FieldAccessExpr newMergedNode) {
        // Find the token ID that was pointing to the orphaned node and update it to point to the merged node
        for (Map.Entry<Integer, Node> entry : tokenIdJPNodeMap.entrySet()) {
            if (entry.getValue() == orphanedNode) {
                entry.setValue(newMergedNode);
                break;
            }
        }
    }

    public Map<Integer, Node> getTokenIdJPNodeMap() {
        return tokenIdJPNodeMap;
    }

    @Override
    public Node visitCompilationUnit(Mvel3Parser.CompilationUnitContext ctx) {
        return CompilationUnitConverter.convertCompilationUnit(ctx, this);
    }
    
    @Override
    public Node visitImportDeclaration(Mvel3Parser.ImportDeclarationContext ctx) {
        return ImportConverter.convertImportDeclaration(ctx);
    }

    @Override
    public Node visitTypeDeclaration(Mvel3Parser.TypeDeclarationContext ctx) {
        return TypeConverter.convertTypeDeclaration(ctx, this);
    }
    
    @Override
    public Node visitClassDeclaration(Mvel3Parser.ClassDeclarationContext ctx) {
        return TypeConverter.convertClassDeclaration(ctx, this);
    }

    @Override
    public Node visitEnumDeclaration(Mvel3Parser.EnumDeclarationContext ctx) {
        return EnumConverter.convertEnumDeclaration(ctx, this);
    }

    @Override
    public Node visitInterfaceDeclaration(Mvel3Parser.InterfaceDeclarationContext ctx) {
        return InterfaceConverter.convertInterfaceDeclaration(ctx, this);
    }

    @Override
    public Node visitAnnotationTypeDeclaration(Mvel3Parser.AnnotationTypeDeclarationContext ctx) {
        return AnnotationTypeConverter.convertAnnotationTypeDeclaration(ctx, this);
    }

    @Override
    public Node visitRecordDeclaration(Mvel3Parser.RecordDeclarationContext ctx) {
        return RecordConverter.convertRecordDeclaration(ctx, this);
    }

    @Override
    public Node visitFieldDeclaration(Mvel3Parser.FieldDeclarationContext ctx) {
        return FieldConverter.convertFieldDeclaration(ctx, this);
    }

    @Override
    public Node visitConstructorDeclaration(Mvel3Parser.ConstructorDeclarationContext ctx) {
        return ConstructorConverter.convertConstructorDeclaration(ctx, this);
    }

    @Override
    public Node visitGenericMethodDeclaration(Mvel3Parser.GenericMethodDeclarationContext ctx) {
        return MethodConverter.convertGenericMethodDeclaration(ctx, this);
    }

    @Override
    public Node visitGenericConstructorDeclaration(Mvel3Parser.GenericConstructorDeclarationContext ctx) {
        return ConstructorConverter.convertGenericConstructorDeclaration(ctx, this);
    }

    @Override
    public Node visitMethodDeclaration(Mvel3Parser.MethodDeclarationContext ctx) {
        return MethodConverter.convertMethodDeclaration(ctx, this);
    }

    @Override
    public Node visitMethodCallExpression(Mvel3Parser.MethodCallExpressionContext ctx) {
        return MethodConverter.convertMethodCallExpression(ctx, this);
    }

    @Override
    public Node visitMvelStart(Mvel3Parser.MvelStartContext ctx) {
        return visit(ctx.mvelExpression());
    }

    @Override
    public Node visitMvelExpression(Mvel3Parser.MvelExpressionContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public Node visitBinaryOperatorExpression(Mvel3Parser.BinaryOperatorExpressionContext ctx) {
        return BinaryOperatorExpressionConverter.convertBinaryOperatorExpression(ctx, this);
    }

    @Override
    public Node visitTernaryExpression(Mvel3Parser.TernaryExpressionContext ctx) {
        return TernaryExpressionConverter.convertTernaryExpression(ctx, this);
    }

    @Override
    public Node visitInstanceOfOperatorExpression(Mvel3Parser.InstanceOfOperatorExpressionContext ctx) {
        return InstanceOfOperatorExpressionConverter.convertInstanceOfOperatorExpression(ctx, this);
    }

    @Override
    public Node visitExpressionSwitch(Mvel3Parser.ExpressionSwitchContext ctx) {
        return SwitchConverter.convertSwitchExpression(ctx.switchExpression(), this);
    }

    @Override
    public Node visitSwitchExpression(Mvel3Parser.SwitchExpressionContext ctx) {
        return SwitchConverter.convertSwitchExpression(ctx, this);
    }

    @Override
    public Node visitMethodReferenceExpression(Mvel3Parser.MethodReferenceExpressionContext ctx) {
        return MethodReferenceExpressionConverter.convertMethodReferenceExpression(ctx, this);
    }

    @Override
    public Node visitMemberReferenceExpression(Mvel3Parser.MemberReferenceExpressionContext ctx) {
        return MemberReferenceExpressionConverter.convertMemberReferenceExpression(ctx, this);
    }

    @Override
    public Node visitPrimaryExpression(Mvel3Parser.PrimaryExpressionContext ctx) {
        return visit(ctx.primary());
    }

    @Override
    public Node visitPrimary(Mvel3Parser.PrimaryContext ctx) {
        return PrimaryConverter.convertPrimary(ctx, this);
    }

    @Override
    public Node visitInlineCastExpression(Mvel3Parser.InlineCastExpressionContext ctx) {
        return InlineCastExpressionConverter.convertInlineCastExpression(ctx, this);
    }

    @Override
    public Node visitListCreationLiteralExpression(Mvel3Parser.ListCreationLiteralExpressionContext ctx) {
        // Visit the listCreationLiteral rule
        return visit(ctx.listCreationLiteral());
    }

    @Override
    public Node visitListCreationLiteral(Mvel3Parser.ListCreationLiteralContext ctx) {
        return ListCreationLiteralConverter.convertListCreationLiteral(ctx, this);
    }

    @Override
    public Node visitMapCreationLiteralExpression(Mvel3Parser.MapCreationLiteralExpressionContext ctx) {
        // Visit the mapCreationLiteral rule
        return visit(ctx.mapCreationLiteral());
    }

    @Override
    public Node visitMapCreationLiteral(Mvel3Parser.MapCreationLiteralContext ctx) {
        return MapCreationLiteralConverter.convertMapCreationLiteral(ctx, this);
    }

    @Override
    public Node visitNullSafeExpression(Mvel3Parser.NullSafeExpressionContext ctx) {
        return NullSafeExpressionConverter.convertNullSafeExpression(ctx, this);
    }

    @Override
    public Node visitCastExpression(Mvel3Parser.CastExpressionContext ctx) {
        return CastExpressionConverter.convertCastExpression(ctx, this);
    }

    @Override
    public Node visitLambdaExpression(Mvel3Parser.LambdaExpressionContext ctx) {
        return LambdaExpressionConverter.convertLambdaExpression(ctx, this);
    }

    @Override
    public Node visitLiteral(Mvel3Parser.LiteralContext ctx) {
        return LiteralConverter.convertLiteral(ctx);
    }

    @Override
    public Node visitClassOrInterfaceType(Mvel3Parser.ClassOrInterfaceTypeContext ctx) {
        return TypeConverter.convertClassOrInterfaceType(ctx, this);
    }

    @Override
    public Node visitTypeType(Mvel3Parser.TypeTypeContext ctx) {
        return TypeConverter.convertTypeType(ctx, this);
    }

    @Override
    public Node visitPrimitiveType(Mvel3Parser.PrimitiveTypeContext ctx) {
        return TypeConverter.convertPrimitiveType(ctx);
    }

    @Override
    public Node visitSquareBracketExpression(Mvel3Parser.SquareBracketExpressionContext ctx) {
        return SquareBracketExpressionConverter.convertSquareBracketExpression(ctx, this);
    }

    @Override
    public Node visitBlock(Mvel3Parser.BlockContext ctx) {
        return BlockConverter.convertBlock(ctx, this);
    }

    @Override
    public Node visitBlockStatement(Mvel3Parser.BlockStatementContext ctx) {
        return BlockConverter.convertBlockStatement(ctx, this);
    }

    @Override
    public Node visitStatement(Mvel3Parser.StatementContext ctx) {
        return StatementConverter.convertStatement(ctx, this);
    }
    
    @Override
    public Node visitFormalParameter(Mvel3Parser.FormalParameterContext ctx) {
        return ParametersConverter.convertFormalParameter(ctx, this);
    }

    @Override
    public Node visitLastFormalParameter(Mvel3Parser.LastFormalParameterContext ctx) {
        return  ParametersConverter.convertLastFormalParameter(ctx, this);
    }

    @Override
    public Node visitModifyStatement(Mvel3Parser.ModifyStatementContext ctx) {
        return StatementConverter.convertModifyStatement(ctx, this);
    }

    @Override
    public Node visitWithStatement(Mvel3Parser.WithStatementContext ctx) {
        return StatementConverter.convertWithStatement(ctx, this);
    }

    @Override
    public Node visitLocalVariableDeclaration(Mvel3Parser.LocalVariableDeclarationContext ctx) {
        return VariableConverter.convertLocalVariableDeclaration(ctx, this);
    }

    @Override
    public Node visitVariableInitializer(Mvel3Parser.VariableInitializerContext ctx) {
        return VariableConverter.convertVariableInitializer(ctx, this);
    }

    @Override
    public Node visitArrayInitializer(Mvel3Parser.ArrayInitializerContext ctx) {
        return ArrayConverter.convertArrayInitializer(ctx, this);
    }

    @Override
    public Node visitObjectCreationExpression(Mvel3Parser.ObjectCreationExpressionContext ctx) {
        return ObjectCreationExpressionConverter.convertObjectCreationExpression(ctx, this);
    }

    @Override
    public Node visitCreator(Mvel3Parser.CreatorContext ctx) {
        return ObjectCreationExpressionConverter.convertCreator(ctx, this);
    }

    @Override
    public Node visitCreatedName(Mvel3Parser.CreatedNameContext ctx) {
        return CreatedNameConverter.convertCreatedName(ctx, this);
    }

    @Override
    public Node visitUnaryOperatorExpression(Mvel3Parser.UnaryOperatorExpressionContext ctx) {
        return UnaryOperatorExpressionConverter.convertUnaryOperatorExpression(ctx, this);
    }

    @Override
    public Node visitPostIncrementDecrementOperatorExpression(Mvel3Parser.PostIncrementDecrementOperatorExpressionContext ctx) {
        return PostIncrementDecrementOperatorExpressionConverter.convertPostIncrementDecrementOperatorExpression(ctx, this);
    }

    @Override
    public Node visitClassType(Mvel3Parser.ClassTypeContext ctx) {
        return TypeConverter.convertClassType(ctx, this);
    }

    @Override
    public Node visitTypeArguments(Mvel3Parser.TypeArgumentsContext ctx) {
        // This is handled by visitClassOrInterfaceType
        // Just return the result of visiting the type arguments
        return visitChildren(ctx);
    }

    @Override
    public Node visitTypeArgument(Mvel3Parser.TypeArgumentContext ctx) {
        return ArgumentsConverter.convertTypeArgument(ctx, this);
    }
}
