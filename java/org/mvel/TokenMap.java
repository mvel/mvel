/**
 * MVEL (The MVFLEX Expression Language)
 *
 * Copyright (C) 2007 Christopher Brock, MVFLEX/Valhalla Project and the Codehaus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.mvel;

public class TokenMap implements TokenIterator {
    private Token firstToken;
    private Token current;
    private int size;


    public TokenMap() {
    }

    public TokenMap(Token firstToken) {
        this.current = this.firstToken = firstToken;
    }

    public void addTokenNode(Token token) {
        if (this.firstToken == null) {
            this.firstToken = this.current = token;
        }
        else {
            this.current = (this.current.nextToken = token);
        }
        size++;
    }

    public void addTokenNode(Token token, Token token2) {
        if (this.firstToken == null) {
            this.current = ((this.firstToken = token).nextToken = token2);
        }
        else {
            this.current = (this.current.nextToken = token).nextToken = token2;
        }
    }


    public Token firstToken() {
        return firstToken;
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


//    public boolean peekNextTokenFlags(int flags) {
//        return current != null && (flags & current.nextToken.getFlags()) != 0;
//    }

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

    public int index() {
        return -1;
    }
}
