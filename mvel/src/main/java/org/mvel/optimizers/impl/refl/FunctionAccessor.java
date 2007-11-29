package org.mvel.optimizers.impl.refl;

import org.mvel.Accessor;
import org.mvel.ast.Function;
import org.mvel.integration.VariableResolverFactory;


public class FunctionAccessor extends BaseAccessor {
    private Function function;
    private Accessor[] parameters;


    public FunctionAccessor(Function function, Accessor[] parms) {
        this.function = function;
        this.parameters = parms;
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        Object[] parms = null;

        if (parameters != null && parameters.length != 0) {
            parms = new Object[parameters.length];
            for (int i = 0; i < parms.length; i++) {
                parms[i] = parameters[i].getValue(ctx, elCtx, variableFactory);
            }
        }

        if (nextNode != null) {
            return nextNode.getValue(function.call(ctx, elCtx, variableFactory, parms), elCtx, variableFactory);
        }
        else {
            return function.call(ctx, elCtx, variableFactory, parms);
        }
    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        throw new RuntimeException("can't write to function");
    }
}
