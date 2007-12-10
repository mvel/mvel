/**
 * MVEL (The MVFLEX Expression Language)
 *
 * Copyright (C) 2007 Christopher Brock, MVFLEX/Valhalla Project and the Codehaus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.mvel.ast;

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
