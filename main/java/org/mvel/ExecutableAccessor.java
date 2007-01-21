package org.mvel;

import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.ExecutableStatement;

public class ExecutableAccessor implements ExecutableStatement {
    private Token accessor;


    public ExecutableAccessor(Token accessor) {
        this.accessor = accessor;
    }


    public Object getValue(Object staticContext, VariableResolverFactory factory) {
        return accessor.getOptimizedValue(staticContext, staticContext, factory);
    }
}


