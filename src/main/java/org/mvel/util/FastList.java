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

import org.mvel.ImmutableElementException;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.*;

public class FastList extends AbstractList implements Externalizable {
    private Object[] elements;
    private int size = 0;
    private boolean updated = false;

    public FastList(int size) {
        elements = new Object[size];
    }

    public FastList(Object[] elements) {
        this.size = (this.elements = elements).length;
    }

    public FastList() {
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(size);
        for (int i = 0; i < size; i++) {
            out.writeObject(elements[i]);
        }
    }

    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException {
        elements = new Object[size = in.readInt()];
        for (int i = 0; i < size; i++) {
            elements[i] = in.readObject();
        }
    }

    public Object get(int index) {
        return elements[index];
    }

    public int size() {
        return size;
    }

    public boolean add(Object o) {
        if (size == elements.length) {
            increaseSize(elements.length * 2);
        }

        elements[size++] = o;
        return true;
    }

    public Object set(int i, Object o) {
        if (!updated) copyArray();
        Object old = elements[i];
        elements[i] = o;
        return old;
    }

    public void add(int i, Object o) {
        if (size == elements.length) {
            increaseSize(elements.length * 2);
        }

        for (int c = size; c != i; c--) {
            elements[c] = elements[c - 1];
        }
        elements[i] = o;
        size++;
    }

    public Object remove(int i) {
        Object old = elements[i];
        for (int c = i + 1; c < size; c++) {
            elements[c - 1] = elements[c];
            elements[c] = null;
        }
        size--;
        return old;
    }

    public int indexOf(Object o) {
        if (o == null) return -1;
        for (int i = 0; i < elements.length; i++) {
            if (o.equals(elements[i])) return i;
        }
        return -1;
    }

    public int lastIndexOf(Object o) {
        if (o == null) return -1;
        for (int i = elements.length - 1; i != -1; i--) {
            if (o.equals(elements[i])) return i;
        }
        return -1;
    }

    public void clear() {
        elements = new Object[0];
    }

    public boolean addAll(int i, Collection collection) {
        int offset = collection.size();
        ensureCapacity(offset);

        for (int c = i; c != (i + offset); c++) {
            elements[c + offset + 1] = elements[c];
        }

        int c = 0;
        for (Object o : collection) {
            elements[offset + c] = o;
        }

        return true;
    }

    public Iterator iterator() {
        final int size = this.size;
        return new Iterator() {
            private int cursor = 0;

            public boolean hasNext() {
                return cursor < size;
            }

            public Object next() {
                return elements[cursor++];
            }

            public void remove() {
                throw new ImmutableElementException("cannot change elements in immutable list");
            }
        };

    }

    public ListIterator listIterator() {
        return new ListIterator() {
            private int i = 0;

            public boolean hasNext() {
                return i < size;
            }

            public Object next() {
                return elements[i++];
            }

            public boolean hasPrevious() {
                return i > 0;
            }

            public Object previous() {
                return elements[i--];
            }

            public int nextIndex() {
                return i++;
            }

            public int previousIndex() {
                return i--;
            }

            public void remove() {
                throw new java.lang.UnsupportedOperationException();
            }

            public void set(Object o) {
                elements[i] = 0;
            }

            public void add(Object o) {
                throw new java.lang.UnsupportedOperationException();
            }
        };
    }

    public ListIterator listIterator(int i) {
        return super.listIterator(i);
    }

    public List subList(int i, int i1) {
        return super.subList(i, i1);
    }

    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof List))
            return false;

        ListIterator e1 = listIterator();
        ListIterator e2 = ((List) o).listIterator();
        while (e1.hasNext() && e2.hasNext()) {
            Object o1 = e1.next();
            Object o2 = e2.next();
            if (!(o1 == null ? o2 == null : o1.equals(o2)))
                return false;
        }
        return !(e1.hasNext() || e2.hasNext());
    }

    public int hashCode() {
        return super.hashCode();
    }

    protected void removeRange(int i, int i1) {
        throw new RuntimeException("not implemented");
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean contains(Object o) {
        return indexOf(o) != -1;
    }

    public Object[] toArray() {
        if (!updated) copyArray();
        return elements;
    }

    public Object[] toArray(Object[] objects) {
        throw new RuntimeException("not implemented");
    }

    public boolean remove(Object o) {
        throw new RuntimeException("not implemented");
    }

    public boolean containsAll(Collection collection) {
        throw new RuntimeException("not implemented");
    }

    public boolean addAll(Collection collection) {
        return addAll(size, collection);
    }

    public boolean removeAll(Collection collection) {
        throw new RuntimeException("not implemented");
    }

    public boolean retainAll(Collection collection) {
        throw new RuntimeException("not implemented");
    }

    private void ensureCapacity(int additional) {
        if ((size + additional) > elements.length) increaseSize((size + additional) * 2);
    }

    private void copyArray() {
        increaseSize(elements.length);
    }

    private void increaseSize(int newSize) {
        Object[] newElements = new Object[newSize];
        for (int i = 0; i < elements.length; i++)
            newElements[i] = elements[i];

        elements = newElements;

        updated = true;
    }


    public String toString() {
        return super.toString();
    }
}
