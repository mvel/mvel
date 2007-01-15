package org.mvel.compiled;

import org.mvel.AccessorNode;
import static org.mvel.DataConversion.convert;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.ExecutableStatement;

import java.lang.reflect.Method;

public class MethodAccessor implements AccessorNode {
    private AccessorNode nextNode;

    private Method method;
    private Class[] parameterTypes;
    private ExecutableStatement[] compiledParameters;
    private boolean coercionNeeded = false;

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory vars) throws Exception {
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

        }
        else {
            if (nextNode != null) {
                return nextNode.getValue(method.invoke(ctx, executeAndCoerce(parameterTypes, elCtx, vars)), elCtx, vars);
            }
            else {
                return method.invoke(ctx, executeAndCoerce(parameterTypes, elCtx, vars));
            }
        }
    }

    private Object[] executeAll(Object ctx, VariableResolverFactory vars) {
        Object[] vals = new Object[compiledParameters.length];
        for (int i = 0; i < vals.length; i++) {
            vals[i] = compiledParameters[i].getValue(ctx, vars);
        }
        return vals;
    }

    private Object[] executeAndCoerce(Class[] target, Object elCtx, VariableResolverFactory vars) {
        Object[] values = new Object[compiledParameters.length];
        for (int i = 0; i < compiledParameters.length; i++) {
            //noinspection unchecked
            values[i] = convert(compiledParameters[i].getValue(elCtx, vars), target[i]);
        }
        return values;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
        this.parameterTypes = this.method.getParameterTypes();
    }


    public ExecutableStatement[] getCompiledParameters() {
        return compiledParameters;
    }

    public void setCompiledParameters(ExecutableStatement[] compiledParameters) {
        this.compiledParameters = compiledParameters;
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


