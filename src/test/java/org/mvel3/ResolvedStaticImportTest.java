package org.mvel3;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.resolution.Solver;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.Test;
import org.mvel3.EvaluatorBuilder.ContextInfoBuilder;
import org.mvel3.EvaluatorBuilder.EvaluatorInfo;
import org.mvel3.parser.MvelParser;
import org.mvel3.parser.printer.MVELToJavaRewriter;
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

public class ResolvedStaticImportTest {

    public <T extends Map, K, R> EvaluatorInfo<T, K, R> compileMapEvaluator(final String content, final Class<R> outClass, final Set<String> imports, Set<String> staticImports, final Map<String, Type<?>> types) {

        EvaluatorBuilder<T, K, R> eval = new EvaluatorBuilder<>();
        eval.setClassManager(new TestClassManager()).setClassLoader(ClassLoader.getSystemClassLoader())
            .setExpression(content)
            .setImports(imports)
            .setStaticImports(staticImports)
            .setVariableInfo(ContextInfoBuilder.create(Type.type(Map.class, "<String, Object>"))
                                               .setVars(Declaration.from(types)))
            .setOutType(Type.type(outClass));

        eval.setGeneratedSuperName(GeneratedParentClass.class.getCanonicalName());

        return eval.build();
    }

    @Test
    public void testResolveMethod() {
        Map<String, Type<?>> types = new HashMap<>();
        types.put("foo", Type.type(Foo.class));
        types.put("bar", Type.type(Bar.class));

        Set<String> staticImports = new HashSet<>();
        staticImports.add(Person.class.getCanonicalName() + ".isEven");
        EvaluatorInfo<Map<String, Object>, Void, String> eval = compileMapEvaluator("foo.getName() + bar.getName() + isEven(1)", String.class, getImports(), staticImports, types);

        TranspilerContext<Map<String, Object>, Void, String> context          = buildTranspilerContext(eval);
        ClassOrInterfaceDeclaration classDeclaration = context.getUnit().addClass(context.getEvaluatorInfo().generatedClassName());
        context.setClassDeclaration(classDeclaration);

        Map<String, Set<ResolvedMethodDeclaration>>          map              = context.getResolvedStaticMethods();
        Set<ResolvedMethodDeclaration> isEventSet = map.get("isEven");
        assertThat(isEventSet).isNotNull();
        assertThat(isEventSet).size().isEqualTo(2);
    }

    @Test
    public void testStaticImportMethodCall() {
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
        EvaluatorInfo<Map<String, Object>, Void, String> eval = compileMapEvaluator("foo.getName() + bar.getName() + isEven(1)", String.class, getImports(), staticImports, types);

        MVELCompiler compiler= new MVELCompiler();
        Evaluator<Map<String, Object>, Void, String> evaluator = compiler.compile(eval);
        assertThat(evaluator.eval(vars)).isEqualTo("xxxyyytrue");
    }

    @Test
    public void testMemberInstanceMethodCall() {
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
        EvaluatorInfo<Map<String, Object>, Void, String> eval = compileMapEvaluator("foo.getName() + bar.getName() + instanceMethod(1)", String.class, getImports(), staticImports, types);

        MVELCompiler compiler= new MVELCompiler();
        Evaluator<Map<String, Object>, Void, String> evaluator = compiler.compile(eval);
        assertThat(evaluator.eval(vars)).isEqualTo("xxxyyyinstance_int1");
    }


    @Test
    public void testMemberStaticMethodCall() {
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
        EvaluatorInfo<Map<String, Object>, Void, String> eval = compileMapEvaluator("foo.getName() + bar.getName() + staticMethod(1)", String.class, getImports(), staticImports, types);

        MVELCompiler compiler= new MVELCompiler();
        Evaluator<Map<String, Object>, Void, String> evaluator = compiler.compile(eval);
        assertThat(evaluator.eval(vars)).isEqualTo("xxxyyystatic_int1");
    }
    @Test
    public void testMemberMethodCallDoesNotExist() {
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
        EvaluatorInfo<Map<String, Object>, Void, String> eval = compileMapEvaluator("foo.getName() + bar.getName() + doesNotExist(1)", String.class, getImports(), staticImports, types);
        MVELCompiler compiler= new MVELCompiler();

        assertThatException().isThrownBy(() -> {
            Evaluator<Map<String, Object>, Void, String> evaluator = compiler.compile(eval);
        });
    }

    @Test
    public void testMemberMethod() {
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
        EvaluatorInfo<Map<String, Object>, Void, String> eval = compileMapEvaluator("foo.getName() + bar.getName() + isEven(1)", String.class, getImports(), staticImports, types);

        MVELCompiler compiler= new MVELCompiler();
        Evaluator<Map<String, Object>, Void, String> evaluator = compiler.compile(eval);
        assertThat(evaluator.eval(vars)).isEqualTo("xxxyyytrue");
    }

    public static <T, K, R> TranspilerContext buildTranspilerContext(EvaluatorInfo<T, K, R> evalInfo) {
        TypeSolver typeSolver = new ReflectionTypeSolver(false);
        JavaSymbolSolver   solver     = new JavaSymbolSolver(typeSolver);

        ParserConfiguration conf = new ParserConfiguration();
        conf.setLanguageLevel(JAVA_15);
        conf.setSymbolResolver(solver);

        MvelParser parser = new MvelParser(conf);

        TranspilerContext context = new TranspilerContext(parser, typeSolver, evalInfo);
        context.setUnit(new CompilationUnit("org.dummy"));
        return context;
    }
}
