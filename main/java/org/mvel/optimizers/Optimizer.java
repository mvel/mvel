package org.mvel.optimizers;

import org.mvel.TokenIterator;

public interface Optimizer {
    public ExecutableStatement optimize(TokenIterator tokenIterator);
}
