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

import org.mvel2.ParserContext;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.math.MathProcessor;

import static org.mvel2.MVEL.eval;
import static org.mvel2.PropertyAccessor.get;
import static org.mvel2.PropertyAccessor.set;

public class DeepOperativeAssignmentNode extends DeepAssignmentNode {

    private final int operation;

    public DeepOperativeAssignmentNode(char[] expr, int start, int offset, int fields, int operation, String name, ParserContext pCtx) {
        super(expr, start, offset, fields, operation, name, pCtx);

        this.operation = operation;
    }

    // No need to override DeepAssignmentNode.getReducedValueAccelerated() because it already works properly (calculate and assign).

    @Override
    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        Object value = get(property, ctx, factory, thisValue, pCtx);
        ctx = MathProcessor.doOperations(value, operation, eval(expr, start, offset, ctx, factory));
        set(ctx, factory, property, ctx, pCtx);
        return ctx;
    }
}
