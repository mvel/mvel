package org.mvel2.tests;

import org.mvel2.DataConversion;
import org.mvel2.compiler.Accessor;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.tests.core.res.Foo;


public class AccessorBMModel implements Accessor {
    private ExecutableStatement p0;

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        return new String[][]{{"2008-04-01", "2008-05-10"}, {"2007-03-01", "2007-02-12"}};
    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        return ((Foo) ctx).getSampleBean().getMap2().put("blah", DataConversion.convert(value, Integer.class));
    }

    public Class getKnownEgressType() {
        return Object.class;
    }
}
