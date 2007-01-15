package org.mvel.optimizers;

import org.mvel.integration.VariableResolverFactory;

import java.io.Serializable;

public interface ExecutableStatement extends Serializable, Cloneable {
    public Object getValue(Object staticContext, VariableResolverFactory factory);
}
