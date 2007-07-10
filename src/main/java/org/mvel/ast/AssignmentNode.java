package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.ExecutableStatement;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ParseTools.*;
import static org.mvel.util.PropertyTools.find;
import org.mvel.util.ParseTools;

/**
 * @author Christopher Brock
 */
public class AssignmentNode extends ASTNode implements Assignment {
    private String name;
    private ExecutableStatement statement;

    public AssignmentNode(char[] expr, int fields) {
        super(expr, fields);

        int assignStart;
        if ((assignStart = find(expr, '=')) != -1) {
            checkNameSafety(name = new String(expr, 0, assignStart).trim());
            this.egressType = (statement = (ExecutableStatement) ParseTools.subCompileExpression(subset(expr, assignStart + 1))).getKnownEgressType();
        }
        else {
            checkNameSafety(name = new String(expr));
        }


    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (statement != null) {
            Object o;
            finalLocalVariableFactory(factory).createVariable(name, o = statement.getValue(ctx, thisValue, factory));
            return o;
        }
        else {
            factory.createVariable(name, null);
            return Void.class;
        }

    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return getReducedValueAccelerated(ctx, thisValue, factory);
    }


    public String getAssignmentVar() {
        return name;
    }
}
