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
package org.mvel.ast.cache;

import org.mvel.compiler.Accessor;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.util.FastList;

import java.util.List;


public class CachedListAccessor implements Accessor {
    private Object[] cached;

    public CachedListAccessor(List toCache) {
        cached = new Object[toCache.size()];
        for (int i = 0; i < cached.length; i++)
            cached[i] = toCache.get(i);
    }

    public CachedListAccessor(Object[] cached) {
        this.cached = cached;
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        return new FastList(cached);
    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        return null;
    }
}
