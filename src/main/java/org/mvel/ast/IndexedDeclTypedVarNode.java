package org.mvel.ast;

import org.mvel.integration.VariableResolverFactory;

/**
 * @author Christopher Brock
 */
public class IndexedDeclTypedVarNode extends ASTNode implements Assignment {
    private int register;

    public IndexedDeclTypedVarNode(int register, Class type) {
        this.egressType = type;
        this.register = register;
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        factory.createIndexedVariable(register, null, egressType);
        return ctx;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        factory.createIndexedVariable(register, null, egressType);
        return null;
    }

    public String getAssignmentVar() {
        return null;
    }

    public char[] getExpression() {
        return new char[0];
    }

    public boolean isAssignment() {
        return true;
    }
}