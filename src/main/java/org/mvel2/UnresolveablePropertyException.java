package org.mvel2;

import org.mvel2.ast.ASTNode;

/**
 * @author Christopher Brock
 */
public class UnresolveablePropertyException extends RuntimeException {

    private String name;

    public UnresolveablePropertyException(ASTNode astNode, Throwable throwable) {
        super("unable to resolve token: " + astNode.getName(), throwable);
        this.name = astNode.getName();
    }

    public UnresolveablePropertyException(ASTNode astNode) {
        super("unable to resolve token: " + astNode.getName());
        this.name = astNode.getName();
    }

    public UnresolveablePropertyException(String name) {
        super("unable to resolve token: " + name);
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
