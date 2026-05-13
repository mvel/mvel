package org.mvel3;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.javaparser.ast.CompilationUnit;
import org.mvel3.javacompiler.KieMemoryCompiler;
import org.mvel3.lambdaextractor.ArtifactRef;
import org.mvel3.lambdaextractor.LambdaArtifactLoader;
import org.mvel3.lambdaextractor.LambdaCatalog;
import org.mvel3.lambdaextractor.LambdaRuntime;
import org.mvel3.parser.printer.PrintUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A batch compiler that accumulates lambdas, deduplicates them, and compiles
 * all unique ones in a single javac call. When constructed with a persistence
 * directory, dedup goes through the global {@link LambdaRuntime} catalog and
 * persisted artifacts are reused. When constructed without one (no-persist
 * mode), dedup uses a batch-local {@link LambdaCatalog} and no global state
 * is read or mutated.
 */
public class MVELBatchCompiler {

    private static final Logger LOG = LoggerFactory.getLogger(MVELBatchCompiler.class);

    private final ClassManager classManager;
    private final Path persistenceDir; // null = no persistence
    // In no-persist mode we use a batch-local catalog so dedup and class
    // renaming still work without touching the global LambdaRuntime catalog.
    // Null when persistenceDir != null.
    private final LambdaCatalog localCatalog;

    // Accumulated state
    private final Map<String, String> pendingSources = new LinkedHashMap<>(); // fqn -> source
    private final Map<Integer, String> physicalIdToFqn = new HashMap<>();    // dedup within batch
    private final List<LambdaHandle> handles = new ArrayList<>();
    private boolean compiled = false;

    public MVELBatchCompiler(ClassManager classManager) {
        this(classManager, null);
    }

    public MVELBatchCompiler(ClassManager classManager, Path persistenceDir) {
        this.classManager = classManager;
        this.persistenceDir = persistenceDir;
        this.localCatalog = persistenceDir == null ? new LambdaCatalog() : null;
    }

    public ClassManager getClassManager() {
        return classManager;
    }

    /**
     * Accumulate a lambda for batch compilation. Returns a handle for later resolution.
     */
    public <T, K, R> LambdaHandle add(CompilerParameters<T, K, R> info) {
        MVELCompiler compiler = new MVELCompiler();
        CompilationUnit unit = compiler.transpileToCompilationUnit(info);
        String fqn = MVELCompiler.evaluatorFullQualifiedName(unit);

        if (persistenceDir == null) {
            // No-persist: rename via a batch-local LambdaCatalog so dedup and
            // unique class names still work, without touching the global
            // LambdaRuntime catalog or persistence manager.
            MVELCompiler.LambdaRegistration reg = MVELCompiler.registerAndRename(unit, fqn, localCatalog);
            int physicalId = reg.physicalId();
            String newFqn = reg.newFqn();

            HandleState state;
            if (physicalIdToFqn.containsKey(physicalId)) {
                newFqn = physicalIdToFqn.get(physicalId);
                state = HandleState.DEDUP;
            } else {
                pendingSources.put(newFqn, PrintUtil.printNode(unit));
                physicalIdToFqn.put(physicalId, newFqn);
                state = HandleState.NEW;
            }
            LambdaHandle handle = new LambdaHandle(physicalId, newFqn, state);
            handles.add(handle);
            return handle;
        }

        // Register with LambdaRuntime catalog and rename class
        MVELCompiler.LambdaRegistration reg = MVELCompiler.registerAndRename(unit, fqn);
        int physicalId = reg.physicalId();
        String newFqn = reg.newFqn();

        HandleState state;
        if (LambdaRuntime.getInstance().persistenceManager().artifactExists(physicalId)) {
            state = HandleState.PRE_PERSISTED;
        } else if (physicalIdToFqn.containsKey(physicalId)) {
            // Same lambda already added in this batch — reuse
            newFqn = physicalIdToFqn.get(physicalId);
            state = HandleState.DEDUP;
        } else {
            // New unique lambda — add source for compilation
            String source = PrintUtil.printNode(unit);
            pendingSources.put(newFqn, source);
            physicalIdToFqn.put(physicalId, newFqn);
            state = HandleState.NEW;
        }

        LambdaHandle handle = new LambdaHandle(physicalId, newFqn, state);
        handles.add(handle);
        return handle;
    }

