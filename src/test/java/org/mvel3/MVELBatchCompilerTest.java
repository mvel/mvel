package org.mvel3;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mvel3.parser.MvelParser;
import org.mvel3.transpiler.context.Declaration;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MVELBatchCompilerTest {

    @BeforeAll
    static void enableAntlrParser() {
        MvelParser.Factory.USE_ANTLR = true;
    }

    @Test
    void testBatchCompileSingleExpression() {
        ClassManager classManager = new ClassManager();
        MVELBatchCompiler batchCompiler = new MVELBatchCompiler(classManager);

        Map<String, Type<?>> types = new HashMap<>();
        types.put("foo", Type.type(Foo.class));
        types.put("bar", Type.type(Bar.class));

        CompilerParameters<Map<String, Object>, Void, String> params = MVEL.<Object>map(Declaration.from(types))
                .<String>out(String.class)
                .expression("foo.getName() + bar.getName()")
                .imports(MVELCompilerTest.getImports())
                .classManager(classManager)
                .build();

        MVELBatchCompiler.LambdaHandle handle = batchCompiler.add(params);
        batchCompiler.compile(ClassLoader.getSystemClassLoader());

        Evaluator<Map<String, Object>, Void, String> evaluator = batchCompiler.resolve(handle);

        Map<String, Object> vars = new HashMap<>();
        Foo foo = new Foo();
        foo.setName("xxx");
        vars.put("foo", foo);
        Bar bar = new Bar();
        bar.setName("yyy");
        vars.put("bar", bar);

        assertThat(evaluator.eval(vars)).isEqualTo("xxxyyy");
    }

    @Test
    void testBatchCompileMultipleExpressions() {
        ClassManager classManager = new ClassManager();
        MVELBatchCompiler batchCompiler = new MVELBatchCompiler(classManager);

        Map<String, Type<?>> types1 = new HashMap<>();
        types1.put("a", Type.type(int.class));
        types1.put("b", Type.type(int.class));

        CompilerParameters<Map<String, Integer>, Void, Integer> params1 = MVEL.<Integer>map(Declaration.from(types1))
                .<Integer>out(Integer.class)
                .expression("a + b")
                .imports(MVELCompilerTest.getImports())
                .classManager(classManager)
                .build();

        Map<String, Type<?>> types2 = new HashMap<>();
        types2.put("x", Type.type(int.class));
        types2.put("y", Type.type(int.class));

        CompilerParameters<Map<String, Integer>, Void, Integer> params2 = MVEL.<Integer>map(Declaration.from(types2))
                .<Integer>out(Integer.class)
                .expression("x * y")
                .imports(MVELCompilerTest.getImports())
                .classManager(classManager)
                .build();

        MVELBatchCompiler.LambdaHandle handle1 = batchCompiler.add(params1);
        MVELBatchCompiler.LambdaHandle handle2 = batchCompiler.add(params2);

        batchCompiler.compile(ClassLoader.getSystemClassLoader());

        Evaluator<Map<String, Integer>, Void, Integer> evaluator1 = batchCompiler.resolve(handle1);
        Evaluator<Map<String, Integer>, Void, Integer> evaluator2 = batchCompiler.resolve(handle2);

        Map<String, Integer> vars1 = new HashMap<>();
        vars1.put("a", 3);
        vars1.put("b", 4);
        assertThat(evaluator1.eval(vars1)).isEqualTo(7);

        Map<String, Integer> vars2 = new HashMap<>();
        vars2.put("x", 5);
        vars2.put("y", 6);
        assertThat(evaluator2.eval(vars2)).isEqualTo(30);
    }

    @Test
    void testBatchCompileDeduplication() {
        ClassManager classManager = new ClassManager();
        MVELBatchCompiler batchCompiler = new MVELBatchCompiler(classManager);

        Map<String, Type<?>> types = new HashMap<>();
        types.put("a", Type.type(int.class));
        types.put("b", Type.type(int.class));

        CompilerParameters<Map<String, Integer>, Void, Integer> params1 = MVEL.<Integer>map(Declaration.from(types))
                .<Integer>out(Integer.class)
                .expression("a + b")
                .imports(MVELCompilerTest.getImports())
                .classManager(classManager)
                .build();

        CompilerParameters<Map<String, Integer>, Void, Integer> params2 = MVEL.<Integer>map(Declaration.from(types))
                .<Integer>out(Integer.class)
                .expression("a + b")
                .imports(MVELCompilerTest.getImports())
                .classManager(classManager)
                .build();

        MVELBatchCompiler.LambdaHandle handle1 = batchCompiler.add(params1);
        MVELBatchCompiler.LambdaHandle handle2 = batchCompiler.add(params2);

        batchCompiler.compile(ClassLoader.getSystemClassLoader());

        // Both handles should resolve to working evaluators
        Evaluator<Map<String, Integer>, Void, Integer> evaluator1 = batchCompiler.resolve(handle1);
        Evaluator<Map<String, Integer>, Void, Integer> evaluator2 = batchCompiler.resolve(handle2);

        Map<String, Integer> vars = new HashMap<>();
        vars.put("a", 10);
        vars.put("b", 20);
        assertThat(evaluator1.eval(vars)).isEqualTo(30);
        assertThat(evaluator2.eval(vars)).isEqualTo(30);

        // Deduplication: both should share the same FQN
        assertThat(batchCompiler.getFqn(handle1)).isEqualTo(batchCompiler.getFqn(handle2));
        assertThat(batchCompiler.getPhysicalId(handle1)).isEqualTo(batchCompiler.getPhysicalId(handle2));
    }

    @Test
    void testResolveBeforeCompileThrows() {
        ClassManager classManager = new ClassManager();
        MVELBatchCompiler batchCompiler = new MVELBatchCompiler(classManager);

        Map<String, Type<?>> types = new HashMap<>();
        types.put("a", Type.type(int.class));

        CompilerParameters<Map<String, Integer>, Void, Integer> params = MVEL.<Integer>map(Declaration.from(types))
                .<Integer>out(Integer.class)
                .expression("a")
                .imports(MVELCompilerTest.getImports())
                .classManager(classManager)
                .build();

        MVELBatchCompiler.LambdaHandle handle = batchCompiler.add(params);

        assertThatThrownBy(() -> batchCompiler.resolve(handle))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("compile() not called yet");
    }

    @Test
    void testBatchCompileWithBlockExpression() {
        ClassManager classManager = new ClassManager();
        MVELBatchCompiler batchCompiler = new MVELBatchCompiler(classManager);

        Map<String, Type<?>> types = new HashMap<>();
        types.put("a", Type.type(int.class));
        types.put("b", Type.type(int.class));

        CompilerParameters<Map<String, Integer>, Void, Integer> params = MVEL.<Integer>map(Declaration.from(types))
                .<Integer>out(Integer.class)
                .block("int c = a + b; return c * 2;")
                .imports(MVELCompilerTest.getImports())
                .classManager(classManager)
                .build();

        MVELBatchCompiler.LambdaHandle handle = batchCompiler.add(params);
        batchCompiler.compile(ClassLoader.getSystemClassLoader());

        Evaluator<Map<String, Integer>, Void, Integer> evaluator = batchCompiler.resolve(handle);

        Map<String, Integer> vars = new HashMap<>();
        vars.put("a", 3);
        vars.put("b", 4);
        assertThat(evaluator.eval(vars)).isEqualTo(14);
    }

    @Test
    void testBatchCompileEmptyBatch() {
        ClassManager classManager = new ClassManager();
        MVELBatchCompiler batchCompiler = new MVELBatchCompiler(classManager);

        // Compiling with no lambdas added should not throw
        batchCompiler.compile(ClassLoader.getSystemClassLoader());
    }

    @Test
    void testGetClassManager() {
        ClassManager classManager = new ClassManager();
        MVELBatchCompiler batchCompiler = new MVELBatchCompiler(classManager);

        assertThat(batchCompiler.getClassManager()).isSameAs(classManager);
    }
}
