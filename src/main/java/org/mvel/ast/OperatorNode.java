package org.mvel.ast;

import org.mvel.integration.VariableResolverFactory;
import org.mvel.CompileException;
import org.mvel.debug.DebugTools;

public class OperatorNode extends ASTNode {
    private Integer operator;

    public OperatorNode(Integer operator) {
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

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        throw new CompileException("illegal use of operator: " + DebugTools.getOperatorSymbol(operator));
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        throw new CompileException("illegal use of operator: " + DebugTools.getOperatorSymbol(operator));
    }
}
