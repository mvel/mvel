package org.mvel3.lambdaextractor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class LambdaRuntimeTest {

    @BeforeEach
    void resetSingleton() {
        LambdaRuntime.resetSingletonForTests();
    }

    @Test
    void M12_runtime_resetAndRemoveAll_cleansEverything(@TempDir Path tmp) throws IOException {
        String prevPath = System.getProperty("mvel3.compiler.lambda.persistence.path");
        String prevReg = System.getProperty("mvel3.compiler.lambda.registry.file");
        String prevReset = System.getProperty("mvel3.compiler.lambda.resetOnTestStartup");
        System.setProperty("mvel3.compiler.lambda.persistence.path", tmp.toString());
        System.setProperty("mvel3.compiler.lambda.registry.file",
                tmp.resolve("lambda-registry.dat").toString());
        // Override the global test-time reset so getInstance() doesn't wipe our @TempDir during init.
        System.setProperty("mvel3.compiler.lambda.resetOnTestStartup", "false");
        LambdaRuntime.resetSingletonForTests();

        try {
            LambdaRuntime rt = LambdaRuntime.getInstance();
            Path dummyClass = tmp.resolve("Dummy.class");
            Files.writeString(dummyClass, "x");
            rt.persistenceManager().attachArtifact(0, new ArtifactRef("X.Y", dummyClass));

            Path registryFile = tmp.resolve("lambda-registry.dat");
            assertThat(Files.exists(registryFile)).isTrue();
            assertThat(Files.exists(dummyClass)).isTrue();

            rt.resetAndRemoveAllPersistedFiles();

            assertThat(Files.exists(registryFile)).isFalse();
            assertThat(Files.exists(dummyClass)).isFalse();
            assertThat(rt.persistenceManager().artifactFor(0)).isEmpty();
        } finally {
            restoreProp("mvel3.compiler.lambda.persistence.path", prevPath);
            restoreProp("mvel3.compiler.lambda.registry.file", prevReg);
            restoreProp("mvel3.compiler.lambda.resetOnTestStartup", prevReset);
            LambdaRuntime.resetSingletonForTests();
        }
    }

    @Test
    void M13_runtime_lazyInit_loadsExistingFile(@TempDir Path tmp) throws IOException {
        String prevPath = System.getProperty("mvel3.compiler.lambda.persistence.path");
        String prevReg = System.getProperty("mvel3.compiler.lambda.registry.file");
        String prevReset = System.getProperty("mvel3.compiler.lambda.resetOnTestStartup");
        System.setProperty("mvel3.compiler.lambda.persistence.path", tmp.toString());
        System.setProperty("mvel3.compiler.lambda.registry.file",
                tmp.resolve("lambda-registry.dat").toString());
        System.setProperty("mvel3.compiler.lambda.resetOnTestStartup", "false");
        LambdaRuntime.resetSingletonForTests();

        try {
            // Pre-write a valid v2 registry file.
            LambdaRegistryStore store = new LambdaRegistryStore(tmp.resolve("lambda-registry.dat"));
            CatalogSnapshot catalog = new CatalogSnapshot(1, 1, List.of(
                    new CatalogEntry(0, "public boolean eval(java.lang.Object o)", "{ return o != null; }")));
            Map<Integer, ArtifactRef> artifacts = Map.of(0, new ArtifactRef("org.example.Gen", tmp.resolve("Gen.class")));
            store.save(new LambdaPersistenceSnapshot(catalog, artifacts));

            LambdaRuntime rt = LambdaRuntime.getInstance();

            assertThat(rt.persistenceManager().artifactFor(0))
                    .isPresent()
                    .hasValueSatisfying(ref -> assertThat(ref.fqn()).isEqualTo("org.example.Gen"));
        } finally {
            restoreProp("mvel3.compiler.lambda.persistence.path", prevPath);
            restoreProp("mvel3.compiler.lambda.registry.file", prevReg);
            restoreProp("mvel3.compiler.lambda.resetOnTestStartup", prevReset);
            LambdaRuntime.resetSingletonForTests();
        }
    }

    private static void restoreProp(String key, String prev) {
        if (prev == null) System.clearProperty(key);
        else System.setProperty(key, prev);
    }
}
