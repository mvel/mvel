package org.mvel.integration.impl;

import org.mvel.integration.VariableResolverFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class TypeInjectionResolverFactoryImpl extends MapVariableResolverFactory implements TypeInjectionResolverFactory {
    public TypeInjectionResolverFactoryImpl(Map<String, Object> variables) {
        super(variables);
    }

    public TypeInjectionResolverFactoryImpl(Map<String, Object> variables, VariableResolverFactory nextFactory) {
        super(variables, nextFactory);
    }

    public TypeInjectionResolverFactoryImpl(Map<String, Object> variables, boolean cachingSafe) {
        super(variables, cachingSafe);
    }


    public Set<String> getKnownVariables() {
        Set<String> knownVars = new HashSet<String>();

        if (nextFactory == null) {
            return knownVars;
        }
        else {
            knownVars.addAll(nextFactory.getKnownVariables());
            return knownVars;
        }
    }
}
