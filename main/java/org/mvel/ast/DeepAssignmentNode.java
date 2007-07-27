package org.mvel.ast;

import static org.mvel.PropertyAccessor.set;
import static org.mvel.MVEL.eval;
import org.mvel.*;
import static org.mvel.MVEL.compileSetExpression;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ParseTools.subCompileExpression;
import static org.mvel.util.ParseTools.subset;
import static org.mvel.util.PropertyTools.find;

/**
 * @author Christopher Brock
 */
public class DeepAssignmentNode extends ASTNode implements Assignment {
    private String property;
    private char[] stmt;

    private CompiledSetExpression set;
    private Accessor statement;

    public DeepAssignmentNode(char[] expr, int fields) {
        super(expr, fields);

        int mark;
        if ((mark = find(expr, '=')) != -1) {
            property = new String(expr, 0, mark).trim();
            statement = (ExecutableStatement) subCompileExpression(stmt = subset(expr, mark + 1));
        }
        else {
            property = new String(expr);
        }

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            set = (CompiledSetExpression) compileSetExpression(property.toCharArray());
        }
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        Object val;
        set.setValue(ctx, factory, val = statement.getValue(ctx, thisValue, factory));
        return val;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        Object val;
        set(ctx, property, val = eval(stmt, ctx, factory));
        return val;
    }

    public String getAssignmentVar() {
        return property;
    }
}
