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

import org.mvel.CompileException;
import org.mvel.integration.VariableResolver;
import org.mvel.integration.VariableResolverFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MapVariableResolverFactory extends BaseVariableResolverFactory {
    /**
     * Holds the instance of the variables.
     */
    protected Map<String, ? extends Object> variables;

    //   private VariableResolverFactory nextFactory;

    private boolean cachingSafe = false;

    public MapVariableResolverFactory(Map<String, ? extends Object> variables) {
        this.variables = variables;
    }


    public MapVariableResolverFactory(Map<String, Object> variables, VariableResolverFactory nextFactory) {
        this.variables = variables;
        this.nextFactory = nextFactory;
    }

    public MapVariableResolverFactory(Map<String, Object> variables, boolean cachingSafe) {
        this.variables = variables;
        this.cachingSafe = cachingSafe;
    }

    public VariableResolver createVariable(String name, Object value) {
        VariableResolver vr = getVariableResolver(name);
        if (vr != null) {
            vr.setValue(value);
            return vr;
        }
        else {
            (vr = new MapVariableResolver(variables, name, cachingSafe)).setValue(value);
            return vr;
        }
    }

    public VariableResolver createVariable(String name, Object value, Class<?> type) {
        VariableResolver vr = getVariableResolver(name);
        if (vr != null && vr.getType() != null) {
            throw new CompileException("variable already defined within scope: " + vr.getType() + " " + name);
        }
        else {
            addResolver(name, vr = new MapVariableResolver(variables, name, type, cachingSafe));
            vr.setValue(value);
            return vr;
        }
    }

    public VariableResolver getVariableResolver(String name) {
        if (variables != null && variables.containsKey(name)) {
            return variableResolvers != null && variableResolvers.containsKey(name) ? variableResolvers.get(name) :
                    new MapVariableResolver(variables, name, cachingSafe);
        }
        else if (nextFactory != null) {
            return nextFactory.getVariableResolver(name);
        }
        return null;
    }

    public boolean isResolveable(String name) {
        if (variableResolvers != null && variableResolvers.containsKey(name)) {
            return true;
        }
        else if (variables != null && variables.containsKey(name)) {
            return true;
        }
        else if (nextFactory != null) {
            return nextFactory.isResolveable(name);
        }
        return false;
    }

    protected void addResolver(String name, VariableResolver vr) {
        if (variableResolvers == null) variableResolvers = new HashMap<String, VariableResolver>();
        variableResolvers.put(name, vr);
    }


    public boolean isTarget(String name) {
        return variableResolvers.containsKey(name);
    }


    public Set<String> getKnownVariables() {
        Set<String> knownVars = new HashSet<String>();

        if (nextFactory == null) {
            if (variables != null) knownVars.addAll(variables.keySet());
            return knownVars;
        }
        else {
            if (variables != null) knownVars.addAll(variables.keySet());
            knownVars.addAll(nextFactory.getKnownVariables());
            return knownVars;
        }
    }
}
