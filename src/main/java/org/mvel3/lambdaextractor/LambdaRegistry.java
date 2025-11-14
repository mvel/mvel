package org.mvel3.lambdaextractor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class LambdaRegistry {

    // hash -> logical IDs (group of conflicting hashes)
    private final Map<Integer, List<Integer>> hashToLogicalIds = new HashMap<>();

    // LambdaKey -> physical ID
    private final Map<LambdaKey, Integer> keyToPhysicalId = new HashMap<>();

    // logical ID -> physical ID
    private final Map<Integer, Integer> logicalToPhysical = new HashMap<>();

    // physical ID -> path to complied lambda class
    private final Map<Integer, String> physicalIdToPath = new HashMap<>();

    private int nextPhysicalId = 0;

    int registerLambda(int logicalId, LambdaKey key, int hash) {
        hashToLogicalIds
                .computeIfAbsent(hash, h -> new ArrayList<>())
                .add(logicalId);

        Integer physicalId = keyToPhysicalId.get(key);
        if (physicalId == null) {
            physicalId = nextPhysicalId++;
            keyToPhysicalId.put(key, physicalId);
        }

        logicalToPhysical.put(logicalId, physicalId);
        return physicalId;
    }

    int getPhysicalId(int logicalId) {
        return logicalToPhysical.get(logicalId);
    }

    List<Integer> getLogicalIdsWithSameHash(int hash) {
        return hashToLogicalIds.getOrDefault(hash, List.of());
    }

    void registerPhysicalPath(int physicalId, String path) {
        physicalIdToPath.put(physicalId, path);
    }
}