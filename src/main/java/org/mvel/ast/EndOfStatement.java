package org.mvel.ast;

import org.mvel.Operator;
import org.mvel.integration.VariableResolverFactory;

/**
 * @author Christopher Brock
 */
public class EndOfStatement extends ASTNode {
    private static final char[] LIT = new char[]{';'};

    public EndOfStatement() {
        super(LIT, OPERATOR);
        this.literal = getOperator();
    }

    public boolean isOperator() {
        return true;
    }


    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return null;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return null;
    }

    public Integer getOperator() {
        return Operator.END_OF_STMT;
    }
}
