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
import static org.mvel.util.ParseTools.createClass;
import static org.mvel.util.ParseTools.getSimpleClassName;
import org.mvel.ParserContext;
import org.mvel.ParserConfiguration;

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

    public ClassImportResolverFactory(ParserConfiguration ctx, VariableResolverFactory nextFactory) {
        packageImports = ctx.getPackageImports();
        Map<String, Object> classes = ctx.getImports();

        this.nextFactory = nextFactory;

        this.variableResolvers = new HashMap<String,VariableResolver>();
        for (String s : classes.keySet()) {
            variableResolvers.put(s, new SimpleValueResolver(classes.get(s)));
        }
    }

    public VariableResolver createVariable(String name, Object value) {
        if (nextFactory == null) {
            nextFactory = new MapVariableResolverFactory(new HashMap());
        }

        return nextFactory.createVariable(name, value);
    }


    public VariableResolver createVariable(String name, Object value, Class type) {
        if (nextFactory == null) {
            nextFactory = new MapVariableResolverFactory(new HashMap());
        }

        return nextFactory.createVariable(name, value);
    }

    public Class addClass(Class clazz) {
        variableResolvers.put(getSimpleClassName(clazz), new SimpleValueResolver(clazz));
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

            for (String s : packageImports) {
                try {
                    addClass(createClass(s + "." + name));
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
            variableResolvers.put(var, new SimpleValueResolver(imports.get(var)));
        }
    }

    public Map<String, Object> getImportedClasses() {
        Map<String, Object> imports = new HashMap<String, Object>();
        for (String var : variableResolvers.keySet()) {
            imports.put(var, variableResolvers.get(var).getValue());
        }

        return imports;
    }

    public void addPackageImport(String packageName) {
        if (packageImports == null) packageImports = new HashSet<String>();
        packageImports.add(packageName);
    }
}
