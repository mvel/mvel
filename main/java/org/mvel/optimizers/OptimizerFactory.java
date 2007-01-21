package org.mvel.optimizers;

import org.mvel.CompileException;
import org.mvel.optimizers.impl.asm.ASMAccessorCompiler;
import org.mvel.optimizers.impl.asm.ASMOptimizer;
import org.mvel.optimizers.impl.refl.ReflectiveAccessor;

import java.util.HashMap;
import java.util.Map;

public class OptimizerFactory {
    private static String defaultOptimizer = "ASM";
    private static final Map<String, Optimizer> optimizers = new HashMap<String, Optimizer>();
    private static final Map<String, AccessorCompiler> accessorCompilers = new HashMap<String, AccessorCompiler>();

    static {
        optimizers.put("ASM", new ASMOptimizer());
        accessorCompilers.put("ASM", new ASMAccessorCompiler());
        accessorCompilers.put("Reflective", new ReflectiveAccessor());
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
}
