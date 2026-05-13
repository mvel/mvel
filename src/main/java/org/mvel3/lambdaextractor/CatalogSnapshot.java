package org.mvel3.lambdaextractor;

import java.util.List;

public record CatalogSnapshot(int nextPhysicalId, int nextLogicalId, List<CatalogEntry> entries) {}
