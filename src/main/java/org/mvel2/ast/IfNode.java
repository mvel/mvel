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
import static org.mvel2.MVEL.eval;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;
import static org.mvel2.util.CompilerTools.expectType;
import static org.mvel2.util.ParseTools.subCompileExpression;

import java.util.HashMap;

/**
 * @author Christopher Brock
 */
public class IfNode extends ASTNode implements NestedStatement {
    protected char[] block;
    protected ExecutableStatement condition;
    protected ExecutableStatement nestedStatement;

    protected IfNode elseIf;
    protected ExecutableStatement elseBlock;

    public IfNode(char[] condition, char[] block, int fields, ParserContext pCtx) {
        if ((this.name = condition) == null || condition.length == 0) {
            throw new CompileException("statement expected");
        }
        this.block = block;

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            expectType(this.condition = (ExecutableStatement) subCompileExpression(condition, pCtx), Boolean.class, true);
            this.nestedStatement = (ExecutableStatement) subCompileExpression(block, pCtx);
        }
    }

    public IfNode(ExecutableStatement condition, ExecutableStatement nestedStatement, ExecutableStatement elseBlock) {
        expectType(this.condition = condition, Boolean.class, true);
        this.nestedStatement = nestedStatement;
        this.elseBlock = elseBlock;
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if ((Boolean) condition.getValue(ctx, thisValue, factory)) {
            return nestedStatement.getValue(ctx, thisValue, new MapVariableResolverFactory(new HashMap(0), factory));
        }
        else if (elseIf != null) {
            return elseIf.getReducedValueAccelerated(ctx, thisValue, new MapVariableResolverFactory(new HashMap(0), factory));
        }
        else if (elseBlock != null) {
            return elseBlock.getValue(ctx, thisValue, new MapVariableResolverFactory(new HashMap(0), factory));
        }
        else {
            return null;
        }
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if ((Boolean) eval(name, ctx, factory)) {
            return eval(block, ctx, new MapVariableResolverFactory(new HashMap(0), factory));
        }
        else if (elseIf != null) {
            return elseIf.getReducedValue(ctx, thisValue, new MapVariableResolverFactory(new HashMap(0), factory));
        }
        else if (elseBlock != null) {
            return elseBlock.getValue(ctx, thisValue, new MapVariableResolverFactory(new HashMap(0), factory));
        }
        else {
            return null;
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

    public String toString() {
        return new String(name);
    }
}
