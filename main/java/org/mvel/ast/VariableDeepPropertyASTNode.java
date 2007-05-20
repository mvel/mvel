package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.Accessor;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.AccessorOptimizer;
import org.mvel.optimizers.OptimizerFactory;

/**
 * @author Christopher Brock
 */
public class VariableDeepPropertyASTNode extends ASTNode {
    private Accessor accessor;

    public VariableDeepPropertyASTNode(char[] expr, int fields) {
        super(expr, fields);
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        try {
            return valRet(accessor.getValue(ctx, thisValue, factory));
        }
        catch (NullPointerException e) {
            if (accessor == null) {

                AccessorOptimizer aO = OptimizerFactory.getDefaultAccessorCompiler();
                accessor = aO.optimize(name, ctx, thisValue, factory, false);
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
