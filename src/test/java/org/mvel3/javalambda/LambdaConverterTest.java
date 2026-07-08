package org.mvel3.javalambda;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LambdaConverterTest {

    @BeforeEach
    void setup() {
        CombinedTypeSolver typeSolver = new CombinedTypeSolver(new ReflectionTypeSolver());
        StaticJavaParser.getParserConfiguration()
                .setSymbolResolver(new JavaSymbolSolver(typeSolver));
    }

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
        MethodDeclaration md = LambdaConverter.toMethodDeclaration(lambda, "test");

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
        MethodDeclaration md = LambdaConverter.toMethodDeclaration(lambda, "test");

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
        MethodDeclaration md = LambdaConverter.toMethodDeclaration(lambda, "test");

        assertThat(md.getParameters()).hasSize(2);
        assertThat(md.getParameter(0).getTypeAsString()).isEqualTo("java.lang.String");
        assertThat(md.getParameter(1).getTypeAsString()).isEqualTo("java.lang.Integer");
    }

    @Test
    void unresolvedParameterType_fallsBackToObject() {
        String source = """
            class Test {
                interface MyFunc<T> { boolean check(T t); }
                void m() {
                    MyFunc<String> f = t -> t.length() > 0;
                }
            }
            """;
        LambdaExpr lambda = parseLambda(source);
        MethodDeclaration md = LambdaConverter.toMethodDeclaration(lambda, "check");

        assertThat(md.getParameters()).hasSize(1);
        assertThat(md.getParameter(0).getTypeAsString()).isNotEmpty();
    }
}
