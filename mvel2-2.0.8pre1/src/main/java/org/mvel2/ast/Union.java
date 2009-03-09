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

import org.mvel2.PropertyAccessor;
import static org.mvel2.compiler.AbstractParser.getCurrentThreadParserContext;
import org.mvel2.compiler.Accessor;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.optimizers.AccessorOptimizer;
import org.mvel2.optimizers.OptimizerFactory;

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
            accessor = o.optimizeAccessor(getCurrentThreadParserContext(), name,
                    main.getReducedValueAccelerated(ctx, thisValue, factory), thisValue, factory, false, main.getEgressType());
            return o.getResultOptPass();
        }
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return PropertyAccessor.get(
                name,
                main.getReducedValue(ctx, thisValue, factory), factory, thisValue);
    }

    public Class getLeftEgressType() {
        return main.getEgressType();
    }

    public String toString() {
        return (main != null ? main.toString() : "") + "-[union]->" + (accessor != null ? accessor.toString() : "");
    }
}
