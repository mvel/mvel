package org.mvel.ast;

import org.mvel.ASTNode;
import static org.mvel.MVEL.eval;
import org.mvel.compiler.ExecutableStatement;
import org.mvel.integration.VariableResolver;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.util.ParseTools;
import static org.mvel.util.ParseTools.doOperations;

public class IndexedOperativeAssign extends ASTNode {
    private final int register;
    private ExecutableStatement statement;
    private final int operation;

    public IndexedOperativeAssign(char[] expr, int operation, int register, int fields) {
        this.operation = operation;
        this.name = expr;
        this.register = register;

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            statement = (ExecutableStatement) ParseTools.subCompileExpression(expr);
        }
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        VariableResolver resolver = factory.getIndexedVariableResolver(register);
        resolver.setValue(ctx = doOperations(resolver.getValue(), operation, statement.getValue(ctx, thisValue, factory)));
        return ctx;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        VariableResolver resolver = factory.getIndexedVariableResolver(register);
        resolver.setValue(ctx = doOperations(resolver.getValue(), operation, eval(name, ctx, factory)));
        return ctx;
    }
}