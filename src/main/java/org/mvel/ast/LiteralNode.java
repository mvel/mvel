package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.integration.VariableResolverFactory;

/**
 * @author Christopher Brock
 */
public class LiteralNode extends ASTNode {
    private Object literal;

    public LiteralNode(char[] expr, int fields, Object literal) {
        super(expr, fields);
        if (literal instanceof String) {
            this.literal = ((String) literal).intern();
        }
        else {
            this.literal = literal;
        }
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return literal;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return literal;
    }
}
