package org.mvel.optimizers;

import java.io.Serializable;

public interface ExecutableStatement extends Serializable, Cloneable {
    public Object getValue();
}
