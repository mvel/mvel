package org.mvel.tests;

import org.mvel.compiler.Accessor;
import org.mvel.compiler.ExecutableStatement;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.tests.core.res.Foo;


public class AccessorBMModel implements Accessor {
    private ExecutableStatement p0;
    private ExecutableStatement p1;

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        p0.setValue(ctx, elCtx, variableFactory, p1.getValue(ctx, elCtx, variableFactory));  
        return ctx;
    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
       // ((List) ctx).set(55, value);
    	return ((String[]) ctx)[(Integer) p0.getValue(ctx, variableFactory)] = (String) value;
    }
}
