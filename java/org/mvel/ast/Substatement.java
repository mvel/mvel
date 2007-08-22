package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.ExecutableStatement;
import org.mvel.MVEL;
import org.mvel.util.ParseTools;
import org.mvel.integration.VariableResolverFactory;

public class Substatement extends ASTNode {                
    private ExecutableStatement statement;

    public Substatement(char[] expr, int fields) {
        this.name = expr;
        this.fields = fields;

        System.out.println("substatement<<" + new String(expr) + ">>");

        if ((fields & COMPILE_IMMEDIATE) != 0) this.statement = (ExecutableStatement) ParseTools.subCompileExpression(this.name);
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return valRet(statement.getValue(ctx, thisValue, factory));
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return valRet(MVEL.eval(this.name, ctx, factory));
    }
}
