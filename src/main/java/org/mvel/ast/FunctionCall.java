package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.ExecutableStatement;
import org.mvel.integration.VariableResolverFactory;

public class FunctionCall extends ASTNode implements Safe {
    protected Function targetFunction;
    protected ExecutableStatement parameters[];

    public FunctionCall(Function targetFunction, ExecutableStatement[] parameters) {
        this.targetFunction = targetFunction;
        this.parameters = parameters;
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return targetFunction.call(ctx, thisValue, factory);
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return targetFunction.call(ctx, thisValue, factory);
    }
}
