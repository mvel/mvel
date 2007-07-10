package org.mvel.integration.impl;

import org.mvel.ParserContext;
import org.mvel.integration.VariableResolver;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Christopher Brock
 */
public class StaticMethodImportResolverFactory extends BaseVariableResolverFactory {

    public StaticMethodImportResolverFactory(ParserContext ctx) {
        this();

        Map<String, Object> imports = ctx.getImports();
        for (String name : imports.keySet()) {
            if (imports.get(name) instanceof Method) {
                createVariable(name, imports.get(name));
            }
        }
    }

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

    public Map<String, Method> getImportedMethods() {
        Map<String, Method> im = new HashMap<String, Method>();
        for (String name : this.variableResolvers.keySet()) {
            im.put(name, (Method) (this.variableResolvers.get(name)).getValue());
        }
        return im;
    }
}
