package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.ExecutableStatement;
import org.mvel.MVEL;
import org.mvel.util.ParseTools;
import org.mvel.integration.VariableResolverFactory;

/**
 * @author Christopher Brock
 */
public class IfNode extends ASTNode implements NestedStatement {
    protected char[] block;
    protected ExecutableStatement condition;
    protected ExecutableStatement nestedStatement;

    protected IfNode elseIf;
    protected ExecutableStatement elseBlock;

    public IfNode(char[] condition, char[] block, int fields) {
        super(condition, fields);
        this.block = block;

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            this.condition = (ExecutableStatement) ParseTools.subCompileExpression(condition);
            this.nestedStatement = (ExecutableStatement) ParseTools.subCompileExpression(block);
        }
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if ((Boolean) condition.getValue(ctx, thisValue, factory)) {
            return nestedStatement.getValue(ctx, thisValue, factory);
        }
        else if (elseIf != null) {
            return elseIf.getReducedValueAccelerated(ctx, thisValue, factory);
        }
        else if (elseBlock != null) {
            return elseBlock.getValue(ctx, thisValue, factory);
        }
        else {
            return Void.class;
        }
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if ((Boolean) MVEL.eval(name, ctx, factory)) {
            return MVEL.eval(block, ctx, factory);
        }
        else if (elseIf != null) {
            return elseIf.getReducedValue(ctx, thisValue, factory);
        }
        else if (elseBlock != null) {
            return elseBlock.getValue(ctx, thisValue, factory);
        }
        else {
            return Void.class;
        }
    }

    public ExecutableStatement getNestedStatement() {
        return nestedStatement;
    }

    public void setElseIf(IfNode elseIf) {
        this.elseIf = elseIf;
    }

    public ExecutableStatement getElseBlock() {
        return elseBlock;
    }

    public void setElseBlock(char[] block) {
        elseBlock = (ExecutableStatement) ParseTools.subCompileExpression(block);
    }
}
