package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.Operator;
import org.mvel.integration.VariableResolver;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ParseTools.doOperations;

/**
 * @author Christopher Brock
 */
public class PostFixDecNode extends ASTNode {
    private String name;

    public PostFixDecNode(char[] expr, int fields) {
        super(expr, fields);
        name = new String(expr);
    }


    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        VariableResolver vResolver = factory.getVariableResolver(name);
        Object o = vResolver.getValue();
        vResolver.setValue(doOperations(o, Operator.SUB, 1));
        return o;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return getReducedValueAccelerated(ctx, thisValue, factory);
    }
}
