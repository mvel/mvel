package org.mvel.ast;

import static org.mvel.AbstractParser.getCurrentThreadParserContext;
import org.mvel.*;
import static org.mvel.MVEL.executeSetExpression;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ParseTools.*;

import java.io.Serializable;
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

        ParserContext pCtx = null;
        if ((fields & COMPILE_IMMEDIATE) != 0) {
            (pCtx = getCurrentThreadParserContext()).setBlockSymbols(true);
        }

        nestedStatement = (ExecutableStatement) subCompileExpression(nestParm = new String(expr).trim());
        compileWithExpressions();

        if (pCtx != null) {
            pCtx.setBlockSymbols(false);
        }
    }

    //todo: performance improvement
    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        Object ctxObject = nestedStatement.getValue(ctx, thisValue, factory);

        for (ParmValuePair pvp : withExpressions) {
            if (pvp.getSetExpression() != null) {
                executeSetExpression(pvp.getSetExpression(), ctxObject, factory, pvp.getStatement().getValue(ctx, thisValue, factory));
            }
            else {
                pvp.getStatement().getValue(ctxObject, ctxObject, factory);
            }
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
        int end = -1;

        int oper = -1;
        for (int i = 0; i < block.length; i++) {
            switch (block[i]) {
                case'{':
                case'[':
                case'(':
                    if ((i = balancedCapture(block, i, block[i])) == -1) {
                        throw new CompileException("unbalanced braces", block, i);
                    }
                    continue;

                case'*':
                    if (i < block.length && block[i + 1] == '=') {
                        oper = Operator.MULT;
                    }
                    continue;

                case'/':
                    if (i < block.length && block[i + 1] == '/') {
                        end = i;
                        while (i < block.length && block[i] != '\n') i++;
                        if (parm == null) start = i;
                    }
                    else if (i < block.length && block[i + 1] == '*') {
                        end = i;

                        while (i < block.length) {
                            switch (block[i++]) {
                                case'*':
                                    if (i < block.length) {
                                        if (block[i] == '/') break;
                                    }
                            }
                        }

                        if (parm == null) start = i;
                    }
                    else if (i < block.length && block[i + 1] == '=') {
                        oper = Operator.DIV;
                    }
                    continue;

                case'-':
                    if (i < block.length && block[i + 1] == '=') {
                        oper = Operator.SUB;
                    }
                    continue;

                case'+':
                    if (i < block.length && block[i + 1] == '=') {
                        oper = Operator.ADD;
                    }
                    continue;

                case'=':
                    parm = new String(block, start, i - start - (oper != -1 ? 1 : 0)).trim();
                    start = ++i;
                    continue;

                case',':
                    if (end == -1) end = i;

                    if (parm == null) {
                        parms.add(new ParmValuePair(
                                null,
                                (ExecutableStatement) subCompileExpression(
                                        createShortFormOperativeAssignment(nestParm + "." + parm, subset(block, start, end - start), oper)
                                )
                        ));

                        oper = -1;
                        start = ++i;
                    }
                    else {
                        parms.add(new ParmValuePair(
                                parm,
                                (ExecutableStatement) subCompileExpression(
                                        createShortFormOperativeAssignment(nestParm + "." + parm, subset(block, start, end - start), oper)
                                )
                        ));

                        parm = null;
                        oper = -1;
                        start = ++i;
                    }

                    end = -1;

                    break;
            }
        }


        if (parm != null && start != (end = block.length)) {
            parms.add(new ParmValuePair(
                    parm,
                    (ExecutableStatement) subCompileExpression(
                            createShortFormOperativeAssignment(nestParm + "." + parm, subset(block, start, end - start), oper)

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

    public static final class ParmValuePair implements Serializable {
        private Serializable setExpression;
        private ExecutableStatement statement;

        public ParmValuePair() {
        }

        public ParmValuePair(String parameter, ExecutableStatement statement) {
            this.setExpression = MVEL.compileSetExpression(parameter);
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
}
