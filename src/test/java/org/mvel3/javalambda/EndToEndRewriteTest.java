package org.mvel3.javalambda;

import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mvel3.javacompiler.KieMemoryCompiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EndToEndRewriteTest {

    @TempDir
    Path tempDir;

    private CombinedTypeSolver typeSolver;

    @BeforeEach
    void setup() {
        typeSolver = new CombinedTypeSolver(new ReflectionTypeSolver());
    }

    @Test
    void fullPipeline_extractGenerateRewrite() throws IOException {
        // --- Input: three lambdas, two are structurally identical ---
        String original = """
            import java.util.function.Predicate;
            class RuleDefinitions {
                static Predicate<String> when(Predicate<String> p) { return p; }
                void rules() {
                    when(s -> s.length() > 5);
                    when(s -> s.isEmpty());
                    when(x -> x.length() > 5);
                }
            }
            """;
        Path sourceFile = tempDir.resolve("RuleDefinitions.java");
        Files.writeString(sourceFile, original);

        // --- Step 1: Extract ---
        // Finds all lambdas, normalizes bodies (s→v1, x→v1), groups by hash.
        // "s -> s.length() > 5" and "x -> x.length() > 5" normalize to the
        // same body → duplicate group (2 members) → processed.
        // "s -> s.isEmpty()" appears once → single occurrence → dropped.
        JavaLambdaExtractor extractor = new JavaLambdaExtractor();
        ExtractionResult result = extractor.extract(List.of(sourceFile), typeSolver);

        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.uniqueCount()).isEqualTo(1);
        assertThat(result.reusedCount()).isEqualTo(1);

        // --- Step 2: Generate registry ---
        // One static final field per unique lambda, typed as the resolved
        // functional interface (Predicate<String>), with the original
        // (non-normalized) lambda source as the initializer.
        LambdaRegistryGenerator generator = new LambdaRegistryGenerator(
                "com.example", "LambdaRegistry");
        String registrySource = generator.generate(result);

        String expectedRegistry = """
                package com.example;

                public final class LambdaRegistry {

                    private LambdaRegistry() {}

                    public static final java.util.function.Predicate<java.lang.String> LAMBDA_0 = s -> s.length() > 5;

                }
                """;
        assertThat(registrySource).isEqualToIgnoringWhitespace(expectedRegistry);

        // --- Step 3: Rewrite source ---
        // Replaces duplicate inline lambdas with registry field references.
        // Non-duplicate lambdas (s.isEmpty()) are left untouched.
        // Adds the registry import after the last existing import.
        LambdaSourceRewriter rewriter = new LambdaSourceRewriter("com.example.LambdaRegistry");
        String rewritten = rewriter.rewrite(result, sourceFile);

        String expectedRewritten = """
            import java.util.function.Predicate;
            import com.example.LambdaRegistry;
            class RuleDefinitions {
                static Predicate<String> when(Predicate<String> p) { return p; }
                void rules() {
                    when(LambdaRegistry.LAMBDA_0);
                    when(s -> s.isEmpty());
                    when(LambdaRegistry.LAMBDA_0);
                }
            }
            """;
        assertThat(rewritten).isEqualTo(expectedRewritten);

        // --- Step 4: Compile both together ---
        // Proves the generated registry type is correct (Predicate<String>,
        // not Object) and the rewritten source references it properly.
        Map<String, String> sources = new HashMap<>();
        sources.put("com.example.LambdaRegistry", registrySource);
        sources.put("RuleDefinitions", rewritten);
        Map<String, byte[]> byteCode = KieMemoryCompiler.compileNoLoad(
                sources, getClass().getClassLoader(), null);
        assertThat(byteCode).containsKeys("com.example.LambdaRegistry", "RuleDefinitions");
    }

    @Test
    void multipleFiles_crossFileDedup() throws IOException {
        String file1Source = """
            import java.util.function.Predicate;
            class Rules1 {
                static Predicate<String> when(Predicate<String> p) { return p; }
                void rules() {
                    when(s -> s.length() > 5);
                }
            }
            """;
        String file2Source = """
            import java.util.function.Predicate;
            class Rules2 {
                static Predicate<String> when(Predicate<String> p) { return p; }
                void rules() {
                    when(x -> x.length() > 5);
                }
            }
            """;
        Path f1 = tempDir.resolve("Rules1.java");
        Path f2 = tempDir.resolve("Rules2.java");
        Files.writeString(f1, file1Source);
        Files.writeString(f2, file2Source);

        JavaLambdaExtractor extractor = new JavaLambdaExtractor();
        ExtractionResult result = extractor.extract(List.of(f1, f2), typeSolver);

        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.uniqueCount()).isEqualTo(1);
        assertThat(result.reusedCount()).isEqualTo(1);
        assertThat(result.allLambdas().get(0).physicalId())
                .isEqualTo(result.allLambdas().get(1).physicalId());
    }
}
