/**
 * MVEL (The MVFLEX Expression Language)
 *
 * Copyright (C) 2007 Christopher Brock, MVFLEX/Valhalla Project and the Codehaus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.mvel.integration.impl;

import org.mvel.integration.VariableResolver;
import org.mvel.integration.VariableResolverFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Use this class to extend you own VariableResolverFactories. It contains most of the baseline implementation needed
 * for the vast majority of integration needs.
 */
public abstract class BaseVariableResolverFactory implements VariableResolverFactory {
    protected Map<String, VariableResolver> variableResolvers;
    protected VariableResolverFactory nextFactory;

    protected String[] indexedVariableNames;
    protected VariableResolver[] indexedVariableResolvers;

    public VariableResolverFactory getNextFactory() {
        return nextFactory;
    }

    public VariableResolverFactory setNextFactory(VariableResolverFactory resolverFactory) {
        return nextFactory = resolverFactory;
    }

    public VariableResolver getVariableResolver(String name) {
        if (isResolveable(name)) {
            if (variableResolvers != null && variableResolvers.containsKey(name)) {
                return variableResolvers.get(name);
            }
            else if (nextFactory != null) {
                return nextFactory.getVariableResolver(name);
            }
        }
        return null;
    }

    public boolean isNextResolveable(String name) {
        return nextFactory != null && nextFactory.isResolveable(name);
    }

    public void appendFactory(VariableResolverFactory resolverFactory) {
        VariableResolverFactory vrf = nextFactory;
        if (vrf == null) {
            nextFactory = resolverFactory;
        }
        else {
            while (vrf.getNextFactory() != null) {
                vrf = vrf.getNextFactory();
            }
            vrf.setNextFactory(nextFactory);
        }
    }

    public void insertFactory(VariableResolverFactory resolverFactory) {
        if (nextFactory == null) {
            nextFactory = resolverFactory;
        }
        else {
            resolverFactory.setNextFactory(resolverFactory);
            nextFactory = resolverFactory;
        }
    }


    public Set<String> getKnownVariables() {
        Set<String> knownVars = new HashSet<String>();

        if (nextFactory == null) {
            if (variableResolvers != null) knownVars.addAll(variableResolvers.keySet());
            return knownVars;
        }
        else {
            if (variableResolvers != null) knownVars.addAll(variableResolvers.keySet());
            knownVars.addAll(nextFactory.getKnownVariables());
            return knownVars;
        }
    }

    public VariableResolver createIndexedVariable(int index, String name, Object value) {
        throw new RuntimeException("cannot create indexed variable: " + name + "(" + index + "). operation not supported by resolver: " + this.getClass().getName());
    }

    public VariableResolver getIndexedVariableResolver(int index) {
        throw new RuntimeException("cannot access indexed variable: " + index + ".  operation not supported by resolver: " + this.getClass().getName());
    }

    public VariableResolver createIndexedVariable(int index, String name, Object value, Class<?> type) {
        throw new RuntimeException("cannot access indexed variable: " + name + "(" + index + ").  operation not supported by resolver.: " + this.getClass().getName());
    }

    public Map<String, VariableResolver> getVariableResolvers() {
        return variableResolvers;
    }

    public void setVariableResolvers(Map<String, VariableResolver> variableResolvers) {
        this.variableResolvers = variableResolvers;
    }

    public String[] getIndexedVariableNames() {
        return indexedVariableNames;
    }

    public void setIndexedVariableNames(String[] indexedVariableNames) {
        this.indexedVariableNames = indexedVariableNames;
    }

    public int variableIndexOf(String name) {
        for (int i = 0; i < indexedVariableNames.length; i++) {
            if (name.equals(indexedVariableNames[i])) return i;
        }
        return -1;
    }

    protected void setIndexedVariable(int index, VariableResolver resolver) {
        if (indexedVariableResolvers == null) {
            indexedVariableResolvers = new VariableResolver[indexedVariableNames.length];
        }
        indexedVariableResolvers[index] = resolver;
    }

    public boolean isIndexedFactory() {
        return false;
    }
}
