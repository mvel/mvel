package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.ExecutableStatement;
import static org.mvel.MVEL.eval;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ParseTools.subCompileExpression;

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
            this.condition = (ExecutableStatement) subCompileExpression(condition);
            this.nestedStatement = (ExecutableStatement) subCompileExpression(block);
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
        if ((Boolean) eval(name, ctx, factory)) {
            return eval(block, ctx, factory);
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

    public IfNode setElseIf(IfNode elseIf) {
        return this.elseIf = elseIf;
    }

    public ExecutableStatement getElseBlock() {
        return elseBlock;
    }

    public IfNode setElseBlock(char[] block) {
        elseBlock = (ExecutableStatement) subCompileExpression(block);
        return this;
    }
}
