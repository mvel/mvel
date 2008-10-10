package org.mvel2;

import org.mvel2.ast.ASTNode;

/**
 * @author Christopher Brock
 */
public class UnresolveablePropertyException extends RuntimeException {
    private ASTNode astNode;

    public UnresolveablePropertyException(ASTNode astNode, Throwable throwable) {
        super("unable to resolve token: " + astNode.getName(), throwable);
        this.astNode = astNode;
    }

    public UnresolveablePropertyException(ASTNode astNode) {
        super("unable to resolve token: " + astNode.getName());
        this.astNode = astNode;
    }

    public ASTNode getToken() {
        return astNode;
    }
}
