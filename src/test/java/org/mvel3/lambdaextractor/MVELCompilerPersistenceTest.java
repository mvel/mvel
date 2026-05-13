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
        LambdaRuntime.getInstance().resetAndRemoveAllPersistedFiles();
        MVELCompiler.resetCompileInvocationCountForTests();
    }

    @AfterEach
    void teardown() {
        LambdaRuntime.getInstance().resetAndRemoveAllPersistedFiles();
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
        Path registryFile = LambdaRuntime.defaultPersistencePath().resolve("lambda-registry.dat");
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
        org.mvel3.MVELBatchCompiler batch = new org.mvel3.MVELBatchCompiler(batchCm, LambdaRuntime.defaultPersistencePath());

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

    @Test
    void M12_batchCompiler_noPersist_doesNotTouchCatalog() {
        // No-persist batch must keep all state local to the instance and not
        // touch LambdaRuntime.getInstance().catalog() (no register() calls).
        ClassManager batchCm = new ClassManager();
        org.mvel3.MVELBatchCompiler batch = new org.mvel3.MVELBatchCompiler(batchCm); // persistenceDir == null

        CompilerParameters<MyPerson, Void, Boolean> info1 = MVEL.<MyPerson>pojo(MyPerson.class,
                        Declaration.of("age", int.class))
                .<Boolean>out(Boolean.class)
                .expression("age > 20")
                .imports(Set.of()).classManager(batchCm)
                .build();
        CompilerParameters<MyPerson, Void, Boolean> info2 = MVEL.<MyPerson>pojo(MyPerson.class,
                        Declaration.of("age", int.class))
                .<Boolean>out(Boolean.class)
                .expression("age < 10")
                .imports(Set.of()).classManager(batchCm)
                .build();
        batch.add(info1);
        batch.add(info2);
        batch.compile(Thread.currentThread().getContextClassLoader());

        // Probe the catalog after the batch: if the batch leaked any
        // catalog.register(...) calls, the next probe registration would
        // receive a non-zero logicalId.
        LambdaKey probe = LambdaUtils.createLambdaKeyFromMethodDeclarationString(
                "public boolean test(int x) { return x > 0; }");
        RegistrationResult result = LambdaRuntime.getInstance().catalog().register(probe);
        assertThat(result.logicalId()).isZero();
    }

    @Test
    void M13_batchCompiler_noPersist_ignoresPreviouslyPersistedArtifact() {
        // Seed: compile lambda X via the single-compiler path with persistence
        // enabled, so the artifact is now persisted and known to the global
        // persistence manager.
        CompilerParameters<MyPerson, Void, Boolean> seed = MVEL.<MyPerson>pojo(MyPerson.class,
                        Declaration.of("age", int.class))
                .<Boolean>out(Boolean.class)
                .expression("age > 20")
                .imports(Set.of()).classManager(new ClassManager())
                .build();
        new MVEL().compilePojoEvaluator(seed);

        int afterSeed = MVELCompiler.compileInvocationCount();

        // Now construct an explicit no-persist batch and ask it to compile the
        // same lambda. It must compile fresh (i.e., not load from disk via the
        // global persistenceManager().artifactExists() shortcut).
        ClassManager batchCm = new ClassManager();
        org.mvel3.MVELBatchCompiler batch = new org.mvel3.MVELBatchCompiler(batchCm); // persistenceDir == null
        CompilerParameters<MyPerson, Void, Boolean> info = MVEL.<MyPerson>pojo(MyPerson.class,
                        Declaration.of("age", int.class))
                .<Boolean>out(Boolean.class)
                .expression("age > 20")
                .imports(Set.of()).classManager(batchCm)
                .build();
        batch.add(info);
        batch.compile(Thread.currentThread().getContextClassLoader());

        assertThat(MVELCompiler.compileInvocationCount()).isEqualTo(afterSeed + 1);
    }

    @Test
    void M11_runtime_persistenceDisabled_noFileWrites(@org.junit.jupiter.api.io.TempDir Path tmp) {
        String prevEnabled = System.getProperty("mvel3.compiler.lambda.persistence");
        String prevPath = System.getProperty("mvel3.compiler.lambda.persistence.path");
        String prevReg = System.getProperty("mvel3.compiler.lambda.registry.file");
        System.setProperty("mvel3.compiler.lambda.persistence", "false");
        System.setProperty("mvel3.compiler.lambda.persistence.path", tmp.toString());
        System.setProperty("mvel3.compiler.lambda.registry.file", tmp.resolve("lambda-registry.dat").toString());
        LambdaRuntime.resetSingletonForTests();

        try {
            CompilerParameters<MyPerson, Void, Boolean> info = MVEL.<MyPerson>pojo(MyPerson.class,
                            Declaration.of("age", int.class))
                    .<Boolean>out(Boolean.class)
                    .expression("age > 20")
                    .imports(Set.of()).classManager(new ClassManager())
                    .build();
            new MVEL().compilePojoEvaluator(info);

            assertThat(Files.exists(tmp.resolve("lambda-registry.dat"))).isFalse();
            LambdaRuntime rt = LambdaRuntime.getInstance();
            assertThat(rt.config().persistenceEnabled()).isFalse();
        } finally {
            restoreProp("mvel3.compiler.lambda.persistence", prevEnabled);
            restoreProp("mvel3.compiler.lambda.persistence.path", prevPath);
            restoreProp("mvel3.compiler.lambda.registry.file", prevReg);
            LambdaRuntime.resetSingletonForTests();
        }
    }

    private static void restoreProp(String key, String prev) {
        if (prev == null) System.clearProperty(key);
        else System.setProperty(key, prev);
    }

    public static class MyPerson {
        private final String name;
        private final int age;
        public MyPerson(String name, int age) { this.name = name; this.age = age; }
        public String getName() { return name; }
        public int getAge() { return age; }
    }
}
