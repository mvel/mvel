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

import java.util.concurrent.TimeUnit;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import org.junit.Test;
import org.mvel3.parser.ast.expr.TemporalLiteralChunkExpr;
import org.mvel3.parser.ast.expr.TemporalLiteralExpr;
import org.mvel3.parser.printer.PrintUtil;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the MVEL parser using ANTLR4.
 * Assertions are based on the JavaParser AST which are created by Mvel3ToJavaParserVisitor.
 */
public class Antlr4MvelParserJavaParserASTTest {

    @Test
    public void testExpression() {
        String expr = "name == \"Mark\"";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<Expression> result = parser.parseExpression(expr);
        assertThat(result.getResult()).isPresent();

        BinaryExpr binaryExpr = ((BinaryExpr) result.getResult().get());
        assertThat(toString(binaryExpr.getLeft())).isEqualTo("name");
        assertThat(toString(binaryExpr.getRight())).isEqualTo("\"Mark\"");
        assertThat(binaryExpr.getOperator()).isEqualTo(BinaryExpr.Operator.EQUALS);
    }

    @Test
    public void testClassOrInterfaceType() {
        String expr = "BigDecimal";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<ClassOrInterfaceType> result = parser.parseClassOrInterfaceType(expr);
        assertThat(result.getResult()).isPresent();

        ClassOrInterfaceType classOrInterfaceType = result.getResult().get();
        assertThat(classOrInterfaceType.getNameAsString()).isEqualTo("BigDecimal");
    }

    @Test
    public void testFieldAccessExpr() {
        String expr = "java.math.MathContext.DECIMAL128";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<Expression> result = parser.parseExpression(expr);
        assertThat(result.getResult()).isPresent();

        Expression expression = result.getResult().get();
        assertThat(expression).isInstanceOf(FieldAccessExpr.class);
        assertThat(expression.toString()).isEqualTo("java.math.MathContext.DECIMAL128");
    }

    @Test
    public void testTypeType() {
        String expr = "java.lang.Void";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<Type> result = parser.parseType(expr);
        assertThat(result.getResult()).isPresent();

        Type type = result.getResult().get();
        assertThat(type).isInstanceOf(ClassOrInterfaceType.class);
        assertThat(type.toString()).isEqualTo("java.lang.Void");
    }

    @Test
    public void testTemporalLiteral() {
        String expr = "1m5s";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<Expression> result = parser.parseExpression(expr);
        assertThat(result.getResult()).isPresent();

        Expression expression = result.getResult().get();
        assertThat(expression).isInstanceOf(TemporalLiteralExpr.class);

        System.out.println(expression);
        System.out.println(toString(expression));

        TemporalLiteralExpr temporalLiteralExpr = (TemporalLiteralExpr) expression;
        assertThat(temporalLiteralExpr.getChunks().size()).isEqualTo(2);
        TemporalLiteralChunkExpr chunk0 = (TemporalLiteralChunkExpr) temporalLiteralExpr.getChunks().get(0);
        assertThat(chunk0.getValue()).isEqualTo(1);
        assertThat(chunk0.getTimeUnit()).isEqualTo(TimeUnit.MINUTES);
        TemporalLiteralChunkExpr chunk1 = (TemporalLiteralChunkExpr) temporalLiteralExpr.getChunks().get(1);
        assertThat(chunk1.getValue()).isEqualTo(5);
        assertThat(chunk1.getTimeUnit()).isEqualTo(TimeUnit.SECONDS);
    }

    private String toString(Node n) {
        return PrintUtil.printNode(n);
    }
}