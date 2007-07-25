package org.mvel.optimizers.impl.refl;

import org.mvel.AccessorNode;
import org.mvel.OptimizationFailure;
import org.mvel.PropertyAccessException;
import org.mvel.integration.VariableResolverFactory;

import java.lang.reflect.Field;

public class StaticVarAccessor implements AccessorNode {
    private AccessorNode nextNode;

    Field field;

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory vars) {
        try {
            if (nextNode != null) {
                return nextNode.getValue(field.get(null), elCtx, vars);
            }
            else {
                return field.get(null);
            }
        }
        catch (Exception e) {
            throw new OptimizationFailure("unable to access static field", e);
        }
    }


    public StaticVarAccessor(Field field) {
        this.field = field;
    }

    public AccessorNode getNextNode() {
        return nextNode;
    }

    public AccessorNode setNextNode(AccessorNode nextNode) {
        return this.nextNode = nextNode;
    }


    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        try {
            field.set(null, value);
        }
        catch (Exception e) {
            throw new PropertyAccessException("error accessing static variable", e);
        }
        return value;
    }
}