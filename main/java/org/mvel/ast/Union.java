package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.Accessor;
import org.mvel.PropertyAccessor;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.AccessorOptimizer;
import org.mvel.optimizers.OptimizerFactory;

public class Union extends ASTNode {
    private ASTNode main;
    private transient Accessor accessor;

    public Union(char[] expr, int start, int end, int fields, ASTNode main) {
        super(expr, start, end, fields);
        this.main = main;
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (accessor != null) {
            return accessor.getValue(main.getReducedValueAccelerated(ctx, thisValue, factory), thisValue, factory);
        }
        else {
            AccessorOptimizer o = OptimizerFactory.getDefaultAccessorCompiler();
            accessor = o.optimizeAccessor(name, main.getReducedValueAccelerated(ctx, thisValue, factory), thisValue, factory, false);
            return o.getResultOptPass();
        }
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return PropertyAccessor.get(
                name,
                main.getReducedValue(ctx, thisValue, factory), factory, thisValue);
    }
}
