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
package org.mvel.optimizers.impl.refl;

import org.mvel.CompileException;
import org.mvel.DataConversion;
import org.mvel.compiler.AccessorNode;
import org.mvel.integration.VariableResolverFactory;

import java.lang.reflect.Method;

public class DynamicSetterAccessor implements AccessorNode {
    private AccessorNode nextNode;

    private final Method method;

    public static final Object[] EMPTY = new Object[0];

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        try {
            return method.invoke(ctx, DataConversion.convert(value, method.getParameterTypes()[0]));
        }
        catch (Exception e) {
            throw new CompileException("error binding property", e);
        }

    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory vars) {
        return null;
    }


    public DynamicSetterAccessor(Method method) {
        this.method = method;
    }

    public Method getMethod() {
        return method;
    }

    public AccessorNode setNextNode(AccessorNode nextNode) {
        return this.nextNode = nextNode;
    }

    public AccessorNode getNextNode() {
        return nextNode;
    }

    public String toString() {
        return method.getDeclaringClass().getName() + "." + method.getName();
    }


}
