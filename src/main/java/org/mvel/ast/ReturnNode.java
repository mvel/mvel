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
import org.mvel.compiler.Accessor;
import org.mvel.compiler.EndWithValue;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ParseTools.subCompileExpression;

/**
 * @author Christopher Brock
 */
public class ReturnNode extends ASTNode {
    private boolean graceful = false;

    public ReturnNode(char[] expr, int fields) {
        super(expr, fields);
        setAccessor((Accessor) subCompileExpression(expr));
    }


    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (accessor == null) {
            setAccessor((Accessor) subCompileExpression(this.name));
        }

        throw new EndWithValue(accessor.getValue(ctx, thisValue, factory));
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        throw new EndWithValue(eval(this.name, ctx, factory));
    }

    public boolean isGraceful() {
        return graceful;
    }

    public void setGraceful(boolean graceful) {
        this.graceful = graceful;
    }
}
