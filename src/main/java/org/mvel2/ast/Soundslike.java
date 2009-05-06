package org.mvel2.ast;

import org.mvel2.CompileException;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.util.CompilerTools;
import static org.mvel2.util.Soundex.soundex;

public class Soundslike extends ASTNode {
    private ASTNode stmt;
    private ASTNode soundslike;

    public Soundslike(ASTNode stmt, ASTNode clsStmt) {
        this.stmt = stmt;
        this.soundslike = clsStmt;
        CompilerTools.expectType(clsStmt, String.class, true);
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return soundex(String.valueOf(soundslike.getReducedValueAccelerated(ctx, thisValue, factory)))
                .equals(soundex((String) stmt.getReducedValueAccelerated(ctx, thisValue, factory)));
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        try {
            String i = String.valueOf(soundslike.getReducedValue(ctx, thisValue, factory));
            if (i == null) throw new ClassCastException();

            String x = (String) stmt.getReducedValue(ctx, thisValue, factory);
            if (x == null) throw new CompileException("not a string: " + stmt.getName());

            return soundex(i).equals(soundex(x));
        }
        catch (ClassCastException e) {
            throw new CompileException("not a string: " + soundslike.getName());
        }

    }

    public Class getEgressType() {
        return Boolean.class;
    }
}
