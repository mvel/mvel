package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.integration.VariableResolverFactory;

public class OperatorNode extends ASTNode {

    private Integer operator;

    public OperatorNode(Integer operator) {
        assert operator != null;
       this.operator = operator;
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

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return operator;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return operator;
    }
}
