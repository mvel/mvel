package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.AccessorOptimizer;
import org.mvel.optimizers.OptimizerFactory;

/**
 * @author Christopher Brock
 */
public class LiteralDeepPropertyNode extends ASTNode {
    private Object literal;

    public LiteralDeepPropertyNode(char[] expr, int fields, Object literal) {
        super(expr, fields);
        this.literal = literal;
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        try {
            return valRet(accessor.getValue(literal, thisValue, factory));
        }
        catch (NullPointerException e) {
            if (accessor == null) {
                AccessorOptimizer aO = OptimizerFactory.getThreadAccessorOptimizer();
                accessor = aO.optimizeAccessor(name, literal, thisValue, factory, false);

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
