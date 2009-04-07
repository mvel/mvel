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

import static org.mvel2.MVEL.compileSetExpression;
import static org.mvel2.MVEL.eval;
import org.mvel2.ParserContext;
import static org.mvel2.PropertyAccessor.set;
import static org.mvel2.compiler.AbstractParser.getCurrentThreadParserContext;
import org.mvel2.compiler.CompiledAccExpression;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.VariableResolverFactory;
import static org.mvel2.util.ParseTools.*;

/**
 * @author Christopher Brock
 */
public class DeepAssignmentNode extends ASTNode implements Assignment {
    private String property;
    private char[] stmt;

    private CompiledAccExpression acc;
    private ExecutableStatement statement;

    public DeepAssignmentNode(char[] expr, int fields, int operation, String name, ParserContext pCtx) {
        this.fields |= DEEP_PROPERTY | fields;

        this.name = expr;
        int mark;


        if (operation != -1) {
            this.egressType = ((statement =
                    (ExecutableStatement) subCompileExpression(stmt =
                            createShortFormOperativeAssignment(this.property = name, expr, operation), pCtx))).getKnownEgressType();
        }
        else if ((mark = find(expr, '=')) != -1) {
            property = createStringTrimmed(expr, 0, mark);
            stmt = subset(expr, mark + 1);

            if ((fields & COMPILE_IMMEDIATE) != 0) {
                statement = (ExecutableStatement) subCompileExpression(stmt, pCtx);
            }
        }
        else {
            property = new String(expr);
        }

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            acc = (CompiledAccExpression) compileSetExpression(property.toCharArray(), pCtx);
        }
    }

    public DeepAssignmentNode(char[] expr, int fields, ParserContext pCtx) {
        this(expr, fields, -1, null, pCtx);
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (statement == null) {
            statement = (ExecutableStatement) subCompileExpression(stmt);
            acc = (CompiledAccExpression) compileSetExpression(property.toCharArray(), statement.getKnownEgressType(),
                    getCurrentThreadParserContext());
        }
        acc.setValue(ctx, thisValue, factory, ctx = statement.getValue(ctx, thisValue, factory));
        return ctx;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        set(ctx, factory, property, ctx = eval(stmt, ctx, factory));
        return ctx;
    }

    @Override
    public String getAbsoluteName() {
        return property.substring(0, property.indexOf('.'));
    }

    public String getAssignmentVar() {
        return property;
    }

    public char[] getExpression() {
        return stmt;
    }

    public boolean isNewDeclaration() {
        return false;
    }

    public boolean isAssignment() {
        return true;
    }

    public void setValueStatement(ExecutableStatement stmt) {
        this.statement = stmt;
    }
}
