package org.mvel;

import org.mvel.integration.VariableResolverFactory;

public interface Accessor {
    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory);
}
