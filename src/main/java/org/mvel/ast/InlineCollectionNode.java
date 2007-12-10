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

import org.mvel.ast.cache.CachedListAccessor;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.AccessorOptimizer;
import org.mvel.optimizers.OptimizerFactory;
import static org.mvel.optimizers.OptimizerFactory.SAFE_REFLECTIVE;
import static org.mvel.optimizers.OptimizerFactory.getAccessorCompiler;

import java.util.List;

/**
 * @author Christopher Brock
 */
public class InlineCollectionNode extends ASTNode {
    public InlineCollectionNode(char[] expr, int start, int end, int fields) {
        super(expr, start, end, fields | INLINE_COLLECTION);
    }

    public InlineCollectionNode(char[] expr, int fields) {
        super(expr, fields);
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        try {
            return accessor.getValue(ctx, thisValue, factory);
        }
        catch (NullPointerException e) {
            if (accessor == null) {
                AccessorOptimizer ao = OptimizerFactory.getThreadAccessorOptimizer();
                accessor = ao.optimizeCollection(name, ctx, thisValue, factory);
                egressType = ao.getEgressType();

                if (ao.isLiteralOnly()) {
                    if (egressType == List.class) {
                        List v = (List) accessor.getValue(null, null, null);
                        accessor = new CachedListAccessor(v);
                        return v;
                    }
                }

                return accessor.getValue(ctx, thisValue, factory);
            }
            else {
                throw e;
            }
        }

    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return getAccessorCompiler(SAFE_REFLECTIVE).optimizeCollection(name, ctx, thisValue, factory).getValue(ctx, thisValue, factory);
    }
}
