package org.mvel3;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import org.mvel3.javacompiler.KieMemoryCompiler;
import org.mvel3.parser.antlr4.Antlr4MvelParser;
import org.mvel3.parser.printer.PrintUtil;
import org.mvel3.transpiler.MVELToJavaRewriter;
import org.mvel3.transpiler.context.Declaration;
import org.mvel3.transpiler.context.TranspilerContext;

import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_15;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the full compilation-unit pipeline: parse MVEL syntax → transpile to Java → compile → execute.
 * Unlike {@link CompilationUnitTranspilerTest} (which only checks the transpiled source),
 * this test compiles the generated Java code in-memory and executes it.
 *
 * Note that this test demonstrates compilation capability by low-level classes like KieMemoryCompiler.
 * We may enhance MVELCompiler to cover this full compilation-unit use case.
 */
class CompilationUnitCompilerTest {

    @Test
    void testGetName() {
        Class<?> clazz = compilePersonService();
        Object service = newInstance(clazz);

        Person p = new Person("Bob");
        assertThat(invoke(service, "getName", Person.class, p)).isEqualTo("Bob");
    }

    @Test
    void testUpdatePerson() {
        Class<?> clazz = compilePersonService();
        Object service = newInstance(clazz);

        Person p = new Person("Bob");
        p.setAge(25);
        invoke(service, "updatePerson", Person.class, p);
        assertThat(p.getName()).isEqualTo("Alice");
        assertThat(p.getAge()).isEqualTo(30);
    }

    @Test
    void testGetCity() {
        Class<?> clazz = compilePersonService();
        Object service = newInstance(clazz);

        Person p = new Person("Bob");
        Address addr = new Address();
        addr.setCity("NYC");
        p.setAddress(addr);
        assertThat(invoke(service, "getCity", Person.class, p)).isEqualTo("NYC");
    }

    @Test
    void testGetPublicField() {
        Class<?> clazz = compilePersonService();
        Object service = newInstance(clazz);

        Person p = new Person("Bob");
        p.nickName = "Bobby";
        assertThat(invoke(service, "getPublicField", Person.class, p)).isEqualTo("Bobby");
    }

    @Test
    void testProcessList() throws Exception {
        Class<?> clazz = compilePersonService();
        Object service = newInstance(clazz);

        Person p = new Person("Bob");
        Address a1 = new Address();
        a1.setCity("NYC");
        Address a2 = new Address();
        a2.setCity("LA");
        p.setAddresses(List.of(a1, a2));

        List<String> results = new ArrayList<>();
        Method method = clazz.getMethod("processList", Person.class, List.class);
        method.invoke(service, p, results);
        assertThat(results).containsExactly("NYC", "LA");
    }

    @Test
    void testInlineCast() {
        Class<?> clazz = compilePersonService();
        Object service = newInstance(clazz);

        List<?> list = new ArrayList<>(List.of("hello"));
        assertThat(invoke(service, "inlineCast", List.class, list)).isEqualTo("hello");
    }

    private Class<?> compilePersonService() {
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

        return compileCompilationUnit(input, "PersonService");
    }

    private Class<?> compileCompilationUnit(String code, String className) {
        String javaSource = transpileCompilationUnit(code);

        Map<String, byte[]> byteCode = KieMemoryCompiler.compileNoLoad(
                Map.of(className, javaSource),
                getClass().getClassLoader(),
                null);

        return defineClass(className, byteCode);
    }

    private String transpileCompilationUnit(String code) {
        TypeSolver typeSolver = new ReflectionTypeSolver(false);
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);

        ParserConfiguration conf = new ParserConfiguration();
        conf.setLanguageLevel(JAVA_15);
        conf.setSymbolResolver(symbolSolver);

        Antlr4MvelParser parser = new Antlr4MvelParser(conf);
        ParseResult<CompilationUnit> parseResult = parser.parse(code);
        assertThat(parseResult.isSuccessful()).isTrue();
        CompilationUnit cu = parseResult.getResult().get();

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

        ClassOrInterfaceDeclaration classDecl = cu.findFirst(ClassOrInterfaceDeclaration.class).orElseThrow();
        context.setClassDeclaration(classDecl);

        symbolSolver.inject(cu);

        MVELToJavaRewriter rewriter = new MVELToJavaRewriter(context);
        for (MethodDeclaration method : classDecl.getMethods()) {
            method.getBody().ifPresent(rewriter::rewriteChildren);
        }

        return PrintUtil.printNode(cu);
    }

    private Class<?> defineClass(String fqn, Map<String, byte[]> byteCode) {
        byte[] bytes = byteCode.get(fqn);
        assertThat(bytes).as("Bytecode for " + fqn).isNotNull();
        return new ClassLoader(getClass().getClassLoader()) {
            Class<?> define() {
                return defineClass(fqn, bytes, 0, bytes.length);
            }
        }.define();
    }

    private static Object newInstance(Class<?> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object invoke(Object target, String methodName, Class<?> paramType, Object arg) {
        try {
            Method method = target.getClass().getMethod(methodName, paramType);
            return method.invoke(target, arg);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
