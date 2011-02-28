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

import java.util.HashMap;

import static org.mvel2.util.CompilerTools.expectType;
import static org.mvel2.util.ParseTools.subCompileExpression;
import static org.mvel2.util.ParseTools.subset;

/**
 * @author Christopher Brock
 */
public class ForNode extends BlockNode {
    protected String item;

    protected ExecutableStatement initializer;
    protected ExecutableStatement condition;

    protected ExecutableStatement after;

    public ForNode(char[] expr, int start, int offset, int blockStart, int blockEnd, int fields, ParserContext pCtx) {
        boolean varsEscape = buildForEach(this.expr = expr, this.start = start, this.offset = offset,
                this.blockStart = blockStart, this.blockOffset = blockEnd, fields, pCtx);

        if ((fields & COMPILE_IMMEDIATE) != 0 && compiledBlock.isEmptyStatement() && !varsEscape) {
            throw new RedundantCodeException();
        }
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        VariableResolverFactory ctxFactory = new MapVariableResolverFactory(new HashMap<String, Object>(1), factory);
        for (initializer.getValue(ctx, thisValue, ctxFactory); (Boolean) condition.getValue(ctx, thisValue, ctxFactory); after.getValue(ctx, thisValue, ctxFactory)) {
            compiledBlock.getValue(ctx, thisValue, ctxFactory);
        }
        return null;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        for (initializer.getValue(ctx, thisValue, factory = new MapVariableResolverFactory(new HashMap<String, Object>(1), factory)); (Boolean) condition.getValue(ctx, thisValue, factory); after.getValue(ctx, thisValue, factory)) {
            compiledBlock.getValue(ctx, thisValue, factory);
        }

        return null;
    }

    private boolean buildForEach(char[] condition, int start, int offset, int blockStart, int blockEnd, int fields, ParserContext pCtx) {
        int end = start + offset;
        int cursor = nextCondPart(condition, start, end, false);

        boolean varsEscape = false;

        try {
            ParserContext spCtx = pCtx;
            if (pCtx != null)
                spCtx = pCtx.createSubcontext().createColoringSubcontext();

            this.initializer = (ExecutableStatement) subCompileExpression(condition, start, cursor - start - 1, spCtx);

            expectType(this.condition = (ExecutableStatement) subCompileExpression(condition, start = cursor,
                    (cursor = nextCondPart(condition, start, end, false)) - start - 1, spCtx), Boolean.class, ((fields & COMPILE_IMMEDIATE) != 0));

            this.after = (ExecutableStatement)
                    subCompileExpression(condition, start = cursor, (nextCondPart(condition, start, end, true)) - start, spCtx);

            if (spCtx != null && (fields & COMPILE_IMMEDIATE) != 0 && spCtx.isVariablesEscape()) {
                if (pCtx != spCtx) pCtx.addVariables(spCtx.getVariables());
                varsEscape = true;
            }

            this.compiledBlock = (ExecutableStatement) subCompileExpression(expr, blockStart, blockEnd, spCtx);
      //      if (pCtx != spCtx) pCtx.addVariables(spCtx.getVariables());

        }
        catch (NegativeArraySizeException e) {
            throw new CompileException("wrong syntax; did you mean to use 'foreach'?", expr, start);
        }
        return varsEscape;
    }

    private static int nextCondPart(char[] condition, int cursor, int end, boolean allowEnd) {
        for (; cursor < end; cursor++) {
            if (condition[cursor] == ';') return ++cursor;
        }
        if (!allowEnd) throw new CompileException("expected ;", condition, cursor);
        return cursor;
    }
}