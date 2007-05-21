package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.ExecutableStatement;
import static org.mvel.MVEL.compileExpression;
import org.mvel.integration.VariableResolverFactory;

/**
 * @author Christopher Brock
 */
public class IfNode extends ASTNode {
    protected char[] block;
    protected ExecutableStatement condition;
    protected ExecutableStatement compiledBlock;

    protected IfNode elseIf;
    protected ExecutableStatement elseBlock;

    public IfNode(char[] condition, char[] block, int fields) {
        super(condition, fields);

        this.condition = (ExecutableStatement) compileExpression(this.name = condition);
        this.compiledBlock = (ExecutableStatement) compileExpression(this.block = block);
    }

    public char[] getBlock() {
        return block;
    }

    public void setBlock(char[] block) {
        this.block = block;
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if ((Boolean) condition.getValue(ctx, thisValue, factory)) {
            Object o = compiledBlock.getValue(ctx, thisValue, factory);
            if (o == null) {
                return Void.class;
            }
            else {
                return o;
            }
        }
        else if (elseIf != null)
            return elseIf.getReducedValueAccelerated(ctx, thisValue, factory);
        else if (elseBlock != null)
            return elseBlock.getValue(ctx, thisValue, factory);
        else
            return Void.class;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return getReducedValueAccelerated(ctx, thisValue, factory);
    }


    public ExecutableStatement getCondition() {
        return condition;
    }

    public void setCondition(ExecutableStatement condition) {
        this.condition = condition;
    }

    public ExecutableStatement getCompiledBlock() {
        return compiledBlock;
    }

    public void setCompiledBlock(ExecutableStatement compiledBlock) {
        this.compiledBlock = compiledBlock;
    }

    public IfNode getElseIf() {
        return elseIf;
    }

    public void setElseIf(IfNode elseIf) {
        this.elseIf = elseIf;
    }

    public ExecutableStatement getElseBlock() {
        return elseBlock;
    }

    public void setElseBlock(ExecutableStatement elseBlock) {
        this.elseBlock = elseBlock;
    }

    public void setElseBlock(char[] block) {
        elseBlock = (ExecutableStatement) compileExpression(block);
    }
}
