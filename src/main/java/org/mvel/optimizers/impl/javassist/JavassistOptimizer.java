package org.mvel.optimizers.impl.javassist;

import org.mvel.TokenIterator;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.ExecutableStatement;
import org.mvel.optimizers.Optimizer;

public class JavassistOptimizer implements Optimizer {


    public ExecutableStatement optimize(TokenIterator tokenIterator, Object staticContext, VariableResolverFactory factory) {
        return null;
    }

    public String getName() {
        return "Javassist";
    }
}
