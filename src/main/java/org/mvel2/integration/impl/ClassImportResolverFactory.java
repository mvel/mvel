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

import org.mvel2.ParserConfiguration;
import org.mvel2.integration.VariableResolver;
import org.mvel2.integration.VariableResolverFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ClassImportResolverFactory extends BaseVariableResolverFactory {
    private Set<String> packageImports;
    private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    public ClassImportResolverFactory() {
        super();

        variableResolvers = new HashMap<String, VariableResolver>();
    }

    public ClassImportResolverFactory(ParserConfiguration ctx, VariableResolverFactory nextFactory) {
        packageImports = ctx.getPackageImports();
        Map<String, Object> classes = ctx.getImports();
        classLoader = ctx.getClassLoader();

        this.nextFactory = nextFactory;
        this.variableResolvers = new HashMap<String, VariableResolver>();

        for (Map.Entry<String, Object> e : classes.entrySet()) {
            variableResolvers.put(e.getKey(), new SimpleValueResolver(e.getValue()));
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
        variableResolvers.put(clazz.getSimpleName(), new SimpleValueResolver(clazz));
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
                    addClass(classLoader.loadClass(s + "." + name));
                    return true;
                }
                catch (ClassNotFoundException e) {
                    // do nothing;
                }
                catch (NoClassDefFoundError e) {
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
        for (Map.Entry<String, Class> e : imports.entrySet()) {
            variableResolvers.put(e.getKey(), new SimpleValueResolver(e.getValue()));
        }
    }

    public Map<String, Object> getImportedClasses() {
        Map<String, Object> imports = new HashMap<String, Object>();

        for (Map.Entry<String, VariableResolver> e : variableResolvers.entrySet()) {
            imports.put(e.getKey(), e.getValue().getValue());
        }

        return imports;
    }

    public void addPackageImport(String packageName) {
        if (packageImports == null) packageImports = new HashSet<String>();
        packageImports.add(packageName);
    }

    @Override
    public Set<String> getKnownVariables() {
        return nextFactory == null ? new HashSet(0) : nextFactory.getKnownVariables();
    }
}
