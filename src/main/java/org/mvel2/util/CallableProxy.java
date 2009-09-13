package org.mvel2.util;

import org.mvel2.integration.VariableResolverFactory;

import java.lang.reflect.InvocationTargetException;

public interface CallableProxy {
    public Object call(Object ctx, Object thisCtx, VariableResolverFactory factory, Object[] parameters);
}
