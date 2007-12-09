package org.mvel.ast;

import org.mvel.MVEL;
import static org.mvel.MVEL.compileSetExpression;
import org.mvel.compiler.CompiledSetExpression;
import org.mvel.compiler.ExecutableStatement;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ArrayTools.findFirst;
import static org.mvel.util.ParseTools.*;
import static org.mvel.util.PropertyTools.find;

/**
 * @author Christopher Brock
 */
public class IndexedAssignmentNode extends ASTNode implements Assignment {
    private String name;
    private int register;
    private transient CompiledSetExpression setExpr;

    private char[] indexTarget;
    private char[] index;

    private char[] stmt;
    private ExecutableStatement statement;

    private boolean col = false;
    //   private String index;

    public IndexedAssignmentNode(char[] expr, int fields, int operation, String name, int register) {
        super(expr, fields);

        this.register = register;

        int assignStart;

        if (operation != -1) {
            checkNameSafety(this.name = name.trim());

            this.egressType = (statement = (ExecutableStatement)
                    subCompileExpression(stmt = createShortFormOperativeAssignment(name, expr, operation))).getKnownEgressType();
        }
        else if ((assignStart = find(expr, '=')) != -1) {
            this.name = new String(expr, 0, assignStart).trim();
            stmt = subset(expr, assignStart + 1);

            this.egressType = (statement = (ExecutableStatement) subCompileExpression(stmt)).getKnownEgressType();

            if (col = ((endOfName = findFirst('[', indexTarget = this.name.toCharArray())) > 0)) {
                if (((this.fields |= COLLECTION) & COMPILE_IMMEDIATE) != 0) {
                    setExpr = (CompiledSetExpression) compileSetExpression(indexTarget);
                }

                this.name = new String(expr, 0, endOfName);
                index = subset(indexTarget, endOfName, indexTarget.length - endOfName);
            }

            checkNameSafety(this.name);
        }
        else {
            checkNameSafety(this.name = new String(expr));
        }
    }


    public IndexedAssignmentNode(char[] expr, int fields, int register) {
        this(expr, fields, -1, null, register);
    }


    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (setExpr == null) {
            setExpr = (CompiledSetExpression) compileSetExpression(indexTarget);
        }

        if (col) {
            setExpr.setValue(ctx, factory, ctx = statement.getValue(ctx, thisValue, factory));
        }
        else if (statement != null) {
            if (factory.isIndexedFactory()) {
                factory.createIndexedVariable(register, name, ctx = statement.getValue(ctx, thisValue, factory));
            }
            else {
                factory.createVariable(name, ctx = statement.getValue(ctx, thisValue, factory));
            }
        }
        else {
            if (factory.isIndexedFactory()) {
                factory.createIndexedVariable(register, name, null);
            }
            else {
                factory.createVariable(name, ctx = statement.getValue(ctx, thisValue, factory));
            }
            return Void.class;
        }

        return ctx;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        //   Object o;

        checkNameSafety(name);

        if (col) {
            MVEL.setProperty(factory.getIndexedVariableResolver(register).getValue(), new String(index), ctx = MVEL.eval(stmt, ctx, factory));
        }
        else {
            factory.createIndexedVariable(register, name, ctx = MVEL.eval(stmt, ctx, factory));
        }

        return ctx;
    }


    public String getAssignmentVar() {
        return name;
    }

    public char[] getExpression() {
        return stmt;
    }

    public int getRegister() {
        return register;
    }

    public void setRegister(int register) {
        this.register = register;
    }

    public boolean isAssignment() {
        return true;
    }
}