package org.mvel.debug;

import org.mvel.integration.VariableResolverFactory;

public class Frame {
    private String sourceName;
    private int lineNumber;
    private VariableResolverFactory factory;

    public Frame(String sourceName, int lineNumber, VariableResolverFactory factory) {
        this.sourceName = sourceName;
        this.lineNumber = lineNumber;
        this.factory = factory;
    }


    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }


    public VariableResolverFactory getFactory() {
        return factory;
    }

    public void setFactory(VariableResolverFactory factory) {
        this.factory = factory;
    }
}
