package org.mvel3;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mvel3.javacompiler.KieMemoryCompiler;
import org.mvel3.transpiler.context.Declaration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demonstrates batch compilation using {@link MVELCompiler#transpileToSource}
 * and {@link MVELCompiler#resolveEvaluator}.
 *
 * <p>Instead of compiling each expression individually (one javac invocation per expression),
 * batch compilation transpiles all expressions to Java source first, then compiles them
 * all in a single javac call via {@link KieMemoryCompiler#compile}.
 */
class BatchCompilationTest {

    @Test
    void testBatchCompilePojoExpressions() {
        MVELCompiler compiler = new MVELCompiler();
        ClassManager classManager = new ClassManager();

        // Transpile three POJO expressions to Java source without compiling
        CompilerParameters<Object, Void, Boolean> ageCheck = MVEL.pojo(Person.class,
                        Declaration.of("age", int.class),
                        Declaration.of("name", String.class))
                .<Boolean>out(Boolean.class)
                .expression("age > 18")
                .classManager(classManager)
                .generatedClassName("BatchEval__0")
                .build();
        MVELCompiler.TranspiledSource src0 = compiler.transpileToSource(ageCheck);

        CompilerParameters<Object, Void, Boolean> nameCheck = MVEL.pojo(Person.class,
                        Declaration.of("age", int.class),
                        Declaration.of("name", String.class))
                .<Boolean>out(Boolean.class)
                .expression("name == \"Alice\"")
                .classManager(classManager)
                .generatedClassName("BatchEval__1")
                .build();
        MVELCompiler.TranspiledSource src1 = compiler.transpileToSource(nameCheck);

        CompilerParameters<Object, Void, Boolean> combinedCheck = MVEL.pojo(Person.class,
                        Declaration.of("age", int.class),
                        Declaration.of("name", String.class))
                .<Boolean>out(Boolean.class)
                .expression("age >= 21 && name != \"Bob\"")
                .classManager(classManager)
                .generatedClassName("BatchEval__2")
                .build();
        MVELCompiler.TranspiledSource src2 = compiler.transpileToSource(combinedCheck);

        // Verify sources were generated
        assertThat(src0.javaSource()).contains("age > 18");
        assertThat(src1.javaSource()).contains("Alice");
        assertThat(src2.javaSource()).contains("age >= 21");

        // Batch compile all three in a single javac invocation
        Map<String, String> sources = new HashMap<>();
        sources.put(src0.fqn(), src0.javaSource());
        sources.put(src1.fqn(), src1.javaSource());
        sources.put(src2.fqn(), src2.javaSource());

        KieMemoryCompiler.compile(classManager, sources, Thread.currentThread().getContextClassLoader());

        // Resolve evaluators from the shared ClassManager
        Evaluator<Object, Void, Boolean> ageEval = MVELCompiler.resolveEvaluator(classManager, src0.fqn());
        Evaluator<Object, Void, Boolean> nameEval = MVELCompiler.resolveEvaluator(classManager, src1.fqn());
        Evaluator<Object, Void, Boolean> combinedEval = MVELCompiler.resolveEvaluator(classManager, src2.fqn());

        // Verify evaluators work correctly
        Person alice = new Person("Alice");
        alice.setAge(30);
        Person bob = new Person("Bob");
        bob.setAge(15);

        assertThat(ageEval.eval(alice)).isTrue();      // age 30 > 18
        assertThat(ageEval.eval(bob)).isFalse();        // age 15 > 18

        assertThat(nameEval.eval(alice)).isTrue();      // name == "Alice"
        assertThat(nameEval.eval(bob)).isFalse();       // name != "Alice"

        assertThat(combinedEval.eval(alice)).isTrue();   // age 30 >= 21 && name != "Bob"
        assertThat(combinedEval.eval(bob)).isFalse();    // age 15 < 21
    }
}
