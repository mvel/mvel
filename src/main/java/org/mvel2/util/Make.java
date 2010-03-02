package org.mvel2.util;

import java.util.ArrayList;
import java.util.HashMap;

public class Make {
    public static class Map<K, V, T extends java.util.Map<K, V>> {
        public static Map s() {
            return start();
        }

        public static Map start() {
            return start(HashMap.class, null, null);
        }

        public static <K, V, T extends java.util.Map<K, V>> Map<?, ?, T> start(Class<T> mapImpl) {
            return (Map<?, ?, T>) start(HashMap.class, Object.class, Object.class);
        }

        public static <K, V, T extends java.util.Map<K, V>> Map<K, V, T> s(Class<K> keyType, Class<V> valType) {
            return (Map<K, V, T>) start(HashMap.class, null, null);
        }

        public static <K, V, T extends java.util.Map<K, V>> Map<K, V, T> s(Class<T> mapImpl, Class<K> keyType, Class<V> valType) {
            return start(mapImpl, null, null);
        }

        public static <K, V, T extends java.util.Map<K, V>> Map<K, V, T> start(Class<T> mapImpl, Class<K> keyType, Class<V> valType) {
            try {
                return new Map(mapImpl.newInstance());
            }
            catch (Throwable t) {
                throw new RuntimeException("error creating instance", t);
            }
        }

        private T mapInstance;

        public Map(T mapInstance) {
            this.mapInstance = mapInstance;
        }

        public Map _(K key, V value) {
            mapInstance.put(key, value);
            return this;
        }

        public T f() {
            return finish();
        }

        public T finish() {
            return mapInstance;
        }
    }

    public static class String {
        public static String s() {
            return start();
        }

        public static String start() {
            return new String(new StringAppender());
        }

        public java.lang.String f() {
            return finish();
        }

        public java.lang.String finish() {
            return stringAppender.toString();
        }

        private StringAppender stringAppender;

        public String(StringAppender stringAppender) {
            this.stringAppender = stringAppender;
        }

        public String _(char c) {
            stringAppender.append(c);
            return this;
        }

        public String _(CharSequence cs) {
            stringAppender.append(cs);
            return this;
        }

        public String _(String s) {
            stringAppender.append(s);
            return this;
        }
    }

    public static class List<V, T extends java.util.List<V>> {
        public static List s() {
            return start();
        }

        public static List start() {
            return start(ArrayList.class, null);
        }

        public static <V, T extends java.util.List<V>> List<V, T> s(Class<T> listImpl) {
            return start(listImpl, null);
        }

        public static <V, T extends java.util.List<V>> List<V, T> s(Class<T> listImpl, Class<V> valType) {
            return start(listImpl, null);
        }

        public static <V, T extends java.util.List<V>> List<V, T> start(Class<T> listImpl, Class<V> valType) {
            try {
                return new List(listImpl.newInstance());
            }
            catch (Throwable t) {
                throw new RuntimeException("error creating instance", t);
            }
        }

        private T listInstance;

        public List(T listInstance) {
            this.listInstance = listInstance;
        }

        public List _(V value) {
            listInstance.add(value);
            return this;
        }

        public T f() {
            return finish();
        }

        public T finish() {
            return listInstance;
        }
    }
}
