package org.mvel.tests;

import org.mvel.Accessor;
import org.mvel.DataConversion;
import org.mvel.ExecutableStatement;
import org.mvel.integration.VariableResolverFactory;


public class AccessorBMModel implements Accessor {
    private ExecutableStatement p0;


    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        return Math.sqrt(DataConversion.convert(p0.getValue(ctx, variableFactory), Double.class).doubleValue());
    }


}
