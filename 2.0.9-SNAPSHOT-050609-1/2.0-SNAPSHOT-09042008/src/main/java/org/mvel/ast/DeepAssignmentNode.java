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

import static org.mvel.MVEL.compileSetExpression;
import static org.mvel.MVEL.eval;
import static org.mvel.PropertyAccessor.set;
import org.mvel.compiler.AbstractParser;
import org.mvel.compiler.Accessor;
import org.mvel.compiler.CompiledSetExpression;
import org.mvel.compiler.ExecutableStatement;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ParseTools.*;
import static org.mvel.util.PropertyTools.createStringTrimmed;
import static org.mvel.util.PropertyTools.find;

/**
 * @author Christopher Brock
 */
public class DeepAssignmentNode extends ASTNode implements Assignment {
    private String property;
    private char[] stmt;

    private CompiledSetExpression set;
    private ExecutableStatement statement;

    public DeepAssignmentNode(char[] expr, int fields, int operation, String name) {
        //  super(expr, fields);

        this.name = expr;
        int mark;

        if (operation != -1) {
            this.egressType = ((statement =
                    (ExecutableStatement) subCompileExpression(stmt =
                            createShortFormOperativeAssignment(this.property = name, expr, operation)))).getKnownEgressType();

        }
        else if ((mark = find(expr, '=')) != -1) {
            property = createStringTrimmed(expr, 0, mark);
            stmt = subset(expr, mark + 1);

            if ((fields & COMPILE_IMMEDIATE) != 0) {
                statement = (ExecutableStatement) subCompileExpression(stmt);
            }
        }
        else {
            property = new String(expr);
        }

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            set = (CompiledSetExpression) compileSetExpression(property.toCharArray());
            AbstractParser.getCurrentThreadParserContext().addVariable(name, egressType);
        }
    }

    public DeepAssignmentNode(char[] expr, int fields) {
        this(expr, fields, -1, null);
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (statement == null) {
            statement = (ExecutableStatement) subCompileExpression(stmt);
            set = (CompiledSetExpression) compileSetExpression(property.toCharArray());
        }
        set.setValue(ctx, thisValue, factory, ctx = statement.getValue(ctx, thisValue, factory));
        return ctx;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        set(ctx, factory, property, ctx = eval(stmt, ctx, factory));
        return ctx;
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
