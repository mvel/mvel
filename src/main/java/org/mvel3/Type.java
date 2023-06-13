package org.mvel3;

public class Type<T> {
    private final Class<T> clazz;

    private final String generics;

    public Type(Class clazz, String generics) {
        this.clazz = (Class<T>) clazz;
        this.generics = generics != null ? generics : "";
    }

    public static <K> Type<K> type(Class clazz) {
        return new Type(clazz, "");
    }

    public static <K> Type<K> type(Class clazz, String generics) {
        return new Type(clazz, generics);
    }

    public Class<T> getClazz() {
        return clazz;
    }

    public String getGenerics() {
        return generics;
    }

    public String getCanonicalGenericsName() {
        return clazz.getCanonicalName() + generics;
    }

    public boolean isVoid() {
        return clazz == Void.class;
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Type<T> type = (Type<T>) o;

        if (!clazz.equals(type.clazz)) {
            return false;
        }
        return generics.equals(type.generics);
    }

    @Override
    public int hashCode() {
        int result = clazz.hashCode();
        result = 31 * result + generics.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Type{" +
               "clazz=" + clazz +
               ", generics='" + generics + '\'' +
               '}';
    }
}
