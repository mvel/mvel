package org.mvel2.ast;

public abstract class BooleanNode extends ASTNode {
    protected ASTNode left;
    protected ASTNode right;

    public ASTNode getLeft() {
        return this.left;
    }

    public ASTNode getRight() {
        return this.right;
    }

    public void setLeft(ASTNode node) {
        this.left = node;
    }

    public void setRight(ASTNode node) {
        this.right = node;
    }

    public abstract void setRightMost(ASTNode right);
    public abstract ASTNode getRightMost();
}
