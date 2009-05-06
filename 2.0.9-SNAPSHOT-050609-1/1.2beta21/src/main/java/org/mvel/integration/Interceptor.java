package org.mvel.integration;

/**
 * @author Christopher Brock
 */
public interface Interceptor {
    public static final int NORMAL_FLOW = 0;
    public static final int SKIP = 1;
    public static final int END = 2;

    public int doBefore(VariableResolverFactory factory);

    public int doAfter(VariableResolverFactory factory);
}
