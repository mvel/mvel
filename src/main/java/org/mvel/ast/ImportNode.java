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
package org.mvel.ast;

import org.mvel.CompileException;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ParseTools.findClassImportResolverFactory;

/**
 * @author Christopher Brock
 */
public class ImportNode extends ASTNode {
    private Class importClass;
    private boolean packageImport;
    private short offset;

    public ImportNode(char[] expr, int fields) {
        super(expr, fields);

        String name = new String(expr);

        if (name.endsWith(".*")) {
            packageImport = true;
            offset = (short) name.lastIndexOf('.');
        }
        else {
            try {
                this.importClass = Thread.currentThread().getContextClassLoader().loadClass(new String(expr));
            }
            catch (ClassNotFoundException e) {
                throw new CompileException("class not found: " + new String(expr));
            }
        }
    }


    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (!packageImport) {
            return findClassImportResolverFactory(factory).addClass(importClass);
        }
        else {
            findClassImportResolverFactory(factory).addPackageImport(new String(name, 0, (int) offset));
            return null;
        }
        //    return importClass;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return getReducedValueAccelerated(ctx, thisValue, factory);
    }


    public Class getImportClass() {
        return importClass;
    }

    public boolean isPackageImport() {
        return packageImport;
    }

    public void setPackageImport(boolean packageImport) {
        this.packageImport = packageImport;
    }

    public String getPackageImport() {
        return new String(name, 0, offset);
    }
}

