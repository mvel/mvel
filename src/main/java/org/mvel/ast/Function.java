/**
 * MVEL (The MVFLEX Expression Language)
 *
 * Copyright (C) 2007 Christopher Brock, MVFLEX/Valhalla Project and the Codehaus
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
 *
 */
package org.mvel.ast;

import org.mvel.ParserContext;
import org.mvel.compiler.AbstractParser;
import org.mvel.compiler.EndWithValue;
import org.mvel.compiler.ExecutableStatement;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.integration.impl.DefaultLocalVariableResolverFactory;
import org.mvel.integration.impl.FunctionVariableResolverFactory;
import org.mvel.util.ParseTools;
import static org.mvel.util.ParseTools.findTypeInjectionResolverFactory;
import static org.mvel.util.ParseTools.parseParameterList;


@SuppressWarnings({"unchecked"})
public class Function extends ASTNode implements Safe {
    protected String name;
    protected ExecutableStatement compiledBlock;

    protected String[] parameters;
    protected int parmNum;

    public Function(String name, char[] parameters, char[] block) {
        this.name = name.trim();
        parmNum = (this.parameters = parseParameterList(parameters, 0, parameters.length)).length;

        ParserContext old = AbstractParser.getCurrentThreadParserContext();

        ParserContext ctx = new ParserContext();
        ctx.addIndexedVariables(this.parameters);
        ctx.declareFunction(this);
        ctx.setIndexAllocation(true);

        /**
         * To prevent the function parameters from being counted as
         * external inputs, we must add them explicitly here.
         */
        for (String s : this.parameters) {
            ctx.addVariable(s, Object.class);
        }

        ParseTools.subCompileExpression(block, ctx);

        ctx.addIndexedVariables(ctx.getVariables().keySet());

        this.compiledBlock = (ExecutableStatement) ParseTools.subCompileExpression(block, ctx);

        AbstractParser.setCurrentThreadParserContext(old);

        this.parameters = (String[]) ctx.getIndexedVariables().toArray(new String[ctx.getIndexedVariables().size()]);

        this.egressType = this.compiledBlock.getKnownEgressType();
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (name.length() != 0) findTypeInjectionResolverFactory(factory).createVariable(name, this);
        return this;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (name.length() != 0) findTypeInjectionResolverFactory(factory).createVariable(name, this);
        return this;
    }

    public Object call(Object ctx, Object thisValue, VariableResolverFactory factory, Object[] parms) {
        try {
            if (parms != null && parms.length != 0) {
                VariableResolverFactory f = new FunctionVariableResolverFactory(factory, parameters, parms);
                return compiledBlock.getValue(ctx, thisValue, f);
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
}
