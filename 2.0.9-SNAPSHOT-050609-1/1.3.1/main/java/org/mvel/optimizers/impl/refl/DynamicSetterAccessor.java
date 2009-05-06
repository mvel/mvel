package org.mvel.optimizers.impl.refl;

import org.mvel.AccessorNode;
import org.mvel.CompileException;
import org.mvel.DataConversion;
import org.mvel.integration.VariableResolverFactory;

import java.lang.reflect.Method;

public class DynamicSetterAccessor implements AccessorNode {
    private AccessorNode nextNode;

    private final Method method;
    private Class targetType;

    public static final Object[] EMPTY = new Object[0];

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        try {
            return method.invoke(ctx, DataConversion.convert(value, targetType));
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
