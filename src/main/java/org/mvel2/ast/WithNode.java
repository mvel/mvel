/**
 * MVEL 2.0
 * Copyright (C) 2007 The Codehaus
 * Mike Brock, Dhanji Prasanna, John Graham, Mark Proctor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mvel2.ast;

import org.mvel2.CompileException;
import org.mvel2.MVEL;
import static org.mvel2.MVEL.executeSetExpression;
import org.mvel2.Operator;
import org.mvel2.ParserContext;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.VariableResolverFactory;
import static org.mvel2.util.ParseTools.*;
import static org.mvel2.util.ParseTools.parseWithExpressions;
import static org.mvel2.util.PropertyTools.getReturnType;
import org.mvel2.util.StringAppender;
import org.mvel2.util.ParseTools;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Christopher Brock
 */
public class WithNode extends BlockNode implements NestedStatement {
    protected String nestParm;
    protected ExecutableStatement nestedStatement;
    protected ParmValuePair[] withExpressions;

    public WithNode(char[] expr, char[] block, int fields, ParserContext pCtx) {
        nestParm = createStringTrimmed(this.name = expr);
        this.block = block;

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            pCtx.setBlockSymbols(true);

            egressType = (nestedStatement = (ExecutableStatement)
                    subCompileExpression(nestParm.toCharArray(), pCtx)).getKnownEgressType();

            withExpressions = compileWithExpressions(block, nestParm, egressType, pCtx);

            pCtx.setBlockSymbols(false);
        }
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        Object ctxObject = nestedStatement.getValue(ctx, thisValue, factory);
        if (ctxObject == null) throw new CompileException("with-block against null pointer");

        for (ParmValuePair pvp : withExpressions) {
            pvp.eval(ctxObject, factory);
        }

        return ctxObject;
    }


    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        parseWithExpressions(nestParm, block, 0, block.length, ctx = MVEL.eval(name, ctx, factory), factory);
        return ctx;
    }

    public static ParmValuePair[] compileWithExpressions(char[] block, String nestParm, Class egressType, ParserContext pCtx) {
        /**
         *
         * MAINTENANCE NOTE: AN INTERPRETED MODE VERSION OF THIS CODE IS DUPLICATED IN: ParseTools
         *
         */

        List<ParmValuePair> parms = new ArrayList<ParmValuePair>();
        String parm = "";

        int start = 0;
        int end = -1;

        int oper = -1;
        for (int i = 0; i < block.length; i++) {
            switch (block[i]) {
                case '{':
                case '[':
                case '(':
                    i = balancedCapture(block, i, block[i]);
                    continue;

                case '/':
                    if (i < block.length && block[i + 1] == '/') {
                        while (i < block.length && block[i] != '\n') block[i++] = ' ';
                        if (parm == null) start = i;
                    }
                    else if (i < block.length && block[i + 1] == '*') {
                        int len = block.length - 1;
                        while (i < len && !(block[i] == '*' && block[i + 1] == '/')) {
                            block[i++] = ' ';
                        }
                        block[i++] = ' ';
                        block[i++] = ' ';

                        if (parm == null) start = i;
                    }
                    else if (i < block.length && block[i + 1] == '=') {
                        oper = Operator.DIV;
                    }
                    continue;

                case '%':
                case '*':
                case '-':
                case '+':
                    if (i + 1 < block.length && block[i + 1] == '=') {
                        oper = opLookup(block[i]);
                    }
                    continue;


                case '=':
                    parm = createStringTrimmed(block, start, i - start - (oper != -1 ? 1 : 0));
                    start = i + 1;
                    continue;

                case ',':
                    if (end == -1) end = i;

                    if (parm == null || parm.length() == 0) {
                        parms.add(
                                new ParmValuePair(null, (ExecutableStatement)
                                        subCompileExpression(
                                                new StringAppender(nestParm).append('.')
                                                        .append(subset(block, start, end - start)).toChars(), pCtx), egressType, pCtx)
                        );

                        oper = -1;
                        start = ++i;
                    }
                    else {
                        parms.add(new ParmValuePair(
                                parm,
                                (ExecutableStatement) subCompileExpression(
                                        createShortFormOperativeAssignment(nestParm + "." + parm,
                                                subset(block, start, end - start), oper), pCtx
                                )
                                , egressType, pCtx));

                        parm = null;
                        oper = -1;
                        start = ++i;
                    }

                    end = -1;

                    break;
            }
        }


        if (start != (end = block.length)) {
            if (parm == null || "".equals(parm)) {
                parms.add(
                        new ParmValuePair(null, (ExecutableStatement)
                                subCompileExpression(new StringAppender(nestParm).append('.')
                                        .append(subset(block, start, end - start)).toChars(), pCtx), egressType, pCtx)
                );
            }
            else {
                parms.add(new ParmValuePair(
                        parm,
                        (ExecutableStatement) subCompileExpression(
                                createShortFormOperativeAssignment(nestParm + "." + parm, subset(block, start, end - start), oper),
                                pCtx

                        )
                        , egressType, pCtx));
            }
        }

        ParmValuePair[] withExpressions;
        parms.toArray(withExpressions = new ParmValuePair[parms.size()]);
        return withExpressions;
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

        public ParmValuePair(String parameter, ExecutableStatement statement, Class ingressType, ParserContext pCtx) {
            if (parameter != null && parameter.length() != 0) {
                this.setExpression = MVEL.compileSetExpression(parameter,
                        ingressType != null ? getReturnType(ingressType, parameter, pCtx) : Object.class
                        , pCtx);
            }
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

        public void eval(Object ctx, VariableResolverFactory factory) {
            if (setExpression == null) {
                this.statement.getValue(ctx, factory);
            }
            else {
                MVEL.executeSetExpression(setExpression, ctx, factory, this.statement.getValue(ctx, factory));
            }
        }
    }
}
