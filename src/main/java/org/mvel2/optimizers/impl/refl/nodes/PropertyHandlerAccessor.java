package org.mvel2.optimizers.impl.refl.nodes;

import org.mvel2.CompileException;
import org.mvel2.integration.PropertyHandler;
import org.mvel2.integration.VariableResolverFactory;


public class PropertyHandlerAccessor extends BaseAccessor {
    private String propertyName;
    private PropertyHandler propertyHandler;

    public PropertyHandlerAccessor(String propertyName, PropertyHandler propertyHandler) {
        this.propertyName = propertyName;
        this.propertyHandler = propertyHandler;
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        try {
            if (nextNode != null) {
                return nextNode.getValue(propertyHandler.getProperty(propertyName, ctx, variableFactory), elCtx, variableFactory);
            }
            else {
                return propertyHandler.getProperty(propertyName, ctx, variableFactory);
            }
        }
        catch (Exception e) {
            throw new CompileException("unable to access field", e);
        }
    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        if (nextNode != null) {
            return nextNode.setValue(propertyHandler.getProperty(propertyName, ctx, variableFactory), ctx, variableFactory, value);
        }
        else {
            return propertyHandler.setProperty(propertyName, ctx, variableFactory, value);
        }
    }

    public Class getKnownEgressType() {
        return Object.class;
    }
}
