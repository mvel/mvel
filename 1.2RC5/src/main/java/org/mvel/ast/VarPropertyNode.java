package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.integration.VariableResolverFactory;

/**
 * @author Christopher Brock
 */
public class VarPropertyNode extends ASTNode {
    private String name;

    public VarPropertyNode(char[] expr, int fields, String name) {
        super(expr, fields);
        this.name = name;
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return factory.getVariableResolver(name).getValue();
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return factory.getVariableResolver(name).getValue();
    }
}
