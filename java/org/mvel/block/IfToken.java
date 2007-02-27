package org.mvel.block;

import org.mvel.ExecutableStatement;
import org.mvel.MVEL;
import org.mvel.Token;
import org.mvel.integration.VariableResolverFactory;

/**
 * @author Christopher Brock
 */
public class IfToken extends Token {
    protected char[] block;
    protected ExecutableStatement condition;
    protected ExecutableStatement compiledBlock;

    public IfToken(char[] condition, char[] block, int fields) {
        super(condition, fields);
        this.condition = (ExecutableStatement) MVEL.compileExpression(this.name = condition);
        this.compiledBlock = (ExecutableStatement) MVEL.compileExpression(this.block = block);
    }

    public char[] getBlock() {
        return block;
    }

    public void setBlock(char[] block) {
        this.block = block;
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        switch ((fields & BLOCK_IF)) {
            case BLOCK_IF:
                if ((Boolean) condition.getValue(ctx, thisValue, factory)) {
                    Object o = compiledBlock.getValue(ctx, thisValue, factory);
                    if (o == null) {
                        return Void.class;
                    }
                    else {
                        return o;
                    }
                }
                else {
                    return Void.class;
                }
            default:
                throw new RuntimeException("critical execution error: unknown block state: " + fields);
        }

    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return getReducedValueAccelerated(ctx, thisValue, factory);
    }
}
