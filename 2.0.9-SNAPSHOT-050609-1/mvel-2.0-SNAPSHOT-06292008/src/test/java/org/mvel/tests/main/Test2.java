package org.mvel.tests.main;

import org.mvel.ast.ASTNode;

public class Test2 {
    public static void main(String[] args) {
        int fields = 0;
        
        fields |= ASTNode.ASSIGN | ASTNode.DEEP_PROPERTY;

        fields ^= ASTNode.ASSIGN;

        System.out.println((fields & ASTNode.DEEP_PROPERTY) != 0);


    }

}
