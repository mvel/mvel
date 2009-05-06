package org.mvel.optimizers.impl.refl.collection;

import org.mvel.Accessor;
import org.mvel.integration.VariableResolverFactory;

/**
 * @author Christopher Brock
 */
public class ArrayCreator implements Accessor {
    public Accessor[] template;

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
      // return null;
        Object[] newArray = new Object[template.length];

        for (int i = 0; i < newArray.length; i++)
            newArray[i] = template[i].getValue(ctx, elCtx, variableFactory);

        return newArray;
    }

    public ArrayCreator(Accessor[] template) {
        this.template = template;
    }
}
