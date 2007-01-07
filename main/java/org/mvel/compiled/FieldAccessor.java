package org.mvel.compiled;

import org.mvel.AccessorNode;
import org.mvel.PropertyAccessException;
import org.mvel.integration.VariableResolverFactory;

import java.lang.reflect.Field;

public class FieldAccessor implements AccessorNode {
    private AccessorNode nextNode;

    private Field field;

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory vars) throws Exception {
        try {
            if (nextNode != null) {
                return nextNode.getValue(field.get(ctx), elCtx, vars);
            }
            else {
                return field.get(ctx);
            }
        }
        catch (Exception e) {
            throw new PropertyAccessException("failed to access field <<" + field.getDeclaringClass().getName() + "." + field.getName() + ">> in: " + ctx.getClass(), e);
        }
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }

    public AccessorNode getNextNode() {
        return nextNode;
    }

    public AccessorNode setNextNode(AccessorNode nextNode) {
        return this.nextNode = nextNode;
    }
}
