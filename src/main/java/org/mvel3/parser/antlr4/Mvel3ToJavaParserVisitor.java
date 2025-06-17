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

import com.github.javaparser.ast.expr.*;

import static org.mvel3.parser.util.AstUtils.getBinaryExprOperator;

/**
 * Visitor that converts ANTLR4 parse tree to JavaParser AST Expression nodes.
 * This implementation focuses on basic expressions needed for testing.
 */
public class Mvel3ToJavaParserVisitor extends Mvel3ParserBaseVisitor<Expression> {

    @Override
    public Expression visitMvelStart(Mvel3Parser.MvelStartContext ctx) {
        return visit(ctx.mvelExpression());
    }

    @Override
    public Expression visitMvelExpression(Mvel3Parser.MvelExpressionContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public Expression visitBinaryOperatorExpression(Mvel3Parser.BinaryOperatorExpressionContext ctx) {
        Expression left = visit(ctx.expression(0));
        Expression right = visit(ctx.expression(1));
        
        // Handle different binary operators
        String operatorText = ctx.bop.getText();
        BinaryExpr.Operator operator = getBinaryExprOperator(operatorText);

        return new BinaryExpr(left, right, operator);
    }


    @Override
    public Expression visitPrimaryExpression(Mvel3Parser.PrimaryExpressionContext ctx) {
        return visit(ctx.primary());
    }

    @Override
    public Expression visitPrimary(Mvel3Parser.PrimaryContext ctx) {
        if (ctx.literal() != null) {
            return visit(ctx.literal());
        } else if (ctx.identifier() != null) {
            return new NameExpr(ctx.identifier().getText());
        } else if (ctx.LPAREN() != null && ctx.expression() != null && ctx.RPAREN() != null) {
            // Parenthesized expression
            return new EnclosedExpr(visit(ctx.expression()));
        } else if (ctx.THIS() != null) {
            return new ThisExpr();
        }
        
        // Handle other primary cases that might be needed
        return visitChildren(ctx);
    }

    @Override
    public Expression visitLiteral(Mvel3Parser.LiteralContext ctx) {
        if (ctx.STRING_LITERAL() != null) {
            String text = ctx.STRING_LITERAL().getText();
            // Remove quotes
            String value = text.substring(1, text.length() - 1);
            return new StringLiteralExpr(value);
        } else if (ctx.DECIMAL_LITERAL() != null) {
            return new IntegerLiteralExpr(ctx.DECIMAL_LITERAL().getText());
        } else if (ctx.FLOAT_LITERAL() != null) {
            return new DoubleLiteralExpr(ctx.FLOAT_LITERAL().getText());
        } else if (ctx.BOOL_LITERAL() != null) {
            return new BooleanLiteralExpr(Boolean.parseBoolean(ctx.BOOL_LITERAL().getText()));
        } else if (ctx.NULL_LITERAL() != null) {
            return new NullLiteralExpr();
        } else if (ctx.CHAR_LITERAL() != null) {
            String text = ctx.CHAR_LITERAL().getText();
            char value = text.charAt(1); // Simple case, more complex handling needed for escape sequences
            return new CharLiteralExpr(value);
        }
        
        // Handle MVEL-specific literals
        if (ctx.BigDecimalLiteral() != null) {
            String text = ctx.BigDecimalLiteral().getText();
            // Remove 'B' suffix
            text = text.substring(0, text.length() - 1);
            return new DoubleLiteralExpr(text);
        } else if (ctx.BigIntegerLiteral() != null) {
            String text = ctx.BigIntegerLiteral().getText();
            // Remove 'I' suffix
            text = text.substring(0, text.length() - 1);
            return new IntegerLiteralExpr(text);
        }
        
        throw new IllegalArgumentException("Unknown literal type: " + ctx.getText());
    }
}