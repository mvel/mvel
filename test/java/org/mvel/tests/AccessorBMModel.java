package org.mvel.tests;

import org.mvel.Accessor;
import org.mvel.ExecutableStatement;
import org.mvel.integration.VariableResolverFactory;

public class AccessorBMModel implements Accessor {
    private ExecutableStatement p0;
    private ExecutableStatement p1;

    private int blah;
    
    public AccessorBMModel() {
    }

    public AccessorBMModel(ExecutableStatement p0, ExecutableStatement p1) {
        this.p0 = p0;
        this.p1 = p1;
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
     //   String.valueOf(ctx);

//        return ((Foo)variableFactory.getVariableResolver("foo").getLiteralValue())
//                .toUC(DataConversion.convert(p0.getLiteralValue(elCtx, variableFactory), String.class));
        
        return blah;
    }
}
