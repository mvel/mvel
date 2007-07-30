package org.mvel.ast;

import org.mvel.*;
import static org.mvel.MVEL.compileSetExpression;
import static org.mvel.MVEL.eval;
import static org.mvel.PropertyAccessor.set;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ParseTools.subCompileExpression;
import static org.mvel.util.ParseTools.subset;
import static org.mvel.util.PropertyTools.find;

import static java.lang.System.arraycopy;

/**
 * @author Christopher Brock
 */
public class DeepAssignmentNode extends ASTNode implements Assignment {
    private String property;
    private char[] stmt;

    private CompiledSetExpression set;
    private Accessor statement;

    public DeepAssignmentNode(char[] expr, int fields, int operation, String name) {
        super(expr, fields);
        int mark;


        if (operation != -1) {
            this.property = name.trim();

            char op = 0;
            switch (operation) {
                case Operator.ADD:
                    op = '+';
                    break;
                case Operator.SUB:
                    op = '-';
                    break;
                case Operator.MULT:
                    op = '*';
                    break;
                case Operator.DIV:
                    op = '/';
                    break;
            }

            arraycopy(this.property.toCharArray(), 0, (stmt = new char[this.property.length() + expr.length + 1]), 0, this.property.length());
            stmt[this.property.length()] = op;
            arraycopy(expr, 0, stmt, this.property.length() + 1, expr.length);

            this.egressType = ((ExecutableStatement)(statement = (ExecutableStatement) subCompileExpression(stmt))).getKnownEgressType();

        }
        else if ((mark = find(expr, '=')) != -1) {
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

    public DeepAssignmentNode(char[] expr, int fields) {
        this(expr, fields, -1, null);
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        Object val;
        set.setValue(ctx, factory, val = statement.getValue(ctx, thisValue, factory));
        return val;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        Object val;
        set(ctx, factory, property, val = eval(stmt, ctx, factory));
        return val;
    }

    public String getAssignmentVar() {
        return property;
    }
}
