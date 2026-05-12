package org.mvel3.lambdaextractor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lambda Registry facade — transitional during the refactor. Dedup and
 * ID-allocation state has moved to {@link LambdaCatalog}; this class still
 * owns the artifact-path tracking until Phase 4 ({@link LambdaPersistenceManager}
 * takes over). The whole facade is deleted in Phase 6.
 */
public enum LambdaRegistry {

    INSTANCE;

    private static final Logger LOG = LoggerFactory.getLogger(LambdaRegistry.class);

    public static final boolean PERSISTENCE_ENABLED = Boolean.parseBoolean(System.getProperty("mvel3.compiler.lambda.persistence", "true"));
    public static final Path DEFAULT_PERSISTENCE_PATH = Path.of(System.getProperty("mvel3.compiler.lambda.persistence.path", "target/generated-classes/mvel"));
    private static final Path REGISTRY_FILE = Path.of(System.getProperty("mvel3.compiler.lambda.registry.file",
                                                                         DEFAULT_PERSISTENCE_PATH.resolve("lambda-registry.dat").toString()));

    static {
        if (PERSISTENCE_ENABLED) {
            INSTANCE.loadFromDisk();
        }
        if (Boolean.getBoolean("mvel3.compiler.lambda.resetOnTestStartup")) {
            INSTANCE.resetAndRemoveAllPersistedFiles();
        }
    }

    private final LambdaCatalog catalog = new LambdaCatalog();

    // physical ID -> entry (artifact path tracking; migrates to LambdaPersistenceManager in Phase 4)
    private final Map<Integer, RegistryEntry> entriesByPhysicalId = new HashMap<>();

    LambdaRegistry() {
        // singleton
    }

    public LambdaCatalog catalog() {
        return catalog;
    }

    public int getNextLogicalId() {
        // Catalog allocates logicalId inside register(); this is informational only now.
        return -1;
    }

    public int registerLambda(int ignoredLogicalId, LambdaKey key) {
        RegistrationResult result = catalog.register(key);
        entriesByPhysicalId.computeIfAbsent(result.physicalId(), RegistryEntry::new);
        return result.physicalId();
    }

    public int getPhysicalId(int logicalId) {
        Integer p = catalog.physicalForLogical(logicalId);
        return p == null ? -1 : p;
    }

    public void registerPhysicalPath(int physicalId, String fqn, Path path) {
        RegistryEntry entry = entriesByPhysicalId.get(physicalId);
        if (entry == null) {
            throw new IllegalStateException("Unknown physical ID " + physicalId);
        }
        entry.fqn = fqn;
        entry.path = path;
        if (PERSISTENCE_ENABLED) {
            persistToDisk();
        }
    }

    public Path getPhysicalPath(int physicalId) {
        RegistryEntry entry = entriesByPhysicalId.get(physicalId);
        return entry == null ? null : entry.path;
    }

    public boolean isPersisted(int physicalId) {
        RegistryEntry entry = entriesByPhysicalId.get(physicalId);
        return entry != null && entry.path != null && Files.exists(entry.path);
    }

    /**
     * Clear all in-memory registry state and remove any persisted class files and registry file.
     * Intended for use in test setup to ensure a clean slate.
     */
    public synchronized void resetAndRemoveAllPersistedFiles() {
        LOG.info("Clean up Lambda Registry and persisted files at {}", DEFAULT_PERSISTENCE_PATH);
        try {
            new LambdaArtifactStore(DEFAULT_PERSISTENCE_PATH).deleteAll();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        catalog.clear();
        entriesByPhysicalId.clear();
    }

    private void loadFromDisk() {
        if (!Files.exists(REGISTRY_FILE)) {
            return;
        }
        try {
            LambdaPersistenceSnapshot snapshot = new LambdaRegistryStore(REGISTRY_FILE).load();
            catalog.applySnapshot(snapshot.catalog());
            for (Map.Entry<Integer, ArtifactRef> e : snapshot.artifacts().entrySet()) {
                RegistryEntry entry = new RegistryEntry(e.getKey());
                entry.fqn = e.getValue().fqn();
                entry.path = e.getValue().classFile();
                entriesByPhysicalId.put(e.getKey(), entry);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load lambda registry from " + REGISTRY_FILE, e);
        }
    }

    private void persistToDisk() {
        try {
            Files.createDirectories(REGISTRY_FILE.getParent());
            Map<Integer, ArtifactRef> artifacts = new HashMap<>();
            for (RegistryEntry entry : entriesByPhysicalId.values()) {
                if (entry.fqn != null && entry.path != null) {
                    artifacts.put(entry.physicalId, new ArtifactRef(entry.fqn, entry.path));
                }
            }
            new LambdaRegistryStore(REGISTRY_FILE).save(
                    new LambdaPersistenceSnapshot(catalog.toSnapshot(), artifacts));
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist lambda registry to " + REGISTRY_FILE, e);
        }
    }

    private static final class RegistryEntry {

        private final int physicalId;
        private String fqn;
        private Path path;

        private RegistryEntry(int physicalId) {
            this.physicalId = physicalId;
        }
    }
}
