package org.mvel3.lambdaextractor;

import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lambda Registry facade — transitional during the refactor. Delegates everything
 * to {@link LambdaRuntime#getInstance()}. Will be deleted in the same Phase 6
 * window once all callers have moved to {@code LambdaRuntime} directly.
 */
public enum LambdaRegistry {

    INSTANCE;

    private static final Logger LOG = LoggerFactory.getLogger(LambdaRegistry.class);

    public static final boolean PERSISTENCE_ENABLED = Boolean.parseBoolean(System.getProperty("mvel3.compiler.lambda.persistence", "true"));
    public static final Path DEFAULT_PERSISTENCE_PATH = Path.of(System.getProperty("mvel3.compiler.lambda.persistence.path", "target/generated-classes/mvel"));

    LambdaRegistry() {
        // singleton
    }

    public LambdaCatalog catalog() {
        return LambdaRuntime.getInstance().catalog();
    }

    public LambdaPersistenceManager persistenceManager() {
        return LambdaRuntime.getInstance().persistenceManager();
    }

    public int getNextLogicalId() {
        // Catalog allocates logicalId inside register(); this is informational only now.
        return -1;
    }

    public int registerLambda(int ignoredLogicalId, LambdaKey key) {
        return LambdaRuntime.getInstance().catalog().register(key).physicalId();
    }

    public int getPhysicalId(int logicalId) {
        Integer p = LambdaRuntime.getInstance().catalog().physicalForLogical(logicalId);
        return p == null ? -1 : p;
    }

    public void registerPhysicalPath(int physicalId, String fqn, Path path) {
        LambdaRuntime.getInstance().persistenceManager().attachArtifact(physicalId, new ArtifactRef(fqn, path));
    }

    public Path getPhysicalPath(int physicalId) {
        return LambdaRuntime.getInstance().persistenceManager().artifactFor(physicalId).map(ArtifactRef::classFile).orElse(null);
    }

    public boolean isPersisted(int physicalId) {
        return LambdaRuntime.getInstance().persistenceManager().artifactExists(physicalId);
    }

    public synchronized void resetAndRemoveAllPersistedFiles() {
        LOG.info("Clean up Lambda Registry and persisted files at {}", LambdaRuntime.defaultPersistencePath());
        LambdaRuntime.getInstance().resetAndRemoveAllPersistedFiles();
    }
}
