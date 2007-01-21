package org.mvel.optimizers.impl.ast;

import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.ExecutableStatement;

public class ASTStatement implements ExecutableStatement {
    private ASTNode nextNode;
    private ASTNode currentNode;

    public ASTStatement() {
    }

    public ASTStatement(ASTNode nextNode) {
        this.nextNode = nextNode;
    }

    public ASTNode getNextNode() {
        return nextNode;
    }

    public void setNextNode(ASTNode nextNode) {
        this.nextNode = nextNode;
    }

    public Object getValue(Object staticContext, VariableResolverFactory factory) {
        return nextNode.getValue(this, staticContext, factory);
    }

    public ASTNode addASTNode(ASTNode astNode) {
        if (nextNode == null) {
            return nextNode = currentNode = astNode;
        }
        else {
            return currentNode = currentNode.setNextNode(astNode);
        }
    }


    public void setKnownIngressType(Class type) {

    }

    public void setKnownEgressType(Class type) {

    }

    public Class getKnownIngressType() {
        return null;
    }

    public Class getKnownEgressType() {
        return null;
    }

    public boolean isConvertableIngressEgress() {
        return false;
    }

    public void computeTypeConversionRule() {

    }
}
