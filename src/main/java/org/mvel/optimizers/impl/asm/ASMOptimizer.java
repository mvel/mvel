package org.mvel.optimizers.impl.asm;

import org.mvel.ExecutableStatement;
import org.mvel.Token;
import org.mvel.TokenIterator;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.Optimizer;

public class ASMOptimizer implements Optimizer {

    public ExecutableStatement optimize(TokenIterator tokenIterator, Object staticContext, VariableResolverFactory factory) {

        Token token;


        while (tokenIterator.hasMoreTokens()) {
            token = tokenIterator.nextToken();

            if (token.isIdentifier()) {
                ASMAccessorCompiler compiler = new ASMAccessorCompiler(token.getNameAsArray(), staticContext, staticContext, factory);
                token.setAccessor(compiler.compileAccessor());




            }
        }

        return null;
    }

    public String getName() {
        return "ASM";
    }
}
