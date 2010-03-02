package org.mvel2.util;

import java.util.HashMap;
import java.util.Map;

public class MapBuilder<K, V, T extends Map<K, V>> {
    public static MapBuilder s() {
        return start();
    }

    public static MapBuilder start() {
        return start(HashMap.class);
    }

    public static <K, V, T extends Map<K, V>> MapBuilder<K, V, T> s(Class<T> mapImpl) {
        return start(mapImpl);
    }

    public static <K, V, T extends Map<K, V>> MapBuilder<K, V, T> start(Class<T> mapImpl) {
        try {
            return new MapBuilder(mapImpl.newInstance());
        }
        catch (Throwable t) {
            throw new RuntimeException("error creating instance", t);
        }
    }

    private T mapInstance;

    public MapBuilder(T mapInstance) {
        this.mapInstance = mapInstance;
    }

    public MapBuilder _(K key, V value) {
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
