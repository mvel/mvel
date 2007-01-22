package org.mvel.optimizers;

import org.mvel.CompileException;
import org.mvel.optimizers.impl.asm.ASMAccessorCompiler;
import org.mvel.optimizers.impl.asm.ASMOptimizer;
import org.mvel.optimizers.impl.refl.ReflectiveAccessor;

import java.util.HashMap;
import java.util.Map;

public class OptimizerFactory {
    public static String SAFE_REFLECTIVE = "Reflective";

    private static String defaultOptimizer;
    private static final Map<String, Optimizer> optimizers = new HashMap<String, Optimizer>();
    private static final Map<String, AccessorCompiler> accessorCompilers = new HashMap<String, AccessorCompiler>();

    static {
        accessorCompilers.put(SAFE_REFLECTIVE, new ReflectiveAccessor());

        /**
         * By default, activate the JIT if ASM is present in the classpath
         */
        try {
            Class.forName("org.objectweb.asm.ClassWriter");

            defaultOptimizer = "ASM";
            optimizers.put("ASM", new ASMOptimizer());
            accessorCompilers.put("ASM", new ASMAccessorCompiler());
        }
        catch (ClassNotFoundException e) {
            defaultOptimizer = SAFE_REFLECTIVE;
        }
    }

    public static void registerOptimizer(Optimizer optimizer) {
        optimizers.put(optimizer.getName(), optimizer);
    }

    public static Optimizer getDefaultOptimizer() {
        return optimizers.get(defaultOptimizer);
    }

    public static AccessorCompiler getDefaultAccessorCompiler() {
        try {
            return accessorCompilers.get(defaultOptimizer).getClass().newInstance();
        }
        catch (Exception e) {
            throw new CompileException("unable to instantiate accessor compiler", e);
        }
    }

    public static AccessorCompiler getAccessorCompiler(String name) {
        try {
            return accessorCompilers.get(name).getClass().newInstance();
        }
        catch (Exception e) {
            throw new CompileException("unable to instantiate accessor compiler", e);
        }
    }
}
