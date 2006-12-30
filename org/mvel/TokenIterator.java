package org.mvel;

public interface TokenIterator {
    public void reset();
    public Token nextToken();
    public Token peekToken();
    public Token peekLast();
    public void back();
    public Token tokensBack(int offset);
    public boolean hasMoreTokens();
    public String showTokenChain();
}
