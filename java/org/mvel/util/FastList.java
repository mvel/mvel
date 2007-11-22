package org.mvel.util;

import org.mvel.ImmutableElementException;

import java.util.*;

public class FastList extends AbstractList {
    private final Object[] elements;
    private int size = 0;


    public FastList(int size) {
        elements = new Object[size];
    }

    public FastList(Object[] elements) {
        this.size = (this.elements = elements).length;
    }

    public Object get(int index) {
        return elements[index];
    }

    public int size() {
        return size;
    }

    public boolean add(Object o) {
        if (size == elements.length) {
            throw new ImmutableElementException("cannot add elements to immutable list");
        }

        elements[size++] = o;
        return true;
    }

    public Object set(int i, Object o) {
        throw new ImmutableElementException("cannot change elements in immutable list");
    }

    public void add(int i, Object o) {
        throw new ImmutableElementException("cannot change elements in immutable list");
    }

    public Object remove(int i) {
        throw new ImmutableElementException("cannot change elements in immutable list");
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
        throw new ImmutableElementException("cannot change elements in immutable list");
    }

    public boolean addAll(int i, Collection collection) {
        throw new ImmutableElementException("cannot change elements in immutable list");
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
        return super.listIterator();    //To change body of overridden methods use File | Settings | File Templates.
    }

    public ListIterator listIterator(int i) {
        return super.listIterator(i);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public List subList(int i, int i1) {
        return super.subList(i, i1);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public boolean equals(Object o) {
        return super.equals(o);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public int hashCode() {
        return super.hashCode();    //To change body of overridden methods use File | Settings | File Templates.
    }

    protected void removeRange(int i, int i1) {
        throw new ImmutableElementException("cannot change elements in immutable list");
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean contains(Object o) {
        return indexOf(o) != -1;
    }

    public Object[] toArray() {
        return super.toArray();    //To change body of overridden methods use File | Settings | File Templates.
    }

    public Object[] toArray(Object[] objects) {
        return super.toArray(objects);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public boolean remove(Object o) {
        throw new ImmutableElementException("cannot change elements in immutable list");
    }

    public boolean containsAll(Collection collection) {
        return super.containsAll(collection);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public boolean addAll(Collection collection) {
        throw new ImmutableElementException("cannot change elements in immutable list");
    }

    public boolean removeAll(Collection collection) {
        throw new ImmutableElementException("cannot change elements in immutable list");
    }

    public boolean retainAll(Collection collection) {
        throw new ImmutableElementException("cannot change elements in immutable list");
    }

    public String toString() {
        return super.toString();    //To change body of overridden methods use File | Settings | File Templates.
    }
}
