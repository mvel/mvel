/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mvel3;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.resolution.types.ResolvedType;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mvel3.parser.MvelParser;
import org.mvel3.parser.ast.expr.BigDecimalLiteralExpr;
import org.mvel3.parser.ast.expr.BigIntegerLiteralExpr;
import org.mvel3.parser.ast.expr.DrlNameExpr;
import org.mvel3.parser.ast.expr.HalfBinaryExpr;
import org.mvel3.parser.ast.expr.HalfPointFreeExpr;
import org.mvel3.parser.ast.expr.InlineCastExpr;
import org.mvel3.parser.ast.expr.ListCreationLiteralExpression;
import org.mvel3.parser.ast.expr.ListCreationLiteralExpressionElement;
import org.mvel3.parser.ast.expr.MapCreationLiteralExpression;
import org.mvel3.parser.ast.expr.MapCreationLiteralExpressionKeyValuePair;
import org.mvel3.parser.ast.expr.NullSafeFieldAccessExpr;
import org.mvel3.parser.ast.expr.NullSafeMethodCallExpr;
import org.mvel3.parser.DrlxParser;
import org.mvel3.parser.ast.expr.PointFreeExpr;
import org.mvel3.parser.ast.expr.TemporalLiteralExpr;
import org.mvel3.transpiler.MVELTranspiler;
import org.mvel3.transpiler.TranspiledResult;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for type resolution with mvel3 custom AST nodes, without rewriting.
 */
public class TypeResolveTest {

    // At the moment, I want to test legacy JavaParser first to check how type resolution works with custom AST nodes.
    @BeforeClass
    public static void chooseParser() {
        MvelParser.Factory.USE_ANTLR = false;
//        MvelParser.Factory.USE_ANTLR = true;
    }

    @BeforeClass
    public static void disableRewrite() {
        MVELTranspiler.ENABLE_REWRITE = false;
    }

    @AfterClass
    public static void restoreRewrite() {
        MVELTranspiler.ENABLE_REWRITE = true;
    }

    private CompilationUnit  transpileWithoutRewrite(Consumer<MVELBuilder<Map<String, Object>, Void,Object>> contextUpdater,
                       String inputExpression) {
        MVELBuilder.ContentBuilder<Map<String, Object>, Void, Object> contentBuilder = MVEL.map().<Object>out(Type.OBJECT);

        MVELBuilder<Map<String, Object>, Void, Object> mvelBuilder;
        if (inputExpression.indexOf(';') > 0 || inputExpression.contains("{")) {
            mvelBuilder = contentBuilder.block(inputExpression);
        } else {
            mvelBuilder = contentBuilder.expression(inputExpression);
        }

        mvelBuilder.classManager(new ClassManager())
                .classLoader(ClassLoader.getSystemClassLoader())
                .generatedSuperName(TranspilerTest.BaseExecutorClass.class.getName());

        Set<String> imports = new HashSet<>();
        imports.add(List.class.getCanonicalName());
        imports.add(java.util.ArrayList.class.getCanonicalName());
        imports.add(java.util.HashMap.class.getCanonicalName());
        imports.add(Map.class.getCanonicalName());
        imports.add(BigDecimal.class.getCanonicalName());
        imports.add(BigInteger.class.getCanonicalName());
        imports.add(Address.class.getCanonicalName());
        imports.add(Person.class.getCanonicalName());
        imports.add(Gender.class.getCanonicalName());

        mvelBuilder.imports(imports) ;

        contextUpdater.accept(mvelBuilder);

        TranspiledResult compiled = new MVELCompiler().transpile(mvelBuilder.build());
        return compiled.getUnit();
    }

    private BlockStmt getFirstMethodBody(CompilationUnit unit) {
        return unit.getType(0).getMember(0).asMethodDeclaration().getBody().get();
    }

