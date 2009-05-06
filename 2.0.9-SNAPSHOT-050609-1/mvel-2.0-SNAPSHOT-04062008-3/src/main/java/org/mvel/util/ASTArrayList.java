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
package org.mvel.util;

import org.mvel.ast.ASTNode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.NoSuchElementException;

/**
 * A fast, array-based implementation of the ASTIterator.  Primarily used in compiled statements for fast execution.
 */
@SuppressWarnings({"ManualArrayCopy"})
public class ASTArrayList implements ASTIterator, Serializable {
    private ASTNode[] astNodes;
    private int length = 0;
    private int cursor = 0;

    public ASTArrayList() {
        astNodes = new ASTNode[7];
    }

    public ASTArrayList(int ensureCapacity) {
        astNodes = new ASTNode[ensureCapacity];
    }

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

    public void addTokenNode(ASTNode node) {
        growIfFull(1);
        astNodes[length++] = node;
    }

    public void addTokenNode(ASTNode node1, ASTNode node2) {
        growIfFull(2);
        astNodes[length++] = node1;
        astNodes[length++] = node2;
    }

    public ASTNode remove(int index) {
        if (index >= length) {
            throw new NoSuchElementException("" + index);
        }

        ASTNode n = astNodes[index];

        for (int i = index; i < (length - 1); i++) {
            astNodes[i] = astNodes[i + 1];
        }

        length--;

        return n;
    }

    public ASTNode remove(ASTNode node) {
        return remove(indexOf(node));
    }

    public int indexOf(ASTNode n) {
        for (int i = 0; i < length; i++) {
            if (astNodes[i].equals(n)) return i;
        }
        return -1;
    }

    public ASTNode set(int index, ASTNode n) {
        if (index >= length) {
            throw new IndexOutOfBoundsException("" + index);
        }

        ASTNode old = astNodes[index];
        astNodes[index] = n;
        return old;
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

    public ASTNode peekLast() {
        if (length > 0) {
            return astNodes[length - 1];
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

    private void growIfFull(int plannedIncrease) {
        if ((length + plannedIncrease) >= astNodes.length) {
            ASTNode[] newNodes = new ASTNode[astNodes.length * 2];

            for (int i = 0; i < length; i++) {
                newNodes[i] = astNodes[i];
            }

            astNodes = newNodes;
        }
    }

    public void finish() {
        ASTArrayList newList = new ASTArrayList(length);

        int len = length;
        for (int i = 0; i < len; i++) {
            if (!astNodes[i].isDiscard()) {
                newList.addTokenNode(astNodes[i]);
            }
            else {
                length--;
            }
        }

        astNodes = newList.astNodes;

        reset();
    }

    public ASTLinkedList toASTLinkedList() {
        ASTLinkedList list = new ASTLinkedList();
        for (int i = 0; i < length; i++) {
            if (astNodes[i] == null) {
                System.out.println("NULL");
            }
            if (!astNodes[i].isDiscard()) list.addTokenNode(astNodes[i]);
        }
        list.reset();
        return list;
    }
}
