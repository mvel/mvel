package org.mvel.ast.cache;

import org.mvel.Accessor;
import org.mvel.integration.VariableResolverFactory;

import java.util.Arrays;
import java.util.List;


public class CachedListAccessor implements Accessor {
    private Object[] cached;

    public CachedListAccessor(List toCache) {
        cached = new Object[toCache.size()];
        for (int i = 0; i < cached.length; i++)
            cached[i] = toCache.get(i);

    }

    public CachedListAccessor(Object[] cached) {
        this.cached = cached;
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        return Arrays.asList(cached);

    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
