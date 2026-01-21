package org.mvel3;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mvel3.parser.MvelParser;
import org.mvel3.test.TestClassManager;
import org.mvel3.transpiler.context.Declaration;
import org.mvel3.transpiler.context.TranspilerContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_15;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mvel3.MVELCompilerTest.getImports;

class ResolvedStaticImportTest {

    // To switch between JavaParser and ANTLR4 parsers. This will be removed once ANTLR4 is the only parser.
    @BeforeAll
    static void enableAntlrParser() {
        MvelParser.Factory.USE_ANTLR = true;
    }

    public <T, R> CompilerParameters<Map<String, T>, Void, R> compileMapEvaluator(final String content, final Class<R> outClass, final Set<String> imports, Set<String> staticImports, final Map<String, Type<?>> types) {

        MVELBuilder<Map<String, T>, Void, R> eval = MVEL.<T>map(Declaration.from(types))
            .<R>out(outClass)
            .expression(content)
            .classManager(new TestClassManager())
            .classLoader(ClassLoader.getSystemClassLoader())
            .imports(imports)
            .staticImports(staticImports);

        eval.generatedSuperName(GeneratedParentClass.class.getCanonicalName());

        return eval.build();
    }

    @Test
    void testResolveMethod() {
        Map<String, Type<?>> types = new HashMap<>();
        types.put("foo", Type.type(Foo.class));
        types.put("bar", Type.type(Bar.class));

        Set<String> staticImports = new HashSet<>();
        staticImports.add(Person.class.getCanonicalName() + ".isEven");
        CompilerParameters<Map<String, Object>, Void, String> eval = compileMapEvaluator("foo.getName() + bar.getName() + isEven(1)", String.class, getImports(), staticImports, types);

        TranspilerContext<Map<String, Object>, Void, String> context          = buildTranspilerContext(eval);
        ClassOrInterfaceDeclaration classDeclaration = context.getUnit().addClass(context.getEvaluatorInfo().generatedClassName());
        context.setClassDeclaration(classDeclaration);

        Map<String, Set<ResolvedMethodDeclaration>>          map              = context.getResolvedStaticMethods();
        Set<ResolvedMethodDeclaration> isEventSet = map.get("isEven");
        assertThat(isEventSet).isNotNull();
        assertThat(isEventSet).size().isEqualTo(2);
    }

    @Test
    void testStaticImportMethodCall() {
        Map<String, Type<?>> types = new HashMap<>();
        types.put("foo", Type.type(Foo.class));
        types.put("bar", Type.type(Bar.class));

        Map<String, Object> vars = new HashMap<>();
        Foo foo = new Foo();
        foo.setName("xxx");
        vars.put("foo", foo);

        Bar bar = new Bar();
        bar.setName("yyy");
        vars.put("bar", bar);

        Set<String> staticImports = new HashSet<>();
        staticImports.add(Person.class.getCanonicalName() + ".isEven");
        CompilerParameters<Map<String, Object>, Void, String> eval = compileMapEvaluator("foo.getName() + bar.getName() + isEven(1)", String.class, getImports(), staticImports, types);

        MVELCompiler compiler= new MVELCompiler();
        Evaluator<Map<String, Object>, Void, String> evaluator = compiler.compile(eval);
        assertThat(evaluator.eval(vars)).isEqualTo("xxxyyytrue");
    }

    @Test
    void testMemberInstanceMethodCall() {
        Map<String, Type<?>> types = new HashMap<>();
        types.put("foo", Type.type(Foo.class));
        types.put("bar", Type.type(Bar.class));

        Map<String, Object> vars = new HashMap<>();
        Foo foo = new Foo();
        foo.setName("xxx");
        vars.put("foo", foo);

        Bar bar = new Bar();
        bar.setName("yyy");
        vars.put("bar", bar);

        Set<String>                                           staticImports = new HashSet<>();
        CompilerParameters<Map<String, Object>, Void, String> eval          = compileMapEvaluator("foo.getName() + bar.getName() + instanceMethod(1)", String.class, getImports(), staticImports, types);

        MVELCompiler compiler= new MVELCompiler();
        Evaluator<Map<String, Object>, Void, String> evaluator = compiler.compile(eval);
        assertThat(evaluator.eval(vars)).isEqualTo("xxxyyyinstance_int1");
    }


