package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.Operator;
import org.mvel.integration.VariableResolver;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ParseTools.doOperations;

/**
 * @author Christopher Brock
 */
public class IndexedPostFixIncNode extends ASTNode {
    private int register;

    public IndexedPostFixIncNode(int register) {
        this.register = register;
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        VariableResolver vResolver = factory.getIndexedVariableResolver(register);
        vResolver.setValue(doOperations(ctx = vResolver.getValue(), Operator.ADD, 1));
        return ctx;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return getReducedValueAccelerated(ctx, thisValue, factory);
    }
}