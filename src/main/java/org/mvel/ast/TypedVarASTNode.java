package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.ExecutableStatement;
import static org.mvel.MVEL.compileExpression;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ParseTools.subset;
import static org.mvel.util.PropertyTools.find;

/**
 * @author Christopher Brock
 */
public class TypedVarASTNode extends ASTNode {
    private String name;
    private Class type;
    private ExecutableStatement statement;

    public TypedVarASTNode(char[] expr, int fields, Class type) {
        super(expr, fields);
        this.type = type;

        int assignStart;
        if ((assignStart = find(expr, '=')) != -1) {
            name = new String(expr, 0, assignStart).trim();
            statement = (ExecutableStatement) compileExpression(subset(expr, assignStart + 1));
        }
        else {
            name = new String(expr);
        }
    }


    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (statement != null) {
            Object o = statement.getValue(ctx, thisValue, factory);
            factory.createVariable(name, o, type);
            return Void.class;
        }
        else {
            factory.createVariable(name, null, type);
            return Void.class;
        }

    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return getReducedValueAccelerated(ctx, thisValue, factory);
    }
}
