package org.mvel2.ast;

import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.compiler.Accessor;
import org.mvel2.util.ParseTools;
import org.mvel2.util.CompilerTools;
import org.mvel2.MVEL;
import org.mvel2.CompileException;


public class Instance extends ASTNode {
    private ASTNode stmt;
    private ASTNode clsStmt;

    public Instance(ASTNode stmt, ASTNode clsStmt) {
        this.stmt = stmt;
        this.clsStmt = clsStmt;
        CompilerTools.expectType(clsStmt, Class.class, true);
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return ((Class) clsStmt.getReducedValueAccelerated(ctx, thisValue, factory)).isInstance(stmt.getReducedValueAccelerated(ctx, thisValue, factory));
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        try {
            Class i = (Class) clsStmt.getReducedValue(ctx, thisValue, factory);
            if (i == null) throw new ClassCastException();

            return i.isInstance(stmt.getReducedValue(ctx, thisValue, factory));
        }
        catch (ClassCastException e) {
            throw new CompileException("not a class reference: " + clsStmt.getName());
        }

    }

    public Class getEgressType() {
        return Boolean.class;
    }
}
