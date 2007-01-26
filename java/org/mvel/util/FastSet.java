package org.mvel.util;

import java.util.AbstractSet;
import java.util.Iterator;

public class FastSet extends AbstractSet {
    Object[] elements;
    private int size;


    public FastSet(int size) {
        this.elements = new Object[size];
    }


    public FastSet(Object[] elements) {
        this.size = (this.elements = elements).length;
    }

    public Iterator iterator() {
        return new SetIterator(elements);
    }

    public int size() {
        return elements.length;
    }


    public boolean add(Object o) {
        elements[size++] = o;
        return true;
    }

    public static class SetIterator implements Iterator {
        public SetIterator(Object[] elements) {
            this.elements = elements;
        }

        Object[] elements;
        int cursor;

        public boolean hasNext() {
            return cursor < elements.length;
        }

        public Object next() {
            return elements[cursor++];
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public boolean equals(Object object) {
        if (object instanceof FastSet) {
            FastSet s = (FastSet) object;
            if (size != s.size) return false;
            for (int i = 0; i < size; i++) {
                if (!elements[i].equals(s.elements[i])) return false;
            }
            return true;
        }
        return false;

    }
}
