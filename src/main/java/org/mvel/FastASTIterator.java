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

import org.mvel.util.StringAppender;

import java.io.Serializable;
import java.util.ArrayList;

public class FastASTIterator implements ASTIterator, Serializable {
    private final ASTNode[] astNodes;
    private int length = 0;
    private int cursor = 0;


    public FastASTIterator(final FastASTIterator fi) {
        astNodes = fi.astNodes;
        length = fi.length;
    }

    public FastASTIterator(ASTIterator map) {
        map.finish();

        if (map instanceof FastASTIterator) {
            this.length = (this.astNodes = ((FastASTIterator) map).astNodes).length;
        }
        else {
            ArrayList<ASTNode> astNodes = new ArrayList<ASTNode>();
            map.reset();
            while (map.hasMoreTokens()) {
                astNodes.add(map.nextToken());
            }

            this.astNodes = astNodes.toArray(new ASTNode[length = astNodes.size()]);
        }
    }

    public void reset() {
        cursor = 0;
    }


    public ASTNode firstToken() {
        return astNodes[0];
    }

    public ASTNode nextToken() {
        if (cursor < length)
            return astNodes[cursor++];
        else
            return null;
    }


    public void skipToken() {
        cursor++;
    }


    public ASTNode peekNext() {
        if (cursor < length)
            return astNodes[cursor + 1];
        else
            return null;
    }

    public ASTNode peekToken() {
        if (cursor < length)
            return astNodes[cursor];
        else
            return null;
    }

//    public boolean peekNextTokenFlags(int flags) {
//        return cursor < length && (token[cursor].getFlags() & flags) != 0;
//    }


    public ASTNode peekLast() {
        if (cursor > 0) {
            return astNodes[cursor - 1];
        }
        else {
            return null;
        }
    }


    public ASTNode tokensBack(int offset) {
        if (cursor - offset >= 0) {
            return astNodes[cursor - offset];
        }
        else {
            return null;
        }
    }

    public void back() {
        cursor--;
    }

    public boolean hasMoreTokens() {
        return cursor < length;
    }


    public String showTokenChain() {
        StringAppender sb = new StringAppender();
        for (int i = 0; i < length; i++) {
            sb.append("(" + i + "): <<" + astNodes[i].getName() + ">> = <<" + astNodes[i].getLiteralValue() + ">> [" + (astNodes[i].getLiteralValue() != null ? astNodes[i].getLiteralValue().getClass() : "null") + "]").append("\n");
        }

        return sb.toString();
    }


    public int size() {
        return length;
    }


    public int index() {
        return cursor;
    }


    public void finish() {


    }
}
