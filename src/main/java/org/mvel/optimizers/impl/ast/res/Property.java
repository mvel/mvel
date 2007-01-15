package org.mvel.optimizers.impl.ast.res;

import org.mvel.AccessorNode;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.impl.ast.AbstractASTNode;

public class Property extends AbstractASTNode {
    private AccessorNode accessor;

    public Property(AccessorNode accessor) {
        this.accessor = accessor;
    }

    public Object getValue(Object baseLineContext, Object staticContext, VariableResolverFactory vFactory) {
        try {
            return accessor.getValue(baseLineContext, staticContext, vFactory);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
