package org.mvel.ast;

import org.mvel.ASTNode;
import static org.mvel.MVEL.eval;
import org.mvel.integration.VariableResolverFactory;

import static java.lang.String.valueOf;
import static java.util.regex.Pattern.compile;

public class RegExMatchNode extends ASTNode {
    private ASTNode node;
    private ASTNode patternNode;

    public RegExMatchNode(ASTNode matchNode, ASTNode patternNode) {
        this.node = matchNode;
        this.patternNode = patternNode;
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return compile(valueOf(patternNode.getReducedValueAccelerated(ctx, thisValue, factory))).matcher(valueOf(node.getReducedValueAccelerated(ctx, thisValue, factory))).matches();
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return compile(valueOf(eval(name, ctx, factory))).matcher(valueOf(eval(name, ctx, factory))).matches();
    }
}