/**
 * MVEL 2.0
 * Copyright (C) 2007 The Codehaus
 * Mike Brock, Dhanji Prasanna, John Graham, Mark Proctor
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
 */

package org.mvel2.integration.impl;

import org.mvel2.UnresolveablePropertyException;
import org.mvel2.integration.VariableResolver;
import org.mvel2.integration.VariableResolverFactory;

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings({"unchecked"})
public class IndexedVariableResolverFactory extends BaseVariableResolverFactory {
    /**
     * Holds the instance of the variables.
     */

    private Object[] values;

    public IndexedVariableResolverFactory(String[] varNames, Object[] values) {
        this.indexedVariableNames = varNames;
        this.values = values;
       // this.nextFactory = new MapVariableResolverFactory();
        this.indexedVariableResolvers = createResolvers(values);
    }

    public IndexedVariableResolverFactory(String[] varNames, Object[] values, VariableResolverFactory nextFactory) {
        this.indexedVariableNames = varNames;
        this.values = values;
        this.nextFactory = new MapVariableResolverFactory();
        this.nextFactory.setNextFactory(nextFactory);
        this.indexedVariableResolvers = createResolvers(values);
    }

    private static VariableResolver[] createResolvers(Object[] values) {
        VariableResolver[] vr = new VariableResolver[values.length];
        for (int i = 0; i < values.length; i++) {
            vr[i] = new IndexVariableResolver(i, values);
        }
        return vr;
    }

    public VariableResolver createIndexedVariable(int index, String name, Object value) {
        throw new RuntimeException("Error: cannot write to factory");
    }

    public VariableResolver getIndexedVariableResolver(int index) {
        return indexedVariableResolvers[index];
    }

    public VariableResolver createVariable(String name, Object value) {
        return nextFactory.createVariable(name, value);
    }

    public VariableResolver createVariable(String name, Object value, Class<?> type) {
        if (nextFactory == null) nextFactory = new MapVariableResolverFactory();
        return nextFactory.createVariable(name, value, type);
    }

    public VariableResolver getVariableResolver(String name) {
        VariableResolver vr = getResolver(name);
        if (vr != null) return vr;
        else if (nextFactory != null) {
            return nextFactory.getVariableResolver(name);
        }

        throw new UnresolveablePropertyException("unable to resolve variable '" + name + "'");
    }


    public boolean isResolveable(String name) {
        return isTarget(name) || (nextFactory != null && nextFactory.isResolveable(name));
    }

    protected VariableResolver addResolver(String name, VariableResolver vr) {
        variableResolvers.put(name, vr);
        return vr;
    }

    private VariableResolver getResolver(String name) {
        for (int i = 0; i < indexedVariableNames.length; i++) {
            if (indexedVariableNames[i].equals(name)) {
                return indexedVariableResolvers[i];
            }
        }
        return null;
    }

    public boolean isTarget(String name) {
        for (int i = 0; i < indexedVariableNames.length; i++) {
            if (indexedVariableNames[i].equals(name)) {
                return true;
            }
        }
        return false;
    }

    public Set<String> getKnownVariables() {

        return new HashSet<String>(0);
    }

    public void clear() {
        // variableResolvers.clear();

    }

    @Override
    public boolean isIndexedFactory() {
        return true;
    }
}