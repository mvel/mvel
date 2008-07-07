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

public class Node implements Cloneable {
    private int nodeType = NodeType.PROPERTY_EX;

    private int startPos;
    private int length;

    private int node;
    private int endNode;

    private String alias;

    private Object register;

    private String name;

    Node() {
    }

    Node(int startPos) {
        this.startPos = startPos;
    }

    Node(int node, int nodeType) {
        this.node = node;
        this.nodeType = nodeType;
    }

    Node(int node, int nodeType, int endNode) {
        this.node = node;
        this.nodeType = nodeType;
        this.endNode = endNode;
    }


    Node(int node, int nodeType, int startPos, int length, int endNode) {
        this.nodeType = nodeType;
        this.startPos = startPos;
        this.length = length;
        this.node = node;
        this.endNode = endNode;
    }

    public int getToken() {
        return nodeType;
    }

    public void setToken(int nodeType) {
        this.nodeType = nodeType;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getStartPos() {
        return startPos;
    }

    public void setStartPos(int startPos) {
        this.startPos = startPos;
    }

    public int getEndPos() {
        return startPos + length;
    }

    public int getNode() {
        return node;
    }

    public void setEndPos(int position) {
        this.length = position - startPos;
    }


    public Node setNode(int node) {
        this.node = node;
        this.endNode = node + 1;

        return this;
    }

    public int getEndNode() {
        return endNode;
    }

    public void setEndNode(int endNode) {
        this.endNode = endNode;
    }

    public Object getRegister() {
        return register;
    }

    public void setRegister(Object register) {
        this.register = register;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNodeType() {
        return nodeType;
    }

    public void setNodeType(int nodeType) {
        this.nodeType = nodeType;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }


    protected Node clone() throws CloneNotSupportedException {
        Node n = new Node();
        n.nodeType = nodeType;
        n.startPos = startPos;
        n.length = length;
        n.node = node;
        n.endNode = endNode;
        n.alias = alias;
        n.name = name;
        n.register = register;

        return n;
    }
}
