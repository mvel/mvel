package org.mvel.util;


import java.util.AbstractMap;
import java.util.Set;

public class FastMap extends AbstractMap {
    private Set entrySet;
    private Node[] values;
    private int size;
    private int capacity;

    public FastMap(int capacity) {
        entrySet = new FastSet(this.capacity = capacity);
        values = new Node[capacity * 2];
    }

    public Set entrySet() {
        return entrySet;
    }


    public Object put(Object key, Object value) {

        size++;
        entrySet.add(key);

        int target = hash(key);

        if (values[target] != null)
            values[target] = values[target].add(key, value);
        else
            values[target] = new Node(key, value);

        return null;
    }


    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    public Object get(Object key) {
        return values[hash(key)].getValue(key);
    }


    public int size() {
        return size;
    }


    public boolean isEmpty() {
        return size == 0;
    }

    public int hash(Object key) {
        int hashCode = key.hashCode();
        hashCode ^= (hashCode >>> 20) ^ (hashCode >>> 12);
        return (hashCode ^ (hashCode >>> 7) ^ (hashCode >>> 4)) & values.length - 1;
    }

    public static class Node {
        private Node next;
        private Object key;
        private Object value;


        public Node(Object key, Object value) {
            this.key = key;
            this.value = value;
        }

        public Object getValue(Object key) {
            Node n = this;
            do {
                if (key.equals(n.key)) return value;
            }
            while ((n = next) != null);

            return null;
        }

        public Node add(Object key, Object value) {
            Node n = new Node(key, value);
            n.next = this;
            return n;
        }
    }


    public boolean equals(Object o) {
        if (o instanceof FastMap) {
            FastMap f = (FastMap) o;
            return entrySet.equals(f.entrySet);
        }
        return false;
    }

    public String toString() {
        return String.valueOf(values);
    }
}
