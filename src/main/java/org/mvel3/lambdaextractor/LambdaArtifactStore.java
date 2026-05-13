package org.mvel3.lambdaextractor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Dumb byte-level I/O for persisted lambda classfiles. The {@code persistenceRoot}
 * is used only by {@link #deleteAll()}; {@link #exists(ArtifactRef)} and
 * {@link #readBytes(ArtifactRef)} work directly off the path embedded in the
 * {@link ArtifactRef}.
 */
public final class LambdaArtifactStore {

    private final Path persistenceRoot;

    public LambdaArtifactStore(Path persistenceRoot) {
        this.persistenceRoot = persistenceRoot;
    }

    public boolean exists(ArtifactRef ref) {
        return Files.exists(ref.classFile());
    }

    public byte[] readBytes(ArtifactRef ref) throws IOException {
        return Files.readAllBytes(ref.classFile());
    }

    public void deleteAll() throws IOException {
        if (!Files.exists(persistenceRoot)) return;
        try (Stream<Path> walk = Files.walk(persistenceRoot)) {
            walk.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                .forEach(path -> {
                    try { Files.deleteIfExists(path); }
                    catch (IOException ignored) { /* best-effort */ }
                });
        }
    }
}
