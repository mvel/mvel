package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.Accessor;
import org.mvel.CompiledSetExpression;
import org.mvel.ExecutableStatement;
import static org.mvel.MVEL.compileSetExpression;
import static org.mvel.MVEL.eval;
import static org.mvel.PropertyAccessor.set;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ParseTools.*;
import static org.mvel.util.PropertyTools.find;

/**
 * @author Christopher Brock
 */
public class DeepAssignmentNode extends ASTNode implements Assignment {
    private String property;
    private char[] stmt;

    private CompiledSetExpression set;
    private transient Accessor statement;

    public DeepAssignmentNode(char[] expr, int fields, int operation, String name) {
        super(expr, fields);
        int mark;

        if (operation != -1) {
            this.egressType = ((ExecutableStatement) (statement =
                    (ExecutableStatement) subCompileExpression(stmt =
                            createShortFormOperativeAssignment(this.property = name.trim(), expr, operation)))).getKnownEgressType();

        }
        else if ((mark = find(expr, '=')) != -1) {
            property = new String(expr, 0, mark).trim();
            stmt = subset(expr, mark + 1);

            if ((fields & COMPILE_IMMEDIATE) != 0) {
                statement = (ExecutableStatement) subCompileExpression(stmt);
            }
        }
        else {
            property = new String(expr);
        }

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            set = (CompiledSetExpression) compileSetExpression(property.toCharArray());
        }
    }

    public DeepAssignmentNode(char[] expr, int fields) {
        this(expr, fields, -1, null);
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (statement == null) {
            statement = (ExecutableStatement) subCompileExpression(stmt);
            set = (CompiledSetExpression) compileSetExpression(property.toCharArray());
        }

        //    Object val;
        set.setValue(ctx, factory, ctx = statement.getValue(ctx, thisValue, factory));
        return ctx;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        //  Object val;
        set(ctx, factory, property, ctx = eval(stmt, ctx, factory));
        return ctx;
    }

    public String getAssignmentVar() {
        return property;
    }

    public boolean isNewDeclaration() {
        return false;
    }
}
