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
package org.mvel2.ast;

import static org.mvel2.MVEL.eval;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.VariableResolverFactory;
import static org.mvel2.util.ParseTools.subCompileExpression;

import static java.lang.String.valueOf;
import static java.util.regex.Pattern.compile;

public class RegExMatch extends ASTNode {
    private ExecutableStatement stmt;
    private ExecutableStatement patternStmt;
    private char[] pattern;

    public RegExMatch(char[] expr, int fields, char[] pattern) {
        super(expr, fields);
        this.pattern = pattern;

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            this.stmt = (ExecutableStatement) subCompileExpression(expr);
            this.patternStmt = (ExecutableStatement) subCompileExpression(pattern);
        }
    }


    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return compile(valueOf(patternStmt.getValue(ctx, thisValue, factory))).matcher(valueOf(stmt.getValue(ctx, thisValue, factory))).matches();
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return compile(valueOf(eval(pattern, ctx, factory))).matcher(valueOf(eval(name, ctx, factory))).matches();
    }

    public Class getEgressType() {
        return Boolean.class;
    }
}
