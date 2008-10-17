package org.mvel.util;


import org.mvel.DataTypes;

import static java.lang.Double.parseDouble;
import static java.lang.Float.parseFloat;
import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
import static java.lang.String.valueOf;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import static java.lang.reflect.Modifier.PUBLIC;
import static java.lang.reflect.Modifier.isPublic;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;

public class PropertyTools {
//    private static final Pattern truePattern = compile("(on|yes|true|1|hi|high|y)");

    public static boolean isEmpty(Object o) {
        if (o != null) {
            if (o instanceof Object[]) {
                return ((Object[]) o).length == 0 ||
                        (((Object[]) o).length == 1 && isEmpty(((Object[]) o)[0]));
            }
            else {
                return ("".equals(valueOf(o)))
                        || "null".equals(valueOf(o))
                        || (o instanceof Collection && ((Collection) o).size() == 0)
                        || (o instanceof Map && ((Map) o).size() == 0);
            }
        }
        return true;
    }


    public static Method getSetter(Class clazz, String property) {
        property = ReflectionUtil.getSetter(property);

        for (Method meth : clazz.getMethods()) {
            if ((meth.getModifiers() & PUBLIC) == 0
                    && meth.getParameterTypes().length != 0) continue;

            if (property.equals(meth.getName())) {
                return meth;
            }
        }

        return null;

    }

    public static boolean hasGetter(Field field) {
        Method meth = getGetter(field.getDeclaringClass(), field.getName());
        return meth != null && field.getType().isAssignableFrom(meth.getReturnType());
    }

    public static boolean hasSetter(Field field) {
        Method meth = getSetter(field.getDeclaringClass(), field.getName());
        return meth != null && meth.getParameterTypes().length == 1 &&
                field.getType().isAssignableFrom(meth.getParameterTypes()[0]);
    }

    public static Method getGetter(Class clazz, String property) {
        String isGet = ReflectionUtil.getIsGetter(property);
        property = ReflectionUtil.getGetter(property);

        for (Method meth : clazz.getMethods()) {
            if ((meth.getModifiers() & PUBLIC) == 0
                    || meth.getParameterTypes().length != 0
                    ) {
            }
            else if (property.equals(meth.getName()) ||
                    isGet.equals(meth.getName())) {
                return meth;
            }
        }

        return null;
    }

    public static boolean isPropertyReadAndWrite(Field field) {
        return isPublic(field.getModifiers()) || hasGetter(field) && hasSetter(field);
    }

    public static boolean isPropertyReadAndWrite(Class clazz, String property) {
        return getWritableFieldOrAccessor(clazz, property) != null &&
                getFieldOrAccessor(clazz, property) != null;
    }

    public static Member getWritableFieldOrAccessor(Class clazz, String property) {
        Field field;
        try {
            if ((field = clazz.getField(property)) != null &&
                    isPublic(field.getModifiers())) return field;
        }
        catch (NullPointerException e) {
            return null;
        }
        catch (NoSuchFieldException e) {
            // do nothing.
        }

        return getSetter(clazz, property);
    }

    public static Member getFieldOrAccessor(Class clazz, String property) {
        if (property.charAt(property.length() - 1) == ')') return getGetter(clazz, property);

        for (Field f : clazz.getFields()) {
            if (property.equals(f.getName())) {
                if ((f.getModifiers() & PUBLIC) != 0) return f;
                break;
            }
        }

        return getGetter(clazz, property);
    }

    public static Member getFieldOrWriteAccessor(Class clazz, String property) {
        Field field;
        try {
            if ((field = clazz.getField(property)) != null &&
                    isPublic(field.getModifiers())) {
                return field;
            }
        }
        catch (NullPointerException e) {
            return null;
        }
        catch (NoSuchFieldException e) {
            // do nothing.
        }

        return getSetter(clazz, property);
    }


    public static boolean isNumeric(Object val) {
        if (val == null) return false;

        Class clz;
        if (val instanceof Class) {
            clz = (Class) val;
        }
        else {
            clz = val.getClass();
        }

        return clz == int.class || clz == long.class || clz == short.class || clz == double.class ||
                clz == float.class || Number.class.isAssignableFrom(clz);

    }


    public static Object handleNumericConversion(final char[] val) {
        switch (numericTest(val)) {
            case DataTypes.FLOAT:
                return parseFloat(new String(val));
            case DataTypes.INTEGER:
                return parseInt(new String(val));
            case DataTypes.LONG:
                return parseLong(new String(val));
            case DataTypes.DOUBLE:
                return parseDouble(new String(val));
            case DataTypes.BIG_DECIMAL:
                // @todo: new String() only needed for jdk1.4, remove when we move to jdk1.5
                return new BigDecimal(new String(val));
            default:
                return new String(val);
        }
    }

