package org.mvel3.javalambda;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LambdaFinderTest {

    private CombinedTypeSolver typeSolver;

    @BeforeEach
    void setup() {
        typeSolver = new CombinedTypeSolver(new ReflectionTypeSolver());
        StaticJavaParser.getParserConfiguration()
                .setSymbolResolver(new JavaSymbolSolver(typeSolver));
    }

    @Test
    void findsLambdaAtTargetSignatureCallSite() {
        String source = """
            import java.util.function.Predicate;
            class Test {
                static Predicate<String> filter(Predicate<String> p) { return p; }
                void m() {
                    filter(s -> s.length() > 5);
                }
            }
            """;
        CompilationUnit cu = StaticJavaParser.parse(source);
        LambdaSignature sig = new LambdaSignature(
                "java.util.function.Predicate", "test", 1);
        LambdaFinder finder = new LambdaFinder(List.of(sig));

        List<ExtractedLambda> found = finder.find(cu, Path.of("Test.java"), typeSolver);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).capturing()).isFalse();
    }

    @Test
    void ignoresLambdaAtNonTargetCallSite() {
        String source = """
            import java.util.List;
            import java.util.ArrayList;
            import java.util.function.Consumer;
            class Test {
                void m() {
                    List<String> list = new ArrayList<>();
                    list.forEach(s -> System.out.println(s));
                }
            }
            """;
        CompilationUnit cu = StaticJavaParser.parse(source);
        LambdaSignature sig = new LambdaSignature(
                "java.util.function.Predicate", "test", 1);
        LambdaFinder finder = new LambdaFinder(List.of(sig));

        List<ExtractedLambda> found = finder.find(cu, Path.of("Test.java"), typeSolver);

        assertThat(found).isEmpty();
    }

    @Test
    void marksCapturingLambda() {
        String source = """
            import java.util.function.Predicate;
            class Test {
                static Predicate<String> filter(Predicate<String> p) { return p; }
                void m() {
                    int threshold = 5;
                    filter(s -> s.length() > threshold);
                }
            }
            """;
        CompilationUnit cu = StaticJavaParser.parse(source);
        LambdaSignature sig = new LambdaSignature(
                "java.util.function.Predicate", "test", 1);
        LambdaFinder finder = new LambdaFinder(List.of(sig));

        List<ExtractedLambda> found = finder.find(cu, Path.of("Test.java"), typeSolver);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).capturing()).isTrue();
    }
}
