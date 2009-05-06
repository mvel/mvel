package org.mvel.tests;

import org.mvel.Accessor;
import org.mvel.ExecutableStatement;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.tests.main.res.Foo;


public class AccessorBMModel implements Accessor {
    private ExecutableStatement p0;

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        return ((Foo) ctx).getBar().getName();
    }
}
