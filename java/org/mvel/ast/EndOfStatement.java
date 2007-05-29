package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.Operator;

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


    public Integer getOperator() {
        return Operator.END_OF_STMT;
    }


    public Object getLiteralValue() {
        return Operator.END_OF_STMT;
    }
}
