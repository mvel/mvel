package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.ast.cache.CachedListAccessor;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.AccessorOptimizer;
import org.mvel.optimizers.OptimizerFactory;
import static org.mvel.optimizers.OptimizerFactory.SAFE_REFLECTIVE;
import static org.mvel.optimizers.OptimizerFactory.getAccessorCompiler;
import org.mvel.util.CollectionParser;

import java.util.List;

/**
 * @author Christopher Brock
 */
public class InlineCollectionNode extends ASTNode {
    public InlineCollectionNode(char[] expr, int start, int end, int fields) {
        super(expr, start, end, fields | INLINE_COLLECTION);

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            new CollectionParser().parseCollection(name, true);
        }
    }

    public InlineCollectionNode(char[] expr, int fields) {
        super(expr, fields);
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (accessor != null) {
            return accessor.getValue(ctx, thisValue, factory);
        }
        else {
            AccessorOptimizer ao = OptimizerFactory.getDefaultAccessorCompiler();
            accessor = ao.optimizeCollection(name, ctx, thisValue, factory);
            egressType = ao.getEgressType();

            if (ao.isLiteralOnly()) {
                if (egressType == List.class) {
                    List v = (List) accessor.getValue(null, null, null);
                    accessor = new CachedListAccessor(v);
                    return v;
                }
            }

            return accessor.getValue(ctx, thisValue, factory);
        }

    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return getAccessorCompiler(SAFE_REFLECTIVE).optimizeCollection(name, ctx, thisValue, factory).getValue(ctx, thisValue, factory);
    }
}
