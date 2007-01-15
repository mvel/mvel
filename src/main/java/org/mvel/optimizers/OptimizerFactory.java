package org.mvel.optimizers;

import org.mvel.optimizers.impl.ast.ASTOptimizer;

import java.util.HashMap;
import java.util.Map;

public class OptimizerFactory {
    private static String defaultOptimizer = "AST";
    private static final Map<String, Optimizer> optimizers = new HashMap<String, Optimizer>();

    static {
        ASTOptimizer astOptimizer = new ASTOptimizer();
        optimizers.put(astOptimizer.getName(), astOptimizer);
    }

    public static void registerOptimizer(Optimizer optimizer) {
        optimizers.put(optimizer.getName(), optimizer);
    }

    public static Optimizer getDefaultOptimizer() {
        return optimizers.get(defaultOptimizer);
    }
}
