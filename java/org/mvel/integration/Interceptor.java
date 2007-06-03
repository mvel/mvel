package org.mvel.integration;

import org.mvel.ASTNode;

/**
 * @author Christopher Brock
 */
public interface Interceptor {
    public static final int NORMAL_FLOW = 0;
    public static final int SKIP = 1;
    public static final int END = 2;

    public int doBefore(ASTNode node, VariableResolverFactory factory);

    public int doAfter(ASTNode node, VariableResolverFactory factory);
}