    @Test
    void testMemberStaticMethodCall() {
        Map<String, Type<?>> types = new HashMap<>();
        types.put("foo", Type.type(Foo.class));
        types.put("bar", Type.type(Bar.class));

        Map<String, Object> vars = new HashMap<>();
        Foo foo = new Foo();
        foo.setName("xxx");
        vars.put("foo", foo);

        Bar bar = new Bar();
        bar.setName("yyy");
        vars.put("bar", bar);

        Set<String>                                           staticImports = new HashSet<>();
        CompilerParameters<Map<String, Object>, Void, String> eval          = compileMapEvaluator("foo.getName() + bar.getName() + staticMethod(1)", String.class, getImports(), staticImports, types);

        MVELCompiler compiler= new MVELCompiler();
        Evaluator<Map<String, Object>, Void, String> evaluator = compiler.compile(eval);
        assertThat(evaluator.eval(vars)).isEqualTo("xxxyyystatic_int1");
    }
    @Test
    void testMemberMethodCallDoesNotExist() {
        Map<String, Type<?>> types = new HashMap<>();
        types.put("foo", Type.type(Foo.class));
        types.put("bar", Type.type(Bar.class));

        Map<String, Object> vars = new HashMap<>();
        Foo foo = new Foo();
        foo.setName("xxx");
        vars.put("foo", foo);

        Bar bar = new Bar();
        bar.setName("yyy");
        vars.put("bar", bar);

        Set<String>                                           staticImports = new HashSet<>();
        CompilerParameters<Map<String, Object>, Void, String> eval          = compileMapEvaluator("foo.getName() + bar.getName() + doesNotExist(1)", String.class, getImports(), staticImports, types);
        MVELCompiler                                          compiler      = new MVELCompiler();

        assertThatException().isThrownBy(() -> {
            Evaluator<Map<String, Object>, Void, String> evaluator = compiler.compile(eval);
        });
    }

    @Test
    void testMemberMethod() {
        Map<String, Type<?>> types = new HashMap<>();
        types.put("foo", Type.type(Foo.class));
        types.put("bar", Type.type(Bar.class));

        Map<String, Object> vars = new HashMap<>();
        Foo foo = new Foo();
        foo.setName("xxx");
        vars.put("foo", foo);

        Bar bar = new Bar();
        bar.setName("yyy");
        vars.put("bar", bar);

        MVEL mvel = new MVEL();

        Set<String> staticImports = new HashSet<>();
        staticImports.add(Person.class.getCanonicalName() + ".isEven");
        CompilerParameters<Map<String, Object>, Void, String> eval = compileMapEvaluator("foo.getName() + bar.getName() + isEven(1)", String.class, getImports(), staticImports, types);

        MVELCompiler compiler= new MVELCompiler();
        Evaluator<Map<String, Object>, Void, String> evaluator = compiler.compile(eval);
        assertThat(evaluator.eval(vars)).isEqualTo("xxxyyytrue");
    }

    public static <T, K, R> TranspilerContext buildTranspilerContext(CompilerParameters<T, K, R> evalInfo) {
        TypeSolver typeSolver = new ReflectionTypeSolver(false);
        JavaSymbolSolver   solver     = new JavaSymbolSolver(typeSolver);

        ParserConfiguration conf = new ParserConfiguration();
        conf.setLanguageLevel(JAVA_15);
        conf.setSymbolResolver(solver);

        MvelParser parser = MvelParser.Factory.get(conf);

        TranspilerContext context = new TranspilerContext(parser, typeSolver, evalInfo);
        context.setUnit(new CompilationUnit("org.dummy"));
        return context;
    }
}
