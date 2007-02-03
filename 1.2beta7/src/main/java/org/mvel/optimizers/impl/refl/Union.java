package org.mvel.optimizers.impl.refl;

import org.mvel.Accessor;
import org.mvel.integration.VariableResolverFactory;

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
            nextAccessor = new ReflectiveOptimizer().optimize(nextExpr, o, elCtx, variableFactory, false);
            return nextAccessor.getValue(o, elCtx, variableFactory);
        }
        else {
            return nextAccessor.getValue(accessor.getValue(ctx, elCtx, variableFactory), elCtx, variableFactory);
        }
    }

    public Union(Accessor accessor, char[] nextAccessor) {
        this.accessor = accessor;
        this.nextExpr = nextAccessor;
    }
}
