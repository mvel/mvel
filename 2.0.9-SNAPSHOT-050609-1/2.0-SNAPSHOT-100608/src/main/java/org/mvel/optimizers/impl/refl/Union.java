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
package org.mvel.optimizers.impl.refl;

import org.mvel.compiler.Accessor;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.AccessorOptimizer;
import org.mvel.optimizers.OptimizerFactory;

/**
 * @author Christopher Brock
 */
public class Union implements Accessor {
    private Accessor accessor;

    private char[] nextExpr;
    private Accessor nextAccessor;

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        if (nextAccessor == null) {
            Object o = accessor.getValue(ctx, elCtx, variableFactory);

            AccessorOptimizer ao = OptimizerFactory.getDefaultAccessorCompiler();
            nextAccessor = ao.optimizeAccessor(nextExpr, o, elCtx, variableFactory, false);

            return ao.getResultOptPass();
            //   return nextAccessor.getValue(o, elCtx, variableFactory);
        }
        else {
            return nextAccessor.getValue(accessor.getValue(ctx, elCtx, variableFactory), elCtx, variableFactory);
        }
    }

    public Union(Accessor accessor, char[] nextAccessor) {
        this.accessor = accessor;
        this.nextExpr = nextAccessor;
    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        return null;
    }
}
