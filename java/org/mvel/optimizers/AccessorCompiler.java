package org.mvel.optimizers;

import org.mvel.Accessor;
import org.mvel.integration.VariableResolverFactory;

public interface AccessorCompiler {
    public Accessor compile(char[] property, Object ctx, Object thisRef, VariableResolverFactory factory, boolean rootThisRef);
    public Object getResultOptPass();
}