    public static int numericTest(final char[] val) {
        boolean fp = false;

        int len = val.length;
        char c;
        int i = 0;

        if (len > 1) {
            if (val[0] == '-') i++;
            else if (val[0] == '~') {
                i++;
                if (val[1] == '-') i++;
            }
        }

        for (; i < len; i++) {
            if (!isDigit(c = val[i])) {
                if (c == '.') {
                    len = 0;
                    fp = true;
                    // continue;
                }
                else {
                    return -1;
                }
            }
        }

        if (len > 0) {
            if (fp) {
                if (len > 17) {
                    return DataTypes.BIG_DECIMAL;
                }
                else if (len > 15) {
                    // requires float
                    return DataTypes.FLOAT;
                }
                else {
                    return DataTypes.DOUBLE;
                }
            }
            else if (len > 11) {
                return DataTypes.BIG_DECIMAL;
            }
            else if (len > 9) {
                return DataTypes.LONG;
            }
            else {
                return DataTypes.INTEGER;
            }
        }
        return -1;
    }


    public static boolean isNumber(Object val) {
        if (val == null) return false;
        if (val instanceof String) return isNumber((String) val);
        if (val instanceof char[]) return isNumber((char[]) val);
        return val instanceof Integer || val instanceof BigDecimal || val instanceof BigInteger
                || val instanceof Float || val instanceof Double || val instanceof Long
                || val instanceof Short || val instanceof Character;
    }


    public static boolean isNumber(final String val) {
        int len = val.length();
        char c;
        boolean f = true;
        int i = 0;
        if (len > 1) {
            if (val.charAt(0) == '-') i++;
            else if (val.charAt(0) == '~') {
                i++;
                if (val.charAt(1) == '-') i++;
            }
        }
        for (; i < len; i++) {
            if (!isDigit(c = val.charAt(i))) {
                if (c == '.' && f) {
                    f = false;
                }
                else {
                    return false;
                }
            }
        }

        return len > 0;
    }

    public static boolean isNumber(char[] val) {
        int len = val.length;
        char c;
        boolean f = true;
        int i = 0;
        if (len > 1) {
            if (val[0] == '-') i++;
            else if (val[0] == '~') {
                i++;
                if (val[1] == '-') i++;
            }
        }
        for (; i < len; i++) {
            if (!isDigit(c = val[i])) {
                if (c == '.' && f) {
                    f = false;
                }
                else {
                    return false;
                }
            }
        }

        return len > 0;
    }


    public static boolean contains(Object toCompare, Object testValue) {
        if (toCompare == null)
            return false;
        else if (toCompare instanceof String)
            // @todo use String.contains once we move to jdk1.5
            return ((String) toCompare).indexOf(valueOf(testValue).toString()) > -1;
        else if (toCompare instanceof Collection)
            return ((Collection) toCompare).contains(testValue);
        else if (toCompare instanceof Map)
            return ((Map) toCompare).containsKey(testValue);
        else if (toCompare.getClass().isArray()) {
            for (Object o : ((Object[]) toCompare)) {
                if (testValue == null && o == null) return true;
                else if (o != null && o.equals(testValue)) return true;
            }
        }
        return false;
    }

    public static int find(char[] c, char find) {
        for (int i = 0; i < c.length; i++) if (c[i] == find) return i;
        return -1;
    }

    public static String createStringTrimmed(char[] s) {
        int start = 0, end = s.length;
        while (start != end && s[start] <= '\u0020') start++;
        while (end != start && s[end - 1] <= '\u0020') end--;

        return new String(s, start, end - start);
    }

    public static String createStringTrimmed(char[] s, int start, int length) {
        int end = start + length;
        while (start != end && s[start] <= '\u0020') {
            start++;
        }
        while (end != start && s[end - 1] <= '\u0020') {
            end--;
        }
        return new String(s, start, end - start);
    }


    public static boolean equals(char[] obj1, String obj2) {
        for (int i = 0; i < obj1.length && i < obj2.length(); i++) {
            if (obj1[i] == obj2.charAt(i)) return false;
        }
        return true;
    }


    public static boolean isIdentifierPart(final int c) {
        return ((c >= 97 && c <= 122)
                || (c >= 65 && c <= 90) || (c >= 48 && c <= 57) || (c == '_') || (c == '$'));
    }

    public static boolean isDigit(final int c) {
        return c >= '0' && c <= '9';
    }


    public static float similarity(String s1, String s2) {
        if (s1 == null || s2 == null)
            return s1 == null && s2 == null ? 1f : 0f;

        char[] c1 = s1.toCharArray();
        char[] c2 = s2.toCharArray();

        char[] comp;
        char[] against;

        float same = 0;
        float baselength;

        int cur1 = 0;

        if (c1.length > c2.length) {
            baselength = c1.length;
            comp = c1;
            against = c2;
        }
        else {
            baselength = c2.length;
            comp = c2;
            against = c1;
        }


        while (cur1 < comp.length && cur1 < against.length) {
            if (comp[cur1] == against[cur1]) {
                same++;
            }

            cur1++;
        }

        return same / baselength;
    }

    public static int findAbsoluteLast(char[] array) {
        int depth = 0;
        for (int i = array.length - 1; i >= 0; i--) {
            if (array[i] == ']') {
                depth++;
            }
            if (array[i] == '[') {
                depth--;
            }

            if (depth == 0 && array[i] == '.' || array[i] == '[') return i;
        }
        return -1;
    }

    public static Class getBaseComponentType(Class cls) {
        while (cls.isArray()) {
            cls = cls.getComponentType();
        }
        return cls;
    }

    public static Class getSubComponentType(Class cls) {
        if (cls.isArray()) {
            cls = cls.getComponentType();
        }
        return cls;
    }

}
