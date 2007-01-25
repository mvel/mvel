package org.mvel.optimizers.impl.refl.collection;

import org.mvel.Accessor;
import org.mvel.ExecutableStatement;
import org.mvel.ExpressionParser;
import org.mvel.integration.VariableResolverFactory;

/**
 * @author Christopher Brock
 */
public class ExprValueAccessor implements Accessor {

    public ExecutableStatement stmt;

    public ExprValueAccessor(String ex) {
         stmt = (ExecutableStatement) ExpressionParser.compileExpression(ex);
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        return stmt.getValue(elCtx, variableFactory);
    }
}
