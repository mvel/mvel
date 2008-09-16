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

public class ExecutionStack implements Stack {
    private StackElement element;
    private int size = 0;

    public boolean isEmpty() {
        return size == 0;
    }

    public void add(Object o) {
        size++;
        StackElement el = element;
        if (el != null) {
            while (el.next != null) {
                el = el.next;
            }
            el.next = new StackElement(null, o);
        }
        else {
            element = new StackElement(null, o);
        }
    }

    public void push(Object o) {
        size++;
        element = new StackElement(element, o);
    }

    public Object pushAndPeek(Object o) {
        size++;
        element = new StackElement(element, o);
        return o;
    }


    public void push(Object obj1, Object obj2) {
        size += 2;
        element = new StackElement(new StackElement(element, obj1), obj2);
    }

    public void push(Object obj1, Object obj2, Object obj3) {
        size += 3;
        element = new StackElement(new StackElement(new StackElement(element, obj1), obj2), obj3);
    }

    public Object peek() {
        if (size == 0) return null;
        else return element.value;
    }

    public Object peek2() {
        if (size < 2) return null;
        return element.next.value;
    }

    public Object pop() {
        if (size-- == 0) return null;
        try {
            return element.value;
        }
        finally {
            element = element.next;
        }
    }

    public void discard() {
        if (size != 0) {
            size--;
            element = element.next;
        }
    }

    public int size() {
        return size;
    }

    public void clear() {
        size = 0;
        element = null;
    }


    public void showStack() {
        StackElement el = element;
        do {
            System.out.println("->" + el.value);
        }
        while ((el = el.next) != null);
    }

    public String toString() {
        StackElement el = element;

        if (element == null) return "<EMPTY>";

        StringAppender appender = new StringAppender();
        appender.append("[");
        do {
            appender.append(String.valueOf(el.value));
            if (el.next != null) appender.append(", ");
        } while ((el = el.next) != null);

        appender.append("]");

        return appender.toString();
    }

}
