package org.mvel3.javalambda;

import java.util.List;
import java.util.Map;

public record ExtractionResult(
    List<NormalizedLambda> allLambdas,
    Map<Integer, NormalizedLambda> uniqueMap,
    int totalCount,
    int uniqueCount,
    int reusedCount,
    int skippedCaptureCount
) {}
