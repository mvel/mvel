package org.mvel;

/**
 * @author Christopher Brock
 */
public class UnresolveablePropertyException extends RuntimeException {
    private Token token;

    public UnresolveablePropertyException(Token token) {
        super(token.getName());
        this.token = token;
    }

    public Token getToken() {
        return token;
    }
}
