package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.Accessor;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.AccessorOptimizer;
import org.mvel.optimizers.OptimizerFactory;

/**
 * @author Christopher Brock
 */
public class VariableDeepPropertyNode extends ASTNode {
    private Accessor accessor;

    public VariableDeepPropertyNode(char[] expr, int fields) {
        super(expr, fields);
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        try {
            return valRet(accessor.getValue(ctx, thisValue, factory));
        }
        catch (NullPointerException e) {
            if (accessor == null) {

                AccessorOptimizer aO = OptimizerFactory.getThreadAccessorOptimizer();
                accessor = aO.optimizeAccessor(name, ctx, thisValue, factory, false);
                return valRet(aO.getResultOptPass());
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
