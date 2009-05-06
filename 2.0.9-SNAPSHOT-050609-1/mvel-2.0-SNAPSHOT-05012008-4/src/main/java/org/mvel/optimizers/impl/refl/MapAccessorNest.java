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

import org.mvel.compiler.AccessorNode;
import org.mvel.compiler.ExecutableStatement;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.util.ParseTools;

import java.util.Map;

/**
 * @author Christopher Brock
 */
public class MapAccessorNest implements AccessorNode {
    private AccessorNode nextNode;
    private ExecutableStatement property;

    public MapAccessorNest() {
    }

    public MapAccessorNest(ExecutableStatement property) {
        this.property = property;
    }


    public MapAccessorNest(String property) {
        this.property = (ExecutableStatement) ParseTools.subCompileExpression(property);
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory vrf) {
        if (nextNode != null) {
            return nextNode.getValue(((Map) ctx).get(property.getValue(ctx, elCtx, vrf)), elCtx, vrf);
        }
        else {
            return ((Map) ctx).get(property.getValue(ctx, elCtx, vrf));
        }
    }


    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        //noinspection unchecked
        ((Map) ctx).put(property.getValue(ctx, elCtx, variableFactory), value);
        return value;
    }

    public ExecutableStatement getProperty() {
        return property;
    }

    public void setProperty(ExecutableStatement property) {
        this.property = property;
    }

    public AccessorNode getNextNode() {
        return nextNode;
    }

    public AccessorNode setNextNode(AccessorNode nextNode) {
        return this.nextNode = nextNode;
    }


    public String toString() {
        return "Map Accessor -> [" + property + "]";
    }
}
