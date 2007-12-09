package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.integration.VariableResolverFactory;

/**
 * @author Christopher Brock
 */
public class IndexedVarPropertyNode extends ASTNode {
    private int register;

    public IndexedVarPropertyNode(char[] expr, int fields, int register) {
        super(expr, fields);
        this.register = register;
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return factory.getIndexedVariableResolver(register).getValue();
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return factory.getIndexedVariableResolver(register).getValue();
    }
}