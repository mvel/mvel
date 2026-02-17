package org.mvel3;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.Test;
import org.mvel3.parser.antlr4.Antlr4MvelParser;
import org.mvel3.parser.printer.PrintUtil;
import org.mvel3.transpiler.MVELToJavaRewriter;
import org.mvel3.transpiler.context.Declaration;
import org.mvel3.transpiler.context.TranspilerContext;

import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_15;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests transpilation of a full Java compilation unit containing MVEL syntax.
 * Parses a complete class, applies the MVEL-to-Java rewriter on each method body,
 * and asserts the resulting source.
 */
class CompilationUnitTranspilerTest {

    /**
     * This test contains the following MVEL syntax:
     * - field access
     * - nested field access
     * - field assignment
     * - inline cast using MVEL's # operator
     */
    @Test
    void testFullClassWithMvelSyntax() {
        String input = """
                import org.mvel3.Person;
                import org.mvel3.Address;
                import java.util.ArrayList;
                import java.util.List;

                public class PersonService {

                    public String getName(Person p) {
                        return p.name;
                    }

                    public void updatePerson(Person p) {
                        p.name = "Alice";
                        p.age = 30;
                    }

                    public String getCity(Person p) {
                        return p.address.city;
                    }

                    public String getPublicField(Person p) {
                        return p.nickName;
                    }

                    public void processList(Person p, List<String> results) {
                        for (var addr : p.addresses) {
                            results.add(addr.city);
                        }
                    }

                    public Object inlineCast(List<?> l) {
                        return l#ArrayList#get(0);
                    }
                }
                """;

        String expected = """
                import org.mvel3.Person;
                import org.mvel3.Address;
                import java.util.ArrayList;
                import java.util.List;

                public class PersonService {

                    public String getName(Person p) {
                        return p.getName();
                    }

                    public void updatePerson(Person p) {
                        p.setName("Alice");
                        p.setAge(30);
                    }

                    public String getCity(Person p) {
                        return p.getAddress().getCity();
                    }

                    public String getPublicField(Person p) {
                        return p.nickName;
                    }

                    public void processList(Person p, List<String> results) {
                        for (var addr : p.getAddresses()) {
                            results.add(addr.getCity());
                        }
                    }

                    public Object inlineCast(List<?> l) {
                        return ((ArrayList) l).get(0);
                    }
                }
                """;

        String actual = transpileCompilationUnit(input);
        assertThat(actual).isEqualToIgnoringWhitespace(expected);
    }

    private String transpileCompilationUnit(String code) {
        // Set up type solver and symbol resolver
        TypeSolver typeSolver = new ReflectionTypeSolver(false);
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);

        ParserConfiguration conf = new ParserConfiguration();
        conf.setLanguageLevel(JAVA_15);
        conf.setSymbolResolver(symbolSolver);

        // Parse the full compilation unit
        Antlr4MvelParser parser = new Antlr4MvelParser(conf);
        ParseResult<CompilationUnit> parseResult = parser.parse(code);
        assertThat(parseResult.isSuccessful()).isTrue();
        CompilationUnit cu = parseResult.getResult().get();

        // Create minimal CompilerParameters for the rewriter context.
        // withDeclaration is VOID so bare NameExpr rewriting (evaluator-specific) is skipped.
        CompilerParameters<Map<String, Object>, Void, Object> params = new CompilerParameters<>(
                ContextType.MAP,
                ClassLoader.getSystemClassLoader(),
                new ClassManager(),
                Set.of(Person.class.getCanonicalName(), Address.class.getCanonicalName()),
                Collections.emptySet(),
                Type.OBJECT,
                Declaration.of("__context", Map.class),
                Collections.emptyList(),
                MVELBuilder.VOID_DECLARATION,
                ContentType.BLOCK,
                "",
                "Generated__",
                "eval",
                null
        );

        TranspilerContext<Map<String, Object>, Void, Object> context = new TranspilerContext<>(parser, typeSolver, params);
        context.setUnit(cu);

        // Set classDeclaration from the parsed class
        ClassOrInterfaceDeclaration classDecl = cu.findFirst(ClassOrInterfaceDeclaration.class).orElseThrow();
        context.setClassDeclaration(classDecl);

        // Inject symbol resolver so AST nodes can resolve types
        symbolSolver.inject(cu);

        // Rewrite each method body
        MVELToJavaRewriter rewriter = new MVELToJavaRewriter(context);
        for (MethodDeclaration method : classDecl.getMethods()) {
            method.getBody().ifPresent(rewriter::rewriteChildren);
        }

        return PrintUtil.printNode(cu);
    }
}
