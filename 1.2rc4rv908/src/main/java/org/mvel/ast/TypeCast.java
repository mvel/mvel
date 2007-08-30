package org.mvel.ast;

import org.mvel.ASTNode;
import static org.mvel.DataConversion.convert;
import org.mvel.ExecutableStatement;
import org.mvel.MVEL;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.util.ParseTools;

public class TypeCast extends ASTNode {
    private ExecutableStatement statement;

    public TypeCast(char[] expr, int start, int end, int fields, Class cast) {
        super(expr, start, end, fields);
        this.egressType = cast;
        if ((fields & COMPILE_IMMEDIATE) != 0) {
            statement = (ExecutableStatement) ParseTools.subCompileExpression(name);
        }
    }


    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return convert(statement.getValue(ctx, thisValue, factory), egressType);
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return convert(MVEL.eval(name, ctx, factory), egressType);
    }
}
