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
import org.mvel.util.PropertyTools;

/**
 * @author Christopher Brock
 */
public class AssignmentNode extends ASTNode implements Assignment {
    private String varName;
    private transient CompiledSetExpression setExpr;

    private char[] indexTarget;
    private String index;

    private char[] stmt;
    private ExecutableStatement statement;

    private boolean col = false;
    //   private String index;

    public AssignmentNode(char[] expr, int fields, int operation, String name) {
        super(expr, fields);

        int assignStart;

        if (operation != -1) {
            checkNameSafety(this.varName = name);

            this.egressType = (statement = (ExecutableStatement)
                    subCompileExpression(stmt = createShortFormOperativeAssignment(name, expr, operation))).getKnownEgressType();
        }
        else if ((assignStart = find(expr, '=')) != -1) {
            this.varName = createStringTrimmed(expr, 0, assignStart);
            stmt = subset(expr, assignStart + 1);

            if ((fields & COMPILE_IMMEDIATE) != 0) {
                this.egressType = (statement = (ExecutableStatement) subCompileExpression(stmt)).getKnownEgressType();
            }

            if (col = ((endOfName = findFirst('[', indexTarget = this.varName.toCharArray())) > 0)) {
                if (((this.fields |= COLLECTION) & COMPILE_IMMEDIATE) != 0) {
                    setExpr = (CompiledSetExpression) compileSetExpression(indexTarget);
                }

                this.varName = new String(expr, 0, endOfName);
               index = new String(indexTarget, endOfName, indexTarget.length - endOfName);
               // index = subset(indexTarget, endOfName, indexTarget.length - endOfName);
            }

            checkNameSafety(this.varName);
        }
        else {
            checkNameSafety(this.varName = new String(expr));
        }

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            AbstractParser.getCurrentThreadParserContext().addVariable(this.varName, egressType);
        }

        this.name = this.varName.toCharArray();
    }

    public AssignmentNode(char[] expr, int fields) {
        this(expr, fields, -1, null);
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (setExpr == null) {
            setExpr = (CompiledSetExpression) compileSetExpression(indexTarget);
            //      statement = (ExecutableStatement) subCompileExpression(stmt);
        }

        //   Object o;

        if (col) {
            setExpr.setValue(ctx, factory, ctx = statement.getValue(ctx, thisValue, factory));
        }
        else if (statement != null) {
            factory.createVariable(varName, ctx = statement.getValue(ctx, thisValue, factory));
        }
        else {
            factory.createVariable(varName, null);
            return Void.class;
        }

        return ctx;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        //   Object o;

        checkNameSafety(varName);

        if (col) {
            MVEL.setProperty(factory.getVariableResolver(varName).getValue(), index, ctx = MVEL.eval(stmt, ctx, factory));
        }
        else {
            factory.createVariable(varName, ctx = MVEL.eval(stmt, ctx, factory));
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

}
