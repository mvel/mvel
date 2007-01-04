package org.mvel.integration;

import java.util.HashMap;
import java.util.Map;

public class VariableTable {
    public Map<String, VariableResolver> variableResolverTable;

    public VariableTable() {
        this.variableResolverTable = new HashMap<String, VariableResolver>();
    }

    public VariableResolver getVariable(String name) {
        return this.variableResolverTable.get(name);
    }

    public void addVariable(String name, VariableResolver resolver) {
        this.variableResolverTable.put(name, resolver);
    }

    public boolean isTarget(String name) {
        return this.variableResolverTable.containsKey(name);
    }
}
