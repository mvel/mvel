package org.mvel3.javalambda;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LambdaRegistryGeneratorTest {

    @Test
    void generatesClassWithFieldsAndReadProps() {
        NormalizedLambda lambda0 = new NormalizedLambda(
                Path.of("Test.java"), 10, 20, null, 0, false,
                Set.of("length"), parseLambdaExpr("s -> s.length() > 5"));
        NormalizedLambda lambda1 = new NormalizedLambda(
                Path.of("Test.java"), 11, 20, null, 1, false,
                Set.of("empty"), parseLambdaExpr("s -> s.isEmpty()"));

        Map<Integer, NormalizedLambda> uniqueMap = new LinkedHashMap<>();
        uniqueMap.put(0, lambda0);
        uniqueMap.put(1, lambda1);
        ExtractionResult result = new ExtractionResult(
                List.of(lambda0, lambda1), uniqueMap, 2, 2, 0, 0);

        LambdaRegistryGenerator generator = new LambdaRegistryGenerator(
                "com.example", "LambdaRegistry");
        String source = generator.generate(result);

        assertThat(source).contains("package com.example;");
        assertThat(source).contains("public final class LambdaRegistry");
        assertThat(source).contains("LAMBDA_0");
        assertThat(source).contains("LAMBDA_1");
        assertThat(source).contains("LAMBDA_0_READ_PROPS");
        assertThat(source).contains("LAMBDA_1_READ_PROPS");
        CompilationUnit cu = StaticJavaParser.parse(source);
        List<FieldDeclaration> fields = cu.findAll(FieldDeclaration.class);
        assertThat(fields).hasSize(4);
    }

    private com.github.javaparser.ast.expr.LambdaExpr parseLambdaExpr(String lambdaSource) {
        String classSource = """
            import java.util.function.Predicate;
            class X {
                void m() { Predicate<String> p = %s; }
            }
            """.formatted(lambdaSource);
        return StaticJavaParser.parse(classSource)
                .findFirst(com.github.javaparser.ast.expr.LambdaExpr.class).orElseThrow();
    }
}