    @Test
    public void testString() {
        // plain Java. Rewrite doesn't matter here
        String input = "{ String str = \"Hello\";\n" +
                "return str.length(); }";

        CompilationUnit unit = transpileWithoutRewrite(ctx -> {}, input);
        BlockStmt body = getFirstMethodBody(unit);

        // "str" is the return expression
        NameExpr nameExpr = body.getStatement(1).asReturnStmt().getExpression().get().asMethodCallExpr().getScope().get().asNameExpr();

        ResolvedType resolvedType = nameExpr.calculateResolvedType();
        assertThat(resolvedType.describe()).isEqualTo("java.lang.String");
    }

    @Test
    public void testCast() {
        CompilationUnit unit = transpileWithoutRewrite(ctx -> ctx.addDeclaration("l", List.class),
                                                       "{ ((ArrayList)l).removeRange(0, 10); }");
        BlockStmt body = getFirstMethodBody(unit);

        // (ArrayList)l
        CastExpr castExpr = body.getStatement(1).asBlockStmt().getStatement(0).asExpressionStmt().getExpression()
                .asMethodCallExpr().getScope().get().asEnclosedExpr().getInner().asCastExpr();

        ResolvedType resolvedType = castExpr.calculateResolvedType();
        assertThat(resolvedType.describe()).isEqualTo("java.util.ArrayList");
    }

    @Test
    public void testInlineCast() {
        CompilationUnit unit = transpileWithoutRewrite(ctx -> ctx.addDeclaration("l", List.class),
                                            "{ l#ArrayList#removeRange(0, 10); }");
        BlockStmt body = getFirstMethodBody(unit);

        // "l#ArrayList#"
        InlineCastExpr inlineCastExpr = body.getStatement(1).asBlockStmt().getStatement(0).asExpressionStmt().getExpression()
                .asMethodCallExpr().getScope().get().asInlineCastExpr();

        ResolvedType resolvedType = inlineCastExpr.calculateResolvedType();
        assertThat(resolvedType.describe()).isEqualTo("java.util.ArrayList");
    }

    @Test
    public void testFullyQualifiedInlineCast() {
        // FullyQualifiedInlineCast is not actually created. We will eventually remove the class
    }

    @Test
    public void testHalfBinaryExpr() {
        CompilationUnit unit = transpileWithoutRewrite(ctx -> ctx.addDeclaration("value", Integer.class),
                                                       "{ return value > 1 && < 5; }");
        BlockStmt body = getFirstMethodBody(unit);

        HalfBinaryExpr halfBinaryExpr = body.findFirst(HalfBinaryExpr.class)
                .orElseThrow(() -> new AssertionError("Missing HalfBinaryExpr"));

        ResolvedType resolvedType = halfBinaryExpr.calculateResolvedType();
        assertThat(resolvedType.describe()).isEqualTo("boolean");
    }

    @Test
    public void testBigDecimalLiteral() {
        CompilationUnit unit = transpileWithoutRewrite(ctx -> {}, "{ return 10B; }");
        BlockStmt body = getFirstMethodBody(unit);

        ReturnStmt returnStmt = body.findFirst(ReturnStmt.class)
                .orElseThrow(() -> new AssertionError("Missing return statement"));
        BigDecimalLiteralExpr bigDecimalLiteral = returnStmt.getExpression()
                .orElseThrow(() -> new AssertionError("Return without expression")).asBigDecimalLiteralExpr();

        ResolvedType resolvedType = bigDecimalLiteral.calculateResolvedType();
        assertThat(resolvedType.describe()).isEqualTo("java.math.BigDecimal");
    }

    @Test
    public void testBigIntegerLiteral() {
        CompilationUnit unit = transpileWithoutRewrite(ctx -> {}, "{ return 10I; }");
        BlockStmt body = getFirstMethodBody(unit);

        ReturnStmt returnStmt = body.findFirst(ReturnStmt.class)
                .orElseThrow(() -> new AssertionError("Missing return statement"));
        BigIntegerLiteralExpr bigIntegerLiteral = returnStmt.getExpression()
                .orElseThrow(() -> new AssertionError("Return without expression")).asBigIntegerLiteralExpr();

        ResolvedType resolvedType = bigIntegerLiteral.calculateResolvedType();
        assertThat(resolvedType.describe()).isEqualTo("java.math.BigInteger");
    }

