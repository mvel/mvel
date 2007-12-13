package org.mvel.ast;

public class OperatorNode extends ASTNode {
    private Integer operator;

    public OperatorNode(Integer operator) {
        super();

        this.literal = this.operator = operator;
    }

    public boolean isOperator() {
        return true;
    }

    public boolean isOperator(Integer operator) {
        return operator.equals(this.operator);
    }

    public Integer getOperator() {
        return operator;
    }
}
