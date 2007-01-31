package org.mvel.util;


import java.util.AbstractMap;
import java.util.Set;

public class FastMap<K, V> extends AbstractMap<K, V> {
    private Set entrySet;
    private Node[] values;
    private int size;
    private boolean init = false;

    private Object[] lateInitKeys;
    private Object[] lateInitVals;

    public FastMap(int size, Object[] keys, Object[] values) {
        this.size = size;
        lateInitKeys = keys;
        lateInitVals = values;
    }

    public FastMap(int capacity) {
        entrySet = new FastSet(capacity);
        values = new Node[capacity * 2];

        lateInitKeys = new Object[capacity];
        lateInitVals = new Object[capacity];
    }

    public Set entrySet() {
        return entrySet;
    }


    public V put(Object key, Object value) {
        if (!init) {
            lateInitKeys[size] = key;
            lateInitVals[size] = value;

            size++;
            return null;
        }

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

    public V get(Object key) {
        if (!init) {
            initialize();
        }

        return (V) values[hash(key)].getValue(key);
    }

    private void initialize() {
        int preInitSize = size;
        size = 0;
        init = true;
        entrySet = new FastSet(preInitSize);
        values = new Node[preInitSize * 2];
        for (int i = 0; i < preInitSize; i++) {
            put(lateInitKeys[i], lateInitVals[i]);
        }
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

    public static class Node<K, V> {
        private Node next;
        private K key;
        private V value;


        public Node(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public V getValue(K key) {
            Node n = this;
            do {
                if (key.equals(n.key)) return value;
            }
            while ((n = next) != null);

            return null;
        }

        public Node add(K key, V value) {
            Node n = new Node(key, value);
            n.next = this;
            return n;
        }


        public String toString() {
            return String.valueOf(value);
        }
    }


    public boolean equals(Object o) {
        if (!init) initialize();
        if ((o instanceof FastMap) && !((FastMap) o).init) ((FastMap) o).initialize();
        return o instanceof FastMap && entrySet.equals(((FastMap) o).entrySet);
    }


    public String toString() {
        return String.valueOf(values);
    }
}
