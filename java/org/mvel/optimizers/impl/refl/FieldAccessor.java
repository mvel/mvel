package org.mvel.optimizers.impl.refl;

import org.mvel.AccessorNode;
import org.mvel.CompileException;
import org.mvel.integration.VariableResolverFactory;

import java.lang.reflect.Field;

public class FieldAccessor implements AccessorNode {
    private AccessorNode nextNode;

    private Field field;

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory vars) {
        try {
            if (nextNode != null) {
                return nextNode.getValue(field.get(ctx), elCtx, vars);
            }
            else {
                return field.get(ctx);
            }
        }
        catch (Exception e) {
            throw new CompileException("unable to access field", e);
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
