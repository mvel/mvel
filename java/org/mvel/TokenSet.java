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

public class TokenSet implements TokenIterator {
    private ASTNode firstASTNode;
    private ASTNode current;
    private int size;


    public TokenSet() {
    }

    public TokenSet(ASTNode firstASTNode) {
        this.current = this.firstASTNode = firstASTNode;
    }

    public void addTokenNode(ASTNode astNode) {
        if (this.firstASTNode == null) {
            this.firstASTNode = this.current = astNode;
        }
        else {
            this.current = (this.current.nextASTNode = astNode);
        }
        size++;
    }

    public void addTokenNode(ASTNode astNode, ASTNode token2) {
        if (this.firstASTNode == null) {
            this.current = ((this.firstASTNode = astNode).nextASTNode = token2);
        }
        else {
            this.current = (this.current.nextASTNode = astNode).nextASTNode = token2;
        }
    }


    public ASTNode firstToken() {
        return firstASTNode;
    }

    public void reset() {
        this.current = firstASTNode;
    }

    public boolean hasMoreTokens() {
        return this.current != null;
    }

    public ASTNode nextToken() {
        if (current == null) return null;
        try {
            return current;
        }
        finally {
            current = current.nextASTNode;
        }
    }

    public void skipToken() {
        if (current != null)
            current = current.nextASTNode;
    }

    public ASTNode peekNext() {
        if (current != null && current.nextASTNode != null)
            return current.nextASTNode;
        else
            return null;
    }

    public ASTNode peekToken() {
        if (current == null) return null;
        return current.nextASTNode;
    }

    public void removeToken() {
        if (current != null) {
            current = current.nextASTNode;
        }
    }

    public ASTNode peekLast() {
        throw new RuntimeException("unimplemented");
    }

    public ASTNode tokensBack(int offset) {
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

    public void finish() {
        reset();

        ASTNode last = null;
        ASTNode curr;

        while (hasMoreTokens()) {
            curr = nextToken();

            if (curr.isDiscard()) {
                if (last == null) {
                    firstASTNode = nextToken();
                }
                else {
                    last.nextASTNode = nextToken();
                }
                continue;
            }

            if (!hasMoreTokens()) break;

            if (nextToken().isDiscard()) {
                curr.nextASTNode = nextToken();
            }

            last = curr;
        }

        reset();
    }

}
