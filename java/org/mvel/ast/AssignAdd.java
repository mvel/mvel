package org.mvel.ast;

import static org.mvel.util.ParseTools.doOperations;
import org.mvel.ASTNode;
import org.mvel.ExecutableStatement;
import org.mvel.MVEL;
import org.mvel.Operator;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.integration.VariableResolver;

public class AssignAdd extends ASTNode {
    private String varName;
    private ExecutableStatement statement;

    public AssignAdd(char[] expr, int fields, String variableName) {
        super(expr, fields);
        this.varName = variableName;

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            statement = (ExecutableStatement) MVEL.compileExpression(expr);
        }
    }


    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        VariableResolver resolver = factory.getVariableResolver(varName);
        Object val = doOperations(resolver.getValue(), Operator.ADD, statement.getValue(ctx, thisValue, factory));
        resolver.setValue(val);
        return val;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        VariableResolver resolver = factory.getVariableResolver(varName);
        Object val = doOperations(resolver.getValue(), Operator.ADD, MVEL.eval(name, ctx, factory));
        resolver.setValue(val);
        return val;
    }

}
