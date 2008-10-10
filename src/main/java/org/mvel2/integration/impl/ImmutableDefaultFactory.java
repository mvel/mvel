package org.mvel2.integration.impl;

import org.mvel2.CompileException;
import org.mvel2.UnresolveablePropertyException;
import org.mvel2.integration.VariableResolver;
import org.mvel2.integration.VariableResolverFactory;

import java.util.Set;

public class ImmutableDefaultFactory implements VariableResolverFactory {
    private void throwError() {
        throw new CompileException("cannot assign variables; no variable resolver factory available.");
    }

    public VariableResolver createVariable(String name, Object value) {
        throwError();
        return null;
    }

    public VariableResolver createIndexedVariable(int index, String name, Object value) {
        throwError();
        return null;
    }

    public VariableResolver createVariable(String name, Object value, Class<?> type) {
        throwError();
        return null;
    }

    public VariableResolver createIndexedVariable(int index, String name, Object value, Class<?> typee) {
        throwError();
        return null;
    }

    public VariableResolver setIndexedVariableResolver(int index, VariableResolver variableResolver) {
        throwError();
        return null;
    }

    public VariableResolverFactory getNextFactory() {
        return null;
    }

    public VariableResolverFactory setNextFactory(VariableResolverFactory resolverFactory) {
        throw new CompileException("cannot chain to this factory");
    }

    public VariableResolver getVariableResolver(String name) {
        throw new UnresolveablePropertyException(name);
    }

    public VariableResolver getIndexedVariableResolver(int index) {
        throwError();
        return null;
    }

    public boolean isTarget(String name) {
        return false;
    }

    public boolean isResolveable(String name) {
        return false;
    }

    public Set<String> getKnownVariables() {
        return null;
    }

    public int variableIndexOf(String name) {
        return -1;
    }

    public boolean isIndexedFactory() {
        return false;
    }
}
