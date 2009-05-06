package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.Accessor;
import org.mvel.EndWithValue;
import org.mvel.MVEL;
import org.mvel.integration.VariableResolverFactory;

/**
 * @author Christopher Brock
 */
public class ReturnNode extends ASTNode {

    private Accessor accessor;

    public ReturnNode(char[] expr, int fields) {
        super(expr, fields);
        accessor = (Accessor) MVEL.compileExpression(expr);
    }


    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        throw new EndWithValue(accessor.getValue(ctx, thisValue, factory));
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        throw new EndWithValue(accessor.getValue(ctx, thisValue, factory));
    }
}
