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
import org.mvel3.lambdaextractor.LambdaRegistry;
import org.mvel3.parser.printer.PrintUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A batch compiler that accumulates lambdas, deduplicates via LambdaRegistry,
 * and compiles all unique ones in a single javac call.
 */
public class MVELBatchCompiler {

    private static final Logger LOG = LoggerFactory.getLogger(MVELBatchCompiler.class);

    private final ClassManager classManager;
    private final Path persistenceDir; // null = no persistence

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

        // Register with LambdaRegistry and rename class
        MVELCompiler.LambdaRegistration reg = MVELCompiler.registerAndRename(unit, fqn);
        int physicalId = reg.physicalId();
        String newFqn = reg.newFqn();

        HandleState state;
        if (LambdaRegistry.INSTANCE.isPersisted(physicalId)) {
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
                // Register physical paths with LambdaRegistry
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
                            LambdaRegistry.INSTANCE.registerPhysicalPath(h.physicalId, path);
                        }
                    }
                }
            } else {
                LOG.info("Batch-compiling {} lambda sources", pendingSources.size());
                KieMemoryCompiler.compile(classManager, pendingSources, classLoader);
            }
        }

        // Load PRE_PERSISTED lambdas from disk
        for (LambdaHandle h : handles) {
            if (h.state == HandleState.PRE_PERSISTED) {
                if (!classManager.getClasses().containsKey(h.fqn)) {
                    Path path = LambdaRegistry.INSTANCE.getPhysicalPath(h.physicalId);
                    try {
                        byte[] bytes = Files.readAllBytes(path);
                        classManager.define(Collections.singletonMap(h.fqn, bytes));
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to load persisted lambda class from " + path, e);
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
