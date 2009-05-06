package org.mvel.ast;

import org.mvel.CompileException;
import org.mvel.MVEL;
import org.mvel.compiler.ExecutableStatement;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ParseTools.subCompileExpression;

public class Negation extends ASTNode {
    private ExecutableStatement stmt;


    public Negation(char[] name, int fields) {
        this.name = name;

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            if ((this.stmt = (ExecutableStatement) subCompileExpression(name)).getKnownEgressType() != null
                    && !stmt.getKnownEgressType().isAssignableFrom(Boolean.class)) {
                throw new CompileException("negation operator cannot be applied to non-boolean type");
            }
        }
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return !((Boolean) stmt.getValue(ctx, thisValue, factory));
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        try {
            return !((Boolean) MVEL.eval(name, ctx, factory));
        }
        catch (ClassCastException e) {
            throw new CompileException("negation operator applied to non-boolean expression");
        }
    }

    public Class getEgressType() {
        return Boolean.class;
    }

}
