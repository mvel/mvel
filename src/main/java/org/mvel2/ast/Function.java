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
import org.mvel2.ParserContext;
import org.mvel2.compiler.AbstractParser;
import org.mvel2.compiler.EndWithValue;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.compiler.ExpressionCompiler;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.VariableResolver;
import org.mvel2.integration.impl.DefaultLocalVariableResolverFactory;
import org.mvel2.integration.impl.FunctionVariableResolverFactory;
import static org.mvel2.util.ParseTools.parseParameterDefList;
import static org.mvel2.util.ParseTools.subCompileExpression;

import java.util.Map;


@SuppressWarnings({"unchecked"})
public class Function extends ASTNode implements Safe {
    protected String name;
    protected ExecutableStatement compiledBlock;

    protected String[] parameters;
    protected int parmNum;

    public Function(String name, char[] parameters, char[] block, ParserContext pCtx) {
        if ((this.name = name) == null || name.length() == 0) {
            this.name = null;
        }

        parmNum = (this.parameters = parseParameterDefList(parameters, 0, parameters.length)).length;

        pCtx.declareFunction(this);

        ParserContext ctx = new ParserContext(pCtx.getParserConfiguration());
        ctx.setIndexAllocation(true);

        /**
         * To prevent the function parameters from being counted as
         * external inputs, we must add them explicitly here.
         */
        for (String s : this.parameters) {
            ctx.addVariable(s, Object.class);
            ctx.addIndexedVariable(s);
        }

        /**
         * Compile the expression so we can determine the input-output delta.
         */

        ExpressionCompiler compiler = new ExpressionCompiler(block);
        compiler.setVerifyOnly(true);
        compiler.compile(ctx);

        /**
         * Add globals as inputs
         */
        if (pCtx.getVariables() != null) {
            for (Map.Entry<String, Class> e : pCtx.getVariables().entrySet()) {
                ctx.addInput(e.getKey(), e.getValue());
            }

            ctx.processTables();
        }

        ctx.addIndexedVariables(ctx.getVariables().keySet());
        ctx.getVariables().clear();

        this.compiledBlock = (ExecutableStatement) subCompileExpression(block, ctx);

        AbstractParser.setCurrentThreadParserContext(pCtx);

        this.parameters = new String[ctx.getIndexedVariables().size()];

        int i = 0;
        for (String s : ctx.getIndexedVariables()) {
            this.parameters[i++] = s;
        }

        this.egressType = this.compiledBlock.getKnownEgressType();
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (name != null) {
            if (factory.isResolveable(name)) throw new CompileException("duplicate function: " + name);
            factory.createVariable(name, this);
        }
        return this;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (name != null) {
            if (factory.isResolveable(name)) throw new CompileException("duplicate function: " + name);
            factory.createVariable(name, this);
        }
        return this;
    }

    public Object call(Object ctx, Object thisValue, VariableResolverFactory factory, Object[] parms) {
        try {
            if (parms != null && parms.length != 0) {
                // detect tail recursion
                if (factory instanceof FunctionVariableResolverFactory
                        && ((FunctionVariableResolverFactory) factory).getIndexedVariableResolvers().length == parms.length) {
                    FunctionVariableResolverFactory fvrf = (FunctionVariableResolverFactory) factory;
                    if (fvrf.getFunction().equals(this)) {
                        VariableResolver[] swapVR = fvrf.getIndexedVariableResolvers();
                        fvrf.updateParameters(parms);
                        try {
                            return compiledBlock.getValue(ctx, thisValue, fvrf);
                        }
                        finally {
                            fvrf.setIndexedVariableResolvers(swapVR);
                        }
                    }
                }
                return compiledBlock.getValue(ctx, thisValue, new FunctionVariableResolverFactory(this, factory, parameters, parms));
            }
            else {
                return compiledBlock.getValue(ctx, thisValue, new DefaultLocalVariableResolverFactory(factory));
            }
        }
        catch (EndWithValue end) {
            return end.getValue();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getParameters() {
        return parameters;
    }

    public void setParameters(String[] parameters) {
        this.parameters = parameters;
    }

    public boolean hasParameters() {
        return this.parameters != null && this.parameters.length != 0;
    }

    public void checkArgumentCount(int passing) {
        if (passing != parmNum) {
            throw new CompileException("bad number of arguments in function call: "
                    + passing + " (expected: " + (parmNum == 0 ? "none" : parmNum) + ")");
        }
    }

    public ExecutableStatement getCompiledBlock() {
        return compiledBlock;
    }

    public String toString() {
        return "FunctionDef:" + (name == null ? "Anonymous" : name);
    }
}

