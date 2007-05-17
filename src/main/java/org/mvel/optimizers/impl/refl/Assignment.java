package org.mvel.optimizers.impl.refl;

import static org.mvel.AbstractParser.isReservedWord;
import org.mvel.Accessor;
import org.mvel.CompileException;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ParseTools.finalLocalVariableFactory;

/**
 * @author Christopher Brock
 */
public class Assignment implements Accessor {
    private String var;
    private Accessor expr;

    public Assignment(String var, Accessor expr) {
        checkName(this.var = var);
        this.expr = expr;
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        Object o;

        variableFactory = finalLocalVariableFactory(variableFactory);
        variableFactory.createVariable(var,
                o = expr.getValue(ctx, elCtx, variableFactory));
        return o;
    }

    private static void checkName(String name) {
        if (isReservedWord(name)) {
            throw new CompileException("reserved word in assignment: " + name);
        }
    }
}
    