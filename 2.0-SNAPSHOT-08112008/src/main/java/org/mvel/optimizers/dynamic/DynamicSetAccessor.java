package org.mvel.optimizers.dynamic;

import org.mvel.compiler.Accessor;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.SetAccessor;

public class DynamicSetAccessor extends SetAccessor {
    private int runcount;
    private SetAccessor _accessor;

    public DynamicSetAccessor(SetAccessor _accessor) {
        this._accessor = _accessor;
    }

    public void setValue(Object ctx, VariableResolverFactory variableFactory, Object value) {
        runcount++;
        _accessor.setValue(ctx, variableFactory, value);
    }
}