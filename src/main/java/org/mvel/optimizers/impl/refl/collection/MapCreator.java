package org.mvel.optimizers.impl.refl.collection;

import java.util.HashMap;
import java.util.Map;

import org.mvel.Accessor;
import org.mvel.integration.VariableResolverFactory;

/**
 * @author Christopher Brock
 */
public class MapCreator implements Accessor {

    private Accessor[] keys;
    private Accessor[] vals;
    private int size;

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {        
      Map map = new HashMap(size);
      for (int i = size - 1; i != -1; i--) {
          map.put( keys[i].getValue(ctx, elCtx, variableFactory), vals[i].getValue(ctx, elCtx, variableFactory) );
      }
      return map;
    }


    public MapCreator(Accessor[] keys, Accessor[] vals) {
        this.size = (this.keys = keys).length;
        this.vals = vals;
    }


    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        // not implemented
        return null;
    }
}
