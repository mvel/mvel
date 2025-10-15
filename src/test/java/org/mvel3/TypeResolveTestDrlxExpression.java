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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mvel3.parser.DrlxParser;
import org.mvel3.parser.ParseStart;
import org.mvel3.parser.ast.expr.DrlxExpression;
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
import org.mvel3.parser.ast.expr.OOPathChunk;
import org.mvel3.parser.ast.expr.OOPathExpr;
import org.mvel3.parser.ast.expr.PointFreeExpr;
import org.mvel3.parser.ast.expr.TemporalLiteralExpr;
import org.mvel3.transpiler.MVELTranspiler;
import org.mvel3.transpiler.TranspiledResult;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for type resolution with mvel3 custom AST nodes, without rewriting.
 * Specific to DrlxExpression-related nodes.
 */
public class TypeResolveTestDrlxExpression {

    private static final Collection<String> operators = new HashSet<>();
    static {
        operators.addAll(Arrays.asList("after", "before", "in", "matches", "includes"));
    }

    final ParseStart<DrlxExpression> parser = DrlxParser.buildDrlxParserWithArguments(operators);

    /**
     * Parse a DrlxExpression and wrap it in a CompilationUnit with proper symbol resolution context.
     * This is needed because DrlxExpression-specific nodes require a CompilationUnit for type resolution.
     *
     * @param contextUpdater Consumer to configure variable declarations (currently unused, for compatibility)
     * @param inputExpression The MVEL/DRL expression string to parse
     * @return CompilationUnit containing the parsed expression wrapped in a method
     */
    private CompilationUnit parseDrlxExpressionWithCompilationUnit(
            Consumer<MVELBuilder<Map<String, Object>, Void, Object>> contextUpdater,
            String inputExpression) {

        // TODO: This may be replaced by MVELTranspiler with ContentType.DRLX_EXPRESSION

        // Create CompilationUnit
        CompilationUnit cu = new CompilationUnit();
        cu.setPackageDeclaration("org.mvel3");

        // Add imports
        cu.addImport(List.class);
        cu.addImport(java.util.ArrayList.class);
        cu.addImport(java.util.HashMap.class);
        cu.addImport(Map.class);
        cu.addImport(BigDecimal.class);
        cu.addImport(BigInteger.class);
        cu.addImport(Address.class);
        cu.addImport(Person.class);
        cu.addImport(Gender.class);

        // Create class
        ClassOrInterfaceDeclaration clazz = cu.addClass("GeneratorEvaluator__", Modifier.Keyword.PUBLIC);

        // Create method
        MethodDeclaration method = clazz.addMethod("eval", Modifier.Keyword.PUBLIC);
        method.setType("java.lang.Object");
        method.addParameter(new ClassOrInterfaceType(null, "java.util.Map"), "__context");

        // Create method body
        BlockStmt methodBody = new BlockStmt();
        method.setBody(methodBody);

        // Parse using DrlxParser with MvelParser
        try {
            Expression parsedExpr = DrlxParser.parseExpression(parser, inputExpression).getExpr();
            ReturnStmt returnStmt = new ReturnStmt(parsedExpr);
            methodBody.addStatement(returnStmt);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse expression: " + inputExpression, e);
        }

        // Set up symbol resolver
        setupSymbolResolver(cu);

        return cu;
    }

