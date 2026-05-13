package org.mvel3.lambdaextractor;

import java.nio.file.Path;

/**
 * Persistence-facing value object. Lambda's persisted classfile + FQN.
 * Used both by MVEL internally and by DRLX as the cross-repo persistence
 * artifact reference.
 */
public record ArtifactRef(String fqn, Path classFile) {}
