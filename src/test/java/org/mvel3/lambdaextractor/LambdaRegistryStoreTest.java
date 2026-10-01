package org.mvel3.lambdaextractor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LambdaRegistryStoreTest {

    @Test
    void M5_registryStore_writeReadRoundTrip(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("lambda-registry.dat");

        CatalogSnapshot catalog = new CatalogSnapshot(2, 3, List.of(
                new CatalogEntry(0, "public boolean eval(java.lang.Object obj)", "{ return obj != null; }"),
                new CatalogEntry(1, "public boolean eval(java.lang.String s)", "{ return s.length() > 0; }")
        ));
        Map<Integer, ArtifactRef> artifacts = Map.of(
                0, new ArtifactRef("org.mvel3.GenA", tmp.resolve("GenA.class")),
                1, new ArtifactRef("org.mvel3.GenB", tmp.resolve("GenB.class"))
        );
        LambdaPersistenceSnapshot snapshot = new LambdaPersistenceSnapshot(catalog, artifacts);

        LambdaRegistryStore store = new LambdaRegistryStore(file);
        store.save(snapshot);
        LambdaPersistenceSnapshot loaded = store.load();

        assertThat(loaded.catalog().nextPhysicalId()).isEqualTo(2);
        assertThat(loaded.catalog().nextLogicalId()).isEqualTo(3);
        assertThat(loaded.catalog().entries()).hasSize(2);
        assertThat(loaded.artifacts()).isEqualTo(artifacts);
    }

    @Test
    void M5b_registryStore_subtypeReuse_writeReadRoundTrip(@TempDir Path tmp) throws IOException {
        final LambdaCatalog catalog = new LambdaCatalog();
        final LambdaKey supertype = LambdaUtils.createLambdaKeyFromMethodDeclarationString(
                "public boolean eval(%s o) { return o != null; }".formatted(Object.class.getCanonicalName()));
        final LambdaKey subtype = LambdaUtils.createLambdaKeyFromMethodDeclarationString(
                "public boolean eval(%s s) { return s != null; }".formatted(String.class.getCanonicalName()));

        final RegistrationResult supertypeResult = catalog.register(supertype);
        final RegistrationResult subtypeResult = catalog.register(subtype);
        assertThat(subtypeResult.reused()).isTrue();
        assertThat(subtypeResult.physicalId()).isEqualTo(supertypeResult.physicalId());

        final Map<Integer, ArtifactRef> artifacts = Map.of(supertypeResult.physicalId(),
                new ArtifactRef("org.mvel3.GenObject", tmp.resolve("GenObject.class")));
        final LambdaPersistenceSnapshot snapshot = new LambdaPersistenceSnapshot(catalog.toSnapshot(), artifacts);
        assertThat(snapshot.catalog().entries()).hasSize(2);

        final LambdaRegistryStore store = new LambdaRegistryStore(tmp.resolve("lambda-registry.dat"));
        store.save(snapshot);
        final LambdaPersistenceSnapshot loaded = store.load();

        assertThat(loaded.catalog().entries()).containsExactlyInAnyOrderElementsOf(snapshot.catalog().entries());
        assertThat(loaded.artifacts()).isEqualTo(artifacts);

        final LambdaCatalog restored = new LambdaCatalog();
        restored.applySnapshot(loaded.catalog());
        assertThat(restored.register(supertype).physicalId()).isEqualTo(supertypeResult.physicalId());
        assertThat(restored.register(subtype).physicalId()).isEqualTo(supertypeResult.physicalId());
    }

    @Test
    void M6_registryStore_unsupportedVersion_throws(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("lambda-registry.dat");
        Files.writeString(file, "format.version=1\ncatalog.nextPhysicalId=0\ncatalog.nextLogicalId=0\n");

        LambdaRegistryStore store = new LambdaRegistryStore(file);

        assertThatThrownBy(store::load)
                .isInstanceOf(InvalidLambdaRegistryException.class)
                .hasMessageContaining("Unsupported");
    }

    @Test
    void M7a_registryStore_duplicateArtifactPhysicalId_throws(@TempDir Path tmp) throws IOException {
        final Path file = tmp.resolve("lambda-registry.dat");
        Files.writeString(file, """
                format.version=2
                catalog.nextPhysicalId=1
                catalog.nextLogicalId=1
                catalog.entry.0.physicalId=0
                catalog.entry.0.methodSignature=public boolean eval(%s o)
                catalog.entry.0.normalizedBody={ return o != null; }
                artifact.0.physicalId=0
                artifact.0.fqn=org.mvel3.GenA
                artifact.0.classFile=GenA.class
                artifact.1.physicalId=0
                artifact.1.fqn=org.mvel3.GenB
                artifact.1.classFile=GenB.class
                """.formatted(Object.class.getCanonicalName()));
        assertThatThrownBy(() -> new LambdaRegistryStore(file).load())
                .isInstanceOf(InvalidLambdaRegistryException.class)
                .hasMessageContaining("Duplicate artifact physicalId");
    }

    @Test
    void M7b_registryStore_missingRequiredKey_throws(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("lambda-registry.dat");
        Files.writeString(file, "format.version=2\n");
        assertThatThrownBy(() -> new LambdaRegistryStore(file).load())
                .isInstanceOf(InvalidLambdaRegistryException.class)
                .hasMessageContaining("Missing key");
    }

    @Test
    void M7d_registryStore_invalidClassFilePath_throws(@TempDir Path tmp) throws IOException {
        // The \\u0000 in the Properties file decodes to a NUL byte, which Path.of()
        // rejects with the unchecked InvalidPathException on Unix file systems.
        // The store must convert that into the typed InvalidLambdaRegistryException
        // so callers can handle malformed metadata uniformly.
        Path file = tmp.resolve("lambda-registry.dat");
        Files.writeString(file, String.join("\n",
                "format.version=2",
                "catalog.nextPhysicalId=1",
                "catalog.nextLogicalId=1",
                "catalog.entry.0.physicalId=0",
                "catalog.entry.0.methodSignature=public boolean eval(java.lang.Object o)",
                "catalog.entry.0.normalizedBody={ return o != null; }",
                "artifact.0.physicalId=0",
                "artifact.0.fqn=org.mvel3.Gen",
                "artifact.0.classFile=foo\\u0000bar.class",
                ""));
        assertThatThrownBy(() -> new LambdaRegistryStore(file).load())
                .isInstanceOf(InvalidLambdaRegistryException.class)
                .hasMessageContaining("classFile");
    }

    @Test
    void M7c_registryStore_danglingArtifactReference_throws(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("lambda-registry.dat");
        Files.writeString(file, String.join("\n",
                "format.version=2",
                "catalog.nextPhysicalId=1",
                "catalog.nextLogicalId=1",
                "catalog.entry.0.physicalId=0",
                "catalog.entry.0.methodSignature=public boolean eval(java.lang.Object o)",
                "catalog.entry.0.normalizedBody={ return o != null; }",
                "artifact.0.physicalId=99",     // no matching catalog entry
                "artifact.0.fqn=org.mvel3.Gen",
                "artifact.0.classFile=/tmp/Gen.class",
                ""));
        assertThatThrownBy(() -> new LambdaRegistryStore(file).load())
                .isInstanceOf(InvalidLambdaRegistryException.class)
                .hasMessageContaining("no matching catalog entry");
    }
}
