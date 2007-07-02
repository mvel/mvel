package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.ExecutableStatement;
import org.mvel.MVEL;
import org.mvel.Operator;
import org.mvel.util.ParseTools;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.integration.VariableResolver;

public class AssignSub extends ASTNode {
    private String varName;
    private ExecutableStatement statement;

    public AssignSub(char[] expr, int fields, String variableName) {
        super(expr, fields);
        this.varName = variableName;

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            statement = (ExecutableStatement) ParseTools.subCompileExpression(expr);
        }
    }


    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        VariableResolver resolver = factory.getVariableResolver(varName);
        Object val = ParseTools.doOperations(resolver.getValue(), Operator.SUB, statement.getValue(ctx, thisValue, factory));
        resolver.setValue(val);
        return val;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        VariableResolver resolver = factory.getVariableResolver(varName);
        Object val = ParseTools.doOperations(resolver.getValue(), Operator.SUB, MVEL.eval(name, ctx, factory));
        resolver.setValue(val);
        return val;
    }

}