    /**
     * Compile all accumulated unique lambdas in a single javac call.
     */
    public void compile(ClassLoader classLoader) {
        if (!pendingSources.isEmpty()) {
            if (persistenceDir != null) {
                LOG.info("Batch-compiling and persisting {} lambda sources", pendingSources.size());
                List<Path> persistedFiles = KieMemoryCompiler.compileAndPersist(
                        classManager, pendingSources, classLoader, null, persistenceDir);
                MVELCompiler.bumpCompileInvocationCount();
                // Register physical paths with the persistence manager
                Map<String, Path> fqnToPath = new HashMap<>();
                for (Path persistedFile : persistedFiles) {
                    String relativePath = persistenceDir.relativize(persistedFile).toString();
                    String fileFqn = relativePath.replace('/', '.').replace(".class", "");
                    fqnToPath.put(fileFqn, persistedFile);
                }
                for (LambdaHandle h : handles) {
                    if (h.state == HandleState.NEW) {
                        Path path = fqnToPath.get(h.fqn);
                        if (path != null) {
                            LambdaRuntime.getInstance().persistenceManager()
                                    .attachArtifact(h.physicalId, new ArtifactRef(h.fqn, path));
                        }
                    }
                }
            } else {
                LOG.info("Batch-compiling {} lambda sources", pendingSources.size());
                KieMemoryCompiler.compile(classManager, pendingSources, classLoader);
                MVELCompiler.bumpCompileInvocationCount();
            }
        }

        // Load PRE_PERSISTED lambdas from disk
        for (LambdaHandle h : handles) {
            if (h.state == HandleState.PRE_PERSISTED) {
                if (!classManager.getClasses().containsKey(h.fqn)) {
                    ArtifactRef ref = LambdaRuntime.getInstance().persistenceManager()
                            .artifactFor(h.physicalId)
                            .orElseThrow(() -> new IllegalStateException("No artifact for handle " + h));
                    try {
                        LambdaArtifactLoader.loadOrDefinePersistedClass(classManager, ref);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to load persisted lambda class from " + ref.classFile(), e);
                    }
                }
            }
        }

        compiled = true;
    }

    /**
     * Resolve a handle to an evaluator instance. Must be called after compile().
     */
    @SuppressWarnings("unchecked")
    public <T, K, R> Evaluator<T, K, R> resolve(LambdaHandle handle) {
        if (!compiled) {
            throw new IllegalStateException("compile() not called yet");
        }
        return MVELCompiler.resolveEvaluator(classManager, handle.fqn);
    }

    /**
     * Get the FQN assigned to a handle (for metadata recording).
     */
    public String getFqn(LambdaHandle handle) {
        return handle.fqn;
    }

    /**
     * Get the physicalId assigned to a handle (for metadata recording).
     */
    public int getPhysicalId(LambdaHandle handle) {
        return handle.physicalId;
    }

    /**
     * Returns the persisted artifact reference for a handle, for cross-repo
     * consumers (DRLX). Internal callers may still use {@link #getFqn(LambdaHandle)}
     * / {@link #getPhysicalId(LambdaHandle)} where appropriate.
     *
     * @throws IllegalStateException if no artifact has been attached for this handle
     */
    public ArtifactRef getArtifactRef(LambdaHandle handle) {
        String fqn = handle.fqn;
        Path classFile = LambdaRuntime.getInstance().persistenceManager()
                .artifactFor(handle.physicalId)
                .map(ArtifactRef::classFile)
                .orElseThrow(() -> new IllegalStateException("No artifact attached for handle " + handle));
        return new ArtifactRef(fqn, classFile);
    }

    enum HandleState {
        NEW,
        DEDUP,
        PRE_PERSISTED
    }

    /**
     * Opaque handle returned by add(), used to resolve the evaluator after compilation.
     */
    public static class LambdaHandle {
        private final int physicalId;
        private final String fqn;
        private final HandleState state;

        LambdaHandle(int physicalId, String fqn, HandleState state) {
            this.physicalId = physicalId;
            this.fqn = fqn;
            this.state = state;
        }
    }
}
