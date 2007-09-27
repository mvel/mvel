package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.ExecutableStatement;
import org.mvel.MVEL;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.util.ParseTools;
import static org.mvel.util.ParseTools.*;
import static org.mvel.util.PropertyTools.find;

/**
 * @author Christopher Brock
 */
public class TypedVarNode extends ASTNode implements Assignment {
    private String name;
    private char[] stmt;

    private transient ExecutableStatement statement;

    public TypedVarNode(char[] expr, int fields, Class type) {
        super(expr, fields);
        this.egressType = type;

        int assignStart;
        if ((assignStart = find(expr, '=')) != -1) {
            fields |= ASSIGN;
            checkNameSafety(name = new String(expr, 0, assignStart).trim());
            stmt = subset(expr, assignStart + 1);

            if ((fields & COMPILE_IMMEDIATE) != 0) {
                statement = (ExecutableStatement) ParseTools.subCompileExpression(stmt);
            }
        }
        else {
            checkNameSafety(name = new String(expr));
        }

    }


    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (statement == null) statement = (ExecutableStatement) ParseTools.subCompileExpression(stmt);
        finalLocalVariableFactory(factory).createVariable(name, ctx = statement.getValue(ctx, thisValue, factory), egressType);
        return ctx;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        Object o = MVEL.eval(stmt, thisValue, factory);
        finalLocalVariableFactory(factory).createVariable(name, o, egressType);
        return o;
    }


    public String getName() {
        return name;
    }


    public String getAssignmentVar() {
        return name;
    }
}
