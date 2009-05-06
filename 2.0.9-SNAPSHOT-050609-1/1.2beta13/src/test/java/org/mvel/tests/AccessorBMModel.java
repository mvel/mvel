package org.mvel.tests;

import org.mvel.Accessor;
import org.mvel.ExecutableStatement;
import org.mvel.integration.VariableResolverFactory;


public class AccessorBMModel implements Accessor {
    private ExecutableStatement p0;


    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        return new String((String) p0.getValue(elCtx, variableFactory));
    }


}
