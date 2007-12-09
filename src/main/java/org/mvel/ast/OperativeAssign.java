package org.mvel.ast;

import static org.mvel.MVEL.eval;
import org.mvel.compiler.ExecutableStatement;
import org.mvel.integration.VariableResolver;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.util.ParseTools;
import static org.mvel.util.ParseTools.doOperations;

public class OperativeAssign extends ASTNode {
    private String varName;
    private ExecutableStatement statement;
    private final int operation;

    public OperativeAssign(String variableName, char[] expr, int operation, int fields) {
        this.varName = variableName;
        this.operation = operation;
        this.name = expr;

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            statement = (ExecutableStatement) ParseTools.subCompileExpression(expr);
        }
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        VariableResolver resolver = factory.getVariableResolver(varName);
        resolver.setValue(ctx = doOperations(resolver.getValue(), operation, statement.getValue(ctx, thisValue, factory)));
        return ctx;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        VariableResolver resolver = factory.getVariableResolver(varName);
        resolver.setValue(ctx = doOperations(resolver.getValue(), operation, eval(name, ctx, factory)));
        return ctx;
    }
}