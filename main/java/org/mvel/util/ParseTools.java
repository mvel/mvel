package org.mvel.util;

import org.mvel.*;
import static org.mvel.DataConversion.canConvert;
import static org.mvel.DataConversion.convert;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.integration.impl.LocalVariableResolverFactory;
import static org.mvel.util.PropertyTools.isNumber;

import static java.lang.Character.isWhitespace;
import static java.lang.Class.forName;
import static java.lang.String.valueOf;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;

public class ParseTools {
    public static final Object[] EMPTY_OBJ_ARR = new Object[0];
    public static final MathContext MATH_CONTEXT = MathContext.DECIMAL128;

    public static String[] parseMethodOrConstructor(char[] parm) {
        int start = -1;
        for (int i = 0; i < parm.length; i++) {
            if (parm[i] == '(') {
                start = ++i;
                break;
            }
        }
        if (start != -1) {
            for (int i = parm.length - 1; i > 0; i--) {
                if (parm[i] == ')') {
                    return parseParameterList(parm, start, i - start);
                }
            }
        }

        return null;
    }

    public static String[] parseParameterList(char[] parm, int offset, int length) {
        List<String> list = new LinkedList<String>();

        if (length == -1)
            length = parm.length;

        int adepth = 0;

        int start = offset;
        int i = offset;
        int end = i + length;

        for (; i < end; i++) {
            switch (parm[i]) {
                case'[':
                case'{':
                    if (adepth++ == 0)
                        start = i;
                    continue;

                case']':
                case'}':
                    if (--adepth == 0) {
                        list.add(new String(parm, start, i - start + 1));

                        while (isWhitespace(parm[i]))
                            i++;

                        start = i + 1;
                    }
                    continue;

                case'\'':
                    int rStart = i;
                    while (++i < end && parm[i] != '\'') {
                        if (parm[i] == '\\')
                            handleEscapeSequence(parm[++i]);
                    }

                    if (i == end || parm[i] != '\'') {
                        throw new CompileException("unterminated literal starting at index " + rStart + ": " + new String(parm, offset, length));
                    }
                    continue;

                case'"':
                    rStart = i;
                    while (++i < end && parm[i] != '"') {
                        if (parm[i] == '\\')
                            handleEscapeSequence(parm[++i]);
                    }

                    if (i == end || parm[i] != '"') {
                        throw new CompileException("unterminated literal starting at index " + rStart + ": " + new String(parm, offset, length));
                    }
                    continue;

                case',':
                    if (adepth != 0)
                        continue;

                    if (i > start) {
                        while (isWhitespace(parm[start]))
                            start++;

                        list.add(new String(parm, start, i - start));
                    }

                    while (isWhitespace(parm[i]))
                        i++;

                    start = i + 1;
            }
        }

        if (start < length && i > start) {
            String s = new String(parm, start, i - start).trim();
            if (s.length() > 0)
                list.add(s);
        }
        else if (list.size() == 0) {
            String s = new String(parm, start, length).trim();
            if (s.length() > 0)
                list.add(s);
        }

        return list.toArray(new String[list.size()]);
    }

    private static Map<String, Map<Integer, Method>> RESOLVED_METH_CACHE = new WeakHashMap<String, Map<Integer, Method>>(10);

