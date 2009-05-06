package org.mvel.optimizers.impl.refl;

import org.mvel.EndWithValue;
import org.mvel.ExecutableStatement;
import org.mvel.integration.VariableResolverFactory;

/**
 * @author Christopher Brock
 */
public class Return extends BaseAccessor {

    private ExecutableStatement statement;

    public Return(ExecutableStatement statement) {
        this.statement = statement;
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        throw new EndWithValue(statement.getValue(ctx, elCtx, variableFactory));
    }


    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        return null;
    }
}
