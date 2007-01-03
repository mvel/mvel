package org.mvel;

import java.util.Map;

public interface AccessorNode {
    public Object getValue(Object ctx, Object elCtx, Map vars) throws Exception;
    public AccessorNode getNextNode();
    public AccessorNode setNextNode(AccessorNode accessorNode);
}
