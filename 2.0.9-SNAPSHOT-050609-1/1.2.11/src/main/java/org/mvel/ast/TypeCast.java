package org.mvel.ast;

import org.mvel.ASTNode;
import static org.mvel.DataConversion.convert;
import org.mvel.ExecutableStatement;
import static org.mvel.MVEL.eval;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ParseTools.subCompileExpression;

public class TypeCast extends ASTNode {
    private ExecutableStatement statement;

    public TypeCast(char[] expr, int start, int end, int fields, Class cast) {
        super(expr, start, end, fields);
        this.egressType = cast;
        if ((fields & COMPILE_IMMEDIATE) != 0) {
            statement = (ExecutableStatement) subCompileExpression(name);
        }
    }


    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        //noinspection unchecked
        return convert(statement.getValue(ctx, thisValue, factory), egressType);
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        //noinspection unchecked
        return convert(eval(name, ctx, factory), egressType);
    }
}
