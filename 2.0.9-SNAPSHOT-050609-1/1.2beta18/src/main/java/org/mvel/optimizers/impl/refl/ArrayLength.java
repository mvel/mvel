package org.mvel.optimizers.impl.refl;

import org.mvel.integration.VariableResolverFactory;

import java.lang.reflect.Array;

/**
 * @author Christopher Brock
 */
public class ArrayLength extends BaseAccessor {

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        if (nextNode != null) {
            return nextNode.getValue(Array.getLength(ctx), elCtx, variableFactory);
        }
        else {
            return Array.getLength(ctx);
        }
    }
}
