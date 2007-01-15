package org.mvel.optimizers.impl.ast;

import org.mvel.integration.VariableResolverFactory;

public interface ASTNode {
    public Object getValue(Object baseLineContext, Object staticContext, VariableResolverFactory vFactory);

    public ASTNode setNextNode(ASTNode nextNode);
}
