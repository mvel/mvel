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
package org.mvel2.optimizers.impl.refl.collection;

import org.mvel2.compiler.Accessor;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.util.FastList;

import java.util.List;

/**
 * @author Christopher Brock
 */
public class ListCreator implements Accessor {
    public Accessor[] values;

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        Object[] template = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            template[i] = values[i].getValue(ctx, elCtx, variableFactory);
        }
        return new FastList(template);
    }

    public ListCreator(Accessor[] values) {
        this.values = values;
    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        return null;
    }

    public Class getKnownEgressType() {
        return List.class;
    }
}
