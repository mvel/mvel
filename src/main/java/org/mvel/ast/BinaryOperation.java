package org.mvel.ast;

import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ParseTools.doOperations;

public class BinaryOperation extends ASTNode {
    private int operation;
    private ASTNode left;
    private ASTNode right;

    public BinaryOperation(int operation, ASTNode left, ASTNode right) {
        super();
        this.operation = operation;
        this.left = left;
        this.right = right;
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return doOperations(left.getReducedValueAccelerated(ctx, thisValue, factory), operation, right.getReducedValueAccelerated(ctx, thisValue, factory));

    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        throw new RuntimeException("unsupported AST operation");
    }


    public int getOperation() {
        return operation;
    }

    public void setOperation(int operation) {
        this.operation = operation;
    }

    public ASTNode getLeft() {
        return left;
    }

    public void setLeft(ASTNode left) {
        this.left = left;
    }

    public ASTNode getRight() {
        return right;
    }

    public void setRight(ASTNode right) {
        this.right = right;
    }
}
