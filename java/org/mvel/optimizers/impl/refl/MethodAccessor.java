package org.mvel.optimizers.impl.refl;

import org.mvel.AccessorNode;
import org.mvel.CompileException;
import static org.mvel.DataConversion.convert;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.ExecutableStatement;

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

    public AccessorNode getNextNode() {
        return nextNode;
    }

    public AccessorNode setNextNode(AccessorNode nextNode) {
        return this.nextNode = nextNode;
    }
}


