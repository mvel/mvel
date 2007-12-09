package org.mvel.ast;

import org.mvel.compiler.ExecutableStatement;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ParseTools.subCompileExpression;

/**
 * @author Christopher Brock
 */
public class WhileNode extends BlockNode {
    protected String item;

    protected char[] cond;
    protected ExecutableStatement condition;
    protected ExecutableStatement compiledBlock;

    public WhileNode(char[] condition, char[] block, int fields) {
        super(condition, fields);

        this.condition = (ExecutableStatement) subCompileExpression(this.cond = condition);
        this.compiledBlock = (ExecutableStatement) subCompileExpression(this.block = block);
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        //   Object ret = null;


        while ((Boolean) condition.getValue(ctx, thisValue, factory)) {
            compiledBlock.getValue(ctx, thisValue, factory);
        }

        return null;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {

        while ((Boolean) condition.getValue(ctx, thisValue, factory)) {
            compiledBlock.getValue(ctx, thisValue, factory);
        }
        return null;
    }

}