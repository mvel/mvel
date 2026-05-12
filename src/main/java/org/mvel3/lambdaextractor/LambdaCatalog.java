package org.mvel3.lambdaextractor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory lambda dedup + ID allocation. Pure: no filesystem access,
 * no parsing of persisted files, no system-property reads.
 * <p>
 * Subtype-overload reuse: a new {@link LambdaKey} whose parameter types are
 * subtypes of an existing key's parameters reuses the existing physical ID.
 */
public final class LambdaCatalog {

    private final Map<LambdaKey, Integer> physicalIdByKey = new HashMap<>();
    private final Map<Integer, List<LambdaKey>> keysByHash = new HashMap<>();
    private final Map<Integer, Integer> physicalByLogical = new HashMap<>();
    private int nextPhysicalId = 0;
    private int nextLogicalId = 0;

    public RegistrationResult register(LambdaKey key) {
        int logicalId = nextLogicalId++;

        Integer existingExact = physicalIdByKey.get(key);
        if (existingExact != null) {
            physicalByLogical.put(logicalId, existingExact);
            return new RegistrationResult(logicalId, existingExact, true);
        }

        Integer reusedSubtype = findSubtypeOverloadReuse(key);
        if (reusedSubtype != null) {
            physicalIdByKey.put(key, reusedSubtype);
            physicalByLogical.put(logicalId, reusedSubtype);
            return new RegistrationResult(logicalId, reusedSubtype, true);
        }

        int physicalId = nextPhysicalId++;
        physicalIdByKey.put(key, physicalId);
        keysByHash.computeIfAbsent(key.hashCode(), h -> new ArrayList<>()).add(key);
        physicalByLogical.put(logicalId, physicalId);
        return new RegistrationResult(logicalId, physicalId, false);
    }

    private Integer findSubtypeOverloadReuse(LambdaKey key) {
        List<LambdaKey> candidates = keysByHash.get(key.hashCode());
        if (candidates == null) return null;
        for (LambdaKey target : candidates) {
            if (!target.getNormalisedBody().equals(key.getNormalisedBody())) continue;
            LambdaKey.MethodSignatureInfo targetInfo = target.getMethodSignatureInfo();
            LambdaKey.MethodSignatureInfo currentInfo = key.getMethodSignatureInfo();
            if (!targetInfo.returnType.equals(currentInfo.returnType)) continue;
            if (!targetInfo.methodName.equals(currentInfo.methodName)) continue;
            if (targetInfo.parameterTypes.size() != currentInfo.parameterTypes.size()) continue;
            if (allParamsAssignable(targetInfo, currentInfo)) {
                return physicalIdByKey.get(target);
            }
        }
        return null;
    }

    private static boolean allParamsAssignable(LambdaKey.MethodSignatureInfo target, LambdaKey.MethodSignatureInfo current) {
        for (int i = 0; i < target.parameterTypes.size(); i++) {
            if (!target.parameterTypes.get(i).isAssignableFrom(current.parameterTypes.get(i))) return false;
        }
        return true;
    }

    /** Test/facade-only helper. */
    public Integer physicalForLogical(int logicalId) {
        return physicalByLogical.get(logicalId);
    }

    /** Test/internal: clears all in-memory state. */
    public void clear() {
        physicalIdByKey.clear();
        keysByHash.clear();
        physicalByLogical.clear();
        nextPhysicalId = 0;
        nextLogicalId = 0;
    }

    /** Used by LambdaRuntime to rehydrate from a loaded snapshot. */
    void applySnapshot(CatalogSnapshot snapshot) {
        clear();
        this.nextPhysicalId = snapshot.nextPhysicalId();
        this.nextLogicalId = snapshot.nextLogicalId();
        for (CatalogEntry entry : snapshot.entries()) {
            LambdaKey key = LambdaUtils.createLambdaKeyFromMethodDeclarationString(
                    entry.methodSignature() + " " + entry.normalizedBody());
            physicalIdByKey.put(key, entry.physicalId());
            keysByHash.computeIfAbsent(key.hashCode(), h -> new ArrayList<>()).add(key);
        }
    }

    /** Used by LambdaRuntime to produce a snapshot for serialization. */
    CatalogSnapshot toSnapshot() {
        List<CatalogEntry> entries = new ArrayList<>();
        for (Map.Entry<LambdaKey, Integer> e : physicalIdByKey.entrySet()) {
            entries.add(new CatalogEntry(e.getValue(), e.getKey().getMethodSignature(), e.getKey().getNormalisedBody()));
        }
        entries.sort((a, b) -> Integer.compare(a.physicalId(), b.physicalId()));
        return new CatalogSnapshot(nextPhysicalId, nextLogicalId, entries);
    }
}
