package org.mvel.integration.impl;

import org.mvel.integration.VariableResolver;

import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * @author Christopher Brock
 */
public class StaticMethodImportResolverFactory extends BaseVariableResolverFactory {
    public StaticMethodImportResolverFactory() {
        this.variableResolvers = new HashMap<String, VariableResolver>();
    }

    public VariableResolver createVariable(String name, Object value) {
        StaticMethodImportResolver methodResolver = new StaticMethodImportResolver(name, (Method) value);
        this.variableResolvers.put(name, methodResolver);
        return methodResolver;
    }

    public VariableResolver createVariable(String name, Object value, Class<?> type) {
        return null;
    }

    public boolean isTarget(String name) {
        return this.variableResolvers.containsKey(name);
    }

    public boolean isResolveable(String name) {
        return isTarget(name) || isNextResolveable(name);
    }
}
