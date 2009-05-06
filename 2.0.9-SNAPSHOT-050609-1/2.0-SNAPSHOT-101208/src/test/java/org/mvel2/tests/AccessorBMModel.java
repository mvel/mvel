package org.mvel2.tests;

import org.mvel2.compiler.Accessor;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.VariableResolverFactory;


public class AccessorBMModel implements Accessor {
    private ExecutableStatement p0;

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        return new String[][]{{"2008-04-01", "2008-05-10"}, {"2007-03-01", "2007-02-12"}};
    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        // ((List) ctx).set(55, value);
        return ((String[]) ctx)[(Integer) p0.getValue(ctx, variableFactory)] = (String) value;
    }

    public Class getKnownEgressType() {
        return Object.class;
    }
}
