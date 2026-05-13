package org.mvel3.lambdaextractor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Owns the {@code physicalId → ArtifactRef} association. Coordinates with
 * {@link LambdaArtifactStore} for on-disk existence checks. Triggers
 * {@link LambdaRuntime#persistSnapshot()} synchronously on {@link #attachArtifact}.
 * <p>
 * Compilers use this for compile-vs-reuse state queries. Not a compile wrapper —
 * compile orchestration remains in {@link org.mvel3.MVELCompiler} and
 * {@link org.mvel3.MVELBatchCompiler}.
 */
public final class LambdaPersistenceManager {

    private final LambdaArtifactStore artifactStore;
    private final LambdaRuntime runtime;     // for persistSnapshot()
    private final Map<Integer, ArtifactRef> artifacts = new HashMap<>();

    LambdaPersistenceManager(LambdaArtifactStore artifactStore, LambdaRuntime runtime) {
        this.artifactStore = artifactStore;
        this.runtime = runtime;
    }

    public Optional<ArtifactRef> artifactFor(int physicalId) {
        return Optional.ofNullable(artifacts.get(physicalId));
    }

    /** True iff an artifact is attached to {@code physicalId} AND its classfile exists on disk. */
    public boolean artifactExists(int physicalId) {
        ArtifactRef ref = artifacts.get(physicalId);
        return ref != null && artifactStore.exists(ref);
    }

    /** Attaches an artifact. Triggers synchronous registry-file save via the runtime. */
    public void attachArtifact(int physicalId, ArtifactRef ref) {
        artifacts.put(physicalId, ref);
        runtime.persistSnapshot();
    }

    /** Internal: rehydrate from a loaded snapshot. */
    void applyArtifacts(Map<Integer, ArtifactRef> loaded) {
        artifacts.clear();
        artifacts.putAll(loaded);
    }

    /** Internal: clear all artifact mappings (catalog reset path). */
    void clear() {
        artifacts.clear();
    }

    /** Internal: produce the snapshot view for serialization. */
    Map<Integer, ArtifactRef> snapshot() {
        return Map.copyOf(artifacts);
    }
}
