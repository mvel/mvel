package org.mvel;

public interface TokenIterator extends Cloneable {
    public void reset();
    public Token nextToken();
    public void skipToken();
    public Token peekNext();
    public Token peekToken();
    public Token peekLast();
    public boolean peekNextTokenFlags(int flags);
    public void back();
    public Token tokensBack(int offset);
    public boolean hasMoreTokens();
    public String showTokenChain();
    @SuppressWarnings({"CloneDoesntDeclareCloneNotSupportedException"})
    public TokenIterator clone();
    public int size();
}
