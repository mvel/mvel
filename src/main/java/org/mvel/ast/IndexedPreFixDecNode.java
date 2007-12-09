package org.mvel.ast;

import org.mvel.Operator;
import org.mvel.integration.VariableResolver;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ParseTools.doOperations;

/**
 * @author Christopher Brock
 */
public class IndexedPreFixDecNode extends ASTNode {
    private int register;

    public IndexedPreFixDecNode(char[] expr, int fields, int register) {
        super(expr, fields);
        this.register = register;
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        VariableResolver vResolver = factory.getIndexedVariableResolver(register);
        vResolver.setValue(ctx = doOperations(vResolver.getValue(), Operator.SUB, 1));
        return ctx;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return getReducedValueAccelerated(ctx, thisValue, factory);
    }
}