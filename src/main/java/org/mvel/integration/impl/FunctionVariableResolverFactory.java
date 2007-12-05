package org.mvel.integration.impl;

import org.mvel.CompileException;
import org.mvel.integration.VariableResolver;
import org.mvel.integration.VariableResolverFactory;

import java.util.HashMap;
import java.util.Map;

public class FunctionVariableResolverFactory extends MapVariableResolverFactory implements LocalVariableResolverFactory {

    public FunctionVariableResolverFactory() {
        super(new HashMap<String, Object>());
    }

    public FunctionVariableResolverFactory(Map<String, Object> variables) {
        super(variables);
    }

    public FunctionVariableResolverFactory(Map<String, Object> variables, VariableResolverFactory nextFactory) {
        super(variables, nextFactory);
    }

    public FunctionVariableResolverFactory(Map<String, Object> variables, boolean cachingSafe) {
        super(variables, cachingSafe);
    }


    public FunctionVariableResolverFactory(VariableResolverFactory nextFactory) {
        super(new HashMap<String, Object>(), nextFactory);
    }

    public VariableResolver createVariable(String name, Object value) {
        VariableResolver vr = this.variableResolvers != null ? this.variableResolvers.get(name) : null;
        if (vr != null) {
            vr.setValue(value);
            return vr;
        }
        else {
            addResolver(name, (vr = new MapVariableResolver(variables, name, false)));
            vr.setValue(value);
            return vr;
        }
    }

    public VariableResolver createVariable(String name, Object value, Class<?> type) {
        VariableResolver vr = this.variableResolvers != null ? this.variableResolvers.get(name) : null;
        if (vr != null && vr.getType() != null) {
            throw new CompileException("variable already defined within scope: " + vr.getType() + " " + name);
        }
        else {
            addResolver(name, vr = new MapVariableResolver(variables, name, type, false));
            createIndexedVariable(variableIndexOf(name), name, value);
            vr.setValue(value);

            return vr;
        }
    }

    public VariableResolver createIndexedVariable(int index, String name, Object value) {
        if (indexedVariableResolvers[index] != null) {
            indexedVariableResolvers[index].setValue(value);
        }
        else {
            VariableResolver resolver = new MapVariableResolver(variables, name, false);
            resolver.setValue(value);
            indexedVariableResolvers[index] = resolver;
        }
        return indexedVariableResolvers[index];
    }

    public VariableResolver createIndexedVariable(int index, String name, Object value, Class<?> type) {
        if (indexedVariableResolvers[index] != null) {
            indexedVariableResolvers[index].setValue(value);
        }
        else {
            VariableResolver resolver = new MapVariableResolver(variables, name, type, false);
            resolver.setValue(value);
            indexedVariableResolvers[index] = resolver;
        }
        return indexedVariableResolvers[index];
    }

    public VariableResolver getIndexedVariableResolver(int index) {
        return indexedVariableResolvers[index];
    }
}