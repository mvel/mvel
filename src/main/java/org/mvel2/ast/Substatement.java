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

import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.VariableResolverFactory;
import static org.mvel2.util.ParseTools.subCompileExpression;

public class Substatement extends ASTNode {
    private ExecutableStatement statement;

    public Substatement(char[] expr, int fields, ParserContext pCtx) {
        this.name = expr;

        if (((this.fields = fields) & COMPILE_IMMEDIATE) != 0) {
            this.egressType = (this.statement = (ExecutableStatement) subCompileExpression(this.name, pCtx))
                    .getKnownEgressType();
        }
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return statement.getValue(ctx, thisValue, factory);
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return MVEL.eval(this.name, ctx, factory);
    }


    public ExecutableStatement getStatement() {
        return statement;
    }

    public String toString() {
        return statement == null ? "(" + new String(name) + ")" : statement.toString();
    }

}
