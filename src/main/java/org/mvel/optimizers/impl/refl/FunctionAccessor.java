package org.mvel.optimizers.impl.refl;

import org.mvel.ast.Function;
import org.mvel.integration.VariableResolverFactory;


public class FunctionAccessor extends BaseAccessor {
    private Function function;

    public FunctionAccessor(Function function) {
        this.function = function;
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        return function.call(ctx, elCtx, variableFactory);
    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
