package org.mvel.optimizers.impl.refl;

import org.mvel.Accessor;
import org.mvel.MVEL;
import static org.mvel.MVEL.compileExpression;
import org.mvel.integration.VariableResolverFactory;

/**
 * @author Christopher Brock
 */
public class DeepAssignment extends BaseAccessor {
    private String property;

    private Accessor baseAccessor;
    private Accessor expr;


    public DeepAssignment(String var, Accessor expr) {
        int mark;
        baseAccessor = (Accessor) compileExpression(var.substring(0, mark = var.indexOf('.')));
        this.property = var.substring(mark + 1);
        this.expr = expr;
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        Object val;
        MVEL.setProperty(baseAccessor.getValue(ctx, elCtx, variableFactory), property,
                val = expr.getValue(ctx, elCtx, variableFactory));

        return val;
    }
}
