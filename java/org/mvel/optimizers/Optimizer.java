package org.mvel.optimizers;

import org.mvel.ExecutableStatement;
import org.mvel.TokenIterator;
import org.mvel.integration.VariableResolverFactory;

public interface Optimizer {
    public ExecutableStatement optimize(TokenIterator tokenIterator, Object staticContext, VariableResolverFactory factory);
    public String getName();
}
