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

import org.mvel2.CompileException;
import org.mvel2.integration.VariableResolver;

public class ClassImportResolver implements VariableResolver {
    private String name;
    private Class type;

    public ClassImportResolver(String fqcn, String name) {
        this.name = name;
        try {
            this.type = Class.forName(fqcn, true, Thread.currentThread().getContextClassLoader());
        }
        catch (Exception e) {
            throw new CompileException("failed import", e);
        }
    }

    public ClassImportResolver(String name, Class type) {
        this.name = name;
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStaticType(Class knownType) {
        this.type = knownType;
    }

    public String getName() {
        return name;
    }

    public Class getType() {
        return Class.class;
    }

    public Object getValue() {
        return type;
    }

    public int getFlags() {
        return 0;
    }

    public void setValue(Object value) {
    }
}
