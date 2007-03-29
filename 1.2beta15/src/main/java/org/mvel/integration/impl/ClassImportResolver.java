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

public class ClassImportResolver implements VariableResolver {
    private String name;
    private Class knownType;

    public ClassImportResolver(String fqcn, String name) {
        this.name = name;
        try {
            this.knownType = Class.forName(fqcn);
        }
        catch (Exception e) {
            throw new CompileException("failed import", e);
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setKnownType(Class knownType) {
        this.knownType = knownType;
    }


    public String getName() {
        return name;
    }

    public Class getKnownType() {
        return Class.class;
    }

    public Object getValue() {
        return knownType;
    }

    public int getFlags() {
        return 0;
    }


    public void setValue(Object value) {

    }
}
