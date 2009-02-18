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

package org.mvel2.ast;

import org.mvel2.CompileException;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.util.ParseTools;
import static org.mvel2.util.ParseTools.findClassImportResolverFactory;

/**
 * @author Christopher Brock
 */
public class ImportNode extends ASTNode {
    private Class importClass;
    private boolean packageImport;
    private short offset;

    private static final char[] WC_TEST = new char[]{'.', '*'};

    public ImportNode(char[] expr) {
        this.name = expr;

        if (ParseTools.endsWith(expr, WC_TEST)) {
            packageImport = true;
            offset = (short) ParseTools.findLast(expr, '.');
        }
        else {
            try {
              this.importClass =  Class.forName(new String(expr), true, Thread.currentThread().getContextClassLoader());
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

