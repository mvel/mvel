package org.mvel2.tests;

import java.util.Map;

import org.mvel2.DataConversion;
import org.mvel2.compiler.Accessor;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.PropertyHandler;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.GlobalListenerFactory;
import org.mvel2.tests.core.res.Foo;


public class AccessorBMModel implements Accessor {
    private ExecutableStatement p0;
    private PropertyHandler handler;
    
    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        GlobalListenerFactory.notifyGetListeners(ctx, "foobie", variableFactory);
       
        Object o = ((Foo)ctx).getName();
       if (o == null) {
    	 	return null;
       }
        
    	return o;
    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        GlobalListenerFactory.notifySetListeners(ctx, "foobie", variableFactory, value);
        return value;
    }

    public Class getKnownEgressType() {
        return Object.class;
    }
}
