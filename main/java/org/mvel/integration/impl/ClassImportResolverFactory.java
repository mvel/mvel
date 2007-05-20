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

import java.util.HashMap;
import java.util.Map;

public class ClassImportResolverFactory extends BaseVariableResolverFactory {
    private Map<String, VariableResolver> importsTable = new HashMap<String, VariableResolver>();

    public VariableResolver createVariable(String name, Object value) {
        VariableResolver vr = new ClassImportResolver(name.substring(name.lastIndexOf('.')), name);
        importsTable.put(vr.getName(), vr);
        return vr;
    }


    public VariableResolver createVariable(String name, Object value, Class type) {
        throw new RuntimeException("attempt to created a typed import type.  this is highly redundant and more " +
                "importantly, not implemented.");
    }

    public void addClass(Class clazz) {
        importsTable.put(clazz.getSimpleName(), new ClassImportResolver(clazz.getSimpleName(), clazz));
    }

    public boolean isTarget(String name) {
        return importsTable.containsKey(name);
    }

    public boolean isResolveable(String name) {
        return importsTable.containsKey(name) || nextFactory.isResolveable(name);
    }
}
