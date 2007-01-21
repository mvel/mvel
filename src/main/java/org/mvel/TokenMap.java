package org.mvel;

public class TokenMap implements TokenIterator {
    private Token firstToken;
    private Token current;
    private int size;

    public TokenMap(Token firstToken) {
        this.current = this.firstToken = firstToken;
    }

    public void addTokenNode(Token token) {
        if (this.firstToken == null) {
            this.firstToken = this.current= token;
        }
        else {
           this.current = (this.current.nextToken = token);
        }
        size++;
    }

    public void reset() {
        this.current = firstToken;
    }

    public boolean hasMoreTokens() {
        return this.current != null;
    }

    public Token nextToken() {
        if (current == null) return null;
        try {
            return current;
        }
        finally {
            current = current.nextToken;
        }
       
       // Token tk = current.token;
      //  current = current.next;
      //  return tk;
    }


    public void skipToken() {
         if (current != null)
            current = current.nextToken;
    }


    public Token peekNext() {
        if (current != null && current.nextToken != null)
            return current.nextToken;
        else
            return null;
    }


    public boolean peekNextTokenFlags(int flags) {
        if (current == null) return false;
        return (flags & current.nextToken.getFlags()) != 0;
    }

    public Token peekToken() {
        if (current == null) return null;
        return current.nextToken;
    }

    public void removeToken() {
        if (current != null) {
            current = current.nextToken;
        }
    }

    public Token peekLast() {
        throw new RuntimeException("unimplemented");
    }

    public Token tokensBack(int offset) {
        throw new RuntimeException("unimplemented");
    }


    public void back() {
        throw new RuntimeException("unimplemented");
    }

    public String showTokenChain() {
        throw new RuntimeException("unimplemented");        
    }


    public int size() {
        return size;
    }

    @SuppressWarnings({"CloneDoesntDeclareCloneNotSupportedException"})
    public TokenIterator clone() {
        return null;
    }
}
