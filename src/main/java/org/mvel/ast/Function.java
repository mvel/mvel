package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.EndWithValue;
import org.mvel.ExecutableStatement;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.integration.impl.DefaultLocalVariableResolverFactory;
import org.mvel.util.ParseTools;
import static org.mvel.util.ParseTools.findTypeInjectionResolverFactory;


public class Function extends ASTNode implements Safe {
    protected String name;
    protected ExecutableStatement compiledBlock;
    protected String[] parameters;

    public Function(String name, char[] parameters, char[] block) {
        this.name = name;
        this.parameters = ParseTools.parseParameterList(parameters, 0, parameters.length);
        this.compiledBlock = (ExecutableStatement) ParseTools.subCompileExpression(block);
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return findTypeInjectionResolverFactory(factory).createVariable(name, this);
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return findTypeInjectionResolverFactory(factory).createVariable(name, this);
    }

    public Object call(Object ctx, Object thisValue, VariableResolverFactory factory, Object[] parms) {
        try {
            if (parms != null && parms.length != 0) {
                VariableResolverFactory f = new DefaultLocalVariableResolverFactory(factory);

                int i = 0;
                for (Object p : parms) {
                    f.createVariable(parameters[i], p);
                }

                return compiledBlock.getValue(ctx, thisValue, f);
            }
            else {
                return compiledBlock.getValue(ctx, thisValue, new DefaultLocalVariableResolverFactory(factory));
            }
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

    public String[] getParameters() {
        return parameters;
    }

    public void setParameters(String[] parameters) {
        this.parameters = parameters;
    }

    public boolean hasParameters() {
        return this.parameters != null && this.parameters.length != 0;
    }
}
