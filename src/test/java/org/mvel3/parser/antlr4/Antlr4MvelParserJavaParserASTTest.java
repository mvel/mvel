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
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.expr.SwitchExpr;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.ThrowStmt;
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
    void testBreakStatement() {
        String block = "{ for (int i = 0; i < 10; i++) { break; } }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<BlockStmt> result = parser.parseBlock(block);
        assertThat(result.getResult()).isPresent();

        // The break is inside the for loop's body block
        BlockStmt forBody = (BlockStmt) result.getResult().get().getStatement(0).asForStmt().getBody();
        BreakStmt breakStmt = (BreakStmt) forBody.getStatement(0);
        assertThat(breakStmt.getLabel()).isEmpty();
    }

    @Test
    void testBreakWithLabel() {
        String block = "{ outer: for (int i = 0; i < 10; i++) { break outer; } }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<BlockStmt> result = parser.parseBlock(block);
        assertThat(result.getResult()).isPresent();
    }

    @Test
    void testContinueStatement() {
        String block = "{ for (int i = 0; i < 10; i++) { continue; } }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<BlockStmt> result = parser.parseBlock(block);
        assertThat(result.getResult()).isPresent();

        BlockStmt forBody = (BlockStmt) result.getResult().get().getStatement(0).asForStmt().getBody();
        ContinueStmt continueStmt = (ContinueStmt) forBody.getStatement(0);
        assertThat(continueStmt.getLabel()).isEmpty();
    }

    @Test
    void testThrowStatement() {
        String block = "{ throw new RuntimeException(\"error\"); }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<BlockStmt> result = parser.parseBlock(block);
        assertThat(result.getResult()).isPresent();

        ThrowStmt throwStmt = (ThrowStmt) result.getResult().get().getStatement(0);
        assertThat(toString(throwStmt.getExpression())).isEqualTo("new RuntimeException(\"error\")");
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

    @Test
    void testInnerClassCreation() {
        // expr.new Inner(args)
        String expr = "outer.new Inner(1, 2)";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<Expression> result = parser.parseExpression(expr);
        assertThat(result.getResult()).isPresent();

        ObjectCreationExpr objectCreation = (ObjectCreationExpr) result.getResult().get();
        assertThat(objectCreation.getType().getNameAsString()).isEqualTo("Inner");
        assertThat(toString(objectCreation.getScope().get())).isEqualTo("outer");
        assertThat(objectCreation.getArguments()).hasSize(2);
        assertThat(objectCreation.getAnonymousClassBody()).isEmpty();
    }

    @Test
    void testInnerClassCreationWithDiamond() {
        // expr.new Inner<>()
        String expr = "outer.new Inner<>()";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<Expression> result = parser.parseExpression(expr);
        assertThat(result.getResult()).isPresent();

        ObjectCreationExpr objectCreation = (ObjectCreationExpr) result.getResult().get();
        assertThat(objectCreation.getType().getNameAsString()).isEqualTo("Inner");
        assertThat(toString(objectCreation.getScope().get())).isEqualTo("outer");
        assertThat(objectCreation.getType().getTypeArguments()).isPresent();
    }

    @Test
    void testAnonymousClassCreation() {
        // new Runnable() { public void run() { } }
        String expr = "new Runnable() { public void run() { } }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<Expression> result = parser.parseExpression(expr);
        assertThat(result.getResult()).isPresent();

        ObjectCreationExpr objectCreation = (ObjectCreationExpr) result.getResult().get();
        assertThat(objectCreation.getType().getNameAsString()).isEqualTo("Runnable");
        assertThat(objectCreation.getAnonymousClassBody()).isPresent();
        assertThat(objectCreation.getAnonymousClassBody().get()).hasSize(1);
        // The anonymous class body should contain a method declaration
        assertThat(objectCreation.getAnonymousClassBody().get().get(0).isMethodDeclaration()).isTrue();
        assertThat(objectCreation.getAnonymousClassBody().get().get(0).asMethodDeclaration().getNameAsString()).isEqualTo("run");
    }

    @Test
    void testAnonymousClassCreationNoBody() {
        // new ArrayList() — no anonymous body
        String expr = "new ArrayList()";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<Expression> result = parser.parseExpression(expr);
        assertThat(result.getResult()).isPresent();

        ObjectCreationExpr objectCreation = (ObjectCreationExpr) result.getResult().get();
        assertThat(objectCreation.getType().getNameAsString()).isEqualTo("ArrayList");
        assertThat(objectCreation.getAnonymousClassBody()).isEmpty();
    }

    @Test
    void testGenericMethodCallInPrimary() {
        // <Type>method(args) — generic method call without scope (in primary)
        String expr = "<String>valueOf(42)";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<Expression> result = parser.parseExpression(expr);
        assertThat(result.getResult()).isPresent();

        MethodCallExpr methodCall = (MethodCallExpr) result.getResult().get();
        assertThat(methodCall.getNameAsString()).isEqualTo("valueOf");
        assertThat(methodCall.getScope()).isEmpty();
        assertThat(methodCall.getTypeArguments()).isPresent();
        assertThat(methodCall.getTypeArguments().get()).hasSize(1);
        assertThat(methodCall.getTypeArguments().get().get(0).asString()).isEqualTo("String");
    }

    @Test
    void testExplicitGenericInvocation() {
        // expr.<Type>method(args)
        String expr = "list.<String>stream()";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<Expression> result = parser.parseExpression(expr);
        assertThat(result.getResult()).isPresent();

        MethodCallExpr methodCall = (MethodCallExpr) result.getResult().get();
        assertThat(methodCall.getNameAsString()).isEqualTo("stream");
        assertThat(toString(methodCall.getScope().get())).isEqualTo("list");
        assertThat(methodCall.getTypeArguments()).isPresent();
        assertThat(methodCall.getTypeArguments().get()).hasSize(1);
        assertThat(methodCall.getTypeArguments().get().get(0).asString()).isEqualTo("String");
    }

    @Test
    void testExplicitGenericInvocationMultipleTypeArgs() {
        // expr.<K, V>method(args)
        String expr = "map.<String, Integer>entrySet()";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<Expression> result = parser.parseExpression(expr);
        assertThat(result.getResult()).isPresent();

        MethodCallExpr methodCall = (MethodCallExpr) result.getResult().get();
        assertThat(methodCall.getNameAsString()).isEqualTo("entrySet");
        assertThat(methodCall.getTypeArguments()).isPresent();
        assertThat(methodCall.getTypeArguments().get()).hasSize(2);
    }

    @Test
    void testSwitchExpressionArrow() {
        // Switch expression as an expression (Java 17)
        String expr = "switch (day) { case 1 -> \"Monday\"; case 2 -> \"Tuesday\"; default -> \"Other\"; }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<Expression> result = parser.parseExpression(expr);
        assertThat(result.getResult()).isPresent();

        SwitchExpr switchExpr = (SwitchExpr) result.getResult().get();
        assertThat(toString(switchExpr.getSelector())).isEqualTo("day");
        assertThat(switchExpr.getEntries()).hasSize(3);
        // case 1 -> "Monday"
        assertThat(switchExpr.getEntry(0).getLabels()).hasSize(1);
        assertThat(switchExpr.getEntry(0).getType()).isEqualTo(SwitchEntry.Type.EXPRESSION);
        // case 2 -> "Tuesday"
        assertThat(switchExpr.getEntry(1).getLabels()).hasSize(1);
        // default -> "Other"
        assertThat(switchExpr.getEntry(2).getLabels()).isEmpty(); // default has empty labels
    }

    @Test
    void testSwitchExpressionMultipleLabels() {
        // Switch expression with multiple labels per case
        String expr = "switch (day) { case 1, 7 -> \"Weekend\"; default -> \"Weekday\"; }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<Expression> result = parser.parseExpression(expr);
        assertThat(result.getResult()).isPresent();

        SwitchExpr switchExpr = (SwitchExpr) result.getResult().get();
        assertThat(switchExpr.getEntries()).hasSize(2);
        // case 1, 7 has 2 labels
        assertThat(switchExpr.getEntry(0).getLabels()).hasSize(2);
    }

    @Test
    void testSwitchExpressionBlock() {
        // Switch expression with block outcome
        String expr = "switch (x) { case 1 -> { int y = x + 1; } default -> { int y = 0; } }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<Expression> result = parser.parseExpression(expr);
        assertThat(result.getResult()).isPresent();

        SwitchExpr switchExpr = (SwitchExpr) result.getResult().get();
        assertThat(switchExpr.getEntries()).hasSize(2);
        assertThat(switchExpr.getEntry(0).getType()).isEqualTo(SwitchEntry.Type.BLOCK);
    }

    @Test
    void testSwitchExpressionAsStatement() {
        // Switch expression used as a statement
        String block = "{ switch (x) { case 1 -> doSomething(); default -> doDefault(); } }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<BlockStmt> result = parser.parseBlock(block);
        assertThat(result.getResult()).isPresent();

        // It should be an ExpressionStmt wrapping a SwitchExpr
        assertThat(result.getResult().get().getStatement(0).isExpressionStmt()).isTrue();
        Expression switchExpr = result.getResult().get().getStatement(0).asExpressionStmt().getExpression();
        assertThat(switchExpr).isInstanceOf(SwitchExpr.class);
    }

    private String toString(Node n) {
        return PrintUtil.printNode(n);
    }
}
