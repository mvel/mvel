package org.mvel2.tests;

import java.util.List;
import java.util.Map;

import org.mvel2.DataConversion;
import org.mvel2.compiler.Accessor;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.PropertyHandler;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.GlobalListenerFactory;
import org.mvel2.tests.core.res.Foo;
import org.mvel2.util.StringAppender;


public class AccessorBMModel implements Accessor {
    private ExecutableStatement p0;
    private long foo;
    
    public PropertyHandler nullPropertyHandler;
    public PropertyHandler nullMethodHandler;
    
    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
    		return null;
    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        Foo foo = (Foo) ctx;
    
        if (value == null) {
        	foo.setCountTest(0);
        }
    	
        return value;
    }

    public Class getKnownEgressType() {
        return Object.class;
    }
    
    public void setNullPropertyHandler(PropertyHandler handler) {
    	this.nullPropertyHandler = handler;
    }
    
    public void setNullMethodHandler(PropertyHandler handler) {
    	this.nullMethodHandler = handler;
    }
    
    public String toString() {
    	return "FOFOSLDJALKJ";
    }
}
