package org.mvel.optimizers.impl.refl.collection;

import org.mvel.Accessor;
import org.mvel.util.FastMap;
import org.mvel.integration.VariableResolverFactory;

/**
 * @author Christopher Brock
 */
public class MapCreator implements Accessor {

    private Accessor[] keys;
    private Accessor[] vals;
    private int size;

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        Object[] k = new Object[size];
        Object[] v = new Object[size];
        for (int i = 0; i < size; i++) {
            k[i] = keys[i].getValue(ctx, elCtx, variableFactory);
            v[i] = vals[i].getValue(ctx, elCtx, variableFactory);
        }
        return new FastMap(size, k, v);

//        FastMap map = new FastMap(keys.length);
//
//        for (int i = 0; i < keys.length; i++) {
//            map.put(keys[i].getValue(ctx, elCtx, variableFactory), vals[i].getValue(ctx, elCtx, variableFactory));
//        }
//
//        return map;
    }


    public MapCreator(Accessor[] keys, Accessor[] vals) {
        this.size = (this.keys = keys).length;
        this.vals = vals;
    }
}
