package org.mvel.debug;

public class Frame {
    private String sourceName;
    private int lineNumber;

    public Frame(String sourceName, int lineNumber) {
        this.sourceName = sourceName;
        this.lineNumber = lineNumber;
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
}
