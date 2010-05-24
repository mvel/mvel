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
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;

import static org.mvel2.util.CompilerTools.expectType;
import static org.mvel2.util.ParseTools.subCompileExpression;
import static org.mvel2.util.ParseTools.subset;

import java.util.HashMap;

/**
 * @author Christopher Brock
 */
public class ForNode extends BlockNode {
    protected String item;

    protected ExecutableStatement initializer;
    protected ExecutableStatement condition;

    protected ExecutableStatement after;

    public ForNode(char[] condition, char[] block, int fields, ParserContext pCtx) {
        boolean varsEscape = handleCond(this.name = condition, fields, pCtx);
        this.compiledBlock = (ExecutableStatement) subCompileExpression(this.block = block, pCtx);

        if ((fields & COMPILE_IMMEDIATE) != 0 && compiledBlock.isEmptyStatement() && !varsEscape) {
            throw new RedundantCodeException();
        }

    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        VariableResolverFactory ctxFactory = new MapVariableResolverFactory(new HashMap<String, Object>(0), factory);
        for (initializer.getValue(ctx, thisValue, ctxFactory); (Boolean) condition.getValue(ctx, thisValue, ctxFactory); after.getValue(ctx, thisValue, ctxFactory)) {
            compiledBlock.getValue(ctx, thisValue, ctxFactory);
        }
        return null;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        for (initializer.getValue(ctx, thisValue, factory = new MapVariableResolverFactory(new HashMap<String, Object>(0), factory)); (Boolean) condition.getValue(ctx, thisValue, factory); after.getValue(ctx, thisValue, factory)) {
            compiledBlock.getValue(ctx, thisValue, factory);
        }

        return null;
    }

    public ForNode() {
        super();    //To change body of overridden methods use File | Settings | File Templates.
    }

    private boolean handleCond(char[] condition, int fields, ParserContext pCtx) {
        int start = 0;
        int cursor = nextCondPart(condition, start, false);
        try {
            if (pCtx != null && (fields & COMPILE_IMMEDIATE) != 0) pCtx = pCtx.createSubcontext().createColoringSubcontext();

            this.initializer = (ExecutableStatement) subCompileExpression(subset(condition, start, cursor - start - 1), pCtx);

            expectType(this.condition = (ExecutableStatement) subCompileExpression(subset(condition, start = cursor,
                    (cursor = nextCondPart(condition, start, false)) - start - 1), pCtx), Boolean.class, ((fields & COMPILE_IMMEDIATE) != 0));

            this.after = (ExecutableStatement)
                    subCompileExpression(subset(condition, start = cursor, (nextCondPart(condition, start, true)) - start), pCtx);

            if (pCtx != null && (fields & COMPILE_IMMEDIATE) != 0 && pCtx.isVariablesEscape()) {
                return true;
            }
        }
        catch (NegativeArraySizeException e) {
            throw new CompileException("wrong syntax; did you mean to use 'foreach'?");
        }
        return false;
    }

    private static int nextCondPart(char[] condition, int cursor, boolean allowEnd) {
        for (; cursor < condition.length; cursor++) {
            if (condition[cursor] == ';') return ++cursor;
        }
        if (!allowEnd) throw new CompileException("expected ;");
        return cursor;
    }
}