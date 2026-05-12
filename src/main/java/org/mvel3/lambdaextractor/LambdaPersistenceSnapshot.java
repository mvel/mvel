package org.mvel3.lambdaextractor;

import java.util.Map;

/** Transfer object between LambdaRuntime and LambdaRegistryStore. */
public record LambdaPersistenceSnapshot(CatalogSnapshot catalog, Map<Integer, ArtifactRef> artifacts) {}
