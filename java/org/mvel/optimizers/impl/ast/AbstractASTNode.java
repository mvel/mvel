package org.mvel.optimizers.impl.ast;

import org.mvel.integration.VariableResolverFactory;

public abstract class AbstractASTNode implements ASTNode {
    protected ASTNode nextNode;

    public ASTNode setNextNode(ASTNode nextNode) {
        return this.nextNode = nextNode;
    }

    public Object returnOrTraverse(Object baseLineContext, Object staticContext, VariableResolverFactory vFactory)
        throws Exception {
        if (nextNode != null) return nextNode.getValue(baseLineContext, staticContext, vFactory);
        return baseLineContext;
    }
}