    @Test
    public void testDrlNameExpr() {
        // TODO: This test fails when using ANTLR parser, because it creates NameExpr instead of DrlNameExpr. Need to fix it later
        CompilationUnit unit = transpileWithoutRewrite(ctx -> ctx.addDeclaration("person", Person.class),
                                                       "{ return person; }");
        BlockStmt body = getFirstMethodBody(unit);

        ReturnStmt returnStmt = body.findFirst(ReturnStmt.class)
                .orElseThrow(() -> new AssertionError("Missing return statement"));
        Expression expression = returnStmt.getExpression()
                .orElseThrow(() -> new AssertionError("Return without expression"));

        assertThat(expression).isInstanceOf(DrlNameExpr.class);
        DrlNameExpr drlNameExpr = (DrlNameExpr) expression;
        assertThat(drlNameExpr.getBackReferencesCount()).isZero();

        ResolvedType resolvedType = drlNameExpr.calculateResolvedType();
        assertThat(resolvedType.describe()).isEqualTo(Person.class.getCanonicalName());
    }

    @Test
    public void testDrlxExpression() {
        // TODO: DrlxExpression is not created when using legacy/ANTLR parser. Leave it for later
        //       DrlsParser can create DrlxExpression, but rather, we will do it in drlx-parser project
    }

    @Ignore("NullSafe is not yet implemented even in legacy JavaCC. Clarify `?.` or `!.`")
    @Test
    public void testNullSafeFieldAccessExpr() {

        CompilationUnit unit = transpileWithoutRewrite(ctx -> ctx.addDeclaration("value", Person.class),
                                                       "{ return value?.address; }");
        BlockStmt body = getFirstMethodBody(unit);

        NullSafeFieldAccessExpr nullSafeFieldAccessExpr = body.findFirst(NullSafeFieldAccessExpr.class)
                .orElseThrow(() -> new AssertionError("Missing NullSafeFieldAccessExpr"));

        ResolvedType resolvedType = nullSafeFieldAccessExpr.calculateResolvedType();
        assertThat(resolvedType.describe()).isEqualTo(Address.class.getCanonicalName());
    }

    @Ignore("NullSafe is not yet implemented even in legacy JavaCC. Clarify `?.` or `!.`")
    @Test
    public void testNullSafeMethodCallExpr() {
        CompilationUnit unit = transpileWithoutRewrite(ctx -> ctx.addDeclaration("value", Person.class),
                                                       "{ return value?.getAddress(); }");
        BlockStmt body = getFirstMethodBody(unit);

        NullSafeMethodCallExpr nullSafeMethodCallExpr = body.findFirst(NullSafeMethodCallExpr.class)
                .orElseThrow(() -> new AssertionError("Missing NullSafeMethodCallExpr"));

        ResolvedType resolvedType = nullSafeMethodCallExpr.calculateResolvedType();
        assertThat(resolvedType.describe()).isEqualTo(Address.class.getCanonicalName());
    }

    @Ignore("Should be tested in drlx-parser project") // PointFreeExpr is inside DrlxExpression
    @Test
    public void testPointFreeExpr() {
        CompilationUnit unit = transpileWithoutRewrite(ctx -> ctx.addDeclaration("value", String.class),
                                                       "{ return value matches \"[A-Z]*\"; }");
        BlockStmt body = getFirstMethodBody(unit);

        PointFreeExpr pointFreeExpr = body.findFirst(PointFreeExpr.class)
                .orElseThrow(() -> new AssertionError("Missing PointFreeExpr"));

        ResolvedType resolvedType = pointFreeExpr.calculateResolvedType();
        assertThat(resolvedType.describe()).isEqualTo("boolean");
    }

