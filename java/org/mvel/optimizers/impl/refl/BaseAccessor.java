package org.mvel.optimizers.impl.refl;

import org.mvel.AccessorNode;

public abstract class BaseAccessor implements AccessorNode {
    protected AccessorNode nextNode;

    public AccessorNode setNextNode(AccessorNode accessorNode) {
        return this.nextNode = accessorNode;
    }

    public AccessorNode getNextNode() {
        return this.nextNode;
    }
}
