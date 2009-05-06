package org.mvel.optimizers.impl.refl;

import org.mvel.Accessor;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ParseTools.doOperations;

/**
 * @author Christopher Brock
 */
public class Arithmetic extends BaseAccessor {
    private int operator;
    private Accessor v1;
    private Accessor v2;

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        if (nextNode != null) {
            return nextNode.getValue(
                    doOperations(v1.getValue(ctx, elCtx, variableFactory),
                            operator,
                            v2.getValue(ctx, elCtx, variableFactory)),
                    elCtx, variableFactory);
        }
        return doOperations(v1.getValue(ctx, elCtx, variableFactory),
                operator,
                v2.getValue(ctx, elCtx, variableFactory));
    }

    public Arithmetic(int operator, Accessor v1, Accessor v2) {
        this.operator = operator;
        this.v1 = v1;
        this.v2 = v2;
    }
}
