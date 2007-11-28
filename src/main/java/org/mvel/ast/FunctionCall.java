package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.Accessor;
import org.mvel.integration.VariableResolverFactory;

public class FunctionCall extends ASTNode implements Safe {
    protected Function targetFunction;
    protected Accessor parameters[];

    public FunctionCall(Function targetFunction, Accessor[] parameters) {
        this.targetFunction = targetFunction;
        this.parameters = parameters;
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return targetFunction.call(ctx, thisValue, factory, null);
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return targetFunction.call(ctx, thisValue, factory, null);
    }
}
