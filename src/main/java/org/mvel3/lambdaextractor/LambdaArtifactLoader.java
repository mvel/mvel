package org.mvel3.lambdaextractor;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import org.mvel3.ClassManager;

/**
 * Stateless helper for loading a persisted lambda classfile into a {@link ClassManager}.
 * Used by both MVEL compilers and DRLX consumers (cross-repo). Idempotent:
 * skips the define if the FQN is already loaded in the manager.
 */
public final class LambdaArtifactLoader {

    private LambdaArtifactLoader() {}

    public static Class<?> loadOrDefinePersistedClass(ClassManager cm, ArtifactRef ref) throws IOException {
        if (cm.getClasses().containsKey(ref.fqn())) {
            return cm.getClass(ref.fqn());
        }
        byte[] bytes = Files.readAllBytes(ref.classFile());
        cm.define(Map.of(ref.fqn(), bytes));
        return cm.getClass(ref.fqn());
    }
}
