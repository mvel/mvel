
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
import static org.mvel.DataConversion.convert;
import org.mvel.ExecutableStatement;
import org.mvel.integration.VariableResolverFactory;

import java.lang.reflect.Method;

public class MethodAccessor implements AccessorNode {
    private AccessorNode nextNode;

    private Method method;
    private Class[] parameterTypes;
    private ExecutableStatement[] parms;
    private int length;
    private boolean coercionNeeded = false;

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory vars) {
        if (!coercionNeeded) {
            try {
                if (nextNode != null) {
                    return nextNode.getValue(method.invoke(ctx, executeAll(elCtx, vars)), elCtx, vars);
                }
                else {
                    return method.invoke(ctx, executeAll(elCtx, vars));
                }
            }
            catch (IllegalArgumentException e) {
                coercionNeeded = true;
                return getValue(ctx, elCtx, vars);
            }
            catch (Exception e) {
                throw new CompileException("cannot invoke method", e);
            }

        }
        else {
            try {
                if (nextNode != null) {
                    return nextNode.getValue(method.invoke(ctx, executeAndCoerce(parameterTypes, elCtx, vars)), elCtx, vars);
                }
                else {
                    return method.invoke(ctx, executeAndCoerce(parameterTypes, elCtx, vars));
                }
            }
            catch (Exception e) {
                throw new CompileException("cannot invoke method", e);
            }
        }
    }

    private Object[] executeAll(Object ctx, VariableResolverFactory vars) {
        if (length == 0) return GetterAccessor.EMPTY;

        Object[] vals = new Object[length];
        for (int i = 0; i < length; i++) {
            vals[i] = parms[i].getValue(ctx, vars);
        }
        return vals;
    }

    private Object[] executeAndCoerce(Class[] target, Object elCtx, VariableResolverFactory vars) {
        Object[] values = new Object[length];
        for (int i = 0; i < length; i++) {
            //noinspection unchecked
            values[i] = convert(parms[i].getValue(elCtx, vars), target[i]);
        }
        return values;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
        this.length = (this.parameterTypes = this.method.getParameterTypes()).length;
    }


    public ExecutableStatement[] getParms() {
        return parms;
    }

    public void setParms(ExecutableStatement[] parms) {
        this.parms = parms;
    }

    public MethodAccessor() {
    }


    public MethodAccessor(Method method, ExecutableStatement[] parms) {
        this.method = method;
        this.length = (this.parameterTypes = this.method.getParameterTypes()).length;
                
        this.parms = parms;
    }

    public AccessorNode getNextNode() {
        return nextNode;
    }

    public AccessorNode setNextNode(AccessorNode nextNode) {
        return this.nextNode = nextNode;
    }


    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        return null;
    }
}


