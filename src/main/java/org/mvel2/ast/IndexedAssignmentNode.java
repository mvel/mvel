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
import org.mvel2.compiler.CompiledAccExpression;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.VariableResolverFactory;
import static org.mvel2.util.ArrayTools.findFirst;
import static org.mvel2.util.ParseTools.*;

/**
 * @author Christopher Brock
 */
public class IndexedAssignmentNode extends ASTNode implements Assignment {
    private String name;
    private int register;
    private transient CompiledAccExpression accExpr;

    private char[] indexTarget;
    private char[] index;

    private char[] stmt;
    private ExecutableStatement statement;

    private boolean col = false;

    public IndexedAssignmentNode(char[] expr, int fields, int operation, String name, int register, ParserContext pCtx) {
        super.name = expr;
        this.register = register;

        int assignStart;

        if (operation != -1) {
            checkNameSafety(this.name = name);

            this.egressType = (statement = (ExecutableStatement)
                    subCompileExpression(stmt = createShortFormOperativeAssignment(name, expr, operation), pCtx)).getKnownEgressType();
        }
        else if ((assignStart = find(expr, '=')) != -1) {
            this.name = createStringTrimmed(expr, 0, assignStart);
            this.egressType = (statement
                    = (ExecutableStatement) subCompileExpression(stmt = subset(expr, assignStart + 1), pCtx))
                    .getKnownEgressType();

            if (col = ((endOfName = (short) findFirst('[', indexTarget = this.name.toCharArray())) > 0)) {
                if (((this.fields |= COLLECTION) & COMPILE_IMMEDIATE) != 0) {
                    accExpr = (CompiledAccExpression) compileSetExpression(indexTarget, pCtx);
                }

                this.name = new String(expr, 0, endOfName);
                index = subset(indexTarget, endOfName, indexTarget.length - endOfName);
            }

            checkNameSafety(this.name);
        }
        else {
            checkNameSafety(this.name = new String(expr));
        }

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            pCtx.addVariable(name, egressType);
        }
    }

    public IndexedAssignmentNode(char[] expr, int fields, int register, ParserContext pCtx) {
        this(expr, fields, -1, null, register, pCtx);
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (accExpr == null) {
            accExpr = (CompiledAccExpression) compileSetExpression(indexTarget);
        }

        if (col) {
            accExpr.setValue(ctx, thisValue, factory, ctx = statement.getValue(ctx, thisValue, factory));
        }
        else if (statement != null) {
            if (factory.isIndexedFactory()) {
                factory.createIndexedVariable(register, name, ctx = statement.getValue(ctx, thisValue, factory));
            }
            else {
                factory.createVariable(name, ctx = statement.getValue(ctx, thisValue, factory));
            }
        }
        else {
            if (factory.isIndexedFactory()) {
                factory.createIndexedVariable(register, name, null);
            }
            else {
                factory.createVariable(name, statement.getValue(ctx, thisValue, factory));
            }
            return Void.class;
        }

        return ctx;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        checkNameSafety(name);

        if (col) {
            MVEL.setProperty(factory.getIndexedVariableResolver(register).getValue(), new String(index), ctx = MVEL.eval(stmt, ctx, factory));
        }
        else {
            factory.createIndexedVariable(register, name, ctx = MVEL.eval(stmt, ctx, factory));
        }

        return ctx;
    }

    public String getAssignmentVar() {
        return name;
    }

    public char[] getExpression() {
        return stmt;
    }

    public int getRegister() {
        return register;
    }

    public void setRegister(int register) {
        this.register = register;
    }

    public boolean isAssignment() {
        return true;
    }

    public boolean isNewDeclaration() {
        return false;
    }

    public void setValueStatement(ExecutableStatement stmt) {
        this.statement = stmt;
    }
}