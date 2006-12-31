package org.mvel;

import java.io.Serializable;

public class CompiledExpression implements Serializable, Cloneable {
    private char[] expression;
    private TokenIterator tokenMap;

    public CompiledExpression(char[] expression, TokenIterator tokenMap) {
        this.expression = expression;
        this.tokenMap = new FastTokenIterator(tokenMap);
    }

    public char[] getExpression() {
        return expression;
    }

    public void setExpression(char[] expression) {
        this.expression = expression;
    }

    public TokenIterator getTokenMap() {
        return tokenMap.clone();
    }

    public void setTokenMap(TokenIterator tokenMap) {
        this.tokenMap = new FastTokenIterator(tokenMap);
    }
}
