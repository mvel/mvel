package org.mvel2.optimizers.impl.refl;

import org.mvel2.MVEL;
import static org.mvel2.MVEL.executeSetExpression;
import org.mvel2.ParserContext;
import static org.mvel2.compiler.AbstractParser.getCurrentThreadParserContext;
import org.mvel2.compiler.AccessorNode;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.util.ParseTools;
import static org.mvel2.util.ParseTools.subCompileExpression;

import java.io.Serializable;

public class WithAccessor implements AccessorNode {
    private AccessorNode nextNode;

    protected String nestParm;
    protected ExecutableStatement nestedStatement;
    protected ExecutablePairs[] withExpressions;

    public WithAccessor(String property, char[] block) {
        ParserContext pCtx = getCurrentThreadParserContext();
        pCtx.setBlockSymbols(true);

        ParseTools.WithStatementPair[] pvp = ParseTools.parseWithExpressions(property, block);
        withExpressions = new ExecutablePairs[pvp.length];

        for (int i = 0; i < pvp.length; i++) {
            withExpressions[i] = new ExecutablePairs(pvp[i].getParm(),
                    (ExecutableStatement) subCompileExpression(pvp[i].getValue().toCharArray()));
        }

        pCtx.setBlockSymbols(false);
    }

    public AccessorNode getNextNode() {
        return this.nextNode;
    }

    public AccessorNode setNextNode(AccessorNode accessorNode) {
        return this.nextNode = accessorNode;
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        if (this.nextNode == null) {
            return processWith(ctx, elCtx, variableFactory);
        }
        else {
            return this.nextNode.getValue(processWith(ctx, elCtx, variableFactory), elCtx, variableFactory);
        }
    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        return null;
    }

    public Object processWith(Object ctx, Object thisValue, VariableResolverFactory factory) {
        for (ExecutablePairs pvp : withExpressions) {
            if (pvp.getSetExpression() != null) {
                executeSetExpression(pvp.getSetExpression(), ctx, factory, pvp.getStatement().getValue(ctx, thisValue, factory));
            }
            else {
                pvp.getStatement().getValue(ctx, thisValue, factory);
            }
        }

        return ctx;
    }

    public static final class ExecutablePairs implements Serializable {
        private Serializable setExpression;
        private ExecutableStatement statement;

        public ExecutablePairs() {
        }

        public ExecutablePairs(String parameter, ExecutableStatement statement) {
            if (parameter != null && parameter.length() != 0)
                this.setExpression = MVEL.compileSetExpression(parameter, getCurrentThreadParserContext());
            this.statement = statement;
        }

        public Serializable getSetExpression() {
            return setExpression;
        }

        public void setSetExpression(Serializable setExpression) {
            this.setExpression = setExpression;
        }

        public ExecutableStatement getStatement() {
            return statement;
        }

        public void setStatement(ExecutableStatement statement) {
            this.statement = statement;
        }
    }

    public Class getKnownEgressType() {
        return Object.class;
    }
}
