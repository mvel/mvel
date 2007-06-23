package org.mvel.optimizers.impl.refl;

import org.mvel.ASTIterator;
import org.mvel.ASTNode;
import org.mvel.ASTSet;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.Optimizer;

/**
 * @author Christopher Brock
 */
public class ReflectiveOptimizer implements Optimizer {

    public ASTIterator optimize(ASTSet tokens, Object ctx, VariableResolverFactory factory) {
        ASTSet optimized = new ASTSet();
        tokens.reset();

        ASTNode[] t = new ASTNode[3];
        int i = 0;

        while (tokens.hasMoreNodes()) {
            t[i++] = tokens.nextNode();
            if (i == 3) {
                if (t[1].isOperator() && t[1].getOperator() < 12) {
                    //     Arithmetic a = new Arithmetic(t[1], t[0].get, t[2]);
                }

            }
        }

        return null;
    }

    public String getName() {
        return "reflective";
    }
}
