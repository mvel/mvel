package org.mvel2.optimizers.impl.refl.nodes;

import org.mvel2.compiler.AccessorNode;
import org.mvel2.integration.VariableResolverFactory;

public class NullSafe implements AccessorNode {
    private AccessorNode nextNode;

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        if (ctx == null) return null;
        return nextNode.getValue(ctx, elCtx, variableFactory);
    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        if (ctx == null) return null;
        return nextNode.setValue(ctx, elCtx, variableFactory, value);
    }

    public AccessorNode getNextNode() {
        return nextNode;
    }

    public AccessorNode setNextNode(AccessorNode accessorNode) {
        return this.nextNode = accessorNode;
    }

    public Class getKnownEgressType() {
        return Object.class;
    }
}
