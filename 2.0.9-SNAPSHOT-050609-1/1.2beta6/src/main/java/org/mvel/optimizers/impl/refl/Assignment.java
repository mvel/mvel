package org.mvel.optimizers.impl.refl;

import org.mvel.Accessor;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ParseTools.finalLocalVariableFactory;
import org.mvel.util.ParseTools;

/**
 * @author Christopher Brock
 */
public class Assignment implements Accessor {
    private String var;
    private Accessor expr;


    public Assignment(String var, Accessor expr) {
        this.var = var;
        this.expr = expr;
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        Object o;
        finalLocalVariableFactory(variableFactory).createVariable(var,
                o = expr.getValue(ctx, elCtx, variableFactory));

        assert ParseTools.debug("ASSIGNMENT <<" + var + ">> = <<" + o + ">>");
        return o;
    }
}
    