package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.ExecutableStatement;
import static org.mvel.MVEL.compileExpression;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ParseTools.*;
import static org.mvel.util.PropertyTools.find;

/**
 * @author Christopher Brock
 */
public class TypedVarNode extends ASTNode {
    private String name;
    private Class type;
    private ExecutableStatement statement;

    public TypedVarNode(char[] expr, int fields, Class type) {
        super(expr, fields);
        this.type = type;

        int assignStart;
        if ((assignStart = find(expr, '=')) != -1) {
            checkNameSafety(name = new String(expr, 0, assignStart).trim());
            statement = (ExecutableStatement) compileExpression(subset(expr, assignStart + 1));
        }
        else {
            checkNameSafety(name = new String(expr));
        }

    }


    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (statement != null) {
            Object o = statement.getValue(ctx, thisValue, factory);

            finalLocalVariableFactory(factory).createVariable(name, o, type);

            return o;
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
