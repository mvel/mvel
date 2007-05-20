package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.Accessor;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.OptimizerFactory;

/**
 * @author Christopher Brock
 */
public class InlineCollectionASTNode extends ASTNode {
    private Accessor accessor;


    public InlineCollectionASTNode(char[] expr, int fields) {
        super(expr, fields);
    }


    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        try {
            return accessor.getValue(ctx, thisValue, factory);
        }
        catch (NullPointerException e) {
            if (accessor == null) {
                accessor = OptimizerFactory.getDefaultAccessorCompiler().optimizeCollection(name, ctx, thisValue, factory);
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
