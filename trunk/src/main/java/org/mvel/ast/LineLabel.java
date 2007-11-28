package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.integration.VariableResolverFactory;

/**
 * @author Christopher Brock
 */
public class LineLabel extends ASTNode {
    private String sourceFile;
    private int lineNumber;

    public LineLabel(int lineNumber) {
        super();
        this.lineNumber = lineNumber;
        this.fields = -1;
    }

   public LineLabel(String sourceFile, int lineNumber) {
       super();
       this.lineNumber = lineNumber;
       this.sourceFile = sourceFile;
       this.fields = -1;
   }

    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
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
