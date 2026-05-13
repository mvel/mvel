package org.mvel3.lambdaextractor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Composition root for MVEL3 lambda persistence. Lazy-initialised via
 * {@link #getInstance()}. Holds {@link LambdaCatalog}, {@link LambdaPersistenceManager},
 * {@link LambdaArtifactStore}, and an internal {@link LambdaRegistryStore}.
 * <p>
 * {@code getInstance()} and the static accessors below are transitional
 * infrastructure, not the desired long-term public model. A later cleanup
 * would inject {@code LambdaRuntime} through compiler constructors.
 */
public final class LambdaRuntime {

    private static volatile LambdaRuntime instance;

    private final RuntimeConfig config;
    private final LambdaCatalog catalog;
    private final LambdaPersistenceManager persistenceManager;
    private final LambdaArtifactStore artifactStore;
    private final LambdaRegistryStore registryStore;

    private LambdaRuntime(RuntimeConfig config) {
        this.config = config;
        this.catalog = new LambdaCatalog();
        this.artifactStore = new LambdaArtifactStore(config.persistenceRoot());
        this.persistenceManager = new LambdaPersistenceManager(artifactStore, this);
        this.registryStore = new LambdaRegistryStore(config.registryFile());
    }

    public static LambdaRuntime getInstance() {
        LambdaRuntime r = instance;
        if (r == null) {
            synchronized (LambdaRuntime.class) {
                r = instance;
                if (r == null) {
                    r = new LambdaRuntime(RuntimeConfig.fromSystemProperties());
                    r.initialize();
                    instance = r;
                }
            }
        }
        return r;
    }

    public LambdaCatalog catalog() { return catalog; }
    public LambdaPersistenceManager persistenceManager() { return persistenceManager; }

    /**
     * Internal/compiler-facing seam. Exposed for compiler-flow access to persistence
     * config (e.g., the top-level config.persistenceEnabled() branch). Read-only.
     * Not intended as long-term external API; same status as getInstance().
     */
    public RuntimeConfig config() { return config; }

    /**
     * Internal/compiler-facing seam. Called by {@link LambdaPersistenceManager#attachArtifact}
     * to flush the snapshot synchronously. No-op when {@code config.persistenceEnabled()}
     * is false. Save-per-attach matches today's behavior; batched flushing is a future
     * optimization. Not intended as long-term external API.
     */
    public void persistSnapshot() {
        if (!config.persistenceEnabled()) return;
        try {
            registryStore.save(new LambdaPersistenceSnapshot(catalog.toSnapshot(), persistenceManager.snapshot()));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to persist lambda registry", e);
        }
    }

    public void reset() {
        catalog.clear();
        persistenceManager.clear();
    }

    public synchronized void resetAndRemoveAllPersistedFiles() {
        reset();
        try {
            artifactStore.deleteAll();
            Files.deleteIfExists(config.registryFile());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void initialize() {
        if (config.resetOnTestStartup()) {
            resetAndRemoveAllPersistedFiles();
            return;
        }
        if (config.persistenceEnabled() && Files.exists(config.registryFile())) {
            try {
                LambdaPersistenceSnapshot snapshot = registryStore.load();
                catalog.applySnapshot(snapshot.catalog());
                persistenceManager.applyArtifacts(snapshot.artifacts());
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to load lambda registry", e);
            }
        }
    }

    // Transitional static accessors used by DRLX:
    public static boolean isPersistenceEnabled() { return getInstance().config.persistenceEnabled(); }
    public static Path defaultPersistencePath() { return getInstance().config.persistenceRoot(); }

    /**
     * Test-only: clears the cached singleton so the next {@link #getInstance()}
     * re-reads system properties. Production code must not mutate the
     * {@code mvel3.compiler.lambda.*} properties after first use.
     */
    public static synchronized void resetSingletonForTests() {
        instance = null;
    }
}
