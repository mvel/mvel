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

import org.mvel.MVEL;
import static org.mvel.MVEL.compileSetExpression;
import org.mvel.compiler.AbstractParser;
import org.mvel.compiler.CompiledSetExpression;
import org.mvel.compiler.ExecutableStatement;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ArrayTools.findFirst;
import static org.mvel.util.ParseTools.*;
import static org.mvel.util.PropertyTools.createStringTrimmed;
import static org.mvel.util.PropertyTools.find;

/**
 * @author Christopher Brock
 */
public class IndexedAssignmentNode extends ASTNode implements Assignment {
    private String name;
    private int register;
    private transient CompiledSetExpression setExpr;

    private char[] indexTarget;
    private char[] index;

    private char[] stmt;
    private ExecutableStatement statement;

    private boolean col = false;

    public IndexedAssignmentNode(char[] expr, int fields, int operation, String name, int register) {
        super.name = expr;
        this.register = register;

        int assignStart;

        if (operation != -1) {
            checkNameSafety(this.name = name);

            this.egressType = (statement = (ExecutableStatement)
                    subCompileExpression(stmt = createShortFormOperativeAssignment(name, expr, operation))).getKnownEgressType();
        }
        else if ((assignStart = find(expr, '=')) != -1) {
            this.name = createStringTrimmed(expr, 0, assignStart);
            this.egressType = (statement = (ExecutableStatement) subCompileExpression(stmt = subset(expr, assignStart + 1))).getKnownEgressType();

            if (col = ((endOfName = findFirst('[', indexTarget = this.name.toCharArray())) > 0)) {
                if (((this.fields |= COLLECTION) & COMPILE_IMMEDIATE) != 0) {
                    setExpr = (CompiledSetExpression) compileSetExpression(indexTarget);
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
            AbstractParser.getCurrentThreadParserContext().addVariable(name, egressType);
        }
    }

    public IndexedAssignmentNode(char[] expr, int fields, int register) {
        this(expr, fields, -1, null, register);
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (setExpr == null) {
            setExpr = (CompiledSetExpression) compileSetExpression(indexTarget);
        }

        if (col) {
            setExpr.setValue(ctx, thisValue, factory, ctx = statement.getValue(ctx, thisValue, factory));
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
        //   Object o;

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
        return new String(indexTarget);
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
}