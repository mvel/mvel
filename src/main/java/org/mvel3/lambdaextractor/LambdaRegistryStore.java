package org.mvel3.lambdaextractor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Properties-based persistence for the lambda registry. Internal to LambdaRuntime;
 * compilers should not interact with this class directly.
 * <p>
 * Format: {@code format.version=2}. See the design doc for the schema.
 */
final class LambdaRegistryStore {

    private static final String FORMAT_VERSION = "2";
    private static final String KEY_VERSION = "format.version";
    private static final String KEY_NEXT_PHYSICAL = "catalog.nextPhysicalId";
    private static final String KEY_NEXT_LOGICAL = "catalog.nextLogicalId";

    private final Path file;

    LambdaRegistryStore(Path file) {
        this.file = file;
    }

    LambdaPersistenceSnapshot load() throws IOException {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
        }
        String version = props.getProperty(KEY_VERSION);
        if (!FORMAT_VERSION.equals(version)) {
            throw new InvalidLambdaRegistryException(
                    "Unsupported lambda-registry format.version: " + version + " (expected " + FORMAT_VERSION + ")");
        }

        int nextPhysical = requiredInt(props, KEY_NEXT_PHYSICAL);
        int nextLogical = requiredInt(props, KEY_NEXT_LOGICAL);

        // Catalog entries
        Map<Integer, CatalogEntry> catalogByPhysicalId = new TreeMap<>();
        Map<Integer, Integer> catalogIndexByIndex = parseIndexed(props, "catalog.entry.");
        for (Integer index : catalogIndexByIndex.keySet()) {
            int physicalId = requiredInt(props, "catalog.entry." + index + ".physicalId");
            String methodSignature = requiredString(props, "catalog.entry." + index + ".methodSignature");
            String normalizedBody = requiredString(props, "catalog.entry." + index + ".normalizedBody");
            if (catalogByPhysicalId.containsKey(physicalId)) {
                throw new InvalidLambdaRegistryException("Duplicate catalog physicalId: " + physicalId);
            }
            catalogByPhysicalId.put(physicalId, new CatalogEntry(physicalId, methodSignature, normalizedBody));
        }

        // Artifacts
        Map<Integer, ArtifactRef> artifacts = new HashMap<>();
        Map<Integer, Integer> artifactIndexByIndex = parseIndexed(props, "artifact.");
        for (Integer index : artifactIndexByIndex.keySet()) {
            int physicalId = requiredInt(props, "artifact." + index + ".physicalId");
            String fqn = requiredString(props, "artifact." + index + ".fqn");
            String classFile = requiredString(props, "artifact." + index + ".classFile");
            if (!catalogByPhysicalId.containsKey(physicalId)) {
                throw new InvalidLambdaRegistryException(
                        "Artifact physicalId " + physicalId + " has no matching catalog entry");
            }
            if (artifacts.containsKey(physicalId)) {
                throw new InvalidLambdaRegistryException("Duplicate artifact physicalId: " + physicalId);
            }
            Path classFilePath;
            try {
                classFilePath = Path.of(classFile);
            } catch (InvalidPathException e) {
                throw new InvalidLambdaRegistryException(
                        "Invalid classFile path for artifact physicalId " + physicalId + ": " + classFile, e);
            }
            artifacts.put(physicalId, new ArtifactRef(fqn, classFilePath));
        }

        return new LambdaPersistenceSnapshot(
                new CatalogSnapshot(nextPhysical, nextLogical, new ArrayList<>(catalogByPhysicalId.values())),
                artifacts);
    }

    void save(LambdaPersistenceSnapshot snapshot) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        Properties props = new Properties();
        props.setProperty(KEY_VERSION, FORMAT_VERSION);
        props.setProperty(KEY_NEXT_PHYSICAL, Integer.toString(snapshot.catalog().nextPhysicalId()));
        props.setProperty(KEY_NEXT_LOGICAL, Integer.toString(snapshot.catalog().nextLogicalId()));

        int i = 0;
        for (CatalogEntry entry : snapshot.catalog().entries()) {
            props.setProperty("catalog.entry." + i + ".physicalId", Integer.toString(entry.physicalId()));
            props.setProperty("catalog.entry." + i + ".methodSignature", entry.methodSignature());
            props.setProperty("catalog.entry." + i + ".normalizedBody", entry.normalizedBody());
            i++;
        }
        int j = 0;
        for (Map.Entry<Integer, ArtifactRef> e : new TreeMap<>(snapshot.artifacts()).entrySet()) {
            props.setProperty("artifact." + j + ".physicalId", Integer.toString(e.getKey()));
            props.setProperty("artifact." + j + ".fqn", e.getValue().fqn());
            props.setProperty("artifact." + j + ".classFile", e.getValue().classFile().toString());
            j++;
        }

        try (OutputStream out = Files.newOutputStream(file)) {
            props.store(out, "MVEL3 lambda registry");
        }
    }

    private static int requiredInt(Properties p, String key) throws InvalidLambdaRegistryException {
        String v = p.getProperty(key);
        if (v == null) throw new InvalidLambdaRegistryException("Missing key: " + key);
        try { return Integer.parseInt(v); }
        catch (NumberFormatException nfe) {
            throw new InvalidLambdaRegistryException("Non-integer value for " + key + ": " + v, nfe);
        }
    }
    private static String requiredString(Properties p, String key) throws InvalidLambdaRegistryException {
        String v = p.getProperty(key);
        if (v == null) throw new InvalidLambdaRegistryException("Missing key: " + key);
        return v;
    }

    /** Returns the set of distinct integer indexes used in keys matching {@code prefix<N>.*}. */
    private static Map<Integer, Integer> parseIndexed(Properties p, String prefix) {
        Map<Integer, Integer> indexes = new TreeMap<>();
        for (String key : p.stringPropertyNames()) {
            if (!key.startsWith(prefix)) continue;
            String rest = key.substring(prefix.length());
            int dot = rest.indexOf('.');
            if (dot <= 0) continue;
            try {
                int n = Integer.parseInt(rest.substring(0, dot));
                indexes.put(n, n);
            } catch (NumberFormatException ignored) { }
        }
        return indexes;
    }
}
