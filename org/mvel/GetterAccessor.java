package org.mvel;

import java.lang.reflect.Method;
import java.io.Serializable;
import java.util.Map;

public class GetterAccessor implements AccessorNode {
    private AccessorNode nextNode;

    private Method method;

    public static final Object[] EMPTY = new Object[0];

    public Object getValue(Object ctx, Object elCtx, Map vars) throws Exception {
        if (nextNode != null) {
            return nextNode.getValue(method.invoke(ctx, EMPTY), elCtx, vars);
        }
        else {
            return method.invoke(ctx, EMPTY);
        }
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public GetterAccessor() {
    }

    public AccessorNode getNextNode() {
        return nextNode;
    }

    public AccessorNode setNextNode(AccessorNode nextNode) {
        return this.nextNode = nextNode;
    }


    public String toString() {
        return method.getDeclaringClass().getName() + "." + method.getName();
    }
}
