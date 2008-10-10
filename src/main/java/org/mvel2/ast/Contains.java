package org.mvel2.ast;

import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.util.ParseTools;

public class Contains extends ASTNode {
    private ASTNode stmt;
    private ASTNode stmt2;

    public Contains(ASTNode stmt, ASTNode stmt2) {
        this.stmt = stmt;
        this.stmt2 = stmt2;
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return ParseTools.containsCheck(stmt.getReducedValueAccelerated(ctx, thisValue, factory), stmt2.getReducedValueAccelerated(ctx, thisValue, factory));
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        throw new RuntimeException("operation not supported");
    }

    public Class getEgressType() {
        return Boolean.class;
    }
}
