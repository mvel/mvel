package org.mvel.optimizers.impl.refl;

import org.mvel.AccessorNode;
import org.mvel.CompileException;
import static org.mvel.DataConversion.convert;
import org.mvel.integration.VariableResolverFactory;

import java.lang.reflect.Method;

public class SetterAccessor implements AccessorNode {
    private AccessorNode nextNode;
    private final Method method;
    
    private Class<? extends Object> targetType;

    private boolean coercionRequired = false;

    public static final Object[] EMPTY = new Object[0];

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        try {
            if (coercionRequired) {
                return method.invoke(ctx, convert(value, targetType));
            }
            else {
                return method.invoke(ctx, value);
            }
        }
        catch (IllegalArgumentException e) {
            if (!coercionRequired) {
                coercionRequired = true;
                return setValue(ctx, elCtx, variableFactory, value);
            }
            throw new CompileException("unable to bind property", e);
        }
        catch (Exception e) {
            throw new CompileException("error binding property", e);
        }

    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory vars) {
        return null;
    }


    public SetterAccessor(Method method) {
        this.method = method;
        this.targetType = method.getParameterTypes()[0];
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