    /**
     * Set up JavaSymbolSolver for type resolution
     */
    private void setupSymbolResolver(CompilationUnit cu) {
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());
        JavaSymbolSolver symbolResolver = new JavaSymbolSolver(typeSolver);
        symbolResolver.inject(cu);
    }

    protected BlockStmt getFirstMethodBody(CompilationUnit unit) {
        return unit.getType(0).getMember(0).asMethodDeclaration().getBody().get();
    }

    @Test
    public void testDrlxExpression() {
        // TODO: DrlxExpression is not created when using legacy/ANTLR parser. Leave it for later
        //       DrlsParser can create DrlxExpression, but rather, we will do it in drlx-parser project
    }

    @Ignore("Not yet implemented in legacy JavaCC DrlxParser") // PointFreeExpr is inside DrlxExpression
    @Test
    public void testPointFreeExpr() {
        CompilationUnit unit = parseDrlxExpressionWithCompilationUnit(ctx -> ctx.addDeclaration("value", String.class),
                                                       "value matches \"[A-Z]*\"");
        BlockStmt body = getFirstMethodBody(unit);

        PointFreeExpr pointFreeExpr = body.findFirst(PointFreeExpr.class)
                .orElseThrow(() -> new AssertionError("Missing PointFreeExpr"));

        ResolvedType resolvedType = pointFreeExpr.calculateResolvedType();
        assertThat(resolvedType.describe()).isEqualTo("boolean");
    }

    @Ignore("Not yet implemented in legacy JavaCC DrlxParser") // PointFreeExpr is inside DrlxExpression
    @Test
    public void testHalfPointFreeExpr() {
        CompilationUnit unit = parseDrlxExpressionWithCompilationUnit(ctx -> ctx.addDeclaration("value", String.class),
                                                       "{ return matches \"[A-Z]*\"; }");
        BlockStmt body = getFirstMethodBody(unit);

        HalfPointFreeExpr halfPointFreeExpr = body.findFirst(HalfPointFreeExpr.class)
                .orElseThrow(() -> new AssertionError("Missing HalfPointFreeExpr"));

        ResolvedType resolvedType = halfPointFreeExpr.calculateResolvedType();
        assertThat(resolvedType.describe()).isEqualTo("boolean");
    }

    @Ignore("Not yet implemented in legacy JavaCC DrlxParser")
    @Test
    public void testTemporalLiteralExpr() {
        // Parse temporal literal directly using DrlxParser
        CompilationUnit unit = parseDrlxExpressionWithCompilationUnit(ctx -> {}, "this after[5,8] $a");
        BlockStmt body = getFirstMethodBody(unit);

        TemporalLiteralExpr temporalLiteral = body.findFirst(TemporalLiteralExpr.class)
                .orElseThrow(() -> new AssertionError("Missing TemporalLiteralExpr"));

        ResolvedType resolvedType = temporalLiteral.calculateResolvedType();
        assertThat(resolvedType.describe()).isEqualTo("long");
    }

    @Test
    public void testOOPathExpr() {
        // Test OOPath navigation through object graph
        CompilationUnit unit = parseDrlxExpressionWithCompilationUnit(ctx -> {}, "/addresses/city");
        BlockStmt body = getFirstMethodBody(unit);

        OOPathExpr ooPathExpr = body.findFirst(OOPathExpr.class)
                .orElseThrow(() -> new AssertionError("Missing OOPathExpr"));

        // OOPathExpr itself doesn't have a resolved type (it's a navigation expression)
        // But we can verify its structure
        assertThat(ooPathExpr.getChunks().size()).isEqualTo(2);
        assertThat(ooPathExpr.getChunks().get(0).getField().asString()).isEqualTo("addresses");
        assertThat(ooPathExpr.getChunks().get(1).getField().asString()).isEqualTo("city");
    }

    @Test
    public void testOOPathChunk() {
        // Test OOPath chunk with condition
        CompilationUnit unit = parseDrlxExpressionWithCompilationUnit(
                ctx -> {}, "/addresses[city == \"Tokyo\"]/city");
        BlockStmt body = getFirstMethodBody(unit);

        OOPathExpr ooPathExpr = body.findFirst(OOPathExpr.class)
                .orElseThrow(() -> new AssertionError("Missing OOPathExpr"));

        // Get the first chunk (addresses with condition)
        OOPathChunk firstChunk = ooPathExpr.getChunks().get(0);
        assertThat(firstChunk.getField().asString()).isEqualTo("addresses");
        assertThat(firstChunk.getConditions().size()).isEqualTo(1);
        ResolvedType resolvedType = firstChunk.getConditions().get(0).calculateResolvedType();
        assertThat(resolvedType.describe()).isEqualTo("boolean");

        // Get the second chunk (city)
        OOPathChunk secondChunk = ooPathExpr.getChunks().get(1);
        assertThat(secondChunk.getField().asString()).isEqualTo("city");
        assertThat(secondChunk.getConditions().size()).isEqualTo(0);
    }

    // Note: No test for rule constructs e.g. RuleDeclaration, RulePattern
    // It should be done in drlx-parser project
}
