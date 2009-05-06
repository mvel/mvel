package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.integration.VariableResolverFactory;

/**
 * @author Christopher Brock
 */
public class LiteralNode extends ASTNode {
    private Object literal;

    public LiteralNode(Object literal, Class type) {
        this(literal);
        this.egressType = type;
    }

    public LiteralNode(Object literal) {
        super();
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

    public Object getLiteralValue() {
        return literal;
    }
}
