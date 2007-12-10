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

import org.mvel.compiler.Accessor;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.AccessorOptimizer;
import static org.mvel.optimizers.OptimizerFactory.getThreadAccessorOptimizer;

/**
 * @author Christopher Brock
 */
public class ContextDeepPropertyNode extends ASTNode {
    private transient Accessor accessor;

    public ContextDeepPropertyNode(char[] expr, int fields) {
        super(expr, fields);
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        try {
            return valRet(accessor.getValue(ctx, thisValue, factory));
        }
        catch (NullPointerException e) {
            if (accessor == null) {
                AccessorOptimizer aO = getThreadAccessorOptimizer();
                accessor = aO.optimizeAccessor(name, ctx, thisValue, factory, false);
                return valRet(aO.getResultOptPass());
            }
            else {
                throw e;
            }
        }
        catch (ClassCastException e) {
            return handleDynamicDeoptimization(ctx, thisValue, factory);
        }
    }

    private Object handleDynamicDeoptimization(Object ctx, Object thisValue, VariableResolverFactory factory) {
        synchronized (this) {
            accessor = null;
            return getReducedValueAccelerated(ctx, thisValue, factory);
        }
    }


    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return getReducedValueAccelerated(ctx, thisValue, factory);
    }
}
