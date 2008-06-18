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
package org.mvel.compiler;

import org.mvel.SetAccessor;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.optimizers.OptimizerFactory.getAccessorCompiler;

import java.io.Serializable;

public class CompiledSetExpression implements Serializable {
    private char[] expression;
    private transient SetAccessor accessor;

    public CompiledSetExpression(char[] expression) {
        this.expression = expression;
    }

    public void setValue(Object ctx, VariableResolverFactory vrf, Object value) {
        if (accessor == null) {
            accessor = getAccessorCompiler("reflective").optimizeSetAccessor(expression, ctx, ctx, vrf, false, value);
        }
        else {
            accessor.setValue(ctx, vrf, value);
        }
    }
}
