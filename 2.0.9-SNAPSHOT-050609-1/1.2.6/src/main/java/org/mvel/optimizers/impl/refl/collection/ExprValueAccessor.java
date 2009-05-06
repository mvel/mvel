package org.mvel.optimizers.impl.refl.collection;

import org.mvel.Accessor;
import org.mvel.ExecutableStatement;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.util.ParseTools;

/**
 * @author Christopher Brock
 */
public class ExprValueAccessor implements Accessor {

    public ExecutableStatement stmt;

    public ExprValueAccessor(String ex) {
         stmt = (ExecutableStatement) ParseTools.subCompileExpression(ex);
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        return stmt.getValue(elCtx, variableFactory);
    }


    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        // not implemented
        return null;
    }
}
