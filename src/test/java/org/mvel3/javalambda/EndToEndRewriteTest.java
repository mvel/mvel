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

        JavaLambdaExtractor extractor = new JavaLambdaExtractor();
        ExtractionResult result = extractor.extract(List.of(sourceFile), typeSolver);

        // s.length() > 5 has 2 occurrences → in result
        // s.isEmpty() has 1 occurrence → excluded
        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.uniqueCount()).isEqualTo(1);
        assertThat(result.reusedCount()).isEqualTo(1);

        LambdaRegistryGenerator generator = new LambdaRegistryGenerator(
                "com.example", "LambdaRegistry");
        String registrySource = generator.generate(result);

        assertThat(registrySource).contains("LAMBDA_0");
        assertThat(registrySource).doesNotContain("LAMBDA_1");

        LambdaSourceRewriter rewriter = new LambdaSourceRewriter("com.example.LambdaRegistry");
        String rewritten = rewriter.rewrite(result, sourceFile);

        assertThat(rewritten).contains("import com.example.LambdaRegistry;");
        long lambda0Count = rewritten.lines()
                .filter(l -> l.contains("LambdaRegistry.LAMBDA_0")).count();
        assertThat(lambda0Count).isEqualTo(2);
        // s.isEmpty() was not duplicated — stays as inline lambda
        assertThat(rewritten).contains("s.isEmpty()");

        // Verify both generated sources compile together
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
