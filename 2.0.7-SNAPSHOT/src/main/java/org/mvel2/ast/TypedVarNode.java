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

import static org.mvel2.MVEL.eval;
import org.mvel2.ParserContext;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.compiler.AbstractParser;
import org.mvel2.integration.VariableResolverFactory;
import static org.mvel2.util.ParseTools.*;

/**
 * @author Christopher Brock
 */
public class TypedVarNode extends ASTNode implements Assignment {
    private String name;
    private char[] stmt;

    private ExecutableStatement statement;

    public TypedVarNode(String name, Class type) {
        this.name = name;
        this.egressType = type;
    }

    public TypedVarNode(char[] expr, int fields, Class type, ParserContext pCtx) {
        this.egressType = type;
        this.fields = fields;

        int assignStart;
        if ((assignStart = find(super.name = expr, '=')) != -1) {
            checkNameSafety(name = createStringTrimmed(expr, 0, assignStart));

            if (((fields |= ASSIGN) & COMPILE_IMMEDIATE) != 0) {
                statement = (ExecutableStatement) subCompileExpression(stmt = subset(expr, assignStart + 1), pCtx);
            }
            else {
                stmt = subset(expr, assignStart + 1);
            }
        }
        else {
            checkNameSafety(name = new String(expr));
        }

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            pCtx.addVariable(name, egressType, true);
        }
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (statement == null) statement = (ExecutableStatement) subCompileExpression(stmt);
        factory.createVariable(name, ctx = statement.getValue(ctx, thisValue, factory), egressType);
        return ctx;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        factory.createVariable(name, ctx = eval(stmt, thisValue, factory), egressType);
        return ctx;
    }


    public String getName() {
        return name;
    }


    public String getAssignmentVar() {
        return name;
    }

    public char[] getExpression() {
        return stmt;
    }

    public boolean isNewDeclaration() {
        return true;
    }

    public void setValueStatement(ExecutableStatement stmt) {
        this.statement = stmt;
    }
}
