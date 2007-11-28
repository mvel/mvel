package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.EndWithValue;
import org.mvel.ExecutableStatement;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.util.ParseTools;


public class Function extends ASTNode implements Safe {
    protected String name;
    protected ExecutableStatement compiledBlock;

    public Function(String name, char[] parameters, char[] block) {
        this.name = name;
        this.compiledBlock = (ExecutableStatement) ParseTools.subCompileExpression(block);
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return ParseTools.findTypeInjectionResolverFactory(factory).createVariable(name, this);
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return ParseTools.findTypeInjectionResolverFactory(factory).createVariable(name, this);
    }

    public Object call(Object ctx, Object thisValue, VariableResolverFactory factory) {
        try {
            return compiledBlock.getValue(ctx, thisValue, factory);
        }
        catch (EndWithValue end) {
            return end.getValue();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
