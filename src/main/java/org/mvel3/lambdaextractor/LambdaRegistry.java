package org.mvel3.lambdaextractor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum LambdaRegistry {

    INSTANCE;

    private static final Logger LOG = LoggerFactory.getLogger(LambdaRegistry.class);

    public static final boolean PERSISTENCE_ENABLED = Boolean.parseBoolean(System.getProperty("mvel3.compiler.lambda.persistence", "true"));
    public static final Path DEFAULT_PERSISTENCE_PATH = Path.of(System.getProperty("mvel3.compiler.lambda.persistence.path", "target/generated-classes/mvel"));
    private static final Path REGISTRY_FILE = Path.of(System.getProperty("mvel3.compiler.lambda.registry.file",
            DEFAULT_PERSISTENCE_PATH.resolve("lambda-registry.dat").toString()));
    private static final String REGISTRY_VERSION = "v1";

    static {
        if (PERSISTENCE_ENABLED) {
            INSTANCE.loadFromDisk();
        }
        if (Boolean.getBoolean("mvel3.compiler.lambda.resetOnTestStartup")) {
            INSTANCE.resetAndRemoveAllPersistedFiles();
        }
    }

    // hash -> logical IDs (group of conflicting hashes)
    private final Map<Integer, List<Integer>> hashToLogicalIds = new HashMap<>();

    // LambdaKey -> entry (physical ID + optional persisted path)
    private final Map<LambdaKey, RegistryEntry> entriesByKey = new HashMap<>();

    // physical ID -> entry
    private final Map<Integer, RegistryEntry> entriesByPhysicalId = new HashMap<>();

    // logical ID -> physical ID
    private final Map<Integer, Integer> logicalToPhysical = new HashMap<>();

    private int nextPhysicalId = 0;

    private int nextLogicalId = 0;

    private LambdaRegistry() {
        // singleton
    }

    public int getNextLogicalId() {
        return nextLogicalId++;
    }

    public int registerLambda(int logicalId, LambdaKey key, int hash) {
        hashToLogicalIds
                .computeIfAbsent(hash, h -> new ArrayList<>())
                .add(logicalId);

        // TODO: revisit to utilize the murmur3 hash
        // currently, we rely on usual HashMap `entriesByKey` (so String.hashCode) for Lambda equality

        RegistryEntry entry = entriesByKey.get(key);
        if (entry == null) {
            entry = new RegistryEntry(key, nextPhysicalId++);
            entriesByKey.put(key, entry);
            entriesByPhysicalId.put(entry.physicalId, entry);
        } else {
            entriesByPhysicalId.putIfAbsent(entry.physicalId, entry);
        }

        logicalToPhysical.put(logicalId, entry.physicalId);
        return entry.physicalId;
    }

    public int getPhysicalId(int logicalId) {
        return logicalToPhysical.get(logicalId);
    }

    List<Integer> getLogicalIdsWithSameHash(int hash) {
        return hashToLogicalIds.getOrDefault(hash, List.of());
    }

    public void registerPhysicalPath(int physicalId, Path path) {
        RegistryEntry entry = entriesByPhysicalId.get(physicalId);
        if (entry == null) {
            throw new IllegalStateException("Unknown physical ID " + physicalId);
        }
        entry.path = path;
        if (PERSISTENCE_ENABLED) {
            persistToDisk(); // TODO: We may call this explicitly at the end of whole build
        }
    }

    public Path getPhysicalPath(int physicalId) {
        RegistryEntry entry = entriesByPhysicalId.get(physicalId);
        return entry == null ? null : entry.path;
    }

    public boolean isPersisted(int physicalId) {
        RegistryEntry entry = entriesByPhysicalId.get(physicalId);
        return entry != null && entry.path != null && Files.exists(entry.path);
    }

    /**
     * Clear all in-memory registry state and remove any persisted class files and registry file.
     * Intended for use in test setup to ensure a clean slate.
     */
    public synchronized void resetAndRemoveAllPersistedFiles() {
        LOG.info("Clean up Lambda Registry and persisted files at {}", DEFAULT_PERSISTENCE_PATH);
        if (Files.exists(DEFAULT_PERSISTENCE_PATH)) {
            try (Stream<Path> walk = Files.walk(DEFAULT_PERSISTENCE_PATH)) {
                walk.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (Exception ignored) {
                                // best-effort cleanup
                            }
                        });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        hashToLogicalIds.clear();
        entriesByKey.clear();
        entriesByPhysicalId.clear();
        logicalToPhysical.clear();
        nextPhysicalId = 0;
        nextLogicalId = 0;
    }

    private void loadFromDisk() {
        if (!Files.exists(REGISTRY_FILE)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(REGISTRY_FILE, StandardCharsets.UTF_8)) {
            String versionLine = reader.readLine();
            if (!REGISTRY_VERSION.equals(versionLine)) {
                return;
            }
            String countersLine = reader.readLine();
            if (countersLine != null) {
                String[] counters = countersLine.split("\\|", -1);
                if (counters.length >= 2) {
                    nextPhysicalId = parseIntSafe(counters[0], nextPhysicalId);
                    nextLogicalId = parseIntSafe(counters[1], nextLogicalId);
                }
            }

            String line;
            int maxPhysicalId = -1;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split("\\|", -1);
                if (parts.length < 4) {
                    continue;
                }
                int physicalId = parseIntSafe(parts[0], -1);
                if (physicalId < 0) {
                    continue;
                }
                String pathString = decode(parts[1]);
                String normalisedBody = decode(parts[2]);
                LambdaKey key = new LambdaKey(normalisedBody);

                RegistryEntry entry = new RegistryEntry(key, physicalId);
                if (!pathString.isEmpty()) {
                    entry.path = Path.of(pathString);
                }
                entriesByKey.put(key, entry);
                entriesByPhysicalId.put(physicalId, entry);
                maxPhysicalId = Math.max(maxPhysicalId, physicalId);
            }
            nextPhysicalId = Math.max(nextPhysicalId, maxPhysicalId + 1);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load lambda registry from " + REGISTRY_FILE, e);
        }
    }

    private void persistToDisk() {
        try {
            Files.createDirectories(REGISTRY_FILE.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(REGISTRY_FILE, StandardCharsets.UTF_8)) {
                writer.write(REGISTRY_VERSION);
                writer.newLine();
                writer.write(nextPhysicalId + "|" + nextLogicalId);
                writer.newLine();

                List<RegistryEntry> entries = entriesByPhysicalId.values().stream()
                        .filter(e -> e.path != null)
                        .sorted((a, b) -> Integer.compare(a.physicalId, b.physicalId))
                        .collect(Collectors.toList());

                for (RegistryEntry entry : entries) {
                    writer.write(entry.physicalId + "|" + encode(entry.path.toString()) + "|" +
                            encode(entry.key.getNormalisedSource()));
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist lambda registry to " + REGISTRY_FILE, e);
        }
    }

    private static int parseIntSafe(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static final class RegistryEntry {
        private final LambdaKey key;
        private final int physicalId;
        private Path path;

        private RegistryEntry(LambdaKey key, int physicalId) {
            this.key = key;
            this.physicalId = physicalId;
        }
    }
}
