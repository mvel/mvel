package org.mvel.compiled;

import org.mvel.AccessorNode;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.io.Serializable;
import java.util.Map;

public class FieldAccessor implements AccessorNode {
    private AccessorNode nextNode;

    private Field field;

    public Object getValue(Object ctx, Object elCtx, Map vars) throws Exception {
        if (nextNode != null) {
            return nextNode.getValue(field.get(ctx), elCtx, vars);
        }
        else {
            return field.get(ctx);
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
