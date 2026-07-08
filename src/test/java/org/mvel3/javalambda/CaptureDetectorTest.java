package org.mvel3.javalambda;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.LambdaExpr;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CaptureDetectorTest {

    private LambdaExpr parseLambda(String classSource) {
        CompilationUnit cu = StaticJavaParser.parse(classSource);
        return cu.findFirst(LambdaExpr.class).orElseThrow();
    }

    @Test
    void nonCapturingLambda_returnsFalse() {
        String source = """
            import java.util.function.Predicate;
            class Test {
                void m() {
                    Predicate<String> p = s -> s.length() > 5;
                }
            }
            """;
        LambdaExpr lambda = parseLambda(source);
        assertThat(CaptureDetector.isCapturing(lambda)).isFalse();
    }

    @Test
    void capturingLocalVariable_returnsTrue() {
        String source = """
            import java.util.function.Predicate;
            class Test {
                void m() {
                    int threshold = 5;
                    Predicate<String> p = s -> s.length() > threshold;
                }
            }
            """;
        LambdaExpr lambda = parseLambda(source);
        assertThat(CaptureDetector.isCapturing(lambda)).isTrue();
    }

    @Test
    void referencingStaticField_conservativelyTreatedAsCapture() {
        String source = """
            import java.util.function.Predicate;
            class Test {
                static final int MIN = 5;
                void m() {
                    Predicate<String> p = s -> s.length() > MIN;
                }
            }
            """;
        LambdaExpr lambda = parseLambda(source);
        assertThat(CaptureDetector.isCapturing(lambda)).isTrue();
    }

    @Test
    void lambdaWithOnlyParameterReferences_returnsFalse() {
        String source = """
            import java.util.function.BiPredicate;
            class Test {
                void m() {
                    BiPredicate<String, Integer> p = (s, n) -> s.length() > n;
                }
            }
            """;
        LambdaExpr lambda = parseLambda(source);
        assertThat(CaptureDetector.isCapturing(lambda)).isFalse();
    }

    @Test
    void lambdaWithMethodCallOnParam_returnsFalse() {
        String source = """
            import java.util.function.Predicate;
            class Test {
                void m() {
                    Predicate<String> p = s -> s.isEmpty();
                }
            }
            """;
        LambdaExpr lambda = parseLambda(source);
        assertThat(CaptureDetector.isCapturing(lambda)).isFalse();
    }

    @Test
    void lambdaWithLiteralOnly_returnsFalse() {
        String source = """
            import java.util.function.Predicate;
            class Test {
                void m() {
                    Predicate<String> p = s -> s.length() > 10;
                }
            }
            """;
        LambdaExpr lambda = parseLambda(source);
        assertThat(CaptureDetector.isCapturing(lambda)).isFalse();
    }
}
