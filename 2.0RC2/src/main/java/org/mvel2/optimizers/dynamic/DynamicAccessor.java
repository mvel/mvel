package org.mvel2.optimizers.dynamic;

import org.mvel2.compiler.Accessor;

public interface DynamicAccessor extends Accessor {
    public void deoptimize();
}
