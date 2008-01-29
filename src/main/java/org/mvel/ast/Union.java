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

import org.mvel.PropertyAccessor;
import org.mvel.compiler.Accessor;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.AccessorOptimizer;
import org.mvel.optimizers.OptimizerFactory;

public class Union extends ASTNode {
    private ASTNode main;
    private transient Accessor accessor;

    public Union(char[] expr, int start, int end, int fields, ASTNode main) {
        super(expr, start, end, fields);
        this.main = main;
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (accessor != null) {
            return accessor.getValue(main.getReducedValueAccelerated(ctx, thisValue, factory), thisValue, factory);
        }
        else {
            AccessorOptimizer o = OptimizerFactory.getThreadAccessorOptimizer();
            accessor = o.optimizeAccessor(name, main.getReducedValueAccelerated(ctx, thisValue, factory), thisValue, factory, false);
            return o.getResultOptPass();
        }
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return PropertyAccessor.get(
                name,
                main.getReducedValue(ctx, thisValue, factory), factory, thisValue);
    }
}
