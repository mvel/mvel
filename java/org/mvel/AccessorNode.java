package org.mvel;

public interface AccessorNode extends Accessor {
    public AccessorNode getNextNode();
    public AccessorNode setNextNode(AccessorNode accessorNode);
}
