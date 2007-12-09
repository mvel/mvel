package org.mvel.optimizers.impl.refl;

import org.mvel.compiler.Accessor;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.AccessorOptimizer;
import org.mvel.optimizers.OptimizerFactory;

/**
 * @author Christopher Brock
 */
public class Union implements Accessor {
    private Accessor accessor;

    private char[] nextExpr;
    private Accessor nextAccessor;


    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        if (nextAccessor == null) {
            Object o = accessor.getValue(ctx, elCtx, variableFactory);

            AccessorOptimizer ao = OptimizerFactory.getDefaultAccessorCompiler();
            nextAccessor = ao.optimizeAccessor(nextExpr, o, elCtx, variableFactory, false);

            return ao.getResultOptPass();
            //   return nextAccessor.getValue(o, elCtx, variableFactory);
        }
        else {
            return nextAccessor.getValue(accessor.getValue(ctx, elCtx, variableFactory), elCtx, variableFactory);
        }
    }

    public Union(Accessor accessor, char[] nextAccessor) {
        this.accessor = accessor;
        this.nextExpr = nextAccessor;
    }


    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        return null;
    }
}
