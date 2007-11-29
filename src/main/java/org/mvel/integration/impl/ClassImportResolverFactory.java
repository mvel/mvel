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
import org.mvel.util.ParseTools;
import static org.mvel.util.ParseTools.getSimpleClassName;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ClassImportResolverFactory extends BaseVariableResolverFactory {
    private Set<String> packageImports;

    public ClassImportResolverFactory() {
        super();

        variableResolvers = new HashMap<String, VariableResolver>();
    }

    public VariableResolver createVariable(String name, Object value) {
        throw new RuntimeException("unimplemented, please use addClass(Class)");

//        VariableResolver vr = new ClassImportResolver(name.substring(name.lastIndexOf('.')), name);
//        variableResolvers.put(vr.getName(), vr);
//        return vr;
    }


    public VariableResolver createVariable(String name, Object value, Class type) {
        throw new RuntimeException("attempt to created a typed import type.  this is highly redundant and more " +
                "importantly, not implemented.");
    }

    public Class addClass(Class clazz) {
        variableResolvers.put(getSimpleClassName(clazz), new ClassImportResolver(getSimpleClassName(clazz), clazz));
        return clazz;
    }

    public boolean isTarget(String name) {
        return variableResolvers.containsKey(name);
    }

    public boolean isResolveable(String name) {
        if (variableResolvers.containsKey(name) || isNextResolveable(name)) {
            return true;
        }
        else if (packageImports != null) {

            Class loadClass;
            for (String s : packageImports) {

                try {
                    loadClass = ParseTools.createClass(s + "." + name);

                    addClass(loadClass);
                    return true;
                }
                catch (ClassNotFoundException e) {
                    // do nothing;
                }
            }
        }
        return false;
    }

    public void clear() {
        variableResolvers.clear();
    }

    public void setImportedClasses(Map<String, Class> imports) {
        if (imports == null) return;
        for (String var : imports.keySet()) {
            variableResolvers.put(var, new ClassImportResolver(var, imports.get(var)));
        }
    }

    public Map<String, Object> getImportedClasses() {
        Map<String, Object> imports = new HashMap<String, Object>();
        for (String var : variableResolvers.keySet()) {
            imports.put(var, (Class) variableResolvers.get(var).getValue());
        }

        return imports;
    }

    public void addPackageImport(String packageName) {
        if (packageImports == null) packageImports = new HashSet<String>();
        packageImports.add(packageName);
    }
}
