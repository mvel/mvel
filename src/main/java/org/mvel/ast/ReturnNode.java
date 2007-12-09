package org.mvel.ast;

import org.mvel.ASTNode;
import static org.mvel.MVEL.eval;
import org.mvel.compiler.Accessor;
import org.mvel.compiler.EndWithValue;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ParseTools.subCompileExpression;

/**
 * @author Christopher Brock
 */
public class ReturnNode extends ASTNode {
    public ReturnNode(char[] expr, int fields) {
        super(expr, fields);
        setAccessor((Accessor) subCompileExpression(expr));
    }


    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (accessor == null) {
            setAccessor((Accessor) subCompileExpression(this.name));
        }
        throw new EndWithValue(accessor.getValue(ctx, thisValue, factory));
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        throw new EndWithValue(eval(this.name, ctx, factory));
    }
}
