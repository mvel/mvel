package org.mvel.ast;

import org.mvel.CompileException;
import org.mvel.ExecutableStatement;
import static org.mvel.MVEL.setProperty;
import org.mvel.Operator;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ParseTools.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Christopher Brock
 */
public class WithNode extends BlockNode implements NestedStatement {
    private String nestParm;
    private ExecutableStatement nestedStatement;
    private ParmValuePair[] withExpressions;

    public WithNode(char[] expr, char[] block, int fields) {
        super(expr, fields, block);

        nestedStatement = (ExecutableStatement) subCompileExpression(nestParm = new String(expr).trim());

        compileWithExpressions();
    }

    //todo: performance improvement
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

        int oper = -1;
        for (int i = 0; i < block.length; i++) {
            switch (block[i]) {
                case'{':
                case'[':
                case'(':
                    if ((i = balancedCapture(block, i, block[i])) == -1) {
                        throw new CompileException("unbalanced braces", block, i);
                    }
                    break;

                case'*':
                    if (i < block.length && block[i + 1] == '=') {
                        oper = Operator.MULT;
                        break;
                    }
                case'/':
                    if (i < block.length && block[i + 1] == '=') {
                        oper = Operator.DIV;
                        break;
                    }
                case'-':
                    if (i < block.length && block[i + 1] == '=') {
                        oper = Operator.SUB;
                        break;
                    }
                case'+':
                    if (i < block.length && block[i + 1] == '=') {
                        oper = Operator.ADD;
                        break;
                    }

                case'=':
                    parm = new String(block, start, i - start - (oper != -1 ? 1 : 0)).trim();
                    start = ++i;
                    break;

                case',':
                    if (parm == null) {
                        parms.add(new ParmValuePair(
                                null,
                                (ExecutableStatement) subCompileExpression(
                                        createShortFormOperativeAssignment(nestParm + "." + parm, subset(block, start, i - start), oper)
                                )
                        ));

                        oper = -1;
                        start = ++i;
                    }
                    else {

                        parms.add(new ParmValuePair(
                                parm,
                                (ExecutableStatement) subCompileExpression(
                                        createShortFormOperativeAssignment(nestParm + "." + parm, subset(block, start, i - start), oper)
                                )
                        ));

                        parm = null;
                        oper = -1;
                        start = ++i;
                    }

                    break;
            }
        }

        if (parm != null && start != block.length) {
            parms.add(new ParmValuePair(
                    parm,
                    (ExecutableStatement) subCompileExpression(
                            createShortFormOperativeAssignment(nestParm + "." + parm, subset(block, start, block.length - start), oper)

                    )
            ));
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
