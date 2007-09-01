package org.mvel.integration.impl;

import org.mvel.integration.VariableResolver;
import org.mvel.util.MethodStub;

/**
 * @author Christopher Brock
 */
public class StaticMethodImportResolver implements VariableResolver {
    private String name;
    private MethodStub method;


    public StaticMethodImportResolver(String name, MethodStub method) {
        this.name = name;
        this.method = method;
    }

    public String getName() {
        return name;
    }

    public Class getType() {
        return null;
    }

    public void setStaticType(Class type) {

    }

    public int getFlags() {
        return 0;
    }

    public MethodStub getValue() {
        return method;
    }

    public void setValue(Object value) {
        this.method = (MethodStub) value;
    }
}
