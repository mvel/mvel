package org.mvel.integration;

public interface VariableResolverFactory {
    public VariableResolver createVariable(String name, Object value);

    public VariableResolverFactory getNextFactory();
    public VariableResolverFactory setNextFactory(VariableResolverFactory resolverFactory);

    public VariableResolver getVariableResolver(String name);

    public boolean isTarget(String name);

    public boolean isResolveable(String name);
}
