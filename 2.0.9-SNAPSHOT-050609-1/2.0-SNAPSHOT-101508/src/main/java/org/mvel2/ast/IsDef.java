package org.mvel2.ast;

import org.mvel2.integration.VariableResolverFactory;

public class IsDef extends ASTNode {
    public IsDef(char[] expr) {
        this.nameCache = new String(this.name = expr);
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return factory.isResolveable(nameCache);
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return factory.isResolveable(nameCache);
    }

    public Class getEgressType() {
        return Boolean.class;
    }
}