    @Ignore("Should be tested in drlx-parser project") // PointFreeExpr is inside DrlxExpression
    @Test
    public void testHalfPointFreeExpr() {
        CompilationUnit unit = transpileWithoutRewrite(ctx -> ctx.addDeclaration("value", String.class),
                                                       "{ return matches \"[A-Z]*\"; }");
        BlockStmt body = getFirstMethodBody(unit);

        HalfPointFreeExpr halfPointFreeExpr = body.findFirst(HalfPointFreeExpr.class)
                .orElseThrow(() -> new AssertionError("Missing HalfPointFreeExpr"));

        ResolvedType resolvedType = halfPointFreeExpr.calculateResolvedType();
        assertThat(resolvedType.describe()).isEqualTo("boolean");
    }

    @Test
    public void testListCreationLiteralExpression() {
        CompilationUnit unit = transpileWithoutRewrite(ctx -> {}, "{ return [1, 2, 3]; }");
        BlockStmt body = getFirstMethodBody(unit);

        Expression expression = body.getStatement(0).asReturnStmt().getExpression()
                .orElseThrow(() -> new AssertionError("Return without expression"));
        assertThat(expression).isInstanceOf(ListCreationLiteralExpression.class);

        ResolvedType resolvedType = expression.calculateResolvedType();
        assertThat(resolvedType.describe()).isEqualTo("java.util.List<E>");
    }

    @Test
    public void testListCreationLiteralExpressionElement() {
        CompilationUnit unit = transpileWithoutRewrite(ctx -> {}, "{ return [\"a\"]; }");
        BlockStmt body = getFirstMethodBody(unit);

        ListCreationLiteralExpression listLiteral = body.findFirst(ListCreationLiteralExpression.class)
                .orElseThrow(() -> new AssertionError("Missing ListCreationLiteralExpression"));
        ListCreationLiteralExpressionElement element = listLiteral.getExpressions().get(0)
                .asListCreationLiteralExpressionElement();

        ResolvedType resolvedType = element.calculateResolvedType();
        assertThat(resolvedType.describe()).isEqualTo("java.lang.String");
    }

    @Test
    public void testMapCreationLiteralExpression() {
        CompilationUnit unit = transpileWithoutRewrite(ctx -> {}, "{ return [\"a\": 1, \"b\": 2]; }");
        BlockStmt body = getFirstMethodBody(unit);

        Expression expression = body.getStatement(0).asReturnStmt().getExpression()
                .orElseThrow(() -> new AssertionError("Return without expression"));
        assertThat(expression).isInstanceOf(MapCreationLiteralExpression.class);

        ResolvedType resolvedType = expression.calculateResolvedType();
        assertThat(resolvedType.describe()).isEqualTo("java.util.Map<K, V>");
    }

    @Test
    public void testMapCreationLiteralExpressionKeyValuePair() {
        CompilationUnit unit = transpileWithoutRewrite(ctx -> {}, "{ return [\"a\": \"b\"]; }");
        BlockStmt body = getFirstMethodBody(unit);

        MapCreationLiteralExpression mapLiteral = body.findFirst(MapCreationLiteralExpression.class)
                .orElseThrow(() -> new AssertionError("Missing MapCreationLiteralExpression"));
        MapCreationLiteralExpressionKeyValuePair entry = mapLiteral.getExpressions().get(0)
                .asMapCreationLiteralExpressionKeyValuePair();

        ResolvedType resolvedType = entry.calculateResolvedType();
        assertThat(resolvedType.describe()).isEqualTo("java.util.Map.Entry<K, V>");
    }

    @Ignore("TemporalLiteralExpr is DRL-specific and requires CompilationUnit context for type resolution. Should be tested in drlx-parser project")
    @Test
    public void testTemporalLiteralExpr() {
        // Parse temporal literal directly using DrlxParser
        TemporalLiteralExpr temporalLiteral = DrlxParser.parseTemporalLiteral("5s");

        ResolvedType resolvedType = temporalLiteral.calculateResolvedType();
        assertThat(resolvedType.describe()).isEqualTo("long");
    }
}
