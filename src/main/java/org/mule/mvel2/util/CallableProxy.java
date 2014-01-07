package org.mule.mvel2.util;

import org.mule.mvel2.integration.VariableResolverFactory;

public interface CallableProxy {
  public Object call(Object ctx, Object thisCtx, VariableResolverFactory factory, Object[] parameters);
}
