package org.mvel2.util;

public class CompatibilityStrategy {

    private CompatibilityStrategy() { }

    public interface CompatibilityEvaluator {
        boolean areCompatible(Class<?> c1, Class<?> c2);
    }

    public static CompatibilityEvaluator compatibilityEvaluator = new DefaultCompatibilityEvaluator();

    public static boolean areCompatible(Class<?> c1, Class<?> c2) {
        return compatibilityEvaluator.areCompatible(c1, c2);
    }

    public static void setCompatibilityEvaluator(CompatibilityEvaluator compatibilityEvaluator) {
        CompatibilityStrategy.compatibilityEvaluator = compatibilityEvaluator;
    }

    public static class DefaultCompatibilityEvaluator implements CompatibilityEvaluator {

        public boolean areCompatible(Class<?> c1, Class<?> c2) {
            if (c1.isAssignableFrom(c2) || c2.isAssignableFrom(c1)) return true;
            if (isBoxedNumber(c1, false) && isBoxedNumber(c2, true)) return true;
            if (c1.isPrimitive()) return c2.isPrimitive() || arePrimitiveCompatible(c1, c2, true);
            if (c2.isPrimitive()) return arePrimitiveCompatible(c2, c1, false);
            return false;
        }

        private boolean arePrimitiveCompatible(Class<?> primitive, Class<?> boxed, boolean leftFirst) {
            if (primitive == Boolean.TYPE) return boxed == Boolean.class;
            if (primitive == Integer.TYPE) return isBoxedNumber(boxed, leftFirst);
            if (primitive == Long.TYPE) return isBoxedNumber(boxed, leftFirst);
            if (primitive == Double.TYPE) return isBoxedNumber(boxed, leftFirst);
            if (primitive == Float.TYPE) return isBoxedNumber(boxed, leftFirst);
            if (primitive == Character.TYPE) return boxed == Character.class;
            if (primitive == Byte.TYPE) return boxed == Byte.class;
            if (primitive == Short.TYPE) return boxed == Short.class;
            return false;
        }

        private boolean isBoxedNumber(Class<?> c, boolean allowString) {
            return Number.class.isAssignableFrom(c) || (allowString && c == String.class);
        }
    }
}