    public static Method getBestCanadidate(Object[] arguments, String method, Method[] methods) {
        Class[] parmTypes;
        Method bestCandidate = null;
        int bestScore = 0;
        int score = 0;

        Class[] targetParms = new Class[arguments.length];

        for (int i = 0; i < arguments.length; i++) {
            targetParms[i] = arguments[i] != null ? arguments[i].getClass() : Object.class;
        }

        Integer hash = createClassSignatureHash(targetParms);

        if (RESOLVED_METH_CACHE.containsKey(method) && RESOLVED_METH_CACHE.get(method).containsKey(hash)) {
            return RESOLVED_METH_CACHE.get(method).get(hash);
        }

        for (Method meth : methods) {
            if (method.equals(meth.getName())) {
                if ((parmTypes = meth.getParameterTypes()).length != arguments.length)
                    continue;
                else if (arguments.length == 0 && parmTypes.length == 0)
                    return meth;

                for (int i = 0; i < arguments.length; i++) {
                    if (parmTypes[i] == targetParms[i]) {
                        score += 5;
                    }
                    else if (parmTypes[i].isPrimitive() && boxPrimitive(parmTypes[i]) == targetParms[i]) {
                        score += 4;
                    }
                    else if (targetParms[i].isPrimitive() && unboxPrimitive(targetParms[i]) == parmTypes[i]) {
                        score += 4;
                    }
                    else if (isNumericallyCoercible(targetParms[i], parmTypes[i])) {
                        score += 3;
                    }
                    else if (parmTypes[i].isAssignableFrom(targetParms[i])) {
                        score += 2;
                    }
                    else if (canConvert(parmTypes[i], targetParms[i])) {
                        score += 1;
                    }
                    else {
                        score = 0;
                        break;
                    }
                }

                if (score != 0 && score > bestScore) {
                    bestCandidate = meth;
                    bestScore = score;
                }
                score = 0;
            }
        }

        if (bestCandidate != null) {
            if (!RESOLVED_METH_CACHE.containsKey(method))
                RESOLVED_METH_CACHE.put(method, new WeakHashMap<Integer, Method>());

            RESOLVED_METH_CACHE.get(method).put(hash, bestCandidate);
        }

        return bestCandidate;
    }

    private static Map<Class, Map<Integer, Constructor>> RESOLVED_CONST_CACHE = new WeakHashMap<Class, Map<Integer, Constructor>>(10);

    private static Map<Constructor, Class[]> CONSTRUCTOR_PARMS_CACHE = new WeakHashMap<Constructor, Class[]>(10);

    private static Class[] getConstructors(Constructor cns) {
        if (CONSTRUCTOR_PARMS_CACHE.containsKey(cns))
            return CONSTRUCTOR_PARMS_CACHE.get(cns);
        else {
            Class[] c = cns.getParameterTypes();
            CONSTRUCTOR_PARMS_CACHE.put(cns, c);
            return c;
        }
    }

    public static Constructor getBestConstructorCanadidate(Object[] arguments, Class cls) {

        Class[] parmTypes;
        Constructor bestCandidate = null;
        int bestScore = 0;
        int score = 0;

        Class[] targetParms = new Class[arguments.length];

        for (int i = 0; i < arguments.length; i++)
            targetParms[i] = arguments[i] != null ? arguments[i].getClass() : Object.class;

        Integer hash = createClassSignatureHash(targetParms);

        if (RESOLVED_CONST_CACHE.containsKey(cls) && RESOLVED_CONST_CACHE.get(cls).containsKey(hash))
            return RESOLVED_CONST_CACHE.get(cls).get(hash);

        for (Constructor construct : getConstructors(cls)) {
            if ((parmTypes = getConstructors(construct)).length != arguments.length)
                continue;
            else if (arguments.length == 0 && parmTypes.length == 0)
                return construct;

            for (int i = 0; i < arguments.length; i++) {
                if (parmTypes[i] == targetParms[i]) {
                    score += 5;
                }
                else if (parmTypes[i].isPrimitive() && boxPrimitive(parmTypes[i]) == targetParms[i]) {
                    score += 4;
                }
                else if (targetParms[i].isPrimitive() && unboxPrimitive(targetParms[i]) == parmTypes[i]) {
                    score += 4;
                }
                else if (isNumericallyCoercible(targetParms[i], parmTypes[i])) {
                    score += 3;
                }
                else if (parmTypes[i].isAssignableFrom(targetParms[i])) {
                    score += 2;
                }
                else if (canConvert(parmTypes[i], targetParms[i])) {
                    score += 1;
                }
                else {
                    score = 0;
                    break;
                }
            }

            if (score != 0 && score > bestScore) {
                bestCandidate = construct;
                bestScore = score;
            }
            score = 0;

        }

        if (bestCandidate != null) {
            if (!RESOLVED_CONST_CACHE.containsKey(cls))
                RESOLVED_CONST_CACHE.put(cls, new WeakHashMap<Integer, Constructor>());

            RESOLVED_CONST_CACHE.get(cls).put(hash, bestCandidate);
        }

        return bestCandidate;
    }

    private static Map<String, Class> CLASS_RESOLVER_CACHE = new WeakHashMap<String, Class>(10);

    private static Map<Class, Constructor[]> CLASS_CONSTRUCTOR_CACHE = new WeakHashMap<Class, Constructor[]>(10);

    public static Class createClass(String className) throws ClassNotFoundException {
        if (CLASS_RESOLVER_CACHE.containsKey(className))
            return CLASS_RESOLVER_CACHE.get(className);
        else {
            Class cls = Class.forName(className);
            CLASS_RESOLVER_CACHE.put(className, cls);
            return cls;
        }
    }

    public static Constructor[] getConstructors(Class cls) {
        if (CLASS_CONSTRUCTOR_CACHE.containsKey(cls))
            return CLASS_CONSTRUCTOR_CACHE.get(cls);
        else {
            Constructor[] cns = cls.getConstructors();
            CLASS_CONSTRUCTOR_CACHE.put(cls, cns);
            return cns;
        }
    }

    public static Object constructObject(String expression, Object ctx, VariableResolverFactory vrf) throws InstantiationException, IllegalAccessException,
            InvocationTargetException, ClassNotFoundException {

        String[] constructorParms = parseMethodOrConstructor(expression.toCharArray());

        if (constructorParms != null) {
            Class cls = AbstractParser.LITERALS.containsKey(expression = expression.substring(0, expression.indexOf('('))) ? ((Class) AbstractParser.LITERALS.get(expression))
                    : createClass(expression);

            Object[] parms = new Object[constructorParms.length];
            for (int i = 0; i < constructorParms.length; i++) {
                parms[i] = (MVEL.eval(constructorParms[i], ctx, vrf));
            }

            Constructor cns = getBestConstructorCanadidate(parms, cls);

            if (cns == null)
                throw new CompileException("unable to find constructor for: " + cls.getName());

            for (int i = 0; i < parms.length; i++) {
                // noinspection unchecked
                parms[i] = convert(parms[i], cns.getParameterTypes()[i]);
            }

            return cns.newInstance(parms);
        }
        else {
            return forName(expression).newInstance();
        }
    }

    public static String[] captureContructorAndResidual(String token) {
        char[] cs = token.toCharArray();

        int depth = 0;

        for (int i = 0; i < cs.length; i++) {
            switch (cs[i]) {
                case'(':
                    depth++;
                    continue;
                case')':
                    if (1 == depth--) {
                        return new String[]{new String(cs, 0, ++i), new String(cs, i, cs.length - i)};
                    }
            }
        }
        return new String[]{token};
    }

    public static String[] captureContructorAndResidual(char[] cs) {
        int depth = 0;
        for (int i = 0; i < cs.length; i++) {
            switch (cs[i]) {
                case'(':
                    depth++;
                    continue;
                case')':
                    if (1 == depth--) {
                        return new String[]{new String(cs, 0, ++i), new String(cs, i, cs.length - i)};
                    }
            }
        }
        return new String[]{new String(cs)};
    }

