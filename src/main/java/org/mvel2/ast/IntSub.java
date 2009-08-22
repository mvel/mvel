package org.mvel2.ast;

import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.Operator;


public class IntSub extends BinaryOperation implements IntOptimized {
    public IntSub(ASTNode left, ASTNode right) {
        super(Operator.SUB);
        this.left = left;
        this.right = right;
    }

    @Override
    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return ((Integer) left.getReducedValueAccelerated(ctx, thisValue, factory))
                - ((Integer) right.getReducedValueAccelerated(ctx, thisValue, factory));
    }

    @Override
    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return ((Integer) left.getReducedValue(ctx, thisValue, factory))
                - ((Integer) right.getReducedValue(ctx, thisValue, factory));
    }


}
