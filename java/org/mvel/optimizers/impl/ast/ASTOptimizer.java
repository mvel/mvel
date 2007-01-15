package org.mvel.optimizers.impl.ast;

import org.mvel.Token;
import org.mvel.TokenIterator;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.ExecutableStatement;
import org.mvel.optimizers.Optimizer;
import org.mvel.optimizers.impl.ast.res.Property;


public class ASTOptimizer implements Optimizer {
    ASTStatement astStatement = new ASTStatement();


    public ExecutableStatement optimize(TokenIterator tokenIterator, Object staticContext, VariableResolverFactory factory) {
        Token token;

        ASTNode currentNode = null;

        while (tokenIterator.hasMoreTokens()) {
            token = tokenIterator.nextToken();

            if (token.isIdentifier()) {
                if (!token.isOptimized()) {
                    token.optimizeAccessor(staticContext, factory);
                }

                currentNode = astStatement.addASTNode(new Property(token.getAccessorNode()));
            }

        }

        return astStatement;
    }


    public String getName() {
        return "AST";
    }
}