    public static Class boxPrimitive(Class cls) {
        if (cls == int.class || cls == Integer.class) {
            return Integer.class;
        }
        else if (cls == int[].class || cls == Integer[].class) {
            return Integer[].class;
        }
        else if (cls == long.class || cls == Long.class) {
            return Long.class;
        }
        else if (cls == long[].class || cls == Long[].class) {
            return Long[].class;
        }
        else if (cls == short.class || cls == Short.class) {
            return Short.class;
        }
        else if (cls == short[].class || cls == Short[].class) {
            return Short[].class;
        }
        else if (cls == double.class || cls == Double.class) {
            return Double.class;
        }
        else if (cls == double[].class || cls == Double[].class) {
            return Double[].class;
        }
        else if (cls == float.class || cls == Float.class) {
            return Float.class;
        }
        else if (cls == float[].class || cls == Float[].class) {
            return Float[].class;
        }
        else if (cls == boolean.class || cls == Boolean.class) {
            return Boolean.class;
        }
        else if (cls == boolean[].class || cls == Boolean[].class) {
            return Boolean[].class;
        }
        else if (cls == byte.class || cls == Byte.class) {
            return Byte.class;
        }
        else if (cls == byte[].class || cls == Byte[].class) {
            return Byte[].class;
        }

        return null;
    }

    public static Class unboxPrimitive(Class cls) {
        if (cls == Integer.class || cls == int.class) {
            return int.class;
        }
        else if (cls == Integer[].class || cls == int[].class) {
            return int[].class;
        }
        else if (cls == Long.class || cls == long.class) {
            return long.class;
        }
        else if (cls == Long[].class || cls == long[].class) {
            return long[].class;
        }
        else if (cls == Short.class || cls == short.class) {
            return short.class;
        }
        else if (cls == Short[].class || cls == short[].class) {
            return short[].class;
        }
        else if (cls == Double.class || cls == double.class) {
            return double.class;
        }
        else if (cls == Double[].class || cls == double[].class) {
            return double[].class;
        }
        else if (cls == Float.class || cls == float.class) {
            return float.class;
        }
        else if (cls == Float[].class || cls == float[].class) {
            return float[].class;
        }
        else if (cls == Boolean.class || cls == boolean.class) {
            return boolean.class;
        }
        else if (cls == Boolean[].class || cls == boolean[].class) {
            return boolean[].class;
        }
        else if (cls == Byte.class || cls == byte.class) {
            return byte.class;
        }
        else if (cls == Byte[].class || cls == byte[].class) {
            return byte[].class;
        }

        return null;
    }

    public static boolean containsCheck(Object compareTo, Object compareTest) {
        if (compareTo == null)
            return false;
        else if (compareTo instanceof String)
            return ((String) compareTo).contains(valueOf(compareTest));
        else if (compareTo instanceof Collection)
            return ((Collection) compareTo).contains(compareTest);
        else if (compareTo instanceof Map)
            return ((Map) compareTo).containsKey(compareTest);
        else if (compareTo.getClass().isArray()) {
            for (Object o : ((Object[]) compareTo)) {
                if (compareTest == null && o == null)
                    return true;
                else if (o != null && o.equals(compareTest))
                    return true;
            }
        }
        return false;
    }

    public static int createClassSignatureHash(Class[] sig) {
        int hash = 0;
        for (Class cls : sig) {
            if (cls != null)
                hash += cls.hashCode();
        }
        return hash + sig.length;
    }

    public static char handleEscapeSequence(char escapedChar) {
        switch (escapedChar) {
            case'\\':
                return '\\';
            case't':
                return '\t';
            case'r':
                return '\r';
            case'\'':
                return '\'';
            case'"':
                return '"';
            default:
                throw new ParseException("illegal escape sequence: " + escapedChar);
        }
    }

    public static VariableResolverFactory finalLocalVariableFactory(VariableResolverFactory factory) {
        VariableResolverFactory v = factory;
        while (v != null) {
            if (v instanceof LocalVariableResolverFactory)
                return v;

            v = v.getNextFactory();
        }

        if (factory == null) {
            throw new OptimizationFailure("unable to assign variables.  no variable resolver factory available.");
        }
        else {
            return new LocalVariableResolverFactory(new HashMap<String, Object>()).setNextFactory(factory);
        }
    }

    public static boolean debug(String str) {
        System.out.println(str);
        return true;
    }

