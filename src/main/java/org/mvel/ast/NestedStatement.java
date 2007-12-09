package org.mvel.ast;

import org.mvel.compiler.ExecutableStatement;

/**
 * @author Christopher Brock
 */
public interface NestedStatement {
    public ExecutableStatement getNestedStatement();
}
