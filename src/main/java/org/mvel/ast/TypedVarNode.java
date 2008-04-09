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
import org.mvel.compiler.AbstractParser;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ParseTools.*;
import static org.mvel.util.PropertyTools.find;
import static org.mvel.util.PropertyTools.createStringTrimmed;
import org.mvel.util.PropertyTools;

/**
 * @author Christopher Brock
 */
public class TypedVarNode extends ASTNode implements Assignment {
    private String name;
    private char[] stmt;

    private transient ExecutableStatement statement;

    public TypedVarNode(char[] expr, int fields, Class type) {
        this.egressType = type;
        this.fields = fields;

        int assignStart;
        if ((assignStart = find(super.name = expr, '=')) != -1) {
            checkNameSafety(name = createStringTrimmed(expr, 0, assignStart));

            if (((fields |= ASSIGN) & COMPILE_IMMEDIATE) != 0) {
                statement = (ExecutableStatement) subCompileExpression(stmt = subset(expr, assignStart + 1));
            }
            else {
                stmt = subset(expr, assignStart + 1);
            }
        }
        else {
            checkNameSafety(name = new String(expr));

        }

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            AbstractParser.getCurrentThreadParserContext().addVariable(name, egressType, true);
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
}
