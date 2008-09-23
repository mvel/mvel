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

@SuppressWarnings({"unchecked"})
public class MapVariableResolverFactory extends BaseVariableResolverFactory {
    /**
     * Holds the instance of the variables.
     */
    protected Map<String, Object> variables;
    private boolean cachingSafe = false;

    public MapVariableResolverFactory(Map variables) {
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
        VariableResolver vr;

        try {
            vr = getVariableResolver(name);
            vr.setValue(value);
            return vr;
        }
        catch (CompileException e) {
            (vr = new MapVariableResolver(variables, name, cachingSafe)).setValue(value);
            return vr;
        }
    //    vr.setValue(value);
    }

    public VariableResolver createVariable(String name, Object value, Class<?> type) {
        VariableResolver vr;
        try {
            vr = getVariableResolver(name);
        }
        catch (CompileException e) {
            vr = null;
        }

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
        assert variables != null;
        if (variables.containsKey(name)) {
            return variableResolvers != null && variableResolvers.containsKey(name) ? variableResolvers.get(name) :
                    new MapVariableResolver(variables, name, cachingSafe);
        }
        else if (nextFactory != null) {
            return nextFactory.getVariableResolver(name);
        }
        throw new CompileException("unable to resolve variable '" + name + "'");
    }


    public boolean isResolveable(String name) {
        return (variableResolvers != null && variableResolvers.containsKey(name))
                || (variables != null && variables.containsKey(name))
                || (nextFactory != null && nextFactory.isResolveable(name));
    }
    
    protected void addResolver(String name, VariableResolver vr) {
        if (variableResolvers == null) variableResolvers = new HashMap<String, VariableResolver>();
        variableResolvers.put(name, vr);
    }


    public boolean isTarget(String name) {
        return variableResolvers != null && variableResolvers.containsKey(name);
    }

    public Set<String> getKnownVariables() {
        if (nextFactory == null) {
            if (variables != null) return new HashSet<String>(variables.keySet());
            return new HashSet<String>(0);
        }
        else {
            if (variables != null) return new HashSet<String>(variables.keySet());
            return new HashSet<String>(0);
        }
    }
}
