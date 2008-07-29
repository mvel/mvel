package org.mvel;

import org.mvel.compiler.Accessor;
import org.mvel.integration.VariableResolverFactory;

public class SetAccessor {
    private Accessor rootAccessor;
    private Accessor setAccessor;

    public SetAccessor() {
    }

    public SetAccessor(Accessor rootAccessor, Accessor setAccessor) {
        this.rootAccessor = rootAccessor;
        this.setAccessor = setAccessor;
    }

    public void setValue(Object ctx, VariableResolverFactory vrf, Object value) {
        if (rootAccessor != null) {
            setAccessor.setValue(rootAccessor.getValue(ctx, ctx, vrf), ctx, vrf, value);
        }
        else {
            setAccessor.setValue(ctx, ctx, vrf, value);
        }
    }
}
