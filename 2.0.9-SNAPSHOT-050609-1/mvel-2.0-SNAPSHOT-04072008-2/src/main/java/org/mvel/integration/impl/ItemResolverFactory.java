package org.mvel.integration.impl;

import org.mvel.integration.VariableResolver;
import org.mvel.integration.VariableResolverFactory;


public class ItemResolverFactory extends BaseVariableResolverFactory {
    private final ItemResolver resolver;

    public ItemResolverFactory(ItemResolver resolver, VariableResolverFactory nextFactory) {
        this.resolver = resolver;
        this.nextFactory = nextFactory;
    }

    public VariableResolver createVariable(String name, Object value) {
        if (isTarget(name)) {
            resolver.setValue(value);
            return resolver;
        }
        else {
            return nextFactory.createVariable(name, value);
        }
    }

    public VariableResolver createVariable(String name, Object value, Class<?> type) {
        if (isTarget(name)) {
            // resolver.setValue(value);
            throw new RuntimeException("variable already defined in scope: " + name);
        }
        else {
            return nextFactory.createVariable(name, value);
        }
    }


    public VariableResolver getVariableResolver(String name) {
        return isTarget(name) ? resolver : nextFactory.getVariableResolver(name);
    }

    public boolean isTarget(String name) {
        return resolver.getName().equals(name);
    }

    public boolean isResolveable(String name) {
        return resolver.getName().equals(name) || (nextFactory != null && nextFactory.isResolveable(name));
    }

    public static class ItemResolver implements VariableResolver {
        private final String name;
        private Class type = Object.class;
        private Object value;


        public ItemResolver(String name) {
            this.name = name;
        }

        public String getName() {
            return name;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public Class getType() {
            return type;
        }

        public void setStaticType(Class type) {
            this.type = type;
        }

        public int getFlags() {
            return 0;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }
}
