package org.mvel;

import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.ExecutableStatement;
import org.mvel.optimizers.OptimizerFactory;

public class MVEL {
    static boolean THREAD_SAFE = Boolean.getBoolean("mvel.threadsafety");
    static boolean USE_OPTIMIZER = Boolean.getBoolean("mvel.optimizer");

    /**
     * Force MVEL to use thread-safe caching.  This can also be specified enivromentally using the
     * <tt>mvflex.expression.threadsafety</tt> system property.
     *
     * @param threadSafe - true enabled thread-safe caching - false disables thread-safety.
     */
    public static void setThreadSafe(boolean threadSafe) {
        THREAD_SAFE = threadSafe;
        PropertyAccessor.configureFactory();
        Interpreter.configureFactory();
        ExpressionParser.configureFactory();
    }

    public static void setUseOptimizer(boolean optimizer) {
        USE_OPTIMIZER = optimizer;
    }

    public static boolean isThreadSafe() {
        return THREAD_SAFE;
    }

    public static boolean useOptimizer() {
        return USE_OPTIMIZER;
    }

    public static Object execute(CompiledExpression compiledStatement, Object rootContext, VariableResolverFactory factory) {
       return null;
    }


    public static ExecutableStatement optimize(TokenIterator tokenIterator, Object rootContext, VariableResolverFactory factory) {
        return OptimizerFactory.getDefaultOptimizer().optimize(tokenIterator, rootContext, factory);
    }

}
