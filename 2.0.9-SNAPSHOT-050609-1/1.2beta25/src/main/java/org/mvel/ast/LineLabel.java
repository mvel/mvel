package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.integration.VariableResolverFactory;

/**
 * @author Christopher Brock
 */
public class LineLabel extends ASTNode {
    private int lineNumber;

    public LineLabel(int lineNumber) {
        super();
        this.lineNumber = lineNumber;
        this.fields = -1;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }


    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return null;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return null;
    }


    public String toString() {
        return "[SourceLine:" + lineNumber + "]";
    }
}
