package org.mvel.ast;

import org.mvel.ExecutableStatement;

/**
 * @author Christopher Brock
 */
public interface NestedStatement {
    public ExecutableStatement getNestedStatement();
}
