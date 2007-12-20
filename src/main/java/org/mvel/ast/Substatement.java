package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.ExecutableStatement;
import org.mvel.MVEL;
import static org.mvel.MVEL.eval;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ParseTools.subCompileExpression;

public class Substatement extends ASTNode {
    private ExecutableStatement statement;

    public Substatement(char[] expr, int fields) {
        this.name = expr;
        this.fields = fields;

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            this.statement = (ExecutableStatement) subCompileExpression(this.name);
            this.egressType = this.statement.getKnownEgressType();
        }
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return valRet(statement.getValue(ctx, thisValue, factory));
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return valRet(eval(this.name, ctx, factory));
    }

    public ExecutableStatement getStatement() {
        return statement;
    }
}
