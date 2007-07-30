package org.mvel.ast;

import org.mvel.*;
import static org.mvel.MVEL.compileSetExpression;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ArrayTools.findFirst;
import static org.mvel.util.ParseTools.*;
import static org.mvel.util.PropertyTools.find;

import static java.lang.System.arraycopy;

/**
 * @author Christopher Brock
 */
public class AssignmentNode extends ASTNode implements Assignment {
    private String name;
    private CompiledSetExpression setExpr;
    private char[] index;

    private char[] stmt;
    private ExecutableStatement statement;

    private boolean col = false;
    //   private String index;

    public AssignmentNode(char[] expr, int fields, int operation, String name) {
        super(expr, fields);

        int assignStart;

        if (operation != -1) {
            checkNameSafety(this.name = name.trim());

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

            arraycopy(this.name.toCharArray(), 0, (stmt = new char[this.name.length() + expr.length + 1]), 0, this.name.length());
            stmt[this.name.length()] = op;
            arraycopy(expr, 0, stmt, this.name.length() + 1, expr.length);

            this.egressType = (statement = (ExecutableStatement) subCompileExpression(stmt)).getKnownEgressType();

        }
        else if ((assignStart = find(expr, '=')) != -1) {


            this.name = new String(expr, 0, assignStart).trim();
            this.egressType = (statement = (ExecutableStatement) subCompileExpression(stmt = subset(expr, assignStart + 1))).getKnownEgressType();

            if (col = ((endOfName = findFirst('[', index = this.name.toCharArray())) > 0)) {
                this.fields |= COLLECTION;

                if ((fields & COMPILE_IMMEDIATE) != 0) {
                    setExpr = (CompiledSetExpression) compileSetExpression(index);
                }

                this.name = new String(expr, 0, endOfName);
                index = subset(index, endOfName, index.length - endOfName);
            }

            checkNameSafety(this.name);
        }
        else {
            checkNameSafety(this.name = new String(expr));
        }
    }

    public AssignmentNode(char[] expr, int fields) {
        this(expr, fields, -1, null);
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        Object o;

        if (col) {
            setExpr.setValue(ctx, factory, o = statement.getValue(ctx, thisValue, factory));
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
        Object o;

        if (col) {
            MVEL.setProperty(factory.getVariableResolver(name).getValue(), new String(index), o = MVEL.eval(stmt, ctx, factory));
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


    public String getAssignmentVar() {
        return name;
    }
}
