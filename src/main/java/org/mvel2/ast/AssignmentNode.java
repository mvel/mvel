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
import static org.mvel2.MVEL.compileSetExpression;
import org.mvel2.ParserContext;
import org.mvel2.PropertyAccessor;
import org.mvel2.compiler.CompiledAccExpression;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.VariableResolverFactory;
import static org.mvel2.util.ArrayTools.findFirst;
import static org.mvel2.util.ParseTools.*;

/**
 * @author Christopher Brock
 */
public class AssignmentNode extends ASTNode implements Assignment {
    private String varName;
    private transient CompiledAccExpression accExpr;

    private char[] indexTarget;
    private String index;

    private char[] stmt;
    private ExecutableStatement statement;
    private boolean col = false;


    public AssignmentNode(char[] expr, int fields, ParserContext pCtx) {
        this.name = expr;
        int assignStart;

        if ((assignStart = find(expr, '=')) != -1) {
            this.varName = createStringTrimmed(expr, 0, assignStart);
            stmt = subset(expr, assignStart + 1);

            if ((fields & COMPILE_IMMEDIATE) != 0) {
                this.egressType = (statement = (ExecutableStatement) subCompileExpression(stmt, pCtx)).getKnownEgressType();
            }

            if (col = ((endOfName = findFirst('[', indexTarget = this.varName.toCharArray())) > 0)) {
                if (((this.fields |= COLLECTION) & COMPILE_IMMEDIATE) != 0) {
                    accExpr = (CompiledAccExpression) compileSetExpression(indexTarget, pCtx);
                }

                this.varName = new String(expr, 0, endOfName);
                index = new String(indexTarget, endOfName, indexTarget.length - endOfName);
            }

            checkNameSafety(this.varName);
        }
        else {
            checkNameSafety(this.varName = new String(expr));
        }

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            pCtx.addVariable(this.varName, egressType);
        }

        this.name = this.varName.toCharArray();
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (accExpr == null) {
            accExpr = (CompiledAccExpression) compileSetExpression(indexTarget);
        }

        if (col) {
            return accExpr.setValue(ctx, thisValue, factory, statement.getValue(ctx, thisValue, factory));
        }
        else if (statement != null) {
            return factory.createVariable(varName, statement.getValue(ctx, thisValue, factory)).getValue();
        }
        else {
            factory.createVariable(varName, null);
            return null;
        }
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        checkNameSafety(varName);

        if (col) {
            PropertyAccessor.set(factory.getVariableResolver(varName).getValue(), factory, index, ctx = MVEL.eval(stmt, ctx, factory));
        }
        else {
            return factory.createVariable(varName, MVEL.eval(stmt, ctx, factory)).getValue();
        }

        return ctx;
    }


    public String getAssignmentVar() {
        return varName;
    }

    public char[] getExpression() {
        return stmt;
    }

    public boolean isNewDeclaration() {
        return false;
    }

    public void setValueStatement(ExecutableStatement stmt) {
        this.statement = stmt;
    }
}
