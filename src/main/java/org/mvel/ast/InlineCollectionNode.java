package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.Accessor;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.OptimizerFactory;
import org.mvel.optimizers.AccessorOptimizer;

/**
 * @author Christopher Brock
 */
public class InlineCollectionNode extends ASTNode {
    private transient Accessor accessor;

    public InlineCollectionNode(char[] expr, int start, int end, int fields) {
        super(expr, start, end, fields | INLINE_COLLECTION);

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            //   AccessorOptimizer ao = OptimizerFactory.getAccessorCompiler(OptimizerFactory.SAFE_REFLECTIVE);
            AccessorOptimizer ao = OptimizerFactory.getDefaultAccessorCompiler();
            accessor = ao.optimizeCollection(name, null, null, null);
            egressType = ao.getEgressType();
        }
    }

    public InlineCollectionNode(char[] expr, int fields) {
        super(expr, fields);
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        try {
            return accessor.getValue(ctx, thisValue, factory);
        }
        catch (NullPointerException e) {
            if (accessor == null) {
                AccessorOptimizer ao = OptimizerFactory.getDefaultAccessorCompiler();
                accessor = ao.optimizeCollection(name, ctx, thisValue, factory);
                egressType = ao.getEgressType();
                return accessor.getValue(ctx, thisValue, factory);
            }
            else {
                throw e;
            }
        }

    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return getReducedValueAccelerated(ctx, thisValue, factory);
    }
}
