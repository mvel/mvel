package org.mvel3.lambdaextractor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lambda Registry facade — transitional during the refactor. Dedup state lives
 * in {@link LambdaCatalog}; artifact map lives in {@link LambdaPersistenceManager};
 * byte-level I/O is in {@link LambdaArtifactStore}. The whole facade is deleted
 * in Phase 6 when {@link LambdaRuntime} becomes the real composition root.
 */
public enum LambdaRegistry {

    INSTANCE;

    private static final Logger LOG = LoggerFactory.getLogger(LambdaRegistry.class);

    public static final boolean PERSISTENCE_ENABLED = Boolean.parseBoolean(System.getProperty("mvel3.compiler.lambda.persistence", "true"));
    public static final Path DEFAULT_PERSISTENCE_PATH = Path.of(System.getProperty("mvel3.compiler.lambda.persistence.path", "target/generated-classes/mvel"));
    private static final Path REGISTRY_FILE = Path.of(System.getProperty("mvel3.compiler.lambda.registry.file",
                                                                         DEFAULT_PERSISTENCE_PATH.resolve("lambda-registry.dat").toString()));

    static {
        // Field init must run after the static path fields are set. Enum constants are
        // built before static fields, so we wire collaborators here in the static block.
        INSTANCE.artifactStore = new LambdaArtifactStore(DEFAULT_PERSISTENCE_PATH);
        INSTANCE.persistenceManager = new LambdaPersistenceManager(INSTANCE.artifactStore, INSTANCE.stubRuntime);

        if (PERSISTENCE_ENABLED) {
            INSTANCE.loadFromDisk();
        }
        if (Boolean.getBoolean("mvel3.compiler.lambda.resetOnTestStartup")) {
            INSTANCE.resetAndRemoveAllPersistedFiles();
        }
    }

    private final LambdaCatalog catalog = new LambdaCatalog();
    private final LambdaRuntime stubRuntime = new LambdaRuntime();      // Phase 6 replaces with real runtime
    private LambdaArtifactStore artifactStore;                          // set in static init
    private LambdaPersistenceManager persistenceManager;                // set in static init

    LambdaRegistry() {
        // singleton
    }

    public LambdaCatalog catalog() {
        return catalog;
    }

    public LambdaPersistenceManager persistenceManager() {
        return persistenceManager;
    }

    public int getNextLogicalId() {
        // Catalog allocates logicalId inside register(); this is informational only now.
        return -1;
    }

    public int registerLambda(int ignoredLogicalId, LambdaKey key) {
        return catalog.register(key).physicalId();
    }

    public int getPhysicalId(int logicalId) {
        Integer p = catalog.physicalForLogical(logicalId);
        return p == null ? -1 : p;
    }

    public void registerPhysicalPath(int physicalId, String fqn, Path path) {
        persistenceManager.attachArtifact(physicalId, new ArtifactRef(fqn, path));
        if (PERSISTENCE_ENABLED) {
            persistToDisk();
        }
    }

    public Path getPhysicalPath(int physicalId) {
        return persistenceManager.artifactFor(physicalId).map(ArtifactRef::classFile).orElse(null);
    }

    public boolean isPersisted(int physicalId) {
        return persistenceManager.artifactExists(physicalId);
    }

    /**
     * Clear all in-memory registry state and remove any persisted class files and registry file.
     * Intended for use in test setup to ensure a clean slate.
     */
    public synchronized void resetAndRemoveAllPersistedFiles() {
        LOG.info("Clean up Lambda Registry and persisted files at {}", DEFAULT_PERSISTENCE_PATH);
        try {
            artifactStore.deleteAll();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        catalog.clear();
        persistenceManager.clear();
        try {
            Files.deleteIfExists(REGISTRY_FILE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadFromDisk() {
        if (!Files.exists(REGISTRY_FILE)) {
            return;
        }
        try {
            LambdaPersistenceSnapshot snapshot = new LambdaRegistryStore(REGISTRY_FILE).load();
            catalog.applySnapshot(snapshot.catalog());
            persistenceManager.applyArtifacts(snapshot.artifacts());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load lambda registry from " + REGISTRY_FILE, e);
        }
    }

    private void persistToDisk() {
        try {
            Files.createDirectories(REGISTRY_FILE.getParent());
            new LambdaRegistryStore(REGISTRY_FILE).save(
                    new LambdaPersistenceSnapshot(catalog.toSnapshot(), persistenceManager.snapshot()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist lambda registry to " + REGISTRY_FILE, e);
        }
    }
}
