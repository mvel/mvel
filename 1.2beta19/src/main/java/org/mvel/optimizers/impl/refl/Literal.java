package org.mvel.optimizers.impl.refl;

import org.mvel.Accessor;
import org.mvel.integration.VariableResolverFactory;

/**
 * @author Christopher Brock
 */
public class Literal implements Accessor {
    private Object literal;


    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        return literal;
    }

    public Literal(Object literal) {
        this.literal = literal;
    }
}
