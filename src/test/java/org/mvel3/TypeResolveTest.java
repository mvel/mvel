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
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.resolution.types.ResolvedType;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mvel3.parser.MvelParser;
import org.mvel3.parser.ast.expr.InlineCastExpr;
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
//        MvelParser.Factory.USE_ANTLR = false;
        MvelParser.Factory.USE_ANTLR = true;
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
}
