package org.mvel.ast;

import org.mvel.Token;

/**
 * @author Christopher Brock
 */
public class BlockToken extends Token {
    protected char[] block;

    public BlockToken(char[] expr, int fields) {
        super(expr, fields);
    }

    public BlockToken(char[] expr, int fields, char[] block) {
        super(expr, fields);
        this.block = block;
    }
}

