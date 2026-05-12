package org.mvel3.lambdaextractor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lambda Registry facade — transitional during the refactor. Dedup and
 * ID-allocation state has moved to {@link LambdaCatalog}; this class still
 * owns the artifact-path tracking until Phase 4 ({@link LambdaPersistenceManager}
 * takes over). The whole facade is deleted in Phase 6.
 */
public enum LambdaRegistry {

    INSTANCE;

    private static final Logger LOG = LoggerFactory.getLogger(LambdaRegistry.class);

    public static final boolean PERSISTENCE_ENABLED = Boolean.parseBoolean(System.getProperty("mvel3.compiler.lambda.persistence", "true"));
    public static final Path DEFAULT_PERSISTENCE_PATH = Path.of(System.getProperty("mvel3.compiler.lambda.persistence.path", "target/generated-classes/mvel"));
    private static final Path REGISTRY_FILE = Path.of(System.getProperty("mvel3.compiler.lambda.registry.file",
                                                                         DEFAULT_PERSISTENCE_PATH.resolve("lambda-registry.dat").toString()));

    // This version has to be incremented when the registry file format changes
    private static final String REGISTRY_VERSION = "v1";

    static {
        if (PERSISTENCE_ENABLED) {
            INSTANCE.loadFromDisk();
        }
        if (Boolean.getBoolean("mvel3.compiler.lambda.resetOnTestStartup")) {
            INSTANCE.resetAndRemoveAllPersistedFiles();
        }
    }

    private final LambdaCatalog catalog = new LambdaCatalog();

    // physical ID -> entry (artifact path tracking; migrates to LambdaPersistenceManager in Phase 4)
    private final Map<Integer, RegistryEntry> entriesByPhysicalId = new HashMap<>();

    LambdaRegistry() {
        // singleton
    }

    public LambdaCatalog catalog() {
        return catalog;
    }

    public int getNextLogicalId() {
        // Catalog allocates logicalId inside register(); this is informational only now.
        return -1;
    }

    public int registerLambda(int ignoredLogicalId, LambdaKey key) {
        RegistrationResult result = catalog.register(key);
        entriesByPhysicalId.computeIfAbsent(result.physicalId(), RegistryEntry::new);
        return result.physicalId();
    }

    public int getPhysicalId(int logicalId) {
        Integer p = catalog.physicalForLogical(logicalId);
        return p == null ? -1 : p;
    }

    public void registerPhysicalPath(int physicalId, String fqn, Path path) {
        RegistryEntry entry = entriesByPhysicalId.get(physicalId);
        if (entry == null) {
            throw new IllegalStateException("Unknown physical ID " + physicalId);
        }
        entry.fqn = fqn;
        entry.path = path;
        if (PERSISTENCE_ENABLED) {
            persistToDisk();
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

        catalog.clear();
        entriesByPhysicalId.clear();
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
            int nextPhysicalId = 0;
            int nextLogicalId = 0;
            if (countersLine != null) {
                String[] counters = countersLine.split("\\|", -1);
                if (counters.length >= 2) {
                    nextPhysicalId = parseIntSafe(counters[0], 0);
                    nextLogicalId = parseIntSafe(counters[1], 0);
                }
            }

            // Buffer entries; build a snapshot for the catalog and populate entriesByPhysicalId here.
            java.util.List<CatalogEntry> snapshotEntries = new java.util.ArrayList<>();
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
                String methodSignature = decode(parts[2]);
                String normalisedBody = decode(parts[3]);

                snapshotEntries.add(new CatalogEntry(physicalId, methodSignature, normalisedBody));

                RegistryEntry entry = new RegistryEntry(physicalId);
                if (!pathString.isEmpty()) {
                    entry.path = Path.of(pathString);
                }
                entriesByPhysicalId.put(physicalId, entry);
                maxPhysicalId = Math.max(maxPhysicalId, physicalId);
            }
            nextPhysicalId = Math.max(nextPhysicalId, maxPhysicalId + 1);

            catalog.applySnapshot(new CatalogSnapshot(nextPhysicalId, nextLogicalId, snapshotEntries));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load lambda registry from " + REGISTRY_FILE, e);
        }
    }

    private void persistToDisk() {
        try {
            Files.createDirectories(REGISTRY_FILE.getParent());
            CatalogSnapshot catSnap = catalog.toSnapshot();
            // Build lookup: physicalId -> (signature, body)
            Map<Integer, CatalogEntry> catByPid = new HashMap<>();
            for (CatalogEntry ce : catSnap.entries()) {
                catByPid.put(ce.physicalId(), ce);
            }
            try (BufferedWriter writer = Files.newBufferedWriter(REGISTRY_FILE, StandardCharsets.UTF_8)) {
                writer.write(REGISTRY_VERSION);
                writer.newLine();
                writer.write(catSnap.nextPhysicalId() + "|" + catSnap.nextLogicalId());
                writer.newLine();

                List<RegistryEntry> entries = entriesByPhysicalId.values().stream()
                        .filter(e -> e.path != null)
                        .sorted((a, b) -> Integer.compare(a.physicalId, b.physicalId))
                        .collect(Collectors.toList());

                for (RegistryEntry entry : entries) {
                    CatalogEntry ce = catByPid.get(entry.physicalId);
                    if (ce == null) continue;
                    writer.write(entry.physicalId + "|" + encode(entry.path.toString()) + "|" +
                                         encode(ce.methodSignature()) + "|" + encode(ce.normalizedBody()));
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

        private final int physicalId;
        private String fqn;
        private Path path;

        private RegistryEntry(int physicalId) {
            this.physicalId = physicalId;
        }
    }
}
