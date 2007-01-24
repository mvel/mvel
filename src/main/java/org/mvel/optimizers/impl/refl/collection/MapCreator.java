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

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        FastMap map = new FastMap(keys.length);

        for (int i = 0; i < keys.length; i++) {
            map.put(keys[i].getValue(ctx, elCtx, variableFactory), vals[i].getValue(ctx, elCtx, variableFactory));
        }

        return map;
    }


    public MapCreator(Accessor[] keys, Accessor[] vals) {
        this.keys = keys;
        this.vals = vals;
    }
}
