package org.mvel.ast;

import org.mvel.ASTNode;

/**
 * @author Christopher Brock
 */
public class BlockASTNode extends ASTNode {
    protected char[] block;

    public BlockASTNode(char[] expr, int fields) {
        super(expr, fields);
    }

    public BlockASTNode(char[] expr, int fields, char[] block) {
        super(expr, fields);
        this.block = block;
    }
}

