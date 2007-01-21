package org.mvel.optimizers.impl.refl;

import org.mvel.AccessorNode;
import org.mvel.CompileException;
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
        catch (Exception e) {
            throw new CompileException("cannot invoke getter", e);

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
}
