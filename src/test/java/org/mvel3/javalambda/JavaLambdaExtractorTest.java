package org.mvel3.javalambda;

import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JavaLambdaExtractorTest {

    private JavaLambdaExtractor extractor;
    private CombinedTypeSolver typeSolver;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setup() {
        extractor = new JavaLambdaExtractor();
        typeSolver = new CombinedTypeSolver(new ReflectionTypeSolver());
    }

    private Path writeSource(String filename, String source) throws IOException {
        Path file = tempDir.resolve(filename);
        Files.writeString(file, source);
        return file;
    }

    @Test
    void identicalLambdasWithDifferentParamNames_samePhysicalId() throws IOException {
        Path file = writeSource("Test.java", """
            import java.util.function.Predicate;
            class Test {
                static Predicate<String> f(Predicate<String> p) { return p; }
                void m() {
                    f(s -> s.length() > 5);
                    f(x -> x.length() > 5);
                }
            }
            """);
        ExtractionResult result = extractor.extract(List.of(file), typeSolver);

        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.uniqueCount()).isEqualTo(1);
        assertThat(result.reusedCount()).isEqualTo(1);
        assertThat(result.allLambdas().get(0).physicalId())
                .isEqualTo(result.allLambdas().get(1).physicalId());
    }

    @Test
    void singleOccurrenceLambdas_excludedFromResult() throws IOException {
        Path file = writeSource("Test.java", """
            import java.util.function.Predicate;
            class Test {
                static Predicate<String> f(Predicate<String> p) { return p; }
                void m() {
                    f(s -> s.length() > 5);
                    f(s -> s.isEmpty());
                }
            }
            """);
        ExtractionResult result = extractor.extract(List.of(file), typeSolver);

        assertThat(result.totalCount()).isEqualTo(0);
        assertThat(result.uniqueCount()).isEqualTo(0);
    }

    @Test
    void duplicatePlusSingle_onlyDuplicateInResult() throws IOException {
        Path file = writeSource("Test.java", """
            import java.util.function.Predicate;
            class Test {
                static Predicate<String> f(Predicate<String> p) { return p; }
                void m() {
                    f(s -> s.length() > 5);
                    f(x -> x.length() > 5);
                    f(s -> s.isEmpty());
                }
            }
            """);
        ExtractionResult result = extractor.extract(List.of(file), typeSolver);

        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.uniqueCount()).isEqualTo(1);
        assertThat(result.reusedCount()).isEqualTo(1);
    }

    @Test
    void capturingLambda_skipped() throws IOException {
        Path file = writeSource("Test.java", """
            import java.util.function.Predicate;
            class Test {
                static Predicate<String> f(Predicate<String> p) { return p; }
                void m() {
                    int limit = 5;
                    f(s -> s.length() > limit);
                    f(s -> s.length() > limit);
                }
            }
            """);
        ExtractionResult result = extractor.extract(List.of(file), typeSolver);

        assertThat(result.totalCount()).isEqualTo(0);
        assertThat(result.skippedCaptureCount()).isEqualTo(2);
    }

    @Test
    void readProperties_extracted() throws IOException {
        Path file = writeSource("Test.java", """
            import java.util.function.Predicate;
            class Test {
                static Predicate<String> f(Predicate<String> p) { return p; }
                void m() {
                    f(s -> s.length() > 5);
                    f(x -> x.length() > 5);
                }
            }
            """);
        ExtractionResult result = extractor.extract(List.of(file), typeSolver);

        assertThat(result.allLambdas()).hasSize(2);
        assertThat(result.allLambdas().get(0).readProperties()).isNotEmpty();
    }
}
