package org.mvel.optimizers.impl.refl;

import org.mvel.Accessor;
import org.mvel.integration.VariableResolverFactory;

import java.util.ArrayList;
import java.util.Collection;

public class Fold implements Accessor {

    private char[] expr;
    private Accessor collection;
    private Accessor propAccessor;

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        Collection<Object> newCollection = new ArrayList<Object>();

        for (Object item : (Collection) collection.getValue(ctx, elCtx, variableFactory)) {
            if (propAccessor == null) {
                ReflectiveAccessorOptimizer optimizer = new ReflectiveAccessorOptimizer();
                propAccessor = optimizer.optimize(expr, item, item, variableFactory, false);
                newCollection.add(optimizer.getResultOptPass());
            }
            else {
                newCollection.add(propAccessor.getValue(item, item, variableFactory));
            }
        }

        return newCollection;
    }

    public Fold(char[] expr, Accessor collection) {
        this.expr = expr;
        this.collection = collection;
    }
}
