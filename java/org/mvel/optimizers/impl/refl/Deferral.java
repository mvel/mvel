package org.mvel.optimizers.impl.refl;

import org.mvel.AccessorNode;
import org.mvel.integration.VariableResolverFactory;

public class Deferral implements AccessorNode {
    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory vr) throws Exception {
        return null;
    }

    public AccessorNode getNextNode() {
        return null;
    }

    public AccessorNode setNextNode(AccessorNode accessorNode) {
        return null;
    }
}
