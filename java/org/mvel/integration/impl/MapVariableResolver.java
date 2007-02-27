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

import java.util.Map;

public class MapVariableResolver implements VariableResolver {
    private String name;
    private Class knownType;

    private Map variableMap;

    public MapVariableResolver(Map variableMap, String name) {
        this.variableMap = variableMap;
        this.name = name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setKnownType(Class knownType) {
        this.knownType = knownType;
    }

    public void setVariableMap(Map variableMap) {
        this.variableMap = variableMap;
    }

    public String getName() {
        return name;
    }

    public Class getKnownType() {
        if (knownType == null && variableMap.containsKey(name)) {
            knownType = variableMap.get(name) != null ? variableMap.get(name).getClass() : Object.class;
        }
        else {
            knownType = Object.class;
        }

        return knownType;
    }


    public void setValue(Object value) {
        variableMap.put(name, value);
    }

    public Object getValue() {
        return variableMap.get(name);
    }

    public int getFlags() {
        return 0;
    }
}
