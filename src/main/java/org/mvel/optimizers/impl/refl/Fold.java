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

import java.util.ArrayList;
import java.util.Collection;

public class Fold implements Accessor {
    private char[] expr;
    private Accessor collection;
    private Accessor propAccessor;

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        Collection<Object> newCollection = new ArrayList<Object>();

        for (Object item : (Collection) collection.getValue(ctx, elCtx, variableFactory)) {
            if (propAccessor == null) {
                ReflectiveAccessorOptimizer optimizer = new ReflectiveAccessorOptimizer();
                propAccessor = optimizer.optimizeAccessor(expr, item, item, variableFactory, false);
                newCollection.add(optimizer.getResultOptPass());
            }
            else {
                newCollection.add(propAccessor.getValue(item, item, variableFactory));
            }
        }

        return newCollection;
    }

    public Fold(char[] expr, Accessor collection) {
        this.expr = expr;
        this.collection = collection;
    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        // not implemented
        return null;
    }
}
