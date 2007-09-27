package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.ExecutableStatement;
import static org.mvel.MVEL.eval;
import org.mvel.Operator;
import org.mvel.integration.VariableResolver;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.util.ParseTools;
import static org.mvel.util.ParseTools.doOperations;

public class AssignMult extends ASTNode {
    private String varName;
    private ExecutableStatement statement;

    public AssignMult(char[] expr, int fields, String variableName) {
        super(expr, fields);
        this.varName = variableName;

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            statement = (ExecutableStatement) ParseTools.subCompileExpression(expr);
        }
    }


    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        VariableResolver resolver = factory.getVariableResolver(varName);
        resolver.setValue(ctx = doOperations(resolver.getValue(), Operator.MULT, statement.getValue(ctx, thisValue, factory)));
        return ctx;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        VariableResolver resolver = factory.getVariableResolver(varName);
        resolver.setValue(ctx = doOperations(resolver.getValue(), Operator.MULT, eval(name, ctx, factory)));
        return ctx;
    }

}
