package org.mvel3.javalambda;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.LambdaExpr;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LambdaConverterTest {

    private LambdaExpr parseLambda(String classSource) {
        CompilationUnit cu = StaticJavaParser.parse(classSource);
        return cu.findFirst(LambdaExpr.class).orElseThrow();
    }

    @Test
    void expressionBody_wrappedInReturnStatement() {
        String source = """
            import java.util.function.Predicate;
            class Test {
                void m() {
                    Predicate<String> p = s -> s.length() > 5;
                }
            }
            """;
        LambdaExpr lambda = parseLambda(source);
        MethodDeclaration md = LambdaConverter.toMethodDeclaration(
                lambda, "test", List.of(String.class));

        assertThat(md.getNameAsString()).isEqualTo("test");
        assertThat(md.getParameters()).hasSize(1);
        assertThat(md.getParameter(0).getTypeAsString()).isEqualTo("java.lang.String");
        assertThat(md.getBody()).isPresent();
        String body = md.getBody().get().toString();
        assertThat(body).contains("return");
        assertThat(body).contains("s.length() > 5");
    }

    @Test
    void blockBody_preservedAsIs() {
        String source = """
            import java.util.function.Predicate;
            class Test {
                void m() {
                    Predicate<String> p = s -> { return s.length() > 5; };
                }
            }
            """;
        LambdaExpr lambda = parseLambda(source);
        MethodDeclaration md = LambdaConverter.toMethodDeclaration(
                lambda, "test", List.of(String.class));

        assertThat(md.getBody()).isPresent();
        String body = md.getBody().get().toString();
        assertThat(body).contains("return s.length() > 5");
    }

    @Test
    void multipleParameters_allTyped() {
        String source = """
            import java.util.function.BiPredicate;
            class Test {
                void m() {
                    BiPredicate<String, Integer> p = (s, n) -> s.length() > n;
                }
            }
            """;
        LambdaExpr lambda = parseLambda(source);
        MethodDeclaration md = LambdaConverter.toMethodDeclaration(
                lambda, "test", List.of(String.class, Integer.class));

        assertThat(md.getParameters()).hasSize(2);
        assertThat(md.getParameter(0).getTypeAsString()).isEqualTo("java.lang.String");
        assertThat(md.getParameter(1).getTypeAsString()).isEqualTo("java.lang.Integer");
    }

    @Test
    void primitiveParameterType() {
        String source = """
            import java.util.function.IntPredicate;
            class Test {
                void m() {
                    IntPredicate p = n -> n > 5;
                }
            }
            """;
        LambdaExpr lambda = parseLambda(source);
        MethodDeclaration md = LambdaConverter.toMethodDeclaration(
                lambda, "test", List.of(int.class));

        assertThat(md.getParameter(0).getTypeAsString()).isEqualTo("int");
    }
}
