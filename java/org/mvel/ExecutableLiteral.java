package org.mvel;

import org.mvel.integration.VariableResolverFactory;
import org.mvel.util.ParseTools;

/**
 * @author Christopher Brock
 */
public class ExecutableLiteral implements ExecutableStatement {
    private Object literal;
    private int integer32;
    private boolean intOptimized;

    public ExecutableLiteral(Object literal) {
        this.literal = ParseTools.handleParserEgress(literal, false);
    }

    public ExecutableLiteral(int literal) {
        this.literal = this.integer32 = literal;
        this.intOptimized = true;
    }

    public int getInteger32() {
        return integer32;
    }

    public void setInteger32(int integer32) {
        this.integer32 = integer32;
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
        return this.literal.getClass();
    }

    public boolean isConvertableIngressEgress() {
        return false;
    }

    public void computeTypeConversionRule() {

    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        return literal;
    }


    public Object getLiteral() {
        return literal;
    }

    public boolean intOptimized() {
        return intOptimized;
    }


    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        // not implemented
        return null;
    }
}
