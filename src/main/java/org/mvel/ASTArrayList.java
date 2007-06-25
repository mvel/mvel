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

/**
 * A fast, array-based implementation of the ASTIterator.  Primarily used in compiled statements for fast execution.
 */
public class ASTArrayList implements ASTIterator, Serializable {
    private final ASTNode[] astNodes;
    private int length = 0;
    private int cursor = 0;

    public ASTArrayList(final ASTArrayList fi) {
        astNodes = fi.astNodes;
        length = fi.length;
    }

    public ASTArrayList(ASTIterator map) {
        map.finish();

        if (map instanceof ASTArrayList) {
            this.length = (this.astNodes = ((ASTArrayList) map).astNodes).length;
        }
        else {
            ArrayList<ASTNode> astNodes = new ArrayList<ASTNode>();
            map.reset();
            while (map.hasMoreNodes()) {
                astNodes.add(map.nextNode());
            }

            this.astNodes = astNodes.toArray(new ASTNode[length = astNodes.size()]);
        }
    }

    public void reset() {
        cursor = 0;
    }


    public ASTNode firstNode() {
        return astNodes[0];
    }

    public ASTNode nextNode() {
        if (cursor < length)
            return astNodes[cursor++];
        else
            return null;
    }


    public void skipNode() {
        cursor++;
    }


    public ASTNode peekNext() {
        if (cursor < length)
            return astNodes[cursor + 1];
        else
            return null;
    }

    public ASTNode peekNode() {
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


    public ASTNode nodesBack(int offset) {
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

    public boolean hasMoreNodes() {
        return cursor < length;
    }


    public String showNodeChain() {
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
