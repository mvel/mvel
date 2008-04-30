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
import org.mvel.integration.VariableResolver;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ParseTools.doOperations;
import static org.mvel.util.ParseTools.subCompileExpression;

public class OperativeAssign extends ASTNode {
    private String varName;
    private ExecutableStatement statement;
    private final int operation;

    public OperativeAssign(String variableName, char[] expr, int operation, int fields) {
        this.varName = variableName;
        this.operation = operation;
        this.name = expr;

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            statement = (ExecutableStatement) subCompileExpression(expr);
        }
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        VariableResolver resolver = factory.getVariableResolver(varName);
        resolver.setValue(ctx = doOperations(resolver.getValue(), operation, statement.getValue(ctx, thisValue, factory)));
        return ctx;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        VariableResolver resolver = factory.getVariableResolver(varName);
        resolver.setValue(ctx = doOperations(resolver.getValue(), operation, eval(name, ctx, factory)));
        return ctx;
    }
}