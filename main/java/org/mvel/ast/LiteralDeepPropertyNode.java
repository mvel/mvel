package org.mvel.ast;

import org.mvel.ASTNode;
import static org.mvel.PropertyAccessor.get;
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
        if (accessor != null) {
            return valRet(accessor.getValue(literal, thisValue, factory));
        }
        else {
            AccessorOptimizer aO = OptimizerFactory.getThreadAccessorOptimizer();
            accessor = aO.optimizeAccessor(name, literal, thisValue, factory, false);

            return valRet(aO.getResultOptPass());
        }
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return get(name, literal, factory, thisValue);
    }
}
