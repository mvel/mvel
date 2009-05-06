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

public class FastExecutionStack implements Stack {
    private final Object[] stack = new Object[15];
    private int size = 0;

    public boolean isEmpty() {
        return size == 0;
    }

    public Object peek() {
        return stack[size];
    }

    public void push(Object obj) {
        stack[++size] = obj;
    }

    public void add(Object obj) {
        throw new RuntimeException("add operation not supported with FastExecutionStack");
    }

    public Object pushAndPeek(Object obj) {
        return stack[++size] = obj;
    }

    public void push(Object obj1, Object obj2) {
        System.out.println("PUSH <<" + obj1 + ">>");
        System.out.println("PUSH <<" + obj2 + ">>");
        stack[++size] = obj1;
        stack[++size] = obj2;
    }

    public void push(Object obj1, Object obj2, Object obj3) {
        stack[++size] = obj1;
        stack[++size] = obj2;
        stack[++size] = obj3;
    }

    public Object pop() {
        return stack[size--];
    }

    public void discard() {
        --size;
    }

    public void clear() {
        size = 0;
    }

    public int size() {
        return size;
    }

    public void showStack() {

    }
}
