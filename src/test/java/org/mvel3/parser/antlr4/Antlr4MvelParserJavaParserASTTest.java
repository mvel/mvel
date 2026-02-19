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
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.CompactConstructorDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.ReceiverParameter;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.modules.ModuleDeclaration;
import com.github.javaparser.ast.modules.ModuleExportsDirective;
import com.github.javaparser.ast.modules.ModuleProvidesDirective;
import com.github.javaparser.ast.modules.ModuleRequiresDirective;
import com.github.javaparser.ast.modules.ModuleUsesDirective;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.PatternExpr;
import com.github.javaparser.ast.expr.SwitchExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.LocalClassDeclarationStmt;
import com.github.javaparser.ast.stmt.LocalRecordDeclarationStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.SynchronizedStmt;
import com.github.javaparser.ast.stmt.YieldStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.type.WildcardType;
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
    void testSuperSuffixMethodCall() {
        // Outer.super.method()
        String expr = "Outer.super.toString()";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<Expression> result = parser.parseExpression(expr);
        assertThat(result.getResult()).isPresent();

        MethodCallExpr methodCall = (MethodCallExpr) result.getResult().get();
        assertThat(methodCall.getNameAsString()).isEqualTo("toString");
        assertThat(methodCall.getScope()).isPresent();
        assertThat(toString(methodCall.getScope().get())).isEqualTo("Outer.super");
    }

    @Test
    void testSuperSuffixFieldAccess() {
        // Outer.super.field
        String expr = "Outer.super.value";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<Expression> result = parser.parseExpression(expr);
        assertThat(result.getResult()).isPresent();

        FieldAccessExpr fieldAccess = (FieldAccessExpr) result.getResult().get();
        assertThat(fieldAccess.getNameAsString()).isEqualTo("value");
        assertThat(toString(fieldAccess.getScope())).isEqualTo("Outer.super");
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
    void testConstructorWithTypeWitness() {
        // new <String>Foo() — explicit type witness on constructor
        String expr = "new <String>Foo()";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<Expression> result = parser.parseExpression(expr);
        assertThat(result.getResult()).isPresent();

        ObjectCreationExpr objectCreation = (ObjectCreationExpr) result.getResult().get();
        assertThat(objectCreation.getType().getNameAsString()).isEqualTo("Foo");
        assertThat(objectCreation.getTypeArguments()).isPresent();
        assertThat(objectCreation.getTypeArguments().get()).hasSize(1);
        assertThat(objectCreation.getTypeArguments().get().get(0).asString()).isEqualTo("String");
    }

    @Test
    void testConstructorWithMultipleTypeWitness() {
        // new <String, Integer>Foo(1) — multiple type witnesses
        String expr = "new <String, Integer>Foo(1)";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<Expression> result = parser.parseExpression(expr);
        assertThat(result.getResult()).isPresent();

        ObjectCreationExpr objectCreation = (ObjectCreationExpr) result.getResult().get();
        assertThat(objectCreation.getType().getNameAsString()).isEqualTo("Foo");
        assertThat(objectCreation.getTypeArguments()).isPresent();
        assertThat(objectCreation.getTypeArguments().get()).hasSize(2);
        assertThat(objectCreation.getTypeArguments().get().get(0).asString()).isEqualTo("String");
        assertThat(objectCreation.getTypeArguments().get().get(1).asString()).isEqualTo("Integer");
        assertThat(objectCreation.getArguments()).hasSize(1);
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

    @Test
    void testSwitchGuardedPatternSimple() {
        // Simple type pattern in switch case
        String expr = "switch (obj) { case String s -> s.toUpperCase(); default -> \"\"; }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<Expression> result = parser.parseExpression(expr);
        assertThat(result.getResult()).isPresent();

        SwitchExpr switchExpr = (SwitchExpr) result.getResult().get();
        assertThat(switchExpr.getEntries()).hasSize(2);
        Expression label = switchExpr.getEntry(0).getLabels().get(0);
        assertThat(label).isInstanceOf(PatternExpr.class);
        PatternExpr patternExpr = (PatternExpr) label;
        assertThat(patternExpr.getType().asString()).isEqualTo("String");
        assertThat(patternExpr.getName().asString()).isEqualTo("s");
    }

    @Test
    void testSwitchGuardedPatternWithGuard() {
        // Guarded pattern with && guard
        String expr = "switch (obj) { case String s && s.length() > 5 -> s; default -> \"\"; }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<Expression> result = parser.parseExpression(expr);
        assertThat(result.getResult()).isPresent();

        SwitchExpr switchExpr = (SwitchExpr) result.getResult().get();
        Expression label = switchExpr.getEntry(0).getLabels().get(0);
        assertThat(label).isInstanceOf(BinaryExpr.class);
        BinaryExpr binaryExpr = (BinaryExpr) label;
        assertThat(binaryExpr.getOperator()).isEqualTo(BinaryExpr.Operator.AND);
        assertThat(binaryExpr.getLeft()).isInstanceOf(PatternExpr.class);
    }

    @Test
    void testSwitchGuardedPatternMultipleGuards() {
        // Multiple guard expressions chained with &&
        // The grammar's ('&&' expression)* treats "s.length() > 5 && !s.isEmpty()" as a single expression
        String expr = "switch (obj) { case String s && s.length() > 5 && !s.isEmpty() -> s; default -> \"\"; }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<Expression> result = parser.parseExpression(expr);
        assertThat(result.getResult()).isPresent();

        SwitchExpr switchExpr = (SwitchExpr) result.getResult().get();
        Expression label = switchExpr.getEntry(0).getLabels().get(0);
        // BinaryExpr(PatternExpr(String s), AND, BinaryExpr(s.length() > 5, AND, !s.isEmpty()))
        assertThat(label).isInstanceOf(BinaryExpr.class);
        BinaryExpr binaryExpr = (BinaryExpr) label;
        assertThat(binaryExpr.getOperator()).isEqualTo(BinaryExpr.Operator.AND);
        assertThat(binaryExpr.getLeft()).isInstanceOf(PatternExpr.class);
        assertThat(binaryExpr.getRight()).isInstanceOf(BinaryExpr.class);
        BinaryExpr guardExpr = (BinaryExpr) binaryExpr.getRight();
        assertThat(guardExpr.getOperator()).isEqualTo(BinaryExpr.Operator.AND);
    }

    @Test
    void testSwitchGuardedPatternFinalModifier() {
        // Guarded pattern with final modifier
        String expr = "switch (obj) { case final String s -> s; default -> \"\"; }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<Expression> result = parser.parseExpression(expr);
        assertThat(result.getResult()).isPresent();

        SwitchExpr switchExpr = (SwitchExpr) result.getResult().get();
        Expression label = switchExpr.getEntry(0).getLabels().get(0);
        assertThat(label).isInstanceOf(PatternExpr.class);
        PatternExpr patternExpr = (PatternExpr) label;
        assertThat(patternExpr.getModifiers()).isNotEmpty();
        assertThat(patternExpr.getModifiers().get(0).getKeyword()).isEqualTo(Modifier.Keyword.FINAL);
    }

    @Test
    void testSwitchGuardedPatternParenthesized() {
        // Parenthesized guarded pattern
        String expr = "switch (obj) { case (String s) -> s; default -> \"\"; }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<Expression> result = parser.parseExpression(expr);
        assertThat(result.getResult()).isPresent();

        SwitchExpr switchExpr = (SwitchExpr) result.getResult().get();
        Expression label = switchExpr.getEntry(0).getLabels().get(0);
        assertThat(label).isInstanceOf(EnclosedExpr.class);
        EnclosedExpr enclosed = (EnclosedExpr) label;
        assertThat(enclosed.getInner()).isInstanceOf(PatternExpr.class);
    }

    @Test
    void testSwitchLabelTypePattern() {
        // Traditional switch statement with type pattern: case String s:
        String block = "{ switch (obj) { case String s: System.out.println(s); break; default: break; } }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<BlockStmt> result = parser.parseBlock(block);
        assertThat(result.getResult()).isPresent();

        SwitchStmt switchStmt = (SwitchStmt) result.getResult().get().getStatement(0);
        assertThat(switchStmt.getEntries()).hasSize(2);
        Expression label = switchStmt.getEntry(0).getLabels().get(0);
        assertThat(label).isInstanceOf(PatternExpr.class);
        PatternExpr patternExpr = (PatternExpr) label;
        assertThat(patternExpr.getType().asString()).isEqualTo("String");
        assertThat(patternExpr.getName().asString()).isEqualTo("s");
    }

    @Test
    void testSwitchLabelTypePatternQualified() {
        // Type pattern with qualified type
        String block = "{ switch (obj) { case java.lang.Integer i: break; default: break; } }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<BlockStmt> result = parser.parseBlock(block);
        assertThat(result.getResult()).isPresent();

        SwitchStmt switchStmt = (SwitchStmt) result.getResult().get().getStatement(0);
        Expression label = switchStmt.getEntry(0).getLabels().get(0);
        assertThat(label).isInstanceOf(PatternExpr.class);
        PatternExpr patternExpr = (PatternExpr) label;
        assertThat(patternExpr.getName().asString()).isEqualTo("i");
    }

    @Test
    void testTypeAnnotationOnArrayDimension() {
        // Annotation on array dimension: String @NonNull []
        String block = "{ String @NonNull [] arr; }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<BlockStmt> result = parser.parseBlock(block);
        assertThat(result.getResult()).isPresent();

        VariableDeclarationExpr varDecl = result.getResult().get().getStatement(0)
                .asExpressionStmt().getExpression().asVariableDeclarationExpr();
        Type type = varDecl.getVariable(0).getType();
        assertThat(type).isInstanceOf(ArrayType.class);
        ArrayType arrayType = (ArrayType) type;
        assertThat(arrayType.getAnnotations()).hasSize(1);
        assertThat(toString(arrayType.getAnnotations().get(0))).isEqualTo("@NonNull");
        assertThat(arrayType.getComponentType().getAnnotations()).isEmpty();
    }

    @Test
    void testTypeAnnotationOnMultiDimArray() {
        // Annotations on multi-dimensional array: int @A [] @B []
        String block = "{ int @A [] @B [] arr; }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<BlockStmt> result = parser.parseBlock(block);
        assertThat(result.getResult()).isPresent();

        VariableDeclarationExpr varDecl = result.getResult().get().getStatement(0)
                .asExpressionStmt().getExpression().asVariableDeclarationExpr();
        Type type = varDecl.getVariable(0).getType();
        assertThat(type).isInstanceOf(ArrayType.class);
        ArrayType outerArray = (ArrayType) type;
        // Outer dimension has @B
        assertThat(outerArray.getAnnotations()).hasSize(1);
        assertThat(toString(outerArray.getAnnotations().get(0))).isEqualTo("@B");
        // Inner dimension has @A
        assertThat(outerArray.getComponentType()).isInstanceOf(ArrayType.class);
        ArrayType innerArray = (ArrayType) outerArray.getComponentType();
        assertThat(innerArray.getAnnotations()).hasSize(1);
        assertThat(toString(innerArray.getAnnotations().get(0))).isEqualTo("@A");
    }

    @Test
    void testEmptyStatement() {
        String block = "{ ; }";


        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<BlockStmt> result = parser.parseBlock(block);
        assertThat(result.getResult()).isPresent();

        assertThat(result.getResult().get().getStatement(0)).isInstanceOf(EmptyStmt.class);
    }

    @Test
    void testLabeledStatement() {
        String block = "{ outer: for (int i = 0; i < 10; i++) { break outer; } }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<BlockStmt> result = parser.parseBlock(block);
        assertThat(result.getResult()).isPresent();

        LabeledStmt labeledStmt = (LabeledStmt) result.getResult().get().getStatement(0);
        assertThat(labeledStmt.getLabel().asString()).isEqualTo("outer");
        assertThat(labeledStmt.getStatement().isForStmt()).isTrue();
    }

    @Test
    void testYieldStatement() {
        // yield is used inside switch expression blocks (Java 17)
        String block = "{ yield 42; }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<BlockStmt> result = parser.parseBlock(block);
        assertThat(result.getResult()).isPresent();

        YieldStmt yieldStmt = (YieldStmt) result.getResult().get().getStatement(0);
        assertThat(toString(yieldStmt.getExpression())).isEqualTo("42");
    }

    @Test
    void testSynchronizedBlock() {
        String block = "{ synchronized (lock) { count++; } }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<BlockStmt> result = parser.parseBlock(block);
        assertThat(result.getResult()).isPresent();

        SynchronizedStmt syncStmt = (SynchronizedStmt) result.getResult().get().getStatement(0);
        assertThat(toString(syncStmt.getExpression())).isEqualTo("lock");
        assertThat(syncStmt.getBody().getStatements()).hasSize(1);
    }

    @Test
    void testAssertStatement() {
        String block = "{ assert x > 0; }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<BlockStmt> result = parser.parseBlock(block);
        assertThat(result.getResult()).isPresent();

        AssertStmt assertStmt = (AssertStmt) result.getResult().get().getStatement(0);
        assertThat(toString(assertStmt.getCheck())).isEqualTo("x > 0");
        assertThat(assertStmt.getMessage()).isEmpty();
    }

    @Test
    void testAssertStatementWithMessage() {
        String block = "{ assert x > 0 : \"x must be positive\"; }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<BlockStmt> result = parser.parseBlock(block);
        assertThat(result.getResult()).isPresent();

        AssertStmt assertStmt = (AssertStmt) result.getResult().get().getStatement(0);
        assertThat(toString(assertStmt.getCheck())).isEqualTo("x > 0");
        assertThat(assertStmt.getMessage()).isPresent();
        assertThat(toString(assertStmt.getMessage().get())).isEqualTo("\"x must be positive\"");
    }

    @Test
    void testClassWithModifiers() {
        String code = "public abstract class Foo { }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        ClassOrInterfaceDeclaration classDecl = (ClassOrInterfaceDeclaration) result.getResult().get().getType(0);
        assertThat(classDecl.getNameAsString()).isEqualTo("Foo");
        assertThat(classDecl.getModifiers()).extracting(Modifier::getKeyword)
                .containsExactly(Modifier.Keyword.PUBLIC, Modifier.Keyword.ABSTRACT);
    }

    @Test
    void testMethodWithModifiers() {
        String expr = "new Runnable() { private static void run() { } }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<Expression> result = parser.parseExpression(expr);
        assertThat(result.getResult()).isPresent();

        ObjectCreationExpr objectCreation = (ObjectCreationExpr) result.getResult().get();
        assertThat(objectCreation.getAnonymousClassBody()).isPresent();
        assertThat(objectCreation.getAnonymousClassBody().get()).hasSize(1);
        var method = objectCreation.getAnonymousClassBody().get().get(0).asMethodDeclaration();
        assertThat(method.getNameAsString()).isEqualTo("run");
        assertThat(method.getModifiers()).extracting(Modifier::getKeyword)
                .containsExactly(Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC);
    }

    @Test
    void testMethodWithReturnType() {
        String code = "public class Foo { public int getValue() { return 0; } }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        MethodDeclaration method = result.getResult().get().getType(0).getMethods().get(0);
        assertThat(method.getNameAsString()).isEqualTo("getValue");
        assertThat(method.getTypeAsString()).isEqualTo("int");
        assertThat(method.getParameters()).isEmpty();
    }

    @Test
    void testMethodWithParametersAndThrows() {
        String code = "public class Foo { public String process(int x, String name) throws Exception { return name; } }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        MethodDeclaration method = result.getResult().get().getType(0).getMethods().get(0);
        assertThat(method.getNameAsString()).isEqualTo("process");
        assertThat(method.getTypeAsString()).isEqualTo("String");
        assertThat(method.getParameters()).hasSize(2);
        assertThat(method.getParameter(0).getTypeAsString()).isEqualTo("int");
        assertThat(method.getParameter(0).getNameAsString()).isEqualTo("x");
        assertThat(method.getParameter(1).getTypeAsString()).isEqualTo("String");
        assertThat(method.getParameter(1).getNameAsString()).isEqualTo("name");
        assertThat(method.getThrownExceptions()).hasSize(1);
        assertThat(method.getThrownException(0).asString()).isEqualTo("Exception");
    }

    @Test
    void testFieldDeclaration() {
        String code = "public class Foo { private int count = 0; }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        FieldDeclaration field = result.getResult().get().getType(0).getFields().get(0);
        assertThat(field.getModifiers()).extracting(Modifier::getKeyword)
                .containsExactly(Modifier.Keyword.PRIVATE);
        assertThat(field.getVariables()).hasSize(1);
        assertThat(field.getVariable(0).getNameAsString()).isEqualTo("count");
        assertThat(field.getVariable(0).getTypeAsString()).isEqualTo("int");
        assertThat(field.getVariable(0).getInitializer()).isPresent();
        assertThat(toString(field.getVariable(0).getInitializer().get())).isEqualTo("0");
    }

    @Test
    void testFieldDeclarationMultipleDeclarators() {
        String code = "public class Foo { private static int x = 1, y, z = 3; }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        FieldDeclaration field = result.getResult().get().getType(0).getFields().get(0);
        assertThat(field.getModifiers()).extracting(Modifier::getKeyword)
                .containsExactly(Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC);
        assertThat(field.getVariables()).hasSize(3);
        assertThat(field.getVariable(0).getNameAsString()).isEqualTo("x");
        assertThat(field.getVariable(0).getInitializer()).isPresent();
        assertThat(field.getVariable(1).getNameAsString()).isEqualTo("y");
        assertThat(field.getVariable(1).getInitializer()).isEmpty();
        assertThat(field.getVariable(2).getNameAsString()).isEqualTo("z");
        assertThat(field.getVariable(2).getInitializer()).isPresent();
    }

    @Test
    void testFieldDeclarationInAnonymousClass() {
        String expr = "new Runnable() { private String name = \"test\"; public void run() { } }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<Expression> result = parser.parseExpression(expr);
        assertThat(result.getResult()).isPresent();

        ObjectCreationExpr objectCreation = (ObjectCreationExpr) result.getResult().get();
        assertThat(objectCreation.getAnonymousClassBody()).isPresent();
        assertThat(objectCreation.getAnonymousClassBody().get()).hasSize(2);

        FieldDeclaration field = objectCreation.getAnonymousClassBody().get().get(0).asFieldDeclaration();
        assertThat(field.getVariable(0).getNameAsString()).isEqualTo("name");
        assertThat(field.getVariable(0).getTypeAsString()).isEqualTo("String");
        assertThat(field.getModifiers()).extracting(Modifier::getKeyword)
                .containsExactly(Modifier.Keyword.PRIVATE);

        var method = objectCreation.getAnonymousClassBody().get().get(1).asMethodDeclaration();
        assertThat(method.getNameAsString()).isEqualTo("run");
    }

    @Test
    void testConstructorDeclaration() {
        String code = "public class Foo { public Foo(int x) { } }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        ConstructorDeclaration constructor = result.getResult().get().getType(0).getConstructors().get(0);
        assertThat(constructor.getNameAsString()).isEqualTo("Foo");
        assertThat(constructor.getModifiers()).extracting(Modifier::getKeyword)
                .containsExactly(Modifier.Keyword.PUBLIC);
        assertThat(constructor.getParameters()).hasSize(1);
        assertThat(constructor.getParameter(0).getTypeAsString()).isEqualTo("int");
        assertThat(constructor.getParameter(0).getNameAsString()).isEqualTo("x");
        assertThat(constructor.getBody()).isNotNull();
    }

    @Test
    void testConstructorDeclarationWithThrows() {
        String code = "public class Foo { protected Foo(String name, int age) throws Exception, IOException { } }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        ConstructorDeclaration constructor = result.getResult().get().getType(0).getConstructors().get(0);
        assertThat(constructor.getNameAsString()).isEqualTo("Foo");
        assertThat(constructor.getModifiers()).extracting(Modifier::getKeyword)
                .containsExactly(Modifier.Keyword.PROTECTED);
        assertThat(constructor.getParameters()).hasSize(2);
        assertThat(constructor.getParameter(0).getTypeAsString()).isEqualTo("String");
        assertThat(constructor.getParameter(0).getNameAsString()).isEqualTo("name");
        assertThat(constructor.getParameter(1).getTypeAsString()).isEqualTo("int");
        assertThat(constructor.getParameter(1).getNameAsString()).isEqualTo("age");
        assertThat(constructor.getThrownExceptions()).hasSize(2);
        assertThat(constructor.getThrownException(0).asString()).isEqualTo("Exception");
        assertThat(constructor.getThrownException(1).asString()).isEqualTo("IOException");
    }

    @Test
    void testGenericMethodDeclaration() {
        String code = "public class Foo { public <T> T getValue(T input) { return input; } }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        MethodDeclaration method = result.getResult().get().getType(0).getMethods().get(0);
        assertThat(method.getNameAsString()).isEqualTo("getValue");
        assertThat(method.getTypeAsString()).isEqualTo("T");
        assertThat(method.getModifiers()).extracting(Modifier::getKeyword)
                .containsExactly(Modifier.Keyword.PUBLIC);
        assertThat(method.getTypeParameters()).hasSize(1);
        assertThat(method.getTypeParameters().get(0).getNameAsString()).isEqualTo("T");
        assertThat(method.getTypeParameters().get(0).getTypeBound()).isEmpty();
    }

    @Test
    void testGenericMethodDeclarationWithBounds() {
        String code = "public class Foo { public <T extends Comparable> int compare(T a, T b) { return 0; } }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        MethodDeclaration method = result.getResult().get().getType(0).getMethods().get(0);
        assertThat(method.getNameAsString()).isEqualTo("compare");
        assertThat(method.getTypeParameters()).hasSize(1);
        assertThat(method.getTypeParameters().get(0).getNameAsString()).isEqualTo("T");
        assertThat(method.getTypeParameters().get(0).getTypeBound()).hasSize(1);
        assertThat(method.getTypeParameters().get(0).getTypeBound().get(0).getNameAsString()).isEqualTo("Comparable");
    }

    @Test
    void testGenericConstructorDeclaration() {
        String code = "public class Foo { public <T> Foo(T value) { } }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        ConstructorDeclaration constructor = result.getResult().get().getType(0).getConstructors().get(0);
        assertThat(constructor.getNameAsString()).isEqualTo("Foo");
        assertThat(constructor.getModifiers()).extracting(Modifier::getKeyword)
                .containsExactly(Modifier.Keyword.PUBLIC);
        assertThat(constructor.getTypeParameters()).hasSize(1);
        assertThat(constructor.getTypeParameters().get(0).getNameAsString()).isEqualTo("T");
        assertThat(constructor.getParameters()).hasSize(1);
        assertThat(constructor.getParameter(0).getTypeAsString()).isEqualTo("T");
    }

    @Test
    void testTypeParameterAnnotation() {
        // Annotation on type parameter: <@NonNull T>
        String code = "class Box<@NonNull T> { T value; }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        ClassOrInterfaceDeclaration classDecl = (ClassOrInterfaceDeclaration) result.getResult().get().getType(0);
        TypeParameter typeParam = classDecl.getTypeParameters().get(0);
        assertThat(typeParam.getNameAsString()).isEqualTo("T");
        assertThat(typeParam.getAnnotations()).hasSize(1);
        assertThat(toString(typeParam.getAnnotations().get(0))).isEqualTo("@NonNull");
    }

    @Test
    void testTypeParameterBoundAnnotation() {
        // Annotation on type parameter bound: <T extends @Nullable Comparable>
        String code = "class Box<T extends @Nullable Comparable> { T value; }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        ClassOrInterfaceDeclaration classDecl = (ClassOrInterfaceDeclaration) result.getResult().get().getType(0);
        TypeParameter typeParam = classDecl.getTypeParameters().get(0);
        assertThat(typeParam.getNameAsString()).isEqualTo("T");
        assertThat(typeParam.getAnnotations()).isEmpty();
        assertThat(typeParam.getTypeBound()).hasSize(1);
        assertThat(typeParam.getTypeBound().get(0).getAnnotations()).hasSize(1);
        assertThat(toString(typeParam.getTypeBound().get(0).getAnnotations().get(0))).isEqualTo("@Nullable");
    }

    @Test
    void testTypeParameterBothAnnotations() {
        // Annotations on both type parameter and bound: <@NonNull T extends @Nullable Comparable>
        String code = "class Box<@NonNull T extends @Nullable Comparable> { T value; }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        ClassOrInterfaceDeclaration classDecl = (ClassOrInterfaceDeclaration) result.getResult().get().getType(0);
        TypeParameter typeParam = classDecl.getTypeParameters().get(0);
        assertThat(typeParam.getNameAsString()).isEqualTo("T");
        assertThat(typeParam.getAnnotations()).hasSize(1);
        assertThat(toString(typeParam.getAnnotations().get(0))).isEqualTo("@NonNull");
        assertThat(typeParam.getTypeBound()).hasSize(1);
        assertThat(typeParam.getTypeBound().get(0).getAnnotations()).hasSize(1);
        assertThat(toString(typeParam.getTypeBound().get(0).getAnnotations().get(0))).isEqualTo("@Nullable");
    }

    @Test
    void testClassDeclarationWithExtends() {
        String code = "public class Foo extends Bar { }";

        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        ClassOrInterfaceDeclaration classDecl = (ClassOrInterfaceDeclaration) result.getResult().get().getType(0);
        assertThat(classDecl.getNameAsString()).isEqualTo("Foo");
        assertThat(classDecl.getExtendedTypes()).hasSize(1);
        assertThat(classDecl.getExtendedTypes().get(0).getNameAsString()).isEqualTo("Bar");
    }

    @Test
    void testClassDeclarationWithImplements() {
        String code = "public class Foo implements Runnable, Comparable { }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        ClassOrInterfaceDeclaration classDecl = (ClassOrInterfaceDeclaration) result.getResult().get().getType(0);
        assertThat(classDecl.getNameAsString()).isEqualTo("Foo");
        assertThat(classDecl.getImplementedTypes()).hasSize(2);
        assertThat(classDecl.getImplementedTypes().get(0).getNameAsString()).isEqualTo("Runnable");
        assertThat(classDecl.getImplementedTypes().get(1).getNameAsString()).isEqualTo("Comparable");
    }

    @Test
    void testClassDeclarationWithTypeParametersAndExtendsImplements() {
        String code = "public class Foo<K, V> extends AbstractMap<K, V> implements Map<K, V> { }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        ClassOrInterfaceDeclaration classDecl = (ClassOrInterfaceDeclaration) result.getResult().get().getType(0);
        assertThat(classDecl.getNameAsString()).isEqualTo("Foo");
        assertThat(classDecl.getTypeParameters()).hasSize(2);
        assertThat(classDecl.getTypeParameters().get(0).getNameAsString()).isEqualTo("K");
        assertThat(classDecl.getTypeParameters().get(1).getNameAsString()).isEqualTo("V");
        assertThat(classDecl.getExtendedTypes()).hasSize(1);
        assertThat(classDecl.getExtendedTypes().get(0).getNameAsString()).isEqualTo("AbstractMap");
        assertThat(classDecl.getImplementedTypes()).hasSize(1);
        assertThat(classDecl.getImplementedTypes().get(0).getNameAsString()).isEqualTo("Map");
    }

    @Test
    void testEnumDeclaration() {
        String code = "public enum Color { RED, GREEN, BLUE }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        EnumDeclaration enumDecl = (EnumDeclaration) result.getResult().get().getType(0);
        assertThat(enumDecl.getNameAsString()).isEqualTo("Color");
        assertThat(enumDecl.getModifiers()).extracting(Modifier::getKeyword)
                .containsExactly(Modifier.Keyword.PUBLIC);
        assertThat(enumDecl.getEntries()).hasSize(3);
        assertThat(enumDecl.getEntries().get(0).getNameAsString()).isEqualTo("RED");
        assertThat(enumDecl.getEntries().get(1).getNameAsString()).isEqualTo("GREEN");
        assertThat(enumDecl.getEntries().get(2).getNameAsString()).isEqualTo("BLUE");
    }

    @Test
    void testEnumDeclarationWithArgumentsAndBody() {
        String code = "public enum Planet { EARTH(1), MARS(2); private int order; Planet(int order) { this.order = order; } public int getOrder() { return order; } }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        EnumDeclaration enumDecl = (EnumDeclaration) result.getResult().get().getType(0);
        assertThat(enumDecl.getNameAsString()).isEqualTo("Planet");
        assertThat(enumDecl.getEntries()).hasSize(2);
        assertThat(enumDecl.getEntries().get(0).getNameAsString()).isEqualTo("EARTH");
        assertThat(enumDecl.getEntries().get(0).getArguments()).hasSize(1);
        assertThat(enumDecl.getEntries().get(1).getNameAsString()).isEqualTo("MARS");
        assertThat(enumDecl.getEntries().get(1).getArguments()).hasSize(1);
        // Check body members: field, constructor, method
        assertThat(enumDecl.getFields()).hasSize(1);
        assertThat(enumDecl.getFields().get(0).getVariable(0).getNameAsString()).isEqualTo("order");
        assertThat(enumDecl.getConstructors()).hasSize(1);
        assertThat(enumDecl.getConstructors().get(0).getNameAsString()).isEqualTo("Planet");
        assertThat(enumDecl.getMethods()).hasSize(1);
        assertThat(enumDecl.getMethods().get(0).getNameAsString()).isEqualTo("getOrder");
    }

    @Test
    void testEnumDeclarationWithImplements() {
        String code = "public enum Direction implements Comparable<Direction> { NORTH, SOUTH }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        EnumDeclaration enumDecl = (EnumDeclaration) result.getResult().get().getType(0);
        assertThat(enumDecl.getNameAsString()).isEqualTo("Direction");
        assertThat(enumDecl.getImplementedTypes()).hasSize(1);
        assertThat(enumDecl.getImplementedTypes().get(0).getNameAsString()).isEqualTo("Comparable");
        assertThat(enumDecl.getEntries()).hasSize(2);
    }

    @Test
    void testEnumConstantAnnotations() {
        String code = "enum Status { @Deprecated ACTIVE, @SuppressWarnings(\"unused\") INACTIVE }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        EnumDeclaration enumDecl = (EnumDeclaration) result.getResult().get().getType(0);
        assertThat(enumDecl.getEntries()).hasSize(2);

        EnumConstantDeclaration active = enumDecl.getEntries().get(0);
        assertThat(active.getNameAsString()).isEqualTo("ACTIVE");
        assertThat(active.getAnnotations()).hasSize(1);
        assertThat(toString(active.getAnnotations().get(0))).isEqualTo("@Deprecated");

        EnumConstantDeclaration inactive = enumDecl.getEntries().get(1);
        assertThat(inactive.getNameAsString()).isEqualTo("INACTIVE");
        assertThat(inactive.getAnnotations()).hasSize(1);
        assertThat(toString(inactive.getAnnotations().get(0))).contains("@SuppressWarnings");
    }

    @Test
    void testInterfaceDeclaration() {
        String code = "public interface Greeter { void greet(String name); }";

        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        ClassOrInterfaceDeclaration interfaceDecl = (ClassOrInterfaceDeclaration) result.getResult().get().getType(0);
        assertThat(interfaceDecl.getNameAsString()).isEqualTo("Greeter");
        assertThat(interfaceDecl.isInterface()).isTrue();
        assertThat(interfaceDecl.getModifiers()).extracting(Modifier::getKeyword)
                .containsExactly(Modifier.Keyword.PUBLIC);
        assertThat(interfaceDecl.getMethods()).hasSize(1);
        assertThat(interfaceDecl.getMethods().get(0).getNameAsString()).isEqualTo("greet");
        assertThat(interfaceDecl.getMethods().get(0).getParameters()).hasSize(1);
    }

    @Test
    void testInterfaceDeclarationWithExtendsAndDefaultMethod() {
        String code = "public interface Foo<T> extends Bar, Baz { default T getValue() { return null; } }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        ClassOrInterfaceDeclaration interfaceDecl = (ClassOrInterfaceDeclaration) result.getResult().get().getType(0);
        assertThat(interfaceDecl.getNameAsString()).isEqualTo("Foo");
        assertThat(interfaceDecl.isInterface()).isTrue();
        assertThat(interfaceDecl.getTypeParameters()).hasSize(1);
        assertThat(interfaceDecl.getTypeParameters().get(0).getNameAsString()).isEqualTo("T");
        assertThat(interfaceDecl.getExtendedTypes()).hasSize(2);
        assertThat(interfaceDecl.getExtendedTypes().get(0).getNameAsString()).isEqualTo("Bar");
        assertThat(interfaceDecl.getExtendedTypes().get(1).getNameAsString()).isEqualTo("Baz");
        assertThat(interfaceDecl.getMethods()).hasSize(1);
        MethodDeclaration method = interfaceDecl.getMethods().get(0);
        assertThat(method.getNameAsString()).isEqualTo("getValue");
        assertThat(method.getModifiers()).extracting(Modifier::getKeyword)
                .containsExactly(Modifier.Keyword.DEFAULT);
        assertThat(method.getBody()).isPresent();
    }

    @Test
    void testInterfaceDeclarationWithConstant() {
        String code = "public interface Constants { int MAX = 100; String NAME = \"test\"; }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        ClassOrInterfaceDeclaration interfaceDecl = (ClassOrInterfaceDeclaration) result.getResult().get().getType(0);
        assertThat(interfaceDecl.getNameAsString()).isEqualTo("Constants");
        assertThat(interfaceDecl.isInterface()).isTrue();
        assertThat(interfaceDecl.getFields()).hasSize(2);
        assertThat(interfaceDecl.getFields().get(0).getVariable(0).getNameAsString()).isEqualTo("MAX");
        assertThat(interfaceDecl.getFields().get(0).getVariable(0).getTypeAsString()).isEqualTo("int");
        assertThat(interfaceDecl.getFields().get(1).getVariable(0).getNameAsString()).isEqualTo("NAME");
    }

    @Test
    void testAnnotationTypeDeclaration() {
        String code = "public @interface MyAnnotation { String value(); int count() default 0; }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        AnnotationDeclaration annotationDecl = (AnnotationDeclaration) result.getResult().get().getType(0);
        assertThat(annotationDecl.getNameAsString()).isEqualTo("MyAnnotation");
        assertThat(annotationDecl.getModifiers()).extracting(Modifier::getKeyword)
                .containsExactly(Modifier.Keyword.PUBLIC);
        assertThat(annotationDecl.getMembers()).hasSize(2);

        AnnotationMemberDeclaration valueMember = (AnnotationMemberDeclaration) annotationDecl.getMembers().get(0);
        assertThat(valueMember.getNameAsString()).isEqualTo("value");
        assertThat(valueMember.getTypeAsString()).isEqualTo("String");
        assertThat(valueMember.getDefaultValue()).isEmpty();

        AnnotationMemberDeclaration countMember = (AnnotationMemberDeclaration) annotationDecl.getMembers().get(1);
        assertThat(countMember.getNameAsString()).isEqualTo("count");
        assertThat(countMember.getTypeAsString()).isEqualTo("int");
        assertThat(countMember.getDefaultValue()).isPresent();
        assertThat(countMember.getDefaultValue().get().toString()).isEqualTo("0");
    }

    @Test
    void testAnnotationTypeDeclarationWithConstantsAndNestedTypes() {
        String code = "public @interface Config { String DEFAULT_NAME = \"test\"; int value(); enum Level { LOW, HIGH } }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        AnnotationDeclaration annotationDecl = (AnnotationDeclaration) result.getResult().get().getType(0);
        assertThat(annotationDecl.getNameAsString()).isEqualTo("Config");
        assertThat(annotationDecl.getMembers()).hasSize(3);

        // Constant field
        FieldDeclaration fieldDecl = (FieldDeclaration) annotationDecl.getMembers().get(0);
        assertThat(fieldDecl.getVariable(0).getNameAsString()).isEqualTo("DEFAULT_NAME");
        assertThat(fieldDecl.getVariable(0).getTypeAsString()).isEqualTo("String");

        // Annotation method
        AnnotationMemberDeclaration methodMember = (AnnotationMemberDeclaration) annotationDecl.getMembers().get(1);
        assertThat(methodMember.getNameAsString()).isEqualTo("value");
        assertThat(methodMember.getTypeAsString()).isEqualTo("int");

        // Nested enum
        EnumDeclaration nestedEnum = (EnumDeclaration) annotationDecl.getMembers().get(2);
        assertThat(nestedEnum.getNameAsString()).isEqualTo("Level");
        assertThat(nestedEnum.getEntries()).hasSize(2);
    }

    @Test
    void testAnnotationTypeDeclarationWithArrayDefault() {
        String code = "public @interface Tags { String[] value() default {\"a\", \"b\"}; }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        AnnotationDeclaration annotationDecl = (AnnotationDeclaration) result.getResult().get().getType(0);
        assertThat(annotationDecl.getNameAsString()).isEqualTo("Tags");
        assertThat(annotationDecl.getMembers()).hasSize(1);

        AnnotationMemberDeclaration member = (AnnotationMemberDeclaration) annotationDecl.getMembers().get(0);
        assertThat(member.getNameAsString()).isEqualTo("value");
        assertThat(member.getTypeAsString()).isEqualTo("String[]");
        assertThat(member.getDefaultValue()).isPresent();
    }

    @Test
    void testRecordDeclaration() {
        String code = "public record Point(int x, int y) {}";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        RecordDeclaration recordDecl = (RecordDeclaration) result.getResult().get().getType(0);
        assertThat(recordDecl.getNameAsString()).isEqualTo("Point");
        assertThat(recordDecl.getModifiers()).extracting(Modifier::getKeyword)
                .containsExactly(Modifier.Keyword.PUBLIC);
        assertThat(recordDecl.getParameters()).hasSize(2);
        assertThat(recordDecl.getParameters().get(0).getNameAsString()).isEqualTo("x");
        assertThat(recordDecl.getParameters().get(0).getTypeAsString()).isEqualTo("int");
        assertThat(recordDecl.getParameters().get(1).getNameAsString()).isEqualTo("y");
        assertThat(recordDecl.getParameters().get(1).getTypeAsString()).isEqualTo("int");
    }

    @Test
    void testRecordDeclarationWithTypeParametersAndImplements() {
        String code = "public record Pair<A, B>(A first, B second) implements Comparable<Pair<A, B>> {}";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        RecordDeclaration recordDecl = (RecordDeclaration) result.getResult().get().getType(0);
        assertThat(recordDecl.getNameAsString()).isEqualTo("Pair");
        assertThat(recordDecl.getTypeParameters()).hasSize(2);
        assertThat(recordDecl.getTypeParameters().get(0).getNameAsString()).isEqualTo("A");
        assertThat(recordDecl.getTypeParameters().get(1).getNameAsString()).isEqualTo("B");
        assertThat(recordDecl.getParameters()).hasSize(2);
        assertThat(recordDecl.getParameters().get(0).getNameAsString()).isEqualTo("first");
        assertThat(recordDecl.getParameters().get(1).getNameAsString()).isEqualTo("second");
        assertThat(recordDecl.getImplementedTypes()).hasSize(1);
        assertThat(recordDecl.getImplementedTypes().get(0).getNameAsString()).isEqualTo("Comparable");
    }

    @Test
    void testRecordDeclarationWithCompactConstructorAndMethods() {
        String code = "public record Name(String value) { public Name { if (value == null) throw new NullPointerException(); } public String upper() { return value.toUpperCase(); } }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        RecordDeclaration recordDecl = (RecordDeclaration) result.getResult().get().getType(0);
        assertThat(recordDecl.getNameAsString()).isEqualTo("Name");
        assertThat(recordDecl.getParameters()).hasSize(1);
        assertThat(recordDecl.getParameters().get(0).getNameAsString()).isEqualTo("value");

        // Compact constructor
        assertThat(recordDecl.getCompactConstructors()).hasSize(1);
        CompactConstructorDeclaration compactCtor = recordDecl.getCompactConstructors().get(0);
        assertThat(compactCtor.getNameAsString()).isEqualTo("Name");
        assertThat(compactCtor.getModifiers()).extracting(Modifier::getKeyword)
                .containsExactly(Modifier.Keyword.PUBLIC);

        // Regular method
        assertThat(recordDecl.getMethods()).hasSize(1);
        assertThat(recordDecl.getMethods().get(0).getNameAsString()).isEqualTo("upper");
    }

    @Test
    void testLocalClassDeclaration() {
        String block = "{ class Helper { int getValue() { return 42; } } Helper h = new Helper(); }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<BlockStmt> result = parser.parseBlock(block);
        assertThat(result.getResult()).isPresent();

        LocalClassDeclarationStmt localStmt = (LocalClassDeclarationStmt) result.getResult().get().getStatement(0);
        ClassOrInterfaceDeclaration classDecl = localStmt.getClassDeclaration();
        assertThat(classDecl.getNameAsString()).isEqualTo("Helper");
        assertThat(classDecl.isInterface()).isFalse();
        assertThat(classDecl.getMethods()).hasSize(1);
        assertThat(classDecl.getMethods().get(0).getNameAsString()).isEqualTo("getValue");
    }

    @Test
    void testLocalRecordDeclaration() {
        String block = "{ record Pair(int x, int y) {} Pair p = new Pair(1, 2); }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<BlockStmt> result = parser.parseBlock(block);
        assertThat(result.getResult()).isPresent();

        LocalRecordDeclarationStmt localStmt = (LocalRecordDeclarationStmt) result.getResult().get().getStatement(0);
        RecordDeclaration recordDecl = localStmt.getRecordDeclaration();
        assertThat(recordDecl.getNameAsString()).isEqualTo("Pair");
        assertThat(recordDecl.getParameters()).hasSize(2);
        assertThat(recordDecl.getParameters().get(0).getNameAsString()).isEqualTo("x");
        assertThat(recordDecl.getParameters().get(1).getNameAsString()).isEqualTo("y");
    }

    @Test
    void testLocalInterfaceDeclaration() {
        String block = "{ interface Greeter { void greet(); } }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<BlockStmt> result = parser.parseBlock(block);
        assertThat(result.getResult()).isPresent();

        LocalClassDeclarationStmt localStmt = (LocalClassDeclarationStmt) result.getResult().get().getStatement(0);
        ClassOrInterfaceDeclaration interfaceDecl = localStmt.getClassDeclaration();
        assertThat(interfaceDecl.getNameAsString()).isEqualTo("Greeter");
        assertThat(interfaceDecl.isInterface()).isTrue();
        assertThat(interfaceDecl.getMethods()).hasSize(1);
    }

    @Test
    void testNestedClassInClass() {
        String code = "public class Outer { private static class Inner { int value; } }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        ClassOrInterfaceDeclaration outer = (ClassOrInterfaceDeclaration) result.getResult().get().getType(0);
        assertThat(outer.getNameAsString()).isEqualTo("Outer");
        assertThat(outer.getMembers()).hasSize(1);

        ClassOrInterfaceDeclaration inner = (ClassOrInterfaceDeclaration) outer.getMembers().get(0);
        assertThat(inner.getNameAsString()).isEqualTo("Inner");
        assertThat(inner.getModifiers()).extracting(Modifier::getKeyword)
                .containsExactlyInAnyOrder(Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC);
        assertThat(inner.getFields()).hasSize(1);
    }

    @Test
    void testNestedEnumInClass() {
        String code = "public class Outer { public enum Status { ACTIVE, INACTIVE } }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        ClassOrInterfaceDeclaration outer = (ClassOrInterfaceDeclaration) result.getResult().get().getType(0);
        assertThat(outer.getMembers()).hasSize(1);

        EnumDeclaration nestedEnum = (EnumDeclaration) outer.getMembers().get(0);
        assertThat(nestedEnum.getNameAsString()).isEqualTo("Status");
        assertThat(nestedEnum.getEntries()).hasSize(2);
    }

    @Test
    void testNestedInterfaceInClass() {
        String code = "public class Outer { public interface Callback { void onComplete(); } }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        ClassOrInterfaceDeclaration outer = (ClassOrInterfaceDeclaration) result.getResult().get().getType(0);
        assertThat(outer.getMembers()).hasSize(1);

        ClassOrInterfaceDeclaration nestedInterface = (ClassOrInterfaceDeclaration) outer.getMembers().get(0);
        assertThat(nestedInterface.getNameAsString()).isEqualTo("Callback");
        assertThat(nestedInterface.isInterface()).isTrue();
        assertThat(nestedInterface.getMethods()).hasSize(1);
    }

    @Test
    void testNestedTypesInInterface() {
        String code = "public interface Container { class DefaultImpl {} enum Type { A, B } }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        ClassOrInterfaceDeclaration container = (ClassOrInterfaceDeclaration) result.getResult().get().getType(0);
        assertThat(container.isInterface()).isTrue();
        assertThat(container.getMembers()).hasSize(2);

        ClassOrInterfaceDeclaration defaultImpl = (ClassOrInterfaceDeclaration) container.getMembers().get(0);
        assertThat(defaultImpl.getNameAsString()).isEqualTo("DefaultImpl");

        EnumDeclaration typeEnum = (EnumDeclaration) container.getMembers().get(1);
        assertThat(typeEnum.getNameAsString()).isEqualTo("Type");
        assertThat(typeEnum.getEntries()).hasSize(2);
    }

    @Test
    void testGenericInterfaceMethodDeclaration() {
        String code = "public interface Converter { <T> T convert(Object input); }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        ClassOrInterfaceDeclaration converter = (ClassOrInterfaceDeclaration) result.getResult().get().getType(0);
        assertThat(converter.getMethods()).hasSize(1);
        MethodDeclaration method = converter.getMethods().get(0);
        assertThat(method.getNameAsString()).isEqualTo("convert");
        assertThat(method.getTypeParameters()).hasSize(1);
        assertThat(method.getTypeParameters().get(0).getNameAsString()).isEqualTo("T");
        assertThat(method.getTypeAsString()).isEqualTo("T");
        assertThat(method.getParameters()).hasSize(1);
    }

    // --- Wildcard type arguments ---

    @Test
    void testWildcardTypeArgument_unbounded() {
        String code = "public class Foo { java.util.List<?> list; }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        FieldDeclaration field = result.getResult().get().getType(0).getFields().get(0);
        ClassOrInterfaceType listType = (ClassOrInterfaceType) field.getVariable(0).getType();
        assertThat(listType.getTypeArguments()).isPresent();
        assertThat(listType.getTypeArguments().get()).hasSize(1);
        Type arg = listType.getTypeArguments().get().get(0);
        assertThat(arg).isInstanceOf(WildcardType.class);
        WildcardType wildcard = (WildcardType) arg;
        assertThat(wildcard.getExtendedType()).isEmpty();
        assertThat(wildcard.getSuperType()).isEmpty();
    }

    @Test
    void testWildcardTypeArgument_extends() {
        String code = "public class Foo { java.util.List<? extends Number> list; }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        FieldDeclaration field = result.getResult().get().getType(0).getFields().get(0);
        ClassOrInterfaceType listType = (ClassOrInterfaceType) field.getVariable(0).getType();
        assertThat(listType.getTypeArguments()).isPresent();
        Type arg = listType.getTypeArguments().get().get(0);
        assertThat(arg).isInstanceOf(WildcardType.class);
        WildcardType wildcard = (WildcardType) arg;
        assertThat(wildcard.getExtendedType()).isPresent();
        assertThat(wildcard.getExtendedType().get().asString()).isEqualTo("Number");
        assertThat(wildcard.getSuperType()).isEmpty();
    }

    @Test
    void testWildcardTypeArgument_super() {
        String code = "public class Foo { java.util.List<? super Exception> list; }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        FieldDeclaration field = result.getResult().get().getType(0).getFields().get(0);
        ClassOrInterfaceType listType = (ClassOrInterfaceType) field.getVariable(0).getType();
        assertThat(listType.getTypeArguments()).isPresent();
        Type arg = listType.getTypeArguments().get().get(0);
        assertThat(arg).isInstanceOf(WildcardType.class);
        WildcardType wildcard = (WildcardType) arg;
        assertThat(wildcard.getSuperType()).isPresent();
        assertThat(wildcard.getSuperType().get().asString()).isEqualTo("Exception");
        assertThat(wildcard.getExtendedType()).isEmpty();
    }

    @Test
    void testWildcardTypeArgument_extendsWithTypeArgs() {
        String code = "public class Foo { java.util.List<? extends Comparable<String>> list; }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        FieldDeclaration field = result.getResult().get().getType(0).getFields().get(0);
        ClassOrInterfaceType listType = (ClassOrInterfaceType) field.getVariable(0).getType();
        assertThat(listType.getTypeArguments()).isPresent();
        Type arg = listType.getTypeArguments().get().get(0);
        assertThat(arg).isInstanceOf(WildcardType.class);
        WildcardType wildcard = (WildcardType) arg;
        assertThat(wildcard.getExtendedType()).isPresent();
        ClassOrInterfaceType extType = (ClassOrInterfaceType) wildcard.getExtendedType().get();
        assertThat(extType.getNameAsString()).isEqualTo("Comparable");
        assertThat(extType.getTypeArguments()).isPresent();
        assertThat(extType.getTypeArguments().get().get(0).asString()).isEqualTo("String");
    }

    // --- Variable modifiers ---

    @Test
    void testFinalLocalVariable() {
        String block = "{ final int x = 1; }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<BlockStmt> result = parser.parseBlock(block);
        assertThat(result.getResult()).isPresent();

        VariableDeclarationExpr varDecl = result.getResult().get().getStatement(0)
                .asExpressionStmt().getExpression().asVariableDeclarationExpr();
        assertThat(varDecl.getModifiers()).extracting(Modifier::getKeyword)
                .containsExactly(Modifier.Keyword.FINAL);
        assertThat(varDecl.getVariable(0).getNameAsString()).isEqualTo("x");
    }

    @Test
    void testFinalEnhancedFor() {
        String block = "{ for (final String s : list) { } }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<BlockStmt> result = parser.parseBlock(block);
        assertThat(result.getResult()).isPresent();

        ForEachStmt forEach = result.getResult().get().getStatement(0).asForEachStmt();
        VariableDeclarationExpr varDecl = forEach.getVariable();
        assertThat(varDecl.getModifiers()).extracting(Modifier::getKeyword)
                .containsExactly(Modifier.Keyword.FINAL);
        assertThat(varDecl.getVariable(0).getNameAsString()).isEqualTo("s");
    }

    @Test
    void testFinalTryWithResources() {
        String block = "{ try (final java.io.InputStream is = create()) { } }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<BlockStmt> result = parser.parseBlock(block);
        assertThat(result.getResult()).isPresent();

        TryStmt tryStmt = (TryStmt) result.getResult().get().getStatement(0);
        assertThat(tryStmt.getResources()).hasSize(1);
        VariableDeclarationExpr varDecl = tryStmt.getResources().get(0).asVariableDeclarationExpr();
        assertThat(varDecl.getModifiers()).extracting(Modifier::getKeyword)
                .containsExactly(Modifier.Keyword.FINAL);
        assertThat(varDecl.getVariable(0).getNameAsString()).isEqualTo("is");
    }

    // --- C-style array dimensions ---

    @Test
    void testCStyleArrayField() {
        String code = "public class Foo { int arr[]; }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        FieldDeclaration field = result.getResult().get().getType(0).getFields().get(0);
        assertThat(field.getVariable(0).getType()).isInstanceOf(ArrayType.class);
        assertThat(field.getVariable(0).getNameAsString()).isEqualTo("arr");
    }

    @Test
    void testCStyleArrayMixedDeclarators() {
        String code = "public class Foo { int x[], y; }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        FieldDeclaration field = result.getResult().get().getType(0).getFields().get(0);
        assertThat(field.getVariables()).hasSize(2);
        // x[] should be int[]
        assertThat(field.getVariable(0).getType()).isInstanceOf(ArrayType.class);
        assertThat(field.getVariable(0).getNameAsString()).isEqualTo("x");
        // y should be plain int
        assertThat(field.getVariable(1).getType().asString()).isEqualTo("int");
        assertThat(field.getVariable(1).getNameAsString()).isEqualTo("y");
    }

    @Test
    void testCStyleArrayLocalVariable() {
        String block = "{ int arr[] = {1, 2}; }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<BlockStmt> result = parser.parseBlock(block);
        assertThat(result.getResult()).isPresent();

        VariableDeclarationExpr varDecl = result.getResult().get().getStatement(0)
                .asExpressionStmt().getExpression().asVariableDeclarationExpr();
        assertThat(varDecl.getVariable(0).getType()).isInstanceOf(ArrayType.class);
        assertThat(varDecl.getVariable(0).getNameAsString()).isEqualTo("arr");
    }

    @Test
    void testCStyleArrayConstDeclaration() {
        String code = "public interface Foo { int SIZES[] = {1, 2}; }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        FieldDeclaration field = result.getResult().get().getType(0).getFields().get(0);
        assertThat(field.getVariable(0).getType()).isInstanceOf(ArrayType.class);
        assertThat(field.getVariable(0).getNameAsString()).isEqualTo("SIZES");
    }

    // --- Initializer blocks ---

    @Test
    void testStaticInitializerBlock() {
        String code = "public class Foo { static int x; static { x = 1; } }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        ClassOrInterfaceDeclaration classDecl = (ClassOrInterfaceDeclaration) result.getResult().get().getType(0);
        // Should have field + initializer
        assertThat(classDecl.getMembers()).hasSizeGreaterThanOrEqualTo(2);
        InitializerDeclaration initDecl = classDecl.getMembers().stream()
                .filter(m -> m instanceof InitializerDeclaration)
                .map(m -> (InitializerDeclaration) m)
                .findFirst().orElseThrow();
        assertThat(initDecl.isStatic()).isTrue();
        assertThat(initDecl.getBody().getStatements()).hasSize(1);
    }

    @Test
    void testInstanceInitializerBlock() {
        String code = "public class Foo { int y; { y = 2; } }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        ClassOrInterfaceDeclaration classDecl = (ClassOrInterfaceDeclaration) result.getResult().get().getType(0);
        InitializerDeclaration initDecl = classDecl.getMembers().stream()
                .filter(m -> m instanceof InitializerDeclaration)
                .map(m -> (InitializerDeclaration) m)
                .findFirst().orElseThrow();
        assertThat(initDecl.isStatic()).isFalse();
        assertThat(initDecl.getBody().getStatements()).hasSize(1);
    }

    // ── Module declarations ──

    @Test
    void testSimpleModuleDeclaration() {
        String code = "module com.example { requires java.base; exports com.example.api; }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        CompilationUnit cu = result.getResult().get();
        assertThat(cu.getModule()).isPresent();
        ModuleDeclaration module = cu.getModule().get();
        assertThat(module.getNameAsString()).isEqualTo("com.example");
        assertThat(module.isOpen()).isFalse();
        assertThat(module.getDirectives()).hasSize(2);
        assertThat(module.getDirectives().get(0)).isInstanceOf(ModuleRequiresDirective.class);
        assertThat(((ModuleRequiresDirective) module.getDirectives().get(0)).getNameAsString()).isEqualTo("java.base");
        assertThat(module.getDirectives().get(1)).isInstanceOf(ModuleExportsDirective.class);
        assertThat(((ModuleExportsDirective) module.getDirectives().get(1)).getNameAsString()).isEqualTo("com.example.api");
    }

    @Test
    void testOpenModuleWithAllDirectiveTypes() {
        String code = "open module com.example {\n"
                + "  requires transitive java.logging;\n"
                + "  exports com.example.api to com.example.client;\n"
                + "  uses com.example.spi.Service;\n"
                + "  provides com.example.spi.Service with com.example.impl.ServiceImpl;\n"
                + "}";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        CompilationUnit cu = result.getResult().get();
        assertThat(cu.getModule()).isPresent();
        ModuleDeclaration module = cu.getModule().get();
        assertThat(module.isOpen()).isTrue();
        assertThat(module.getDirectives()).hasSize(4);

        // requires transitive
        ModuleRequiresDirective requiresDir = (ModuleRequiresDirective) module.getDirectives().get(0);
        assertThat(requiresDir.getNameAsString()).isEqualTo("java.logging");
        assertThat(requiresDir.isTransitive()).isTrue();

        // exports ... to ...
        ModuleExportsDirective exportsDir = (ModuleExportsDirective) module.getDirectives().get(1);
        assertThat(exportsDir.getNameAsString()).isEqualTo("com.example.api");
        assertThat(exportsDir.getModuleNames()).hasSize(1);
        assertThat(exportsDir.getModuleNames().get(0).asString()).isEqualTo("com.example.client");

        // uses
        ModuleUsesDirective usesDir = (ModuleUsesDirective) module.getDirectives().get(2);
        assertThat(usesDir.getNameAsString()).isEqualTo("com.example.spi.Service");

        // provides ... with ...
        ModuleProvidesDirective providesDir = (ModuleProvidesDirective) module.getDirectives().get(3);
        assertThat(providesDir.getNameAsString()).isEqualTo("com.example.spi.Service");
        assertThat(providesDir.getWith()).hasSize(1);
        assertThat(providesDir.getWith().get(0).asString()).isEqualTo("com.example.impl.ServiceImpl");
    }

    // ── Receiver parameters ──

    @Test
    void testSimpleReceiverParameter() {
        String code = "public class Foo { void method(Foo this) {} }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        ClassOrInterfaceDeclaration classDecl = (ClassOrInterfaceDeclaration) result.getResult().get().getType(0);
        MethodDeclaration method = classDecl.getMethods().get(0);
        assertThat(method.getReceiverParameter()).isPresent();
        ReceiverParameter rp = method.getReceiverParameter().get();
        assertThat(rp.getType().asString()).isEqualTo("Foo");
        assertThat(rp.getName().asString()).isEqualTo("this");
    }

    @Test
    void testQualifiedReceiverParameter() {
        // In inner class constructors, the receiver parameter uses: typeType identifier '.' THIS
        // e.g., Outer Outer.this — the qualifier goes before "this"
        String code = "public class Outer { class Inner { Inner(Outer Outer.this) {} } }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        ClassOrInterfaceDeclaration outerDecl = (ClassOrInterfaceDeclaration) result.getResult().get().getType(0);
        ClassOrInterfaceDeclaration innerDecl = (ClassOrInterfaceDeclaration) outerDecl.getMembers().stream()
                .filter(m -> m instanceof ClassOrInterfaceDeclaration)
                .findFirst().orElseThrow();
        ConstructorDeclaration ctor = innerDecl.getConstructors().get(0);
        assertThat(ctor.getReceiverParameter()).isPresent();
        ReceiverParameter rp = ctor.getReceiverParameter().get();
        assertThat(rp.getType().asString()).isEqualTo("Outer");
        assertThat(rp.getName().asString()).isEqualTo("Outer.this");
    }

    // ── Generic super invocation ──

    @Test
    void testGenericSuperConstructorCall() {
        // expression.<Type>super(args) inside a class
        String code = "public class Child extends Parent {"
                + "  class Inner {"
                + "    void test() { Child.<String>super.method(); }"
                + "  }"
                + "}";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        ClassOrInterfaceDeclaration classDecl = (ClassOrInterfaceDeclaration) result.getResult().get().getType(0);
        ClassOrInterfaceDeclaration innerDecl = (ClassOrInterfaceDeclaration) classDecl.getMembers().stream()
                .filter(m -> m instanceof ClassOrInterfaceDeclaration)
                .findFirst().orElseThrow();
        MethodDeclaration method = innerDecl.getMethods().get(0);
        BlockStmt body = method.getBody().orElseThrow();
        assertThat(body.getStatements()).hasSize(1);
        MethodCallExpr methodCall = (MethodCallExpr) body.getStatement(0).asExpressionStmt().getExpression();
        assertThat(methodCall.getNameAsString()).isEqualTo("method");
        assertThat(methodCall.getTypeArguments()).isPresent();
        assertThat(methodCall.getTypeArguments().get()).hasSize(1);
    }

    // ── Type arguments on intermediate qualifiers ──

    @Test
    void testTypeArgsOnIntermediateQualifier() {
        // Outer<String>.Inner — type arg on intermediate, none on final
        String code = "public class Foo { Outer<String>.Inner field; }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        ClassOrInterfaceDeclaration classDecl = (ClassOrInterfaceDeclaration) result.getResult().get().getType(0);
        FieldDeclaration field = classDecl.getFields().get(0);
        Type fieldType = field.getVariable(0).getType();
        assertThat(fieldType).isInstanceOf(ClassOrInterfaceType.class);
        ClassOrInterfaceType innerType = (ClassOrInterfaceType) fieldType;
        assertThat(innerType.getNameAsString()).isEqualTo("Inner");
        assertThat(innerType.getTypeArguments()).isNotPresent();
        // Check the scope has type arguments
        assertThat(innerType.getScope()).isPresent();
        ClassOrInterfaceType outerType = innerType.getScope().get();
        assertThat(outerType.getNameAsString()).isEqualTo("Outer");
        assertThat(outerType.getTypeArguments()).isPresent();
        assertThat(outerType.getTypeArguments().get()).hasSize(1);
        assertThat(outerType.getTypeArguments().get().get(0).asString()).isEqualTo("String");
    }

    @Test
    void testTypeArgsOnBothIntermediateAndFinal() {
        // Outer<String>.Inner<Integer>
        String code = "public class Foo { Outer<String>.Inner<Integer> field; }";
        Antlr4MvelParser parser = new Antlr4MvelParser();
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertThat(result.getResult()).isPresent();

        ClassOrInterfaceDeclaration classDecl = (ClassOrInterfaceDeclaration) result.getResult().get().getType(0);
        FieldDeclaration field = classDecl.getFields().get(0);
        ClassOrInterfaceType innerType = (ClassOrInterfaceType) field.getVariable(0).getType();
        assertThat(innerType.getNameAsString()).isEqualTo("Inner");
        assertThat(innerType.getTypeArguments()).isPresent();
        assertThat(innerType.getTypeArguments().get().get(0).asString()).isEqualTo("Integer");
        // Check scope
        ClassOrInterfaceType outerType = innerType.getScope().get();
        assertThat(outerType.getNameAsString()).isEqualTo("Outer");
        assertThat(outerType.getTypeArguments()).isPresent();
        assertThat(outerType.getTypeArguments().get().get(0).asString()).isEqualTo("String");
    }

    private String toString(Node n) {
        return PrintUtil.printNode(n);
    }
}
