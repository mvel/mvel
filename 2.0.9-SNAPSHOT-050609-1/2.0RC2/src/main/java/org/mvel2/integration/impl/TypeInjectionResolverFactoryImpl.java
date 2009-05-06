package org.mvel2.integration.impl;

import org.mvel2.ParserContext;
import org.mvel2.integration.VariableResolver;
import org.mvel2.integration.VariableResolverFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class TypeInjectionResolverFactoryImpl extends MapVariableResolverFactory implements TypeInjectionResolverFactory {
    public TypeInjectionResolverFactoryImpl() {
        this.variables = new HashMap();
    }

    public TypeInjectionResolverFactoryImpl(Map<String, Object> variables) {
        this.variables = variables;
    }

    public TypeInjectionResolverFactoryImpl(ParserContext ctx, VariableResolverFactory nextVariableResolverFactory) {
        super(ctx.getImports(), ctx.hasFunction()
                ? new TypeInjectionResolverFactoryImpl(ctx.getFunctions(), nextVariableResolverFactory) :
                nextVariableResolverFactory);
    }

    public TypeInjectionResolverFactoryImpl(Map<String, Object> variables, VariableResolverFactory nextFactory) {
        super(variables, nextFactory);
    }

    public TypeInjectionResolverFactoryImpl(Map<String, Object> variables, boolean cachingSafe) {
        super(variables, cachingSafe);
    }

    public VariableResolver createVariable(String name, Object value) {
        if (nextFactory == null) {
            nextFactory = new MapVariableResolverFactory(new HashMap());
        }
        /**
         * Delegate to the next factory.
         */
        return nextFactory.createVariable(name, value);
    }

    public VariableResolver createVariable(String name, Object value, Class<?> type) {
        if (nextFactory == null) {
            nextFactory = new MapVariableResolverFactory(new HashMap());
        }
        /**
         * Delegate to the next factory.
         */
        return nextFactory.createVariable(name, value, type);
    }

    public Set<String> getKnownVariables() {
        if (nextFactory == null) {
            return new HashSet<String>(0);
        }
        else {
            return nextFactory.getKnownVariables();
        }
    }
}
