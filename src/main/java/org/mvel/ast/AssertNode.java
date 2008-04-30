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

import org.mvel.CompileException;
import org.mvel.MVEL;
import org.mvel.compiler.ExecutableStatement;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ParseTools.subCompileExpression;

/**
 * @author Christopher Brock
 */
public class AssertNode extends ASTNode {
    public ExecutableStatement assertion;

    public AssertNode(char[] expr, int fields) {
        this.name = expr;
        if ((fields & COMPILE_IMMEDIATE) != 0) {
            assertion = (ExecutableStatement) subCompileExpression(expr);
        }
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        try {
            Boolean bool = (Boolean) assertion.getValue(ctx, thisValue, factory);
            if (!bool) throw new AssertionError("assertion failed in expression: " + new String(this.name));
            return bool;
        }
        catch (ClassCastException e) {
            throw new CompileException("assertion does not contain a boolean statement");
        }
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        try {
            Boolean bool = (Boolean) MVEL.eval(this.name, ctx, factory);
            if (!bool) throw new AssertionError("assertion failed in expression: " + new String(this.name));
            return bool;
        }
        catch (ClassCastException e) {
            throw new CompileException("assertion does not contain a boolean statement");
        }
    }
}
