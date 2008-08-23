package org.mvel.optimizers.dynamic;

import org.mvel.compiler.Accessor;

public interface DynamicAccessor extends Accessor {
    public void deoptimize();
}
