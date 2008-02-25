package org.mvel.integration.impl;

import org.mvel.CompileException;
import org.mvel.integration.VariableResolver;
import org.mvel.integration.VariableResolverFactory;

import java.util.HashMap;

public class FunctionVariableResolverFactory extends MapVariableResolverFactory implements LocalVariableResolverFactory {

    
    public FunctionVariableResolverFactory() {
        super(null);
    }

    public FunctionVariableResolverFactory(VariableResolverFactory nextFactory) {
        super(null, nextFactory);
    }

    public FunctionVariableResolverFactory(VariableResolverFactory nextFactory, String[] indexedVariables, Object[] parameters) {
        super(null, nextFactory);
        this.indexedVariableNames = indexedVariables;
        this.indexedVariableResolvers = new VariableResolver[indexedVariableNames.length];
        for (int i = 0; i < parameters.length; i++) {
            this.indexedVariableResolvers[i] = new SimpleValueResolver(parameters[i]);
        }
    }

    public boolean isResolveable(String name) {
        for (String s : indexedVariableNames) {
            if (name.equals(s)) return true;
        }
        return super.isResolveable(name);
    }

    public VariableResolver createVariable(String name, Object value) {
        VariableResolver resolver = getVariableResolver(name);
        if (resolver == null) {
            int idx = increaseRegisterTableSize();
            this.indexedVariableNames[idx] = name;
            resolver = this.indexedVariableResolvers[idx] = new SimpleValueResolver(value);
        }
        else {
            resolver.setValue(value);
        }
        return resolver;
    }

    public VariableResolver createVariable(String name, Object value, Class<?> type) {
        VariableResolver vr = this.variableResolvers != null ? this.variableResolvers.get(name) : null;
        if (vr != null && vr.getType() != null) {
            throw new CompileException("variable already defined within scope: " + vr.getType() + " " + name);
        }
        else {
            //      addResolver(name, vr = new MapVariableResolver(variables, name, type, false));
            return createIndexedVariable(variableIndexOf(name), name, value);
        }
    }

    public VariableResolver createIndexedVariable(int index, String name, Object value) {
        if (indexedVariableResolvers[index] != null) {
            indexedVariableResolvers[index].setValue(value);
        }
        else {
            VariableResolver resolver = new SimpleValueResolver(value);
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
            VariableResolver resolver = new SimpleValueResolver(value);
            resolver.setValue(value);
            indexedVariableResolvers[index] = resolver;
        }
        return indexedVariableResolvers[index];
    }

    public VariableResolver getIndexedVariableResolver(int index) {
        return indexedVariableResolvers[index];
    }

    public VariableResolver getVariableResolver(String name) {
        int idx = variableIndexOf(name);
        if (idx != -1) {
            return indexedVariableResolvers[idx];
        }
        return super.getVariableResolver(name);
    }

    public boolean isIndexedFactory() {
        return true;
    }

    private int increaseRegisterTableSize() {
        String[] oldNames = indexedVariableNames;
        VariableResolver[] oldResolvers = indexedVariableResolvers;

        int newLength = oldNames.length + 1;
        indexedVariableNames = new String[newLength];
        indexedVariableResolvers = new VariableResolver[newLength];

        for (int i = 0; i < oldNames.length; i++) {
            indexedVariableNames[i] = oldNames[i];
            indexedVariableResolvers[i] = oldResolvers[i];
        }

        return newLength - 1;
    }

}