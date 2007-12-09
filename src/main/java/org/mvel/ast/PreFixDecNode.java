package org.mvel.ast;

import org.mvel.Operator;
import org.mvel.integration.VariableResolver;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ParseTools.doOperations;

/**
 * @author Christopher Brock
 */
public class PreFixDecNode extends ASTNode {
    private String name;

    public PreFixDecNode(char[] expr, int fields) {
        super(expr, fields);
        name = new String(expr);
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        VariableResolver vResolver = factory.getVariableResolver(name);
        vResolver.setValue(ctx = doOperations(vResolver.getValue(), Operator.SUB, 1));
        return ctx;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return getReducedValueAccelerated(ctx, thisValue, factory);
    }
}
