package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.PropertyAccessException;
import org.mvel.integration.VariableResolverFactory;

import java.lang.reflect.Field;

public class StaticField extends ASTNode {
    Class clazz;
    Field field;

    public StaticField(Class cls, Field field) {
        super();

        this.clazz = cls;
        this.field = field;
    }


    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        try {
            return field.get(null);
        }
        catch (Exception e) {
            throw new PropertyAccessException("unable to access static field", e);
        }
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return getReducedValueAccelerated(ctx, thisValue, factory);  
    }
}
