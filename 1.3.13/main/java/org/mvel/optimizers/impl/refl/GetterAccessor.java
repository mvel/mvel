
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

import org.mvel.AccessorNode;
import org.mvel.CompileException;
import org.mvel.MVEL;
import org.mvel.integration.VariableResolverFactory;

import java.lang.reflect.Method;

public class GetterAccessor implements AccessorNode {
    private AccessorNode nextNode;

    private final Method method;

    public static final Object[] EMPTY = new Object[0];

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory vars) {
        try {
            if (nextNode != null) {
                return nextNode.getValue(method.invoke(ctx, EMPTY), elCtx, vars);
            }
            else {
                return method.invoke(ctx, EMPTY);
            }
        }
        catch (IllegalArgumentException e) {
            /**
             * HACK: Try to access this another way.
             */
            if (nextNode != null) {
                return nextNode.getValue(MVEL.getProperty(method.getName() + "()", ctx), elCtx, vars);
            }
            else {
                return MVEL.getProperty(method.getName() + "()", ctx);
            }
        }
        catch (Exception e) {
            throw new CompileException("cannot invoke getter: " + method.getName()
                    + " [declr.class: " + method.getDeclaringClass().getName() + "; act.class: "
                    + (ctx != null ? ctx.getClass().getName() : "null") + "]", e);                                         
        }
    }


    public GetterAccessor(Method method) {
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


    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        // not implemented
        return null;
    }
}
