package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.integration.Interceptor;
import org.mvel.integration.VariableResolverFactory;

/**
 * @author Christopher Brock
 */
public class InterceptorWrapper extends ASTNode {
    private Interceptor interceptor;
    private ASTNode node;

    public InterceptorWrapper(Interceptor interceptor, ASTNode node) {
        this.interceptor = interceptor;
        this.node = node;
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        interceptor.doBefore(factory);
        Object val = node.getReducedValueAccelerated(ctx, thisValue, factory);
        interceptor.doAfter(factory);
        return val;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        interceptor.doBefore(factory);
        Object val = node.getReducedValue(ctx, thisValue, factory);
        interceptor.doAfter(factory);
        return val;
    }
}
