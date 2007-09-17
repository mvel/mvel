package org.mvel.optimizers.impl.refl;

import org.mvel.AccessorNode;
import org.mvel.CompileException;
import org.mvel.DataConversion;
import org.mvel.integration.VariableResolverFactory;

import java.lang.reflect.Field;

public class DynamicFieldAccessor implements AccessorNode {
    private AccessorNode nextNode;
    private Field field;


    public DynamicFieldAccessor() {
    }

    public DynamicFieldAccessor(Field field) {
        this.field = field;
    }

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

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
       try {
           field.set(ctx, DataConversion.convert(value, field.getType()));
       }
       catch (Exception e) {
           throw new CompileException("unable to access field", e);
       }
        return value;
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
