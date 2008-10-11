package org.mvel2.ast;

import org.mvel2.CompileException;
import static org.mvel2.debug.DebugTools.getOperatorSymbol;
import org.mvel2.integration.VariableResolverFactory;

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
        throw new CompileException("illegal use of operator: " + getOperatorSymbol(operator));
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        throw new CompileException("illegal use of operator: " + getOperatorSymbol(operator));
    }
}
