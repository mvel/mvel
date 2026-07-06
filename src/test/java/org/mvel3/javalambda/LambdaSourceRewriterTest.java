package org.mvel3.javalambda;

import com.github.javaparser.StaticJavaParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LambdaSourceRewriterTest {

    @TempDir
    Path tempDir;

    @Test
    void replacesLambdaWithRegistryReference() throws IOException {
        String original = """
            import java.util.function.Predicate;
            class Test {
                static Predicate<String> f(Predicate<String> p) { return p; }
                void m() {
                    f(s -> s.length() > 5);
                }
            }
            """;
        Path file = tempDir.resolve("Test.java");
        Files.writeString(file, original);

        var cu = StaticJavaParser.parse(original);
        var lambdaExpr = cu.findFirst(com.github.javaparser.ast.expr.LambdaExpr.class).orElseThrow();
        int line = lambdaExpr.getBegin().get().line;
        int column = lambdaExpr.getBegin().get().column;

        NormalizedLambda lambda0 = new NormalizedLambda(
                file, line, column, null, 0, false,
                Set.of("s"), lambdaExpr, "java.util.function.Predicate");

        Map<Integer, NormalizedLambda> uniqueMap = new LinkedHashMap<>();
        uniqueMap.put(0, lambda0);
        ExtractionResult result = new ExtractionResult(
                List.of(lambda0), uniqueMap, 1, 1, 0, 0);

        LambdaSourceRewriter rewriter = new LambdaSourceRewriter("com.example.LambdaRegistry");
        String rewritten = rewriter.rewrite(result, file);

        assertThat(rewritten).contains("LambdaRegistry.LAMBDA_0");
        assertThat(rewritten).contains("import com.example.LambdaRegistry;");
        assertThat(rewritten).doesNotContain("s -> s.length() > 5");
    }

    @Test
    void preservesNonTargetLambdas() throws IOException {
        String original = """
            import java.util.function.Predicate;
            import java.util.List;
            import java.util.ArrayList;
            class Test {
                void m() {
                    List<String> list = new ArrayList<>();
                    list.forEach(s -> System.out.println(s));
                }
            }
            """;
        Path file = tempDir.resolve("Test.java");
        Files.writeString(file, original);

        ExtractionResult result = new ExtractionResult(
                List.of(), Map.of(), 0, 0, 0, 0);

        LambdaSourceRewriter rewriter = new LambdaSourceRewriter("com.example.LambdaRegistry");
        String rewritten = rewriter.rewrite(result, file);

        assertThat(rewritten).contains("s -> System.out.println(s)");
        assertThat(rewritten).doesNotContain("import com.example.LambdaRegistry;");
    }
}
