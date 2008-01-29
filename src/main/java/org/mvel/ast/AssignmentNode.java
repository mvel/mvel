package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.CompiledSetExpression;
import org.mvel.ExecutableStatement;
import org.mvel.MVEL;
import static org.mvel.MVEL.compileSetExpression;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ArrayTools.findFirst;
import static org.mvel.util.ParseTools.*;
import static org.mvel.util.PropertyTools.find;

/**
 * @author Christopher Brock
 */
public class AssignmentNode extends ASTNode implements Assignment {
    private String name;
    private transient CompiledSetExpression setExpr;

    private char[] indexTarget;
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

            this.egressType = (statement = (ExecutableStatement)
                    subCompileExpression(stmt = createShortFormOperativeAssignment(name, expr, operation))).getKnownEgressType();
        }
        else if ((assignStart = find(expr, '=')) != -1) {
            this.name = new String(expr, 0, assignStart).trim();
            stmt = subset(expr, assignStart + 1);

            if ((fields & COMPILE_IMMEDIATE) != 0) {
                this.egressType = (statement = (ExecutableStatement) subCompileExpression(stmt)).getKnownEgressType();
            }

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

    public AssignmentNode(char[] expr, int fields) {
        this(expr, fields, -1, null);
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (setExpr == null) {
            setExpr = (CompiledSetExpression) compileSetExpression(indexTarget);
            //      statement = (ExecutableStatement) subCompileExpression(stmt);
        }

        //   Object o;

        if (col) {
            setExpr.setValue(ctx, factory, ctx = statement.getValue(ctx, thisValue, factory));
        }
        else if (statement != null) {
            finalLocalVariableFactory(factory).createVariable(name, ctx = statement.getValue(ctx, thisValue, factory));
        }
        else {
            factory.createVariable(name, null);
            return Void.class;
        }

        return ctx;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        //   Object o;

        checkNameSafety(name);

        if (col) {
            MVEL.setProperty(factory.getVariableResolver(name).getValue(), new String(index), ctx = MVEL.eval(stmt, ctx, factory));
        }
        else {
            finalLocalVariableFactory(factory).createVariable(name, ctx = MVEL.eval(stmt, ctx, factory));
        }

        return ctx;
    }


    public String getAssignmentVar() {
        return name;
    }

    public boolean isNewDeclaration() {
        return false;
    }
}