    public static boolean debug(Throwable t) {
        t.printStackTrace();
        return true;
    }

    public static char[] subset(char[] array, int start, int length) {
        char[] newArray = new char[length];
        System.arraycopy(array, start, newArray, 0, length);
        return newArray;
    }

    public static char[] subset(char[] array, int start) {
        char[] newArray = new char[array.length - start];
        System.arraycopy(array, start, newArray, 0, newArray.length);
        return newArray;
    }

    public static int resolveType(Class cls) {
        if (cls == null)
            return 0;
        if (BigDecimal.class == cls)
            return DataTypes.BIG_DECIMAL;
        if (String.class == cls)
            return DataTypes.STRING;

        if (int.class == cls)
            return DataTypes.INTEGER;
        if (short.class == cls)
            return DataTypes.SHORT;
        if (float.class == cls)
            return DataTypes.FLOAT;
        if (double.class == cls)
            return DataTypes.DOUBLE;
        if (long.class == cls)
            return DataTypes.LONG;
        if (boolean.class == cls)
            return DataTypes.BOOLEAN;
        if (byte.class == cls)
            return DataTypes.BYTE;
        if (char.class == cls)
            return DataTypes.CHAR;


        if (Integer.class == cls)
            return DataTypes.W_INTEGER;
        if (Short.class == cls)
            return DataTypes.W_SHORT;
        if (Float.class == cls)
            return DataTypes.W_FLOAT;
        if (Double.class == cls)
            return DataTypes.W_DOUBLE;
        if (Long.class == cls)
            return DataTypes.W_LONG;
        if (Boolean.class == cls)
            return DataTypes.W_BOOLEAN;
        if (Byte.class == cls)
            return DataTypes.W_BYTE;
        if (Character.class == cls)
            return DataTypes.W_CHAR;


        return DataTypes.OBJECT;
    }

    public static BigDecimal getBigDecimalFromType(Object in, int type) {
        if (in == null)
            return new BigDecimal(0);
        switch (type) {
            case DataTypes.BIG_DECIMAL:
                return (BigDecimal) in;
            case DataTypes.W_INTEGER:
                return new BigDecimal((Integer) in);
            case DataTypes.W_LONG:
                return new BigDecimal((Long) in);
            case DataTypes.STRING:
                return new BigDecimal((String) in);
            case DataTypes.W_FLOAT:
                return new BigDecimal((Float) in);
            case DataTypes.W_DOUBLE:
                return new BigDecimal((Double) in);
            case DataTypes.W_SHORT:
                return new BigDecimal((Short) in);

        }

        throw new ConversionException("cannot convert <" + in + "> to a numeric type");
    }

    public static Object valueOnly(Object o) {
        return (o instanceof Token) ? ((Token) o).getLiteralValue() : o;
    }

    public static boolean isNumericallyCoercible(Class target, Class parm) {
        Class boxedTarget = target.isPrimitive() ? boxPrimitive(target) : target;

        if (boxedTarget != null && Number.class.isAssignableFrom(target)) {
            Class boxedParm = parm.isPrimitive() ? boxPrimitive(parm) : parm;

            if (boxedParm != null) {
                return Number.class.isAssignableFrom(boxedParm);
            }
        }
        return false;
    }

    public static Object handleParserEgress(Object result, boolean returnBigDecimal) {
        if (result instanceof BigDecimal) {
            if (returnBigDecimal) return result;
            else if (((BigDecimal) result).scale() > 14) {
                return ((BigDecimal) result).doubleValue();
            }
            else if (((BigDecimal) result).scale() > 0) {
                return ((BigDecimal) result).floatValue();
            }
            else if (((BigDecimal) result).longValue() > Integer.MAX_VALUE) {
                return ((BigDecimal) result).longValue();
            }
            else {
                return ((BigDecimal) result).intValue();
            }
        }
        else
            return result;

    }

