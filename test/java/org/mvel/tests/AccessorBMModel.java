package org.mvel.tests;

import org.mvel.Accessor;
import org.mvel.DataConversion;
import org.mvel.ExecutableStatement;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.tests.main.res.Foo;

public class AccessorBMModel implements Accessor {
    private ExecutableStatement p0;
    private ExecutableStatement p1;

    public AccessorBMModel() {
    }

    public AccessorBMModel(ExecutableStatement p0, ExecutableStatement p1) {
        this.p0 = p0;
        this.p1 = p1;
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        String.valueOf(ctx);

        return ((Foo)variableFactory.getVariableResolver("foo").getValue())
                .toUC(DataConversion.convert(p0.getValue(elCtx, variableFactory), String.class));
    }
}
