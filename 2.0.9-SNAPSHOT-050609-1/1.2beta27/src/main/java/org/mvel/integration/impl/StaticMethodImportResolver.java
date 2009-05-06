package org.mvel.integration.impl;

import org.mvel.integration.VariableResolver;

import java.lang.reflect.Method;

/**
 * @author Christopher Brock
 */
public class StaticMethodImportResolver implements VariableResolver {
    private String name;
    private Method method;


    public StaticMethodImportResolver(String name, Method method) {
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

    public Method getValue() {
        return method;
    }

    public void setValue(Object value) {
        this.method = (Method) value;
    }
}
