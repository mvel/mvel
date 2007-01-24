package org.mvel.optimizers.impl.refl.collection;

import org.mvel.Accessor;
import org.mvel.integration.VariableResolverFactory;

import java.util.ArrayList;

/**
 * @author Christopher Brock
 */
public class ListCreator implements Accessor {

    public Accessor[] values;

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        ArrayList list = new ArrayList(values.length);
        for (int i = 0; i < values.length; i++) {
            list.add(values[i].getValue(ctx, elCtx, variableFactory));
        }
        return list;
    }


    public ListCreator(Accessor[] values) {
        this.values = values;
    }
}
