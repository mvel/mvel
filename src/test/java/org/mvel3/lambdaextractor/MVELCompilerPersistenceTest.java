package org.mvel3.lambdaextractor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mvel3.ClassManager;
import org.mvel3.CompilerParameters;
import org.mvel3.MVEL;
import org.mvel3.MVELCompiler;
import org.mvel3.transpiler.context.Declaration;

import static org.assertj.core.api.Assertions.assertThat;

public class MVELCompilerPersistenceTest {

    @BeforeEach
    void cleanState() {
        LambdaRegistry.INSTANCE.resetAndRemoveAllPersistedFiles();
        MVELCompiler.resetCompileInvocationCountForTests();
    }

    @AfterEach
    void teardown() {
        LambdaRegistry.INSTANCE.resetAndRemoveAllPersistedFiles();
    }

    @Test
    void M8_compiler_freshLambda_persistsAndAttaches() {
        int before = MVELCompiler.compileInvocationCount();

        CompilerParameters<MyPerson, Void, Boolean> info = MVEL.<MyPerson>pojo(MyPerson.class,
                        Declaration.of("age", int.class))
                .<Boolean>out(Boolean.class)
                .expression("age > 20")
                .imports(Set.of()).classManager(new ClassManager())
                .build();
        MVEL mvel = new MVEL();
        mvel.compilePojoEvaluator(info);

        assertThat(MVELCompiler.compileInvocationCount()).isEqualTo(before + 1);
        Path registryFile = LambdaRegistry.DEFAULT_PERSISTENCE_PATH.resolve("lambda-registry.dat");
        assertThat(Files.exists(registryFile)).isTrue();
    }

    @Test
    void M9_compiler_knownLambda_reusesPersisted() {
        CompilerParameters<MyPerson, Void, Boolean> info1 = MVEL.<MyPerson>pojo(MyPerson.class,
                        Declaration.of("age", int.class))
                .<Boolean>out(Boolean.class)
                .expression("age > 20")
                .imports(Set.of()).classManager(new ClassManager())
                .build();
        new MVEL().compilePojoEvaluator(info1);

        int afterFirst = MVELCompiler.compileInvocationCount();

        CompilerParameters<MyPerson, Void, Boolean> info2 = MVEL.<MyPerson>pojo(MyPerson.class,
                        Declaration.of("age", int.class))
                .<Boolean>out(Boolean.class)
                .expression("age > 20")
                .imports(Set.of()).classManager(new ClassManager())
                .build();
        new MVEL().compilePojoEvaluator(info2);

        assertThat(MVELCompiler.compileInvocationCount()).isEqualTo(afterFirst);   // no recompile
    }

    @Test
    void M10_batchCompiler_mixed_freshAndKnown() {
        // Pre-seed: compile lambda A once via single-compiler path.
        CompilerParameters<MyPerson, Void, Boolean> infoSeed = MVEL.<MyPerson>pojo(MyPerson.class,
                        Declaration.of("age", int.class))
                .<Boolean>out(Boolean.class)
                .expression("age > 20")
                .imports(Set.of()).classManager(new ClassManager())
                .build();
        new MVEL().compilePojoEvaluator(infoSeed);

        int afterSeed = MVELCompiler.compileInvocationCount();

        // Batch: same lambda (known) + a new one. Use a fresh ClassManager so we
        // exercise the load-from-disk path for the known one.
        ClassManager batchCm = new ClassManager();
        org.mvel3.MVELBatchCompiler batch = new org.mvel3.MVELBatchCompiler(batchCm, LambdaRegistry.DEFAULT_PERSISTENCE_PATH);

        CompilerParameters<MyPerson, Void, Boolean> infoKnown = MVEL.<MyPerson>pojo(MyPerson.class,
                        Declaration.of("age", int.class))
                .<Boolean>out(Boolean.class)
                .expression("age > 20")
                .imports(Set.of()).classManager(batchCm)
                .build();
        CompilerParameters<MyPerson, Void, Boolean> infoNew = MVEL.<MyPerson>pojo(MyPerson.class,
                        Declaration.of("age", int.class))
                .<Boolean>out(Boolean.class)
                .expression("age < 10")
                .imports(Set.of()).classManager(batchCm)
                .build();
        batch.add(infoKnown);
        batch.add(infoNew);
        batch.compile(Thread.currentThread().getContextClassLoader());

        // Exactly one bulk compileAndPersist invocation (for the new lambda).
        assertThat(MVELCompiler.compileInvocationCount()).isEqualTo(afterSeed + 1);
    }

    // M11 is added in Phase 6a once LambdaRuntime.resetSingletonForTests() exists.

    public static class MyPerson {
        private final String name;
        private final int age;
        public MyPerson(String name, int age) { this.name = name; this.age = age; }
        public String getName() { return name; }
        public int getAge() { return age; }
    }
}
