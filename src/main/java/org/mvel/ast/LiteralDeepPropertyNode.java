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

import static org.mvel.PropertyAccessor.get;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.AccessorOptimizer;
import static org.mvel.optimizers.OptimizerFactory.getThreadAccessorOptimizer;

/**
 * @author Christopher Brock
 */
@SuppressWarnings({"CaughtExceptionImmediatelyRethrown"})
public class LiteralDeepPropertyNode extends ASTNode {
    private Object literal;

    public LiteralDeepPropertyNode(char[] expr, int fields, Object literal) {
        this.fields = fields;
        this.name = expr;
        this.literal = literal;
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (accessor != null) {
            return valRet(accessor.getValue(literal, thisValue, factory));
        }
        else {
            AccessorOptimizer aO = getThreadAccessorOptimizer();
            accessor = aO.optimizeAccessor(name, literal, thisValue, factory, false);
            return valRet(aO.getResultOptPass());
        }
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return get(name, literal, factory, thisValue);
    }
}
