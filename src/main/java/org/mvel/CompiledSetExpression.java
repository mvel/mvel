package org.mvel;

import org.mvel.integration.VariableResolverFactory;
import static org.mvel.optimizers.OptimizerFactory.getAccessorCompiler;

import java.io.Serializable;

public class CompiledSetExpression implements Serializable {
    private char[] expression;
    private transient SetAccessor accessor;

    public CompiledSetExpression(char[] expression) {
        this.expression = expression;
    }

    public void setValue(Object ctx, VariableResolverFactory vrf, Object value) {
        if (accessor == null) {
            accessor = getAccessorCompiler("reflective").optimizeSetAccessor(expression, ctx, ctx, vrf, false, value);
        }
        else {
            accessor.setValue(ctx, vrf, value);
        }
    }
}
