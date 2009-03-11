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
import static org.mvel2.util.ArrayTools.findLast;
import static org.mvel2.util.ParseTools.subset;

import static java.lang.Thread.currentThread;
import java.lang.reflect.Method;
import static java.lang.reflect.Modifier.isStatic;

/**
 * @author Christopher Brock
 */
public class StaticImportNode extends ASTNode {
    private Class declaringClass;
    private String methodName;
    private transient Method method;

    public StaticImportNode(char[] expr) {
        try {
            declaringClass = Class.forName(new String(subset(expr, 0, findLast('.', this.name = expr))),
                    true, currentThread().getContextClassLoader());

            methodName = new String(subset(expr, findLast('.', expr) + 1));

            if (resolveMethod() == null) {
                throw new CompileException("can not find method for static import: "
                        + declaringClass.getName() + "." + methodName);
            }
        }
        catch (Exception e) {
            throw new CompileException("unable to import class", e);
        }
    }

    private Method resolveMethod() {
        for (Method meth : declaringClass.getMethods()) {
            if (isStatic(meth.getModifiers()) && methodName.equals(meth.getName())) {
                return method = meth;
            }
        }
        return null;
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        factory.createVariable(methodName, method == null ? method = resolveMethod() : method);
        return null;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return getReducedValueAccelerated(ctx, thisValue, factory);
    }
}