    public static Method determineActualTargetMethod(Method method) {
        String name = method.getName();

        for (Class cls : method.getDeclaringClass().getInterfaces()) {
            for (Method meth : cls.getMethods()) {
                if (meth.getParameterTypes().length == 0 && name.equals(meth.getName())) {
                    return meth;
                }
            }
        }

        return null;
    }

    public static Object doOperations(Object val1, int operation, Object val2) {
        int type1 = val1 == null ? DataTypes.NULL : resolveType(val1.getClass());
        int type2 = val2 == null ? DataTypes.NULL : resolveType(val2.getClass());

        if (type1 == DataTypes.BIG_DECIMAL) {
            if (type2 == DataTypes.BIG_DECIMAL) {
                return doBigDecimalArithmetic((BigDecimal) val1, operation, (BigDecimal) val2);
            }
            else if (type2 > 99) {
                return doBigDecimalArithmetic((BigDecimal) val1, operation, getBigDecimalFromType(val2, type2));
            }
            else {
                return _doOperations(type1, val1, operation, type2, val2);
            }
        }
        else if (type2 == DataTypes.BIG_DECIMAL && (type1 > 99 || (type1 == DataTypes.STRING && isNumber(val1)))) {
            return doBigDecimalArithmetic(getBigDecimalFromType(val1, type1), operation, (BigDecimal) val2);
        }
        else {
            return _doOperations(type1, val1, operation, type2, val2);
        }
    }

    private static Object doBigDecimalArithmetic(BigDecimal val1, int operation, BigDecimal val2) {
        switch (operation) {
            case Operator.ADD:
                return val1.add(val2);
            case Operator.DIV:
                return val1.divide(val2, MATH_CONTEXT);
            case Operator.SUB:
                return val1.subtract(val2);
            case Operator.MULT:
                return val1.multiply(val2);
            case Operator.POWER:
                return Math.pow(val1.doubleValue(), val2.doubleValue());
            case Operator.MOD:
                return val1.remainder(val2);

            case Operator.GTHAN:
                return val1.compareTo(val2) == 1;
            case Operator.GETHAN:
                return val1.compareTo(val2) >= 0;
            case Operator.LTHAN:
                return val1.compareTo(val2) == -1;
            case Operator.LETHAN:
                return val1.compareTo(val2) <= 0;
            case Operator.EQUAL:
                return val1.compareTo(val2) == 0;
            case Operator.NEQUAL:
                return val1.compareTo(val2) != 0;
        }
        return null;
    }

    private static Object _doOperations(int type1, Object val1, int operation, int type2, Object val2) {
        if (operation < 10 || operation == Operator.EQUAL || operation == Operator.NEQUAL) {
            if (type1 > 99 && type1 == type2) {
                return doOperationsSameType(type1, val1, operation, val2);
            }
            else if ((type1 > 99 && (type2 > 99)) || (isNumber(val1) && isNumber(val2))) {
                return doBigDecimalArithmetic(getBigDecimalFromType(val1, type1), operation, getBigDecimalFromType(val2, type2));
            }
        }
        return doOperationNonNumeric(val1, operation, val2);
    }

    private static Object doOperationNonNumeric(Object val1, int operation, Object val2) {
        switch (operation) {
            case Operator.ADD:
                return valueOf(val1) + valueOf(val2);

            case Operator.EQUAL:
                return safeEquals(val2, val1);

            case Operator.NEQUAL:
                return safeNotEquals(val2, val1);

            case Operator.SUB:
            case Operator.DIV:
            case Operator.MULT:
            case Operator.MOD:
            case Operator.GTHAN:
            case Operator.GETHAN:
            case Operator.LTHAN:
            case Operator.LETHAN:
                throw new CompileException("could not perform numeric operation on non-numeric types");
        }

        throw new CompileException("unable to perform operation");
    }

    private static Boolean safeEquals(Object val1, Object val2) {
        if (val1 != null) {
            return val1.equals(val2);
        }
        else if (val2 != null) {
            return val2.equals(val1);
        }
        else {
            return val1 == val2;
        }
    }

