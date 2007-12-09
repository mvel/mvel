package org.mvel.integration;

import org.mvel.ast.ASTNode;

/**
 * An interceptor can be used to decorate functionality into an expression.
 *
 * @author Christopher Brock
 */
public interface Interceptor {
    public static final int NORMAL_FLOW = 0;
    public static final int SKIP = 1;
    public static final int END = 2;

    /**
     * This method is executed before the wrapped statement.
     *
     * @param node    The ASTNode wrapped by the interceptor
     * @param factory The variable factory
     * @return The response code.  Should return 0.
     */
    public int doBefore(ASTNode node, VariableResolverFactory factory);

    /**
     * @param exitStackValue The value on the top of the stack after executing the statement.
     * @param node           The ASTNode wrapped by the interceptor
     * @param factory        The variable factory
     * @return The response code.  Should return 0.
     */
    public int doAfter(Object exitStackValue, ASTNode node, VariableResolverFactory factory);
}
