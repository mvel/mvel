package org.mvel.ast;

import org.mvel.CompileException;
import org.mvel.MVEL;
import org.mvel.compiler.ExecutableStatement;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.util.ParseTools;

/**
 * @author Christopher Brock
 */
public class AssertNode extends ASTNode {
    public ExecutableStatement assertion;

    public AssertNode(char[] expr, int fields) {
        super(expr, fields);

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            assertion = (ExecutableStatement) ParseTools.subCompileExpression(expr);
        }
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        try {
            Boolean bool = (Boolean) assertion.getValue(ctx, thisValue, factory);
            if (!bool) throw new AssertionError("assertion failed in expression: " + new String(this.name));
            return bool;
        }
        catch (ClassCastException e) {
            throw new CompileException("assertion does not contain a boolean statement");
        }
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        try {
            Boolean bool = (Boolean) MVEL.eval(this.name, ctx, factory);
            if (!bool) throw new AssertionError("assertion failed in expression: " + new String(this.name));
            return bool;
        }
        catch (ClassCastException e) {
            throw new CompileException("assertion does not contain a boolean statement");
        }
        //   return getReducedValueAccelerated(ctx, thisValue, factory);
    }
}
