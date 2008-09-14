package org.mvel.ast;

import org.mvel.MVEL;
import org.mvel.compiler.ExecutableStatement;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ParseTools.subCompileExpression;

public class Negation extends ASTNode {
    private ExecutableStatement stmt;

    public Negation(char[] name, int fields) {
        this.name = name;

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            this.stmt = (ExecutableStatement) subCompileExpression(name);
        }
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return !((Boolean) stmt.getValue(ctx, thisValue, factory));
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return !((Boolean) MVEL.eval(name, ctx, factory));
    }
}
