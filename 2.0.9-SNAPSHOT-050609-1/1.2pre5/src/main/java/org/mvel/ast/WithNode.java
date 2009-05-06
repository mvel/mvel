package org.mvel.ast;

import org.mvel.CompileException;
import org.mvel.ExecutableStatement;
import static org.mvel.MVEL.setProperty;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.util.ParseTools;
import static org.mvel.util.ParseTools.balancedCapture;
import static org.mvel.util.ParseTools.subset;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Christopher Brock
 */
public class WithNode extends BlockNode implements NestedStatement {
    private ExecutableStatement nestedStatement;
    private ParmValuePair[] withExpressions;
   
    public WithNode(char[] expr, char[] block, int fields) {
        super(expr, fields, block);

        nestedStatement = (ExecutableStatement) ParseTools.subCompileExpression(new String(expr).trim());

        compileWithExpressions();
    }


    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        Object ctxObject = nestedStatement.getValue(ctx, thisValue, factory);

        for (ParmValuePair pvp : withExpressions) {
            if (pvp.getParameter() != null)
                setProperty(ctxObject, pvp.getParameter(), pvp.getStatement().getValue(ctx, thisValue, factory));
            else
                pvp.getStatement().getValue(ctxObject, ctxObject, factory);
        }

        return ctxObject;
    }


    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return getReducedValueAccelerated(ctx, thisValue, factory);
    }

    private void compileWithExpressions() {
        List<ParmValuePair> parms = new ArrayList<ParmValuePair>();

        int start = 0;
        String parm = "";

        for (int i = 0; i < block.length; i++) {
            switch (block[i]) {
                case'{':
                case'[':
                case'(':
                    if ((i = balancedCapture(block, i, block[i])) == -1) {
                        throw new CompileException("unbalanced braces", block, i);
                    }
                    break;

                case'=':
                    parm = new String(block, start, i - start).trim();
                    start = ++i;
                    break;

                case',':
                    if (parm == null) {
                        parms.add(new ParmValuePair(
                                null,
                                (ExecutableStatement) ParseTools.subCompileExpression(subset(block, start, i - start))
                        ));
                        start = ++i;
                    }
                    else {

                        parms.add(new ParmValuePair(
                                parm,
                                (ExecutableStatement) ParseTools.subCompileExpression(subset(block, start, i - start)))
                        );

                        parm = null;
                        start = ++i;
                    }

                    break;
            }
        }

        if (parm != null && start != block.length) {
            parms.add(new ParmValuePair(
                    parm,
                    (ExecutableStatement) ParseTools.subCompileExpression(subset(block, start, block.length - start)))
            );
        }

        parms.toArray(withExpressions = new ParmValuePair[parms.size()]);
    }


    public ExecutableStatement getNestedStatement() {
        return nestedStatement;
    }

    public ParmValuePair[] getWithExpressions() {
        return withExpressions;
    }

    public static final class ParmValuePair {
        private String parameter;
        private ExecutableStatement statement;

        public ParmValuePair() {
        }

        public ParmValuePair(String parameter, ExecutableStatement statement) {
            this.parameter = parameter;
            this.statement = statement;
        }

        public String getParameter() {
            return parameter;
        }

        public void setParameter(String parameter) {
            this.parameter = parameter;
        }

        public ExecutableStatement getStatement() {
            return statement;
        }

        public void setStatement(ExecutableStatement statement) {
            this.statement = statement;
        }
    }


}
