package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.ExecutableStatement;
import org.mvel.MVEL;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ArrayTools.findFirst;
import org.mvel.util.ParseTools;
import static org.mvel.util.ParseTools.*;
import static org.mvel.util.PropertyTools.find;

/**
 * @author Christopher Brock
 */
public class AssignmentNode extends ASTNode implements Assignment {
    private String name;
    private ExecutableStatement statement;

    private boolean col = false;
    private String index;

    public AssignmentNode(char[] expr, int fields) {
        super(expr, fields);


        int assignStart;
        if ((assignStart = find(expr, '=')) != -1) {
            checkNameSafety(name = new String(expr, 0, assignStart).trim());
            this.egressType = (statement = (ExecutableStatement) ParseTools.subCompileExpression(subset(expr, assignStart + 1))).getKnownEgressType();

            char[] nm;
            if (col = ((endOfName = findFirst('[', nm = name.toCharArray())) > 0)) {
                this.fields |= COLLECTION;
                name = new String(nm, 0, endOfName);
                index = new String(nm, endOfName, nm.length - endOfName);
            }
        }
        else {
            checkNameSafety(name = new String(expr));
        }


    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        Object o;

        if (col)  {
            MVEL.setProperty(factory.getVariableResolver(name).getValue(), index, o = statement.getValue(ctx, thisValue, factory));
        }
        else if (statement != null) {
            finalLocalVariableFactory(factory).createVariable(name, o = statement.getValue(ctx, thisValue, factory));
        }
        else {
            factory.createVariable(name, null);
            return Void.class;
        }

        return o;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return getReducedValueAccelerated(ctx, thisValue, factory);
    }


    public String getAssignmentVar() {
        return name;
    }
}
