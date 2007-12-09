package org.mvel.optimizers.impl.refl.collection;

import org.mvel.compiler.Accessor;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.util.FastList;

/**
 * @author Christopher Brock
 */
public class ListCreator implements Accessor {

    public Accessor[] values;

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        Object[] template = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            template[i] = values[i].getValue(ctx, elCtx, variableFactory);
        }
        return new FastList(template);
    }


    public ListCreator(Accessor[] values) {
        this.values = values;
    }


    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        return null;
    }
}
