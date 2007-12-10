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

import static org.mvel.DataConversion.convert;
import static org.mvel.MVEL.eval;
import org.mvel.compiler.ExecutableStatement;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ParseTools.subCompileExpression;

public class TypeCast extends ASTNode {
    private ExecutableStatement statement;

    public TypeCast(char[] expr, int start, int end, int fields, Class cast) {
        super(expr, start, end, fields);
        this.egressType = cast;
        if ((fields & COMPILE_IMMEDIATE) != 0) {
            statement = (ExecutableStatement) subCompileExpression(name);
        }
    }


    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        //noinspection unchecked
        return convert(statement.getValue(ctx, thisValue, factory), egressType);
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        //noinspection unchecked
        return convert(eval(name, ctx, factory), egressType);
    }
}
