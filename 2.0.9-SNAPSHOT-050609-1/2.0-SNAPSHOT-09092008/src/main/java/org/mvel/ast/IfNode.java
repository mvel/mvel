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

import static org.mvel.MVEL.eval;
import org.mvel.compiler.ExecutableStatement;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ParseTools.subCompileExpression;

/**
 * @author Christopher Brock
 */
public class IfNode extends ASTNode implements NestedStatement {
    protected char[] block;
    protected ExecutableStatement condition;
    protected ExecutableStatement nestedStatement;

    protected IfNode elseIf;
    protected ExecutableStatement elseBlock;

    public IfNode(char[] condition, char[] block, int fields) {
        this.name = condition;
        this.block = block;

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            this.condition = (ExecutableStatement) subCompileExpression(condition);
            this.nestedStatement = (ExecutableStatement) subCompileExpression(block);
        }
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if ((Boolean) condition.getValue(ctx, thisValue, factory)) {
            return nestedStatement.getValue(ctx, thisValue, factory);
        }
        else if (elseIf != null) {
            return elseIf.getReducedValueAccelerated(ctx, thisValue, factory);
        }
        else if (elseBlock != null) {
            return elseBlock.getValue(ctx, thisValue, factory);
        }
        else {
            return Void.class;
        }
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if ((Boolean) eval(name, ctx, factory)) {
            return eval(block, ctx, factory);
        }
        else if (elseIf != null) {
            return elseIf.getReducedValue(ctx, thisValue, factory);
        }
        else if (elseBlock != null) {
            return elseBlock.getValue(ctx, thisValue, factory);
        }
        else {
            return Void.class;
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
