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
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import org.junit.jupiter.api.Test;
import org.mvel3.parser.ast.expr.TemporalLiteralChunkExpr;
import org.mvel3.parser.ast.expr.TemporalLiteralExpr;
import org.mvel3.parser.printer.PrintUtil;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the MVEL parser using ANTLR4.
 * Assertions are based on the JavaParser AST which are created by Mvel3ToJavaParserVisitor.
 */
class Antlr4MvelParserJavaParserASTTest {

    @Test
    void testExpression() {
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
    public void testCompilationUnit() {
        String code = "package org.example;\n"
                + "import org.example.util.MyUtil;\n"
                + "public class Sample { void hello() { MyUtil.doSomething(); } }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        CompilationUnit compilationUnit = result.getResult().get();
        assertThat(compilationUnit.getPackageDeclaration()).isPresent();
        assertThat(compilationUnit.getPackageDeclaration().get().getNameAsString()).isEqualTo("org.example");
        assertThat(compilationUnit.getImports()).hasSize(1);
        assertThat(compilationUnit.getImports().get(0).getNameAsString()).isEqualTo("org.example.util.MyUtil");
        assertThat(compilationUnit.getType(0).getNameAsString()).isEqualTo("Sample");
        assertThat(compilationUnit.getType(0).isPublic()).isTrue();
        assertThat(compilationUnit.getType(0).getMethods()).hasSize(1);
        assertThat(compilationUnit.getType(0).getMethods().get(0).getNameAsString()).isEqualTo("hello");
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

    @Test
    void testInstanceOf() {
        String expr = "obj instanceof String";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<Expression> result = parser.parseExpression(expr);
        assertThat(result.getResult()).isPresent();

        InstanceOfExpr instanceOfExpr = (InstanceOfExpr) result.getResult().get();
        assertThat(toString(instanceOfExpr.getExpression())).isEqualTo("obj");
        assertThat(instanceOfExpr.getType().asString()).isEqualTo("String");
        assertThat(instanceOfExpr.getPattern()).isEmpty();
    }

    @Test
    void testInstanceOfPatternMatching() {
        String expr = "obj instanceof String s";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<Expression> result = parser.parseExpression(expr);
        assertThat(result.getResult()).isPresent();

        InstanceOfExpr instanceOfExpr = (InstanceOfExpr) result.getResult().get();
        assertThat(toString(instanceOfExpr.getExpression())).isEqualTo("obj");
        assertThat(instanceOfExpr.getPattern()).isPresent();
        assertThat(instanceOfExpr.getName()).isPresent();
        assertThat(instanceOfExpr.getName().get().asString()).isEqualTo("s");
    }

    @Test
    void testHexLiteral() {
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<Expression> result = parser.parseExpression("0xFF");
        assertThat(result.getResult()).isPresent();

        IntegerLiteralExpr intExpr = (IntegerLiteralExpr) result.getResult().get();
        assertThat(intExpr.getValue()).isEqualTo("0xFF");
    }

    @Test
    void testHexLongLiteral() {
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<Expression> result = parser.parseExpression("0xFFL");
        assertThat(result.getResult()).isPresent();

        LongLiteralExpr longExpr = (LongLiteralExpr) result.getResult().get();
        assertThat(longExpr.getValue()).isEqualTo("0xFFL");
    }

    @Test
    void testOctalLiteral() {
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<Expression> result = parser.parseExpression("077");
        assertThat(result.getResult()).isPresent();

        IntegerLiteralExpr intExpr = (IntegerLiteralExpr) result.getResult().get();
        assertThat(intExpr.getValue()).isEqualTo("077");
    }

    @Test
    void testBinaryLiteral() {
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<Expression> result = parser.parseExpression("0b1010");
        assertThat(result.getResult()).isPresent();

        IntegerLiteralExpr intExpr = (IntegerLiteralExpr) result.getResult().get();
        assertThat(intExpr.getValue()).isEqualTo("0b1010");
    }

    @Test
    void testHexFloatLiteral() {
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<Expression> result = parser.parseExpression("0x1.0p10");
        assertThat(result.getResult()).isPresent();

        DoubleLiteralExpr doubleExpr = (DoubleLiteralExpr) result.getResult().get();
        assertThat(doubleExpr.getValue()).isEqualTo("0x1.0p10");
    }

    @Test
    void testClassLiteral() {
        String expr = "String.class";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<Expression> result = parser.parseExpression(expr);
        assertThat(result.getResult()).isPresent();

        ClassExpr classExpr = (ClassExpr) result.getResult().get();
        assertThat(classExpr.getType().asString()).isEqualTo("String");
    }

    @Test
    void testPrimitiveClassLiteral() {
        String expr = "int.class";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<Expression> result = parser.parseExpression(expr);
        assertThat(result.getResult()).isPresent();

        ClassExpr classExpr = (ClassExpr) result.getResult().get();
        assertThat(classExpr.getType().asString()).isEqualTo("int");
    }

    @Test
    void testVoidClassLiteral() {
        String expr = "void.class";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<Expression> result = parser.parseExpression(expr);
        assertThat(result.getResult()).isPresent();

        ClassExpr classExpr = (ClassExpr) result.getResult().get();
        assertThat(classExpr.getType().asString()).isEqualTo("void");
    }

    @Test
    void testMethodReferenceWithExpression() {
        String expr = "System.out::println";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<Expression> result = parser.parseExpression(expr);
        assertThat(result.getResult()).isPresent();

        MethodReferenceExpr methodRef = (MethodReferenceExpr) result.getResult().get();
        assertThat(toString(methodRef.getScope())).isEqualTo("System.out");
        assertThat(methodRef.getIdentifier()).isEqualTo("println");
    }

    @Test
    void testMethodReferenceWithType() {
        String expr = "String::valueOf";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<Expression> result = parser.parseExpression(expr);
        assertThat(result.getResult()).isPresent();

        MethodReferenceExpr methodRef = (MethodReferenceExpr) result.getResult().get();
        assertThat(methodRef.getIdentifier()).isEqualTo("valueOf");
    }

    @Test
    void testConstructorReference() {
        String expr = "ArrayList::new";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<Expression> result = parser.parseExpression(expr);
        assertThat(result.getResult()).isPresent();

        MethodReferenceExpr methodRef = (MethodReferenceExpr) result.getResult().get();
        assertThat(methodRef.getIdentifier()).isEqualTo("new");
    }

    @Test
    void testTernaryExpression() {
        String expr = "x > 0 ? x : -x";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<Expression> result = parser.parseExpression(expr);
        assertThat(result.getResult()).isPresent();

        ConditionalExpr conditionalExpr = (ConditionalExpr) result.getResult().get();
        assertThat(toString(conditionalExpr.getCondition())).isEqualTo("x > 0");
        assertThat(toString(conditionalExpr.getThenExpr())).isEqualTo("x");
        assertThat(toString(conditionalExpr.getElseExpr())).isEqualTo("-x");
    }

    @Test
    void testTryCatchFinally() {
        String block = "{ try { x = 1; } catch (Exception e) { x = 2; } finally { x = 3; } }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<BlockStmt> result = parser.parseBlock(block);
        assertThat(result.getResult()).isPresent();

        TryStmt tryStmt = (TryStmt) result.getResult().get().getStatement(0);
        assertThat(tryStmt.getCatchClauses()).hasSize(1);
        assertThat(tryStmt.getCatchClauses().get(0).getParameter().getNameAsString()).isEqualTo("e");
        assertThat(tryStmt.getCatchClauses().get(0).getParameter().getType().asString()).isEqualTo("Exception");
        assertThat(tryStmt.getFinallyBlock()).isPresent();
    }

    @Test
    void testTryMultiCatch() {
        String block = "{ try { x = 1; } catch (IOException | NullPointerException e) { x = 2; } }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<BlockStmt> result = parser.parseBlock(block);
        assertThat(result.getResult()).isPresent();

        TryStmt tryStmt = (TryStmt) result.getResult().get().getStatement(0);
        assertThat(tryStmt.getCatchClauses()).hasSize(1);
        assertThat(tryStmt.getCatchClauses().get(0).getParameter().getType().asString()).isEqualTo("IOException|NullPointerException");
    }

    @Test
    void testTryWithResources() {
        String block = "{ try (InputStream is = new FileInputStream(\"f\")) { x = 1; } catch (Exception e) { x = 2; } }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<BlockStmt> result = parser.parseBlock(block);
        assertThat(result.getResult()).isPresent();

        TryStmt tryStmt = (TryStmt) result.getResult().get().getStatement(0);
        assertThat(tryStmt.getResources()).hasSize(1);
        assertThat(tryStmt.getCatchClauses()).hasSize(1);
    }

    private String toString(Node n) {
        return PrintUtil.printNode(n);
    }
}
