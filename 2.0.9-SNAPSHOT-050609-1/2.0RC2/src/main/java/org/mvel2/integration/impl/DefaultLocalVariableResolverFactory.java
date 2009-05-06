package org.mvel2.integration.impl;

import org.mvel2.integration.VariableResolverFactory;

import java.util.HashMap;
import java.util.Map;

public class DefaultLocalVariableResolverFactory extends MapVariableResolverFactory implements LocalVariableResolverFactory {
    public DefaultLocalVariableResolverFactory() {
        super(new HashMap<String, Object>());
    }

    public DefaultLocalVariableResolverFactory(Map<String, Object> variables) {
        super(variables);
    }

    public DefaultLocalVariableResolverFactory(Map<String, Object> variables, VariableResolverFactory nextFactory) {
        super(variables, nextFactory);
    }

    public DefaultLocalVariableResolverFactory(Map<String, Object> variables, boolean cachingSafe) {
        super(variables, cachingSafe);
    }

    public DefaultLocalVariableResolverFactory(VariableResolverFactory nextFactory) {
        super(new HashMap<String, Object>(), nextFactory);
    }
}
