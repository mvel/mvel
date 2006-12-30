package org.mvel;

public class TokenNode {
    public TokenNode( Token token) {
        this.token = token;
    }

    public Token token;
    public TokenNode next;
}
