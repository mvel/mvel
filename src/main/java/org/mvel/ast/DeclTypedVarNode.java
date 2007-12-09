package org.mvel.ast;

import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ParseTools.checkNameSafety;

/**
 * @author Christopher Brock
 */
public class DeclTypedVarNode extends ASTNode implements Assignment {
    private String name;

    public DeclTypedVarNode(char[] expr, int fields, Class type) {
        super(expr, fields);
        this.egressType = type;

        checkNameSafety(name = new String(expr));
    }

    public DeclTypedVarNode(String name, int fields, Class type) {
        super(null, fields);
        this.egressType = type;

        checkNameSafety(this.name = name);
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        factory.createVariable(name, null, egressType);
        return ctx;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        factory.createVariable(name, null, egressType);
        return null;
    }

    public String getName() {
        return name;
    }

    public String getAssignmentVar() {
        return name;
    }

    public char[] getExpression() {
        return new char[0];
    }

    public boolean isAssignment() {
        return true;
    }
}