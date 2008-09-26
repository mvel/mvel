package org.mvel.integration.impl;

import org.mvel.ParserContext;
import org.mvel.integration.VariableResolver;
import org.mvel.util.MethodStub;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Christopher Brock
 */
public class StaticMethodImportResolverFactory extends BaseVariableResolverFactory {
    public StaticMethodImportResolverFactory(ParserContext ctx) {
        this.variableResolvers = new HashMap<String, VariableResolver>();
        for (Map.Entry<String,Object> entry : ctx.getImports().entrySet()) {
            if (entry.getValue() instanceof Method) {
                createVariable(entry.getKey(), entry.getValue());
            }
        }
    }

    public StaticMethodImportResolverFactory() {
        this.variableResolvers = new HashMap<String, VariableResolver>();
    }

    public VariableResolver createVariable(String name, Object value) {
        if (value instanceof Method) value = new MethodStub((Method) value);

        StaticMethodImportResolver methodResolver = new StaticMethodImportResolver(name, (MethodStub) value);
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

    public Map<String, Method> getImportedMethods() {
        Map<String, Method> im = new HashMap<String, Method>();
        for (Map.Entry<String, VariableResolver> e : this.variableResolvers.entrySet()) {
            im.put(e.getKey(), (Method) e.getValue().getValue());
        }
        return im;
    }

}
