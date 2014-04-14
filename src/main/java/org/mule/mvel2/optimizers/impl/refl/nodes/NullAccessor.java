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
package org.mule.mvel2.optimizers.impl.refl.nodes;

import org.mule.mvel2.compiler.AccessorNode;
import org.mule.mvel2.integration.VariableResolverFactory;

public class NullAccessor implements AccessorNode {
    private AccessorNode nextNode;

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory)
    {
        if (nextNode != null) {
            return nextNode.getValue(null, elCtx, variableFactory);
        }
        return null;
    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value)
    {
        throw new NullPointerException("Can't set a property that is null");
    }

    public Class getKnownEgressType()
    {
        return Object.class;
    }

    public AccessorNode getNextNode()
    {
        return nextNode;
    }

    public AccessorNode setNextNode(AccessorNode accessorNode)
    {
        return this.nextNode = accessorNode;
    }
}
