package org.mvel.optimizers.impl.refl;

import org.mvel.AccessorNode;
import org.mvel.ExecutableStatement;
import org.mvel.integration.VariableResolverFactory;

import java.util.Map;

/**
 * @author Christopher Brock
 */
public class MapAccessorNest implements AccessorNode {
    private AccessorNode nextNode;
    private ExecutableStatement property;

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
