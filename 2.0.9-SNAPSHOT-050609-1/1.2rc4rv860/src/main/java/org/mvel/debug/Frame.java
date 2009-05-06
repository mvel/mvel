package org.mvel.debug;

import org.mvel.integration.VariableResolverFactory;
import org.mvel.ParserContext;
import org.mvel.ast.LineLabel;

public class Frame {
    private String sourceName;
    private int lineNumber;

    private VariableResolverFactory factory;
    private ParserContext parserContext;

    public Frame(LineLabel label, VariableResolverFactory factory, ParserContext pCtx) {
        this.sourceName = label.getSourceFile();
        this.lineNumber = label.getLineNumber();
        this.factory = factory;
        this.parserContext = pCtx;
    }

    public Frame(String sourceName, int lineNumber, VariableResolverFactory factory, ParserContext pCtx) {
        this.sourceName = sourceName;
        this.lineNumber = lineNumber;
        this.factory = factory;
        this.parserContext = pCtx;
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

    public ParserContext getParserContext() {
        return parserContext;
    }

    public void setParserContext(ParserContext parserContext) {
        this.parserContext = parserContext;
    }
}
