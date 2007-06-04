package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.Accessor;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.optimizers.OptimizerFactory.getDefaultAccessorCompiler;

/**
 * @author Christopher Brock
 */
public class NewObjectNode extends ASTNode {
    private Accessor newObjectOptimizer;

    public NewObjectNode(char[] expr, int fields) {
        super(expr, fields);
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (newObjectOptimizer == null) {
            newObjectOptimizer = getDefaultAccessorCompiler().optimizeObjectCreation(name, ctx, thisValue, factory);
        }

        return newObjectOptimizer.getValue(ctx, thisValue, factory);
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return getReducedValueAccelerated(ctx, thisValue, factory);
    }


    public Accessor getNewObjectOptimizer() {
        return newObjectOptimizer;
    }
}