    private static Boolean safeNotEquals(Object val1, Object val2) {
        if (val1 != null) {
            return !val1.equals(val2);
        }
        else return val2 != null && !val2.equals(val1);
    }

    private static Object doOperationsSameType(int type1, Object val1, int operation, Object val2) {
        switch (type1) {
            case DataTypes.INTEGER:
            case DataTypes.W_INTEGER:
                switch (operation) {
                    case Operator.ADD:
                        return ((Integer) val1) + ((Integer) val2);
                    case Operator.SUB:
                        return ((Integer) val1) - ((Integer) val2);
                    case Operator.DIV:
                        return new BigDecimal((Integer) val1).divide(new BigDecimal((Integer) val2), MATH_CONTEXT);
                    case Operator.MULT:
                        return ((Integer) val1) * ((Integer) val2);
                    case Operator.POWER:
                        double d = Math.pow((Integer) val1, (Integer) val2);
                        if (d > Integer.MAX_VALUE) return d;
                        else return (int) d;
                    case Operator.MOD:
                        return ((Integer) val1) % ((Integer) val2);

                    case Operator.GTHAN:
                        return ((Integer) val1) > ((Integer) val2);
                    case Operator.GETHAN:
                        return ((Integer) val1) >= ((Integer) val2);
                    case Operator.LTHAN:
                        return ((Integer) val1) < ((Integer) val2);
                    case Operator.LETHAN:
                        return ((Integer) val1) <= ((Integer) val2);
                    case Operator.EQUAL:
                        return ((Integer) val1).intValue() == ((Integer) val2).intValue();
                    case Operator.NEQUAL:
                        return ((Integer) val1).intValue() != ((Integer) val2).intValue();

                }

            case DataTypes.SHORT:
            case DataTypes.W_SHORT:
                switch (operation) {
                    case Operator.ADD:
                        return ((Short) val1) + ((Short) val2);
                    case Operator.SUB:
                        return ((Short) val1) - ((Short) val2);
                    case Operator.DIV:
                        return new BigDecimal((Short) val1).divide(new BigDecimal((Short) val2), MATH_CONTEXT);
                    case Operator.MULT:
                        return ((Short) val1) * ((Short) val2);
                    case Operator.POWER:
                        double d = Math.pow((Short) val1, (Short) val2);
                        if (d > Short.MAX_VALUE) return d;
                        else return (short) d;
                    case Operator.MOD:
                        return ((Short) val1) % ((Short) val2);

                    case Operator.GTHAN:
                        return ((Short) val1) > ((Short) val2);
                    case Operator.GETHAN:
                        return ((Short) val1) >= ((Short) val2);
                    case Operator.LTHAN:
                        return ((Short) val1) < ((Short) val2);
                    case Operator.LETHAN:
                        return ((Short) val1) <= ((Short) val2);
                    case Operator.EQUAL:
                        return ((Short) val1).shortValue() == ((Short) val2).shortValue();
                    case Operator.NEQUAL:
                        return ((Short) val1).shortValue() != ((Short) val2).shortValue();
                }

            case DataTypes.LONG:
            case DataTypes.W_LONG:
                switch (operation) {
                    case Operator.ADD:
                        return ((Long) val1) + ((Long) val2);
                    case Operator.SUB:
                        return ((Long) val1) - ((Long) val2);
                    case Operator.DIV:
                        return new BigDecimal((Long) val1).divide(new BigDecimal((Long) val2), MATH_CONTEXT);
                    case Operator.MULT:
                        return ((Long) val1) * ((Long) val2);
                    case Operator.POWER:
                        double d = Math.pow((Long) val1, (Long) val2);
                        if (d > Long.MAX_VALUE) return d;
                        else return (long) d;
                    case Operator.MOD:
                        return ((Long) val1) % ((Long) val2);

                    case Operator.GTHAN:
                        return ((Long) val1) > ((Long) val2);
                    case Operator.GETHAN:
                        return ((Long) val1) >= ((Long) val2);
                    case Operator.LTHAN:
                        return ((Long) val1) < ((Long) val2);
                    case Operator.LETHAN:
                        return ((Long) val1) <= ((Long) val2);
                    case Operator.EQUAL:
                        return ((Long) val1).longValue() == ((Long) val2).longValue();
                    case Operator.NEQUAL:
                        return ((Long) val1).longValue() != ((Long) val2).longValue();
                }

            case DataTypes.DOUBLE:
            case DataTypes.W_DOUBLE:
                switch (operation) {
                    case Operator.ADD:
                        return ((Double) val1) + ((Double) val2);
                    case Operator.SUB:
                        return ((Double) val1) - ((Double) val2);
                    case Operator.DIV:
                        return new BigDecimal((Double) val1).divide(new BigDecimal((Double) val2), MATH_CONTEXT);
                    case Operator.MULT:
                        return ((Double) val1) * ((Double) val2);
                    case Operator.POWER:
                        return Math.pow((Double) val1, (Double) val2);
                    case Operator.MOD:
                        return ((Double) val1) % ((Double) val2);

                    case Operator.GTHAN:
                        return ((Double) val1) > ((Double) val2);
                    case Operator.GETHAN:
                        return ((Double) val1) >= ((Double) val2);
                    case Operator.LTHAN:
                        return ((Double) val1) < ((Double) val2);
                    case Operator.LETHAN:
                        return ((Double) val1) <= ((Double) val2);
                    case Operator.EQUAL:
                        return ((Double) val1).doubleValue() == ((Double) val2).doubleValue();
                    case Operator.NEQUAL:
                        return ((Double) val1).doubleValue() != ((Double) val2).doubleValue();
                }

            case DataTypes.FLOAT:
            case DataTypes.W_FLOAT:
                switch (operation) {
                    case Operator.ADD:
                        return ((Float) val1) + ((Float) val2);
                    case Operator.SUB:
                        return ((Float) val1) - ((Float) val2);
                    case Operator.DIV:
                        return new BigDecimal((Float) val1).divide(new BigDecimal((Float) val2), MATH_CONTEXT);
                    case Operator.MULT:
                        return ((Float) val1) * ((Float) val2);
                    case Operator.POWER:
                        return new BigDecimal((Float) val1).pow(new BigDecimal((Float) val2).intValue());
                    case Operator.MOD:
                        return ((Float) val1) % ((Float) val2);

                    case Operator.GTHAN:
                        return ((Float) val1) > ((Float) val2);
                    case Operator.GETHAN:
                        return ((Float) val1) >= ((Float) val2);
                    case Operator.LTHAN:
                        return ((Float) val1) < ((Float) val2);
                    case Operator.LETHAN:
                        return ((Float) val1) <= ((Float) val2);
                    case Operator.EQUAL:
                        return ((Float) val1).floatValue() == ((Float) val2).floatValue();
                    case Operator.NEQUAL:
                        return ((Float) val1).floatValue() != ((Float) val2).floatValue();
                }


            default:
                switch (operation) {
                    case Operator.EQUAL:
                        return safeEquals(val2, val1);
                    case Operator.NEQUAL:
                        return safeNotEquals(val2, val1);
                    case Operator.ADD:
                        return valueOf(val1) + valueOf(val2);
                }
        }
        return null;

    }

    public static Object increment(Object o) {
        if (o instanceof Integer) {
            return (Integer) o + 1;
        }
        else if (o instanceof Double) {
            return (Double) o + 1;
        }
        else if (o instanceof Float) {
            return (Float) o + 1;
        }
        else if (o instanceof Short) {
            return (Short) o + 1;
        }
        else if (o instanceof Character) {
            return (Character) o + 1;
        }
        else {
            throw new CompileException("unable to increment type: " + (o != null ? o.getClass().getName() : "null"));   
        }
    }
}
