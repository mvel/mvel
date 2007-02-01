package org.mvel;

import org.mvel.integration.VariableResolverFactory;
import org.mvel.util.ParseTools;

/**
 * @author Christopher Brock
 */
public class ExecutableLiteral implements ExecutableStatement {
    private Object literal;


    public ExecutableLiteral(Object literal) {
        this.literal = ParseTools.handleParserEgress(literal, false, false);
    }

    public Object getValue(Object staticContext, VariableResolverFactory factory) {
        return literal;
    }

    public void setKnownIngressType(Class type) {

    }

    public void setKnownEgressType(Class type) {

    }

    public Class getKnownIngressType() {
        return null;
    }

    public Class getKnownEgressType() {
        return null;
    }

    public boolean isConvertableIngressEgress() {
        return false;
    }

    public void computeTypeConversionRule() {

    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        return literal;
    }
}
