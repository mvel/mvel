package org.mvel;

import java.util.Map;

public class MapAccessor implements AccessorNode {
    private AccessorNode nextNode;

    private Object property;

    public Object getValue(Object ctx, Object elCtx, Map vars) throws Exception {
        if (nextNode != null) {
            return nextNode.getValue(((Map)ctx).get(property), elCtx, vars);
        }
        else {
            return vars.get(property);
        }
    }


    public Object getProperty() {
        return property;
    }

    public void setProperty(Object property) {
        this.property = property;
    }

    public AccessorNode getNextNode() {
        return nextNode;
    }

    public AccessorNode setNextNode(AccessorNode nextNode) {
        return this.nextNode = nextNode;
    }
}
