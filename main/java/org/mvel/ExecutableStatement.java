package org.mvel;

import org.mvel.integration.VariableResolverFactory;

import java.io.Serializable;

public interface ExecutableStatement extends Serializable, Cloneable {
    public Object getValue(Object staticContext, VariableResolverFactory factory);

    public void setKnownIngressType(Class type);
    public void setKnownEgressType(Class type);

    public Class getKnownIngressType();
    public Class getKnownEgressType();

    public boolean isConvertableIngressEgress();
    public void computeTypeConversionRule();
}
