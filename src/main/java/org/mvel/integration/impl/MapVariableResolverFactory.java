package org.mvel.integration.impl;

import org.mvel.integration.VariableResolverFactory;
import org.mvel.integration.VariableResolver;

import java.util.Map;
import java.util.HashMap;

public class MapVariableResolverFactory implements VariableResolverFactory {
    /**
     * Holds the instance of the variables.
     */
    private Map<String, Object> variables;

    private Map<String, VariableResolver> variableResolvers;
    private VariableResolverFactory nextFactory;


    public MapVariableResolverFactory(Map<String, Object> variables) {
        this.variables = variables;
        this.variableResolvers = new HashMap<String, VariableResolver>();
    }

    public VariableResolver createVariable(String name, Object value) {
        variables.put(name, value);
        return new MapVariableResolver(variables, name);
    }

    public VariableResolverFactory getNextFactory() {
        return nextFactory;
    }

    public VariableResolverFactory setNextFactory(VariableResolverFactory resolverFactory) {
        return nextFactory = resolverFactory;
    }

    public VariableResolver getVariableResolver(String name) {
        return variableResolvers.containsKey(name) ? variableResolvers.get(name) :
             nextFactory != null ? nextFactory.getVariableResolver(name) : null;
    }


    public boolean isResolveable(String name) {
        if (variableResolvers.containsKey(name)) {
            return true;
        }
        else if (variables.containsKey(name)) {
            variableResolvers.put(name, new MapVariableResolver(variables, name));
            return true;
        }
        else if (nextFactory != null) {
            return nextFactory.isTarget(name);
        }
        return false;
    }

    public boolean isTarget(String name) {
        return variableResolvers.containsKey(name);
    }
}
