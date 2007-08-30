package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.Accessor;
import org.mvel.EndWithValue;
import org.mvel.MVEL;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.util.ParseTools;

/**
 * @author Christopher Brock
 */
public class ReturnNode extends ASTNode {

    private transient Accessor accessor;

    public ReturnNode(char[] expr, int fields) {
        super(expr, fields);
        accessor = (Accessor) ParseTools.subCompileExpression(expr);
    }


    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (accessor == null) accessor = (Accessor) ParseTools.subCompileExpression(this.name);
        throw new EndWithValue(accessor.getValue(ctx, thisValue, factory));
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        throw new EndWithValue(MVEL.eval(this.name, ctx, factory));
    }
}
