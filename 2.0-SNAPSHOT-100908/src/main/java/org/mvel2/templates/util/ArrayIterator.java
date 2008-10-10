package org.mvel2.templates.util;

import java.util.Iterator;

public class ArrayIterator implements Iterator {
    private Object[] array;
    private int cursor = 0;

    public ArrayIterator(Object[] array) {
        this.array = array;
    }

    public boolean hasNext() {
        return cursor != array.length;
    }

    public Object next() {
        return array[cursor++];
    }

    public void remove() {
    }
}
