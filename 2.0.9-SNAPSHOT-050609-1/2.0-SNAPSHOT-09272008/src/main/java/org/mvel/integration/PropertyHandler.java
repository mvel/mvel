package org.mvel.integration;

/**
 * This interface allows an external property handler to resolve a property against the provided context.
 */
public interface PropertyHandler {
    /**
     * Retrieves the value of the property.
     * @param name - the name of the property to be resolved.
     * @param contextObj - the current context object.
     * @param variableFactory - the root variable factory provided by the runtime.
     * @return - the value of the property.
     */
    public Object getProperty(String name, Object contextObj, VariableResolverFactory variableFactory);


    /**
     * Sets the value of the property.
     * @param name - the name of the property to be resolved.
     * @param contextObj - the current context object.
     * @param variableFactory - the root variable factory provided by the runtime.
     * @param value - the value to be set to the resolved property
     * @return - the resultant value of the property (should normally be the same as the value passed)
     */
    public Object setProperty(String name,  Object contextObj, VariableResolverFactory variableFactory, Object value);
}
