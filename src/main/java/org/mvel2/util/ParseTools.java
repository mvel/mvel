/**
 * MVEL 2.0
 * Copyright (C) 2007 The Codehaus
 * Mike Brock, Dhanji Prasanna, John Graham, Mark Proctor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mvel2.util;

import org.mvel2.*;

import static org.mvel2.DataConversion.canConvert;
import static org.mvel2.DataTypes.*;
import static org.mvel2.MVEL.getDebuggingOutputFileName;

import org.mvel2.ast.ASTNode;
import org.mvel2.compiler.*;

import static org.mvel2.compiler.AbstractParser.LITERALS;
import static org.mvel2.integration.ResolverTools.insertFactory;

import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.ClassImportResolverFactory;
import org.mvel2.math.MathProcessor;

import java.io.*;

import static java.lang.Class.forName;
import static java.lang.Double.parseDouble;
import static java.lang.String.valueOf;
import static java.lang.System.arraycopy;
import static java.lang.Thread.currentThread;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.nio.ByteBuffer;

import static java.nio.ByteBuffer.allocateDirect;

import java.nio.channels.ReadableByteChannel;
import java.util.*;


@SuppressWarnings({"ManualArrayCopy"})
public class ParseTools {
    public static final Object[] EMPTY_OBJ_ARR = new Object[0];
    public static final Class[] EMPTY_CLS_ARR = new Class[0];

    static {
        try {
            double version = parseDouble(System.getProperty("java.version").substring(0, 3));
            if (version < 1.5) {
                throw new RuntimeException("unsupported java version: " + version);
            }
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new RuntimeException("unable to initialize math processor", e);
        }
    }

    public static String[] parseMethodOrConstructor(char[] parm) {
        int start = -1;
        for (int i = 0; i < parm.length; i++) {
            if (parm[i] == '(') {
                start = ++i;
                break;
            }
        }
        if (start != -1) {
            return parseParameterList(parm, --start + 1, balancedCapture(parm, start, '(') - start - 1);
        }

        return null;
    }


    public static String[] parseParameterDefList(char[] parm, int offset, int length) {
        List<String> list = new LinkedList<String>();

        if (length == -1)
            length = parm.length;

        int start = offset;
        int i = offset;
        int end = i + length;
        String s;

        for (; i < end; i++) {
            switch (parm[i]) {
                case '(':
                case '[':
                case '{':
                    i = balancedCapture(parm, i, parm[i]);
                    continue;

                case '\'':
                    i = captureStringLiteral('\'', parm, i, parm.length);
                    continue;

                case '"':
                    i = captureStringLiteral('"', parm, i, parm.length);
                    continue;

                case ',':
                    if (i > start) {
                        while (isWhitespace(parm[start]))
                            start++;

                        checkNameSafety(s = new String(parm, start, i - start));

                        list.add(s);
                    }

                    while (isWhitespace(parm[i]))
                        i++;

                    start = i + 1;
                    continue;

                default:
                    if (!isWhitespace(parm[i]) && !isIdentifierPart(parm[i])) {
                        throw new CompileException("expected parameter");
                    }
            }
        }

        if (start < (length + offset) && i > start) {
            if ((s = createStringTrimmed(parm, start, i - start)).length() > 0) {
                checkNameSafety(s);
                list.add(s);
            }
        } else if (list.size() == 0) {
            if ((s = createStringTrimmed(parm, start, length)).length() > 0) {
                checkNameSafety(s);
                list.add(s);
            }
        }

        return list.toArray(new String[list.size()]);
    }


    public static String[] parseParameterList(char[] parm, int offset, int length) {
        List<String> list = new LinkedList<String>();

        if (length == -1)
            length = parm.length;

        int start = offset;
        int i = offset;
        int end = i + length;

        for (; i < end; i++) {
            switch (parm[i]) {
                case '(':
                case '[':
                case '{':
                    i = balancedCapture(parm, i, parm[i]);
                    continue;

                case '\'':
                    i = captureStringLiteral('\'', parm, i, parm.length);
                    continue;

                case '"':
                    i = captureStringLiteral('"', parm, i, parm.length);
                    continue;

                case ',':
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

        if (start < (length + offset) && i > start) {
            String s = createStringTrimmed(parm, start, i - start);
            if (s.length() > 0)
                list.add(s);
        } else if (list.size() == 0) {
            String s = createStringTrimmed(parm, start, length);
            if (s.length() > 0)
                list.add(s);
        }

        return list.toArray(new String[list.size()]);
    }

    private static Map<String, Map<Integer, WeakReference<Method>>> RESOLVED_METH_CACHE = new WeakHashMap<String, Map<Integer, WeakReference<Method>>>(10);

    public static Method getBestCandidate(Object[] arguments, String method, Class decl, Method[] methods, boolean requireExact) {
        Class[] targetParms = new Class[arguments.length];
        for (int i = 0; i != arguments.length; i++) {
            targetParms[i] = arguments[i] != null ? arguments[i].getClass() : null;
        }
        return getBestCandidate(targetParms, method, decl, methods, requireExact);
    }

    public static Method getBestCandidate(Class[] arguments, String method, Class decl, Method[] methods, boolean requireExact) {
        if (methods.length == 0) {
            return null;
        }

        Class[] parmTypes;
        Method bestCandidate = null;
        int bestScore = 0;
        int score = 0;

        Integer hash = createClassSignatureHash(decl, arguments);

        Map<Integer, WeakReference<Method>> methCache = RESOLVED_METH_CACHE.get(method);

        WeakReference<Method> ref;

        if (methCache != null && (ref = methCache.get(hash)) != null && (bestCandidate = ref.get()) != null) {
            return bestCandidate;
        }

        for (Method meth : methods) {
            if (method.equals(meth.getName())) {
                if ((parmTypes = meth.getParameterTypes()).length != arguments.length) {
                    continue;
                } else if (arguments.length == 0 && parmTypes.length == 0) {
                    bestCandidate = meth;
                    break;
                }

                for (int i = 0; i != arguments.length; i++) {
                    if (arguments[i] == null) {
                        if (!parmTypes[i].isPrimitive()) {
                            score += 5;
                        } else {
                            score = 0;
                            break;
                        }
                    } else if (parmTypes[i] == arguments[i]) {
                        score += 6;
                    } else if (parmTypes[i].isPrimitive() && boxPrimitive(parmTypes[i]) == arguments[i]) {
                        score += 5;
                    } else if (arguments[i].isPrimitive() && unboxPrimitive(arguments[i]) == parmTypes[i]) {
                        score += 5;
                    } else if (isNumericallyCoercible(arguments[i], parmTypes[i])) {
                        score += 4;
                    } else if (boxPrimitive(parmTypes[i]).isAssignableFrom(boxPrimitive(arguments[i]))) {
                        score += 3;
                    } else if (!requireExact && canConvert(parmTypes[i], arguments[i])) {
                        if (parmTypes[i].isArray() && arguments[i].isArray()) score += 1;
                        else if (parmTypes[i] == char.class && arguments[i] == String.class) score += 1;

                        score += 1;
                    } else if (arguments[i] == Object.class) {
                        score += 1;
                    } else {
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
            if (methCache == null) {
                RESOLVED_METH_CACHE.put(method, methCache = new WeakHashMap<Integer, WeakReference<Method>>());
            }

            methCache.put(hash, new WeakReference<Method>(bestCandidate));
        }

        return bestCandidate;
    }

    public static Method getExactMatch(String name, Class[] args, Class returnType, Class cls) {
        for (Method meth : cls.getMethods()) {
            if (name.equals(meth.getName()) && returnType == meth.getReturnType()) {
                Class[] parameterTypes = meth.getParameterTypes();
                if (parameterTypes.length != args.length) continue;

                for (int i = 0; i < parameterTypes.length; i++) {
                    if (parameterTypes[i] != args[i]) return null;
                }
                return meth;
            }
        }
        return null;
    }

    public static Method getWidenedTarget(Method method) {
        Class cls = method.getDeclaringClass();
        Method m = method, best = method;
        Class[] args = method.getParameterTypes();
        String name = method.getName();
        Class rt = m.getReturnType();

        do {
            if (cls.getInterfaces().length != 0) {
                for (Class iface : cls.getInterfaces()) {
                    if ((m = getExactMatch(name, args, rt, iface)) != null) {
                        if ((best = m).getDeclaringClass().getSuperclass() != null) {
                            cls = m.getDeclaringClass();
                        }
                    }
                }
            }
            if (cls != method.getDeclaringClass()) {
                if ((m = getExactMatch(name, args, rt, cls)) != null) {
                    if ((best = m).getDeclaringClass().getSuperclass() != null) {
                        cls = m.getDeclaringClass();
                    }
                }
            }
        }
        while ((cls = cls.getSuperclass()) != null);

        return best;
    }

    private static Map<Class, Map<Integer, WeakReference<Constructor>>> RESOLVED_CONST_CACHE
            = new WeakHashMap<Class, Map<Integer, WeakReference<Constructor>>>(10);
    private static Map<Constructor, WeakReference<Class[]>> CONSTRUCTOR_PARMS_CACHE
            = new WeakHashMap<Constructor, WeakReference<Class[]>>(10);

    private static Class[] getConstructors(Constructor cns) {
        WeakReference<Class[]> ref = CONSTRUCTOR_PARMS_CACHE.get(cns);
        Class[] parms;
        if (ref != null && (parms = ref.get()) != null) {
            return parms;
        } else {
            CONSTRUCTOR_PARMS_CACHE.put(cns, new WeakReference<Class[]>(parms = cns.getParameterTypes()));
            return parms;
        }
    }

    public static Constructor getBestConstructorCanadidate(Object[] args, Class cls, boolean requireExact) {
        Class[] parmTypes;
        Constructor bestCandidate = null;
        int bestScore = 0;
        int score = 0;

        Class[] arguments = new Class[args.length];

        for (int i = 0; i != args.length; i++) {
            if (args[i] != null) {
                arguments[i] = args[i].getClass();
            }
        }
        Integer hash = createClassSignatureHash(cls, arguments);

        Map<Integer, WeakReference<Constructor>> cache = RESOLVED_CONST_CACHE.get(cls);
        WeakReference<Constructor> ref;
        if (cache != null && (ref = cache.get(hash)) != null && (bestCandidate = ref.get()) != null) {
            return bestCandidate;
        }

        for (Constructor construct : getConstructors(cls)) {
            if ((parmTypes = getConstructors(construct)).length != args.length) {
                continue;
            } else if (args.length == 0 && parmTypes.length == 0) {
                return construct;
            }

            for (int i = 0; i != args.length; i++) {
                if (arguments[i] == null) {
                    if (!parmTypes[i].isPrimitive()) {
                        score += 5;
                    } else {
                        score = 0;
                        break;
                    }
                } else if (parmTypes[i] == arguments[i]) {
                    score += 6;
                } else if (parmTypes[i].isPrimitive() && boxPrimitive(parmTypes[i]) == arguments[i]) {
                    score += 5;
                } else if (arguments[i].isPrimitive() && unboxPrimitive(arguments[i]) == parmTypes[i]) {
                    score += 5;
                } else if (isNumericallyCoercible(arguments[i], parmTypes[i])) {
                    score += 4;
                } else if (boxPrimitive(parmTypes[i]).isAssignableFrom(boxPrimitive(arguments[i]))) {
                    score += 3;
                } else if (!requireExact && canConvert(parmTypes[i], arguments[i])) {
                    if (parmTypes[i].isArray() && arguments[i].isArray()) score += 1;
                    else if (parmTypes[i] == char.class && arguments[i] == String.class) score += 1;

                    score += 1;
                } else if (arguments[i] == Object.class) {
                    score += 1;
                } else {
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
            if (cache == null) {
                RESOLVED_CONST_CACHE.put(cls, cache = new WeakHashMap<Integer, WeakReference<Constructor>>());
            }
            cache.put(hash, new WeakReference<Constructor>(bestCandidate));
        }

        return bestCandidate;
    }


    private static Map<ClassLoader, Map<String, WeakReference<Class>>> CLASS_RESOLVER_CACHE
            = new WeakHashMap<ClassLoader, Map<String, WeakReference<Class>>>(1, 1.0f);
    private static Map<Class, WeakReference<Constructor[]>> CLASS_CONSTRUCTOR_CACHE
            = new WeakHashMap<Class, WeakReference<Constructor[]>>(10);


    public static Class createClass(String className, ParserContext pCtx) throws ClassNotFoundException {
        ClassLoader classLoader = currentThread().getContextClassLoader();
        Map<String, WeakReference<Class>> cache = CLASS_RESOLVER_CACHE.get(classLoader);

        if (cache == null) {
            CLASS_RESOLVER_CACHE.put(classLoader, cache = new WeakHashMap<String, WeakReference<Class>>(10));
        }


        WeakReference<Class> ref;
        Class cls;

        if ((ref = cache.get(className)) != null && (cls = ref.get()) != null) {
            return cls;
        } else {
            try {
                cls = pCtx == null ? Class.forName(className, true, Thread.currentThread().getContextClassLoader()) :
                        Class.forName(className, true, pCtx.getParserConfiguration().getClassLoader());
            }
            catch (ClassNotFoundException e) {
                /**
                 * Now try the system classloader.
                 */
                cls = forName(className, true, Thread.currentThread().getContextClassLoader());
            }

            cache.put(className, new WeakReference<Class>(cls));
            return cls;
        }
    }


    public static Constructor[] getConstructors(Class cls) {
        WeakReference<Constructor[]> ref = CLASS_CONSTRUCTOR_CACHE.get(cls);
        Constructor[] cns;

        if (ref != null && (cns = ref.get()) != null) {
            return cns;
        } else {
            CLASS_CONSTRUCTOR_CACHE.put(cls, new WeakReference<Constructor[]>(cns = cls.getConstructors()));
            return cns;
        }
    }


    public static String[] captureContructorAndResidual(char[] cs) {
        int depth = 0;
        for (int i = 0; i < cs.length; i++) {
            switch (cs[i]) {
                case '(':
                    depth++;
                    continue;
                case ')':
                    if (1 == depth--) {
                        return new String[]{new String(cs, 0, ++i), createStringTrimmed(cs, i, cs.length - i)};
                    }
            }
        }
        return new String[]{new String(cs)};
    }


    public static Class boxPrimitive(Class cls) {
        if (cls == int.class || cls == Integer.class) {
            return Integer.class;
        } else if (cls == int[].class || cls == Integer[].class) {
            return Integer[].class;
        } else if (cls == char.class || cls == Character.class) {
            return Character.class;
        } else if (cls == char[].class || cls == Character[].class) {
            return Character[].class;
        } else if (cls == long.class || cls == Long.class) {
            return Long.class;
        } else if (cls == long[].class || cls == Long[].class) {
            return Long[].class;
        } else if (cls == short.class || cls == Short.class) {
            return Short.class;
        } else if (cls == short[].class || cls == Short[].class) {
            return Short[].class;
        } else if (cls == double.class || cls == Double.class) {
            return Double.class;
        } else if (cls == double[].class || cls == Double[].class) {
            return Double[].class;
        } else if (cls == float.class || cls == Float.class) {
            return Float.class;
        } else if (cls == float[].class || cls == Float[].class) {
            return Float[].class;
        } else if (cls == boolean.class || cls == Boolean.class) {
            return Boolean.class;
        } else if (cls == boolean[].class || cls == Boolean[].class) {
            return Boolean[].class;
        } else if (cls == byte.class || cls == Byte.class) {
            return Byte.class;
        } else if (cls == byte[].class || cls == Byte[].class) {
            return Byte[].class;
        }

        return cls;
    }

    public static Class unboxPrimitive(Class cls) {
        if (cls == Integer.class || cls == int.class) {
            return int.class;
        } else if (cls == Integer[].class || cls == int[].class) {
            return int[].class;
        } else if (cls == Long.class || cls == long.class) {
            return long.class;
        } else if (cls == Long[].class || cls == long[].class) {
            return long[].class;
        } else if (cls == Short.class || cls == short.class) {
            return short.class;
        } else if (cls == Short[].class || cls == short[].class) {
            return short[].class;
        } else if (cls == Double.class || cls == double.class) {
            return double.class;
        } else if (cls == Double[].class || cls == double[].class) {
            return double[].class;
        } else if (cls == Float.class || cls == float.class) {
            return float.class;
        } else if (cls == Float[].class || cls == float[].class) {
            return float[].class;
        } else if (cls == Boolean.class || cls == boolean.class) {
            return boolean.class;
        } else if (cls == Boolean[].class || cls == boolean[].class) {
            return boolean[].class;
        } else if (cls == Byte.class || cls == byte.class) {
            return byte.class;
        } else if (cls == Byte[].class || cls == byte[].class) {
            return byte[].class;
        }

        return cls;
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
                else if ((Boolean) MathProcessor.doOperations(o, Operator.EQUAL, compareTest))
                    return true;
            }
        }
        return false;
    }

    public static int createClassSignatureHash(Class declaring, Class[] sig) {
        int hash = 0;
        for (Class cls : sig) {
            if (cls != null)
                hash += cls.hashCode();
        }

        return hash + sig.length + declaring.hashCode();
    }

    /**
     * Replace escape sequences and return trim required.
     *
     * @param escapeStr -
     * @param pos       -
     * @return -
     */
    public static int handleEscapeSequence(char[] escapeStr, int pos) {
        escapeStr[pos - 1] = 0;

        switch (escapeStr[pos]) {
            case '\\':
                escapeStr[pos] = '\\';
                return 1;
            case 'b':
                escapeStr[pos] = '\b';
                return 1;
            case 'f':
                escapeStr[pos] = '\f';
                return 1;
            case 't':
                escapeStr[pos] = '\t';
                return 1;
            case 'r':
                escapeStr[pos] = '\r';
                return 1;
            case 'n':
                escapeStr[pos] = '\n';
                return 1;
            case '\'':
                escapeStr[pos] = '\'';
                return 1;
            case '"':
                escapeStr[pos] = '\"';
                return 1;
            case 'u':
                //unicode
                int s = pos;
                if (s + 4 > escapeStr.length) throw new CompileException("illegal unicode escape sequence");
                else {
                    while (++pos - s != 5) {
                        if ((escapeStr[pos] > ('0' - 1) && escapeStr[pos] < ('9' + 1)) ||
                                (escapeStr[pos] > ('A' - 1) && escapeStr[pos] < ('F' + 1))) {
                        } else {
                            throw new CompileException("illegal unicode escape sequence");
                        }
                    }

                    escapeStr[s - 1] = (char) Integer.decode("0x" + new String(escapeStr, s + 1, 4)).intValue();
                    escapeStr[s] = 0;
                    escapeStr[s + 1] = 0;
                    escapeStr[s + 2] = 0;
                    escapeStr[s + 3] = 0;
                    escapeStr[s + 4] = 0;

                    return 5;
                }


            default:
                //octal
                s = pos;
                while (escapeStr[pos] >= '0' && escapeStr[pos] < '8') {
                    if (pos != s && escapeStr[s] > '3') {
                        escapeStr[s - 1] = (char) Integer.decode("0" + new String(escapeStr, s, pos - s + 1)).intValue();
                        escapeStr[s] = 0;
                        escapeStr[s + 1] = 0;
                        return 2;
                    } else if ((pos - s) == 2) {
                        escapeStr[s - 1] = (char) Integer.decode("0" + new String(escapeStr, s, pos - s + 1)).intValue();
                        escapeStr[s] = 0;
                        escapeStr[s + 1] = 0;
                        escapeStr[s + 2] = 0;
                        return 3;
                    }

                    if (pos + 1 == escapeStr.length || (escapeStr[pos] < '0' || escapeStr[pos] > '7')) {
                        escapeStr[s - 1] = (char) Integer.decode("0" + new String(escapeStr, s, pos - s + 1)).intValue();
                        escapeStr[s] = 0;
                        return 1;
                    }

                    pos++;
                }
                throw new CompileException("illegal escape sequence: " + escapeStr[pos]);
        }
    }

    public static char[] createShortFormOperativeAssignment(String name, char[] statement, int operation) {
        if (operation == -1) {
            return statement;
        }

        char[] stmt;
        char op = 0;
        switch (operation) {
            case Operator.ADD:
                op = '+';
                break;
            case Operator.STR_APPEND:
                op = '#';
                break;
            case Operator.SUB:
                op = '-';
                break;
            case Operator.MULT:
                op = '*';
                break;
            case Operator.MOD:
                op = '%';
                break;
            case Operator.DIV:
                op = '/';
                break;
            case Operator.BW_AND:
                op = '&';
                break;
            case Operator.BW_OR:
                op = '|';
                break;
            case Operator.BW_SHIFT_LEFT:
                op = '\u00AB';
                break;
            case Operator.BW_SHIFT_RIGHT:
                op = '\u00BB';
                break;
            case Operator.BW_USHIFT_RIGHT:
                op = '\u00AC';
                break;
        }

        arraycopy(name.toCharArray(), 0, (stmt = new char[name.length() + statement.length + 1]), 0, name.length());
        stmt[name.length()] = op;
        arraycopy(statement, 0, stmt, name.length() + 1, statement.length);

        return stmt;
    }


    public static ClassImportResolverFactory findClassImportResolverFactory(VariableResolverFactory factory) {
        VariableResolverFactory v = factory;
        while (v != null) {
            if (v instanceof ClassImportResolverFactory) {
                return (ClassImportResolverFactory) v;
            }
            v = v.getNextFactory();
        }

        if (factory == null) {
            throw new OptimizationFailure("unable to import classes.  no variable resolver factory available.");
        } else {
            return insertFactory(factory, new ClassImportResolverFactory());
        }
    }

    public static Class findClass(VariableResolverFactory factory, String name, ParserContext ctx) throws ClassNotFoundException {
        try {
            if (LITERALS.containsKey(name)) {
                return (Class) LITERALS.get(name);
            } else if (factory != null && factory.isResolveable(name)) {
                return (Class) factory.getVariableResolver(name).getValue();
            } else if (ctx != null && ctx.hasImport(name)) {
                return ctx.getImport(name);
            } else {
                return createClass(name, ctx);
            }
        }
        catch (ClassNotFoundException e) {
            throw e;
        }
        catch (Exception e) {
            throw new CompileException("class not found: " + name, e);
        }
    }

    public static char[] subset(char[] array, int start, int length) {
        if (length < 0) return new char[0];
        char[] newArray = new char[length];

        for (int i = 0; i < newArray.length; i++) {
            newArray[i] = array[i + start];
        }

        return newArray;
    }

    public static char[] subset(char[] array, int start) {
        char[] newArray = new char[array.length - start];

        for (int i = 0; i < newArray.length; i++) {
            newArray[i] = array[i + start];
        }

        return newArray;
    }

    private static final HashMap<Class, Integer> typeResolveMap = new HashMap<Class, Integer>();

    static {
        Map<Class, Integer> t = typeResolveMap;
        t.put(BigDecimal.class, DataTypes.BIG_DECIMAL);
        t.put(BigInteger.class, DataTypes.BIG_INTEGER);
        t.put(String.class, DataTypes.STRING);

        t.put(int.class, INTEGER);
        t.put(Integer.class, DataTypes.W_INTEGER);

        t.put(short.class, DataTypes.SHORT);
        t.put(Short.class, DataTypes.W_SHORT);

        t.put(float.class, DataTypes.FLOAT);
        t.put(Float.class, DataTypes.W_FLOAT);

        t.put(double.class, DOUBLE);
        t.put(Double.class, DataTypes.W_DOUBLE);

        t.put(long.class, LONG);
        t.put(Long.class, DataTypes.W_LONG);

        t.put(boolean.class, DataTypes.BOOLEAN);
        t.put(Boolean.class, DataTypes.W_BOOLEAN);

        t.put(byte.class, DataTypes.BYTE);
        t.put(Byte.class, DataTypes.W_BYTE);

        t.put(char.class, DataTypes.CHAR);
        t.put(Character.class, DataTypes.W_CHAR);

        t.put(BlankLiteral.class, DataTypes.EMPTY);
    }

    public static int resolveType(Object o) {
        if (o == null) return DataTypes.OBJECT;
        else return __resolveType(o.getClass());
    }

    public static int resolveType(Class cls) {
        Integer i = typeResolveMap.get(cls);
        if (i == null) return DataTypes.OBJECT;
        else {
            return i;
        }
    }

    public static int __resolveType(Class cls) {
        if (Integer.class == cls)
            return DataTypes.W_INTEGER;
        if (Double.class == cls)
            return DataTypes.W_DOUBLE;
        if (Boolean.class == cls)
            return DataTypes.W_BOOLEAN;
        if (String.class == cls)
            return DataTypes.STRING;
        if (Long.class == cls)
            return DataTypes.W_LONG;

        if (Short.class == cls)
            return DataTypes.W_SHORT;
        if (Float.class == cls)
            return DataTypes.W_FLOAT;

        if (Byte.class == cls)
            return DataTypes.W_BYTE;
        if (Character.class == cls)
            return DataTypes.W_CHAR;

        if (BigDecimal.class == cls)
            return DataTypes.BIG_DECIMAL;

        if (BigInteger.class == cls)
            return DataTypes.BIG_INTEGER;

        if (int.class == cls)
            return INTEGER;
        if (short.class == cls)
            return DataTypes.SHORT;
        if (float.class == cls)
            return DataTypes.FLOAT;
        if (double.class == cls)
            return DOUBLE;
        if (long.class == cls)
            return LONG;
        if (boolean.class == cls)
            return DataTypes.BOOLEAN;
        if (byte.class == cls)
            return DataTypes.BYTE;
        if (char.class == cls)
            return DataTypes.CHAR;

        if (BlankLiteral.class == cls)
            return DataTypes.EMPTY;

        return DataTypes.OBJECT;
    }

    public static boolean isNumericallyCoercible(Class target, Class parm) {
        Class boxedTarget = target.isPrimitive() ? boxPrimitive(target) : target;

        if (boxedTarget != null && Number.class.isAssignableFrom(target)) {
            if ((boxedTarget = parm.isPrimitive() ? boxPrimitive(parm) : parm) != null) {
                return Number.class.isAssignableFrom(boxedTarget);
            }
        }
        return false;
    }

    public static Object narrowType(final BigDecimal result, int returnTarget) {
        if (returnTarget == DataTypes.W_DOUBLE || result.scale() > 0) {
            return result.doubleValue();
        } else if (returnTarget == DataTypes.W_LONG || result.longValue() > Integer.MAX_VALUE) {
            return result.longValue();
        } else {
            return result.intValue();
        }
    }

    public static Method determineActualTargetMethod(Method method) {
        String name = method.getName();

        /**
         * Follow our way up the class heirarchy until we find the physical target method.
         */
        for (Class cls : method.getDeclaringClass().getInterfaces()) {
            for (Method meth : cls.getMethods()) {
                if (meth.getParameterTypes().length == 0 && name.equals(meth.getName())) {
                    return meth;
                }
            }
        }

        return null;
    }

    public static int captureToNextTokenJunction(char[] expr, int cursor, ParserContext pCtx) {
        while (cursor != expr.length) {
            switch (expr[cursor]) {
                case '{':
                case '(':
                    return cursor;
                case '[':
                    cursor = balancedCaptureWithLineAccounting(expr, cursor, '[', pCtx) + 1;
                    continue;
                default:
                    if (isWhitespace(expr[cursor])) {
                        return cursor;
                    }
                    cursor++;
            }
        }
        return cursor;
    }

    public static int nextNonBlank(char[] expr, int cursor) {
        if ((cursor + 1) >= expr.length) {
            throw new CompileException("unexpected end of statement", expr, cursor);
        }
        int i = cursor;
        while (i != expr.length && isWhitespace(expr[i])) i++;
        return i;
    }

    public static int skipWhitespace(char[] expr, int cursor, ParserContext pCtx) {
        int line = pCtx.getLineCount();
        int lastLineStart = pCtx.getLineOffset();

        Skip:
        while (cursor != expr.length) {
            switch (expr[cursor]) {
                case '\n':
                    line++;
                    lastLineStart = cursor;
                case '\r':
                    cursor++;
                    continue;
                case '/':
                    if (cursor + 1 != expr.length) {
                        switch (expr[cursor + 1]) {
                            case '/':
                                expr[cursor++] = ' ';
                                while (cursor != expr.length && expr[cursor] != '\n') expr[cursor++] = ' ';
                                if (cursor != expr.length) expr[cursor++] = ' ';

                                line++;
                                lastLineStart = cursor;

                                continue;

                            case '*':
                                int len = expr.length - 1;
                                expr[cursor++] = ' ';
                                while (cursor != len && !(expr[cursor] == '*' && expr[cursor + 1] == '/')) {
                                    if (expr[cursor] == '\n') {
                                        line++;
                                        lastLineStart = cursor;
                                    }

                                    expr[cursor++] = ' ';
                                }
                                if (cursor != len) expr[cursor++] = expr[cursor++] = ' ';
                                continue;

                            default:
                                break Skip;

                        }
                    }
                default:
                    if (!isWhitespace(expr[cursor])) break Skip;

            }
            cursor++;
        }
        return cursor;

    }

    public static boolean isStatementNotManuallyTerminated(char[] expr, int cursor) {
        if (cursor >= expr.length) return false;
        int c = cursor;
        while (c != expr.length && isWhitespace(expr[c])) c++;
        return !(c != expr.length && expr[c] == ';');
    }


    public static int captureToEOS(char[] expr, int cursor, ParserContext pCtx) {
        while (cursor != expr.length) {
            switch (expr[cursor]) {
                case '(':
                case '[':
                case '{':
                    if ((cursor = balancedCaptureWithLineAccounting(expr, cursor, expr[cursor], pCtx)) >= expr.length)
                        return cursor;
                    break;

                case '"':
                case '\'':
                    cursor = captureStringLiteral(expr[cursor], expr, cursor, expr.length);
                    break;

                case ',':
                case ';':
                case '}':
                    return cursor;
            }
            cursor++;
        }
        return cursor;
    }


    /**
     * From the specified cursor position, trim out any whitespace between the current position and the end of the
     * last non-whitespace character.
     *
     * @param pos - current position
     * @return new position.
     */
    public static int trimLeft(char[] expr, int start, int pos) {
        if (pos > expr.length) pos = expr.length;
        while (pos != 0 && pos >= start && isWhitespace(expr[pos - 1])) pos--;
        return pos;
    }

    /**
     * From the specified cursor position, trim out any whitespace between the current position and beginning of the
     * first non-whitespace character.
     *
     * @param pos -
     * @return -
     */
    public static int trimRight(char[] expr, int start, int pos) {
        while (pos != expr.length && isWhitespace(expr[pos])) pos++;
        return pos;
    }

    public static char[] subArray(char[] expr, final int start, final int end) {
        if (start >= end) return new char[0];

        char[] newA = new char[end - start];
        for (int i = 0; i != newA.length; i++) {
            newA[i] = expr[i + start];
        }

        return newA;
    }


    /**
     * This is an important aspect of the core parser tools.  This method is used throughout the core parser
     * and sub-lexical parsers to capture a balanced capture between opening and terminating tokens such as:
     * <em>( [ { ' " </em>
     * <br>
     * <br>
     * For example: ((foo + bar + (bar - foo)) * 20;<br>
     * <br>
     * <p/>
     * If a balanced capture is performed from position 2, we get "(foo + bar + (bar - foo))" back.<br>
     * If a balanced capture is performed from position 15, we get "(bar - foo)" back.<br>
     * Etc.
     *
     * @param chars -
     * @param start -
     * @param type  -
     * @return -
     */
    public static int balancedCapture(char[] chars, int start, char type) {
        int depth = 1;
        char term = type;
        switch (type) {
            case '[':
                term = ']';
                break;
            case '{':
                term = '}';
                break;
            case '(':
                term = ')';
                break;
        }

        if (type == term) {
            for (start++; start < chars.length; start++) {
                if (chars[start] == type) {
                    return start;
                }
            }
        } else {
            for (start++; start < chars.length; start++) {
                if (start < chars.length && chars[start] == '/') {
                    if (start + 1 == chars.length) return start;
                    if (chars[start + 1] == '/') {
                        start++;
                        while (start < chars.length && chars[start] != '\n') start++;
                    } else if (chars[start + 1] == '*') {
                        start += 2;
                        while (start < chars.length) {
                            switch (chars[start]) {
                                case '*':
                                    if (start + 1 < chars.length && chars[start + 1] == '/') {
                                        break;
                                    }
                                case '\r':
                                case '\n':

                                    break;
                            }
                            start++;
                        }
                    }
                }
                if (start == chars.length) return start;
                if (chars[start] == '\'' || chars[start] == '"') {
                    start = captureStringLiteral(chars[start], chars, start, chars.length);
                } else if (chars[start] == type) {
                    depth++;
                } else if (chars[start] == term && --depth == 0) {
                    return start;
                }
            }
        }

        switch (type) {
            case '[':
                throw new CompileException("unbalanced braces [ ... ]", chars, start);
            case '{':
                throw new CompileException("unbalanced braces { ... }", chars, start);
            case '(':
                throw new CompileException("unbalanced braces ( ... )", chars, start);
            default:
                throw new CompileException("unterminated string literal", chars, start);
        }
    }

    public static int balancedCaptureWithLineAccounting(char[] chars, int start, char type, ParserContext pCtx) {
        int depth = 1;
        char term = type;
        switch (type) {
            case '[':
                term = ']';
                break;
            case '{':
                term = '}';
                break;
            case '(':
                term = ')';
                break;
        }

        if (type == term) {
            for (start++; start != chars.length; start++) {
                if (chars[start] == type) {
                    return start;
                }
            }
        } else {
            int lines = 0;
            for (start++; start < chars.length; start++) {
                if (isWhitespace(chars[start])) {
                    switch (chars[start]) {
                        case '\r':
                            continue;
                        case '\n':
                            if (pCtx != null) pCtx.setLineOffset((short) start);
                            lines++;
                    }
                } else if (start < chars.length && chars[start] == '/') {
                    if (start + 1 == chars.length) return start;
                    if (chars[start + 1] == '/') {
                        start++;
                        while (start < chars.length && chars[start] != '\n') start++;
                    } else if (chars[start + 1] == '*') {
                        start += 2;
                        Skiploop:
                        while (start != chars.length) {
                            switch (chars[start]) {
                                case '*':
                                    if (start + 1 < chars.length && chars[start + 1] == '/') {
                                        break Skiploop;
                                    }
                                case '\r':
                                case '\n':
                                    if (pCtx != null) pCtx.setLineOffset((short) start);
                                    lines++;
                                    break;
                            }
                            start++;
                        }
                    }
                }
                if (start == chars.length) return start;
                if (chars[start] == '\'' || chars[start] == '"') {
                    start = captureStringLiteral(chars[start], chars, start, chars.length);
                } else if (chars[start] == type) {
                    depth++;
                } else if (chars[start] == term && --depth == 0) {
                    if (pCtx != null) pCtx.incrementLineCount(lines);
                    return start;
                }
            }
        }

        switch (type) {
            case '[':
                throw new CompileException("unbalanced braces [ ... ]", chars, start);
            case '{':
                throw new CompileException("unbalanced braces { ... }", chars, start);
            case '(':
                throw new CompileException("unbalanced braces ( ... )", chars, start);
            default:
                throw new CompileException("unterminated string literal", chars, start);
        }
    }

    public static String handleStringEscapes(char[] input) {
        int escapes = 0;
        for (int i = 0; i < input.length; i++) {
            if (input[i] == '\\') {
                escapes += handleEscapeSequence(input, ++i);
            }
        }

        if (escapes == 0) return new String(input);

        char[] processedEscapeString = new char[input.length - escapes];
        int cursor = 0;
        for (char aName : input) {
            if (aName != 0) {
                processedEscapeString[cursor++] = aName;
            }
        }

        return new String(processedEscapeString);
    }

    public static int captureStringLiteral(final char type, final char[] expr, int cursor, int length) {
        while (++cursor < length && expr[cursor] != type) {
            if (expr[cursor] == '\\') cursor++;
        }

        if (cursor >= length || expr[cursor] != type) {
            throw new CompileException("unterminated literal", expr, cursor);
        }

        return cursor;
    }


    public static void parseWithExpressions(String nestParm, char[] block, int begin, int ending,
                                            Object ctx, VariableResolverFactory factory) {
        /**
         *
         * MAINTENANCE NOTE: A COMPILING VERSION OF THIS CODE IS DUPLICATED IN: WithNode
         *
         */
        int start = begin;
        String parm = "";
        int end = -1;
        int oper = -1;

        for (int i = begin; i < ending; i++) {
            switch (block[i]) {
                case '{':
                case '[':
                case '(':
                    i = balancedCapture(block, i, block[i]);
                    continue;


                case '/':
                    if (i < ending && block[i + 1] == '/') {
                        while (i < ending && block[i] != '\n') block[i++] = ' ';
                        if (parm == null) start = i;
                    } else if (i < ending && block[i + 1] == '*') {
                        int len = ending - 1;
                        while (i < len && !(block[i] == '*' && block[i + 1] == '/')) {
                            block[i++] = ' ';
                        }
                        block[i++] = ' ';
                        block[i++] = ' ';

                        if (parm == null) start = i;
                    } else if (i < ending && block[i + 1] == '=') {
                        oper = Operator.DIV;
                    }
                    continue;

                case '%':
                case '*':
                case '-':
                case '+':
                    if (i + 1 < ending && block[i + 1] == '=') {
                        oper = opLookup(block[i]);
                    }
                    continue;


                case '=':
                    parm = new String(block, start, i - start - (oper != -1 ? 1 : 0)).trim();
                    start = i + 1;
                    continue;

                case ',':
                    if (end == -1) end = i;

                    if (parm == null) {
                        MVEL.eval(new StringAppender(nestParm).append('.')
                                .append(block, start, end - start).toString(), ctx, factory);

                        oper = -1;
                        start = ++i;
                    } else {
                        MVEL.setProperty(ctx, parm, MVEL.eval(new String(createShortFormOperativeAssignment(new StringAppender(nestParm).append(".").append(parm).toString(),
                                subset(block, start, end - start), oper)), ctx, factory));

                        parm = null;
                        oper = -1;
                        start = ++i;
                    }

                    end = -1;
                    break;
            }
        }

        if (start != (end = ending)) {
            if (parm == null || "".equals(parm)) {
                MVEL.eval(new StringAppender(nestParm).append('.')
                        .append(block, start, end - start).toString(), ctx, factory);
            } else {
                MVEL.setProperty(ctx, parm, MVEL.eval(
                        new String(createShortFormOperativeAssignment(new StringAppender(nestParm).append(".").append(parm).toString(),
                                subset(block, start, end - start), oper)), ctx, factory));
            }
        }
    }


    public static Object handleNumericConversion(final char[] val) {
        if (val.length != 1 && val[0] == '0' && val[1] != '.') {
            if (!isDigit(val[val.length - 1])) {
                switch (val[val.length - 1]) {
                    case 'L':
                    case 'l':
                        return Long.decode(new String(val, 0, val.length - 1));
                    case 'I':
                        return BigInteger.valueOf(Long.decode(new String(val, 0, val.length - 1)));
                    case 'D':
                        return BigDecimal.valueOf(Long.decode(new String(val, 0, val.length - 1)));
                }
            }

            return Integer.decode(new String(val));
        } else if (!isDigit(val[val.length - 1])) {
            switch (val[val.length - 1]) {
                case 'l':
                case 'L':
                    return java.lang.Long.parseLong(new String(val, 0, val.length - 1));
                case '.':
                case 'd':
                case 'D':
                    return parseDouble(new String(val, 0, val.length - 1));
                case 'f':
                case 'F':
                    return java.lang.Float.parseFloat(new String(val, 0, val.length - 1));
                case 'I':
                    return new BigInteger(new String(val, 0, val.length - 1));
                case 'B':
                    return new BigDecimal(new String(val, 0, val.length - 1));
            }
            throw new CompileException("unrecognized numeric literal");
        } else {
            switch (numericTest(val)) {
                case DataTypes.FLOAT:
                    return java.lang.Float.parseFloat(new String(val));
                case INTEGER:
                    return java.lang.Integer.parseInt(new String(val));
                case LONG:
                    return java.lang.Long.parseLong(new String(val));
                case DOUBLE:
                    return parseDouble(new String(val));
                case DataTypes.BIG_DECIMAL:
                    return new BigDecimal(val, MathContext.DECIMAL128);
                default:
                    return new String(val);
            }
        }
    }

    public static boolean isNumeric(Object val) {
        if (val == null) return false;

        Class clz;
        if (val instanceof Class) {
            clz = (Class) val;
        } else {
            clz = val.getClass();
        }

        return clz == int.class || clz == long.class || clz == short.class || clz == double.class ||
                clz == float.class || Number.class.isAssignableFrom(clz);
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
                switch (c) {
                    case '.':
                        fp = true;
                        break;
                    case 'e':
                    case 'E':
                        fp = true;
                        if (i++ < len && val[i] == '-') i++;
                        break;

                    default:
                        return -1;
                }
            }
        }

        if (len != 0) {
            if (fp) {
                return DOUBLE;
            } else if (len > 9) {
                return LONG;
            } else {
                return INTEGER;
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
                } else {
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
            switch (val[0]) {
                case '-':
                    if (val[1] == '-') i++;
                case '~':
                    i++;
            }
        }
        for (; i < len; i++) {
            if (!isDigit(c = val[i])) {
                if (f && c == '.') {
                    f = false;
                } else if (len != 1 && i == len - 1) {
                    switch (c) {
                        case 'l':
                        case 'L':
                        case 'f':
                        case 'F':
                        case 'd':
                        case 'D':
                        case 'I':
                        case 'B':
                            return true;
                        case '.':
                            throw new CompileException("invalid number literal: " + new String(val));
                    }
                    return false;
                } else if (i == 1 && c == 'x' && val[0] == '0') {
                    for (i++; i < len; i++) {
                        if (!isDigit(c = val[i])) {
                            if ((c < 'A' || c > 'F') && (c < 'a' || c > 'f')) {
                                if (i == len - 1) {
                                    switch (c) {
                                        case 'l':
                                        case 'L':
                                        case 'I':
                                        case 'B':
                                            return true;
                                    }
                                }

                                return false;
                            }
                        }
                    }
                    return len - 2 > 0;

                } else if (i != 0 && (i + 1) < len && (c == 'E' || c == 'e')) {
                    if (val[++i] == '-' || val[i] == '+') i++;
                } else {
                    if (i != 0) throw new CompileException("invalid number literal: " + new String(val));
                    return false;
                }
            }
        }

        return len > 0;
    }

    public static int find(char[] c, char find) {
        for (int i = 0; i < c.length; i++) if (c[i] == find) return i;
        return -1;
    }

    public static int findLast(char[] c, char find) {
        for (int i = c.length - 1; i != -1; i--) if (c[i] == find) return i;
        return -1;
    }

    public static String createStringTrimmed(char[] s) {
        int start = 0, end = s.length;
        while (start != end && s[start] < '\u0020' + 1) start++;
        while (end != start && s[end - 1] < '\u0020' + 1) end--;
        return new String(s, start, end - start);
    }

    public static String createStringTrimmed(char[] s, int start, int length) {
        if ((length = start + length) > s.length) return new String(s);
        while (start != length && s[start] < '\u0020' + 1) {
            start++;
        }
        while (length != start && s[length - 1] < '\u0020' + 1) {
            length--;
        }
        return new String(s, start, length - start);
    }

    public static boolean endsWith(char[] c, char[] test) {
        if (test.length > c.length) return false;

        int tD = test.length - 1;
        int cD = c.length - 1;
        while (tD != -1) {
            if (c[cD--] != test[tD--]) return false;
        }

        return true;
    }

    public static boolean isIdentifierPart(final int c) {
        return ((c > 96 && c < 123)
                || (c > 64 && c < 91) || (c > 47 && c < 58) || (c == '_') || (c == '$')
                || Character.isJavaIdentifierPart(c));
    }

    public static boolean isDigit(final int c) {
        return c > ('0' - 1) && c < ('9' + 1);
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
        } else {
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

    public static boolean isJunct(char c) {
        switch (c) {
            case '[':
            case '(':
                return true;
            default:
                return isWhitespace(c);
        }
    }

    public static int opLookup(char c) {
        switch (c) {
            case '|':
                return Operator.BW_OR;
            case '&':
                return Operator.BW_AND;
            case '^':
                return Operator.BW_XOR;
            case '*':
                return Operator.MULT;
            case '/':
                return Operator.DIV;
            case '+':
                return Operator.ADD;
            case '%':
                return Operator.MOD;
            case '\u00AB':
                return Operator.BW_SHIFT_LEFT;
            case '\u00BB':
                return Operator.BW_SHIFT_RIGHT;
            case '\u00AC':
                return Operator.BW_USHIFT_RIGHT;
        }
        return -1;
    }

    /**
     * Check if the specified string is a reserved word in the parser.
     *
     * @param name -
     * @return -
     */
    public static boolean isReservedWord(String name) {
        return LITERALS.containsKey(name) || AbstractParser.OPERATORS.containsKey(name);
    }

    /**
     * Check if the specfied string represents a valid name of label.
     *
     * @param name -
     * @return -
     */
    public static boolean isNotValidNameorLabel(String name) {
        for (char c : name.toCharArray()) {
            if (c == '.') return true;
            else if (!isIdentifierPart(c)) return true;
        }
        return false;
    }

    public static final class WithStatementPair implements java.io.Serializable {
        private String parm;
        private String value;

        public WithStatementPair(String parm, String value) {
            this.parm = parm;
            this.value = value;
        }

        public String getParm() {
            return parm;
        }

        public void setParm(String parm) {
            this.parm = parm;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public void eval(Object ctx, VariableResolverFactory vrf) {
            if (parm == null) {
                MVEL.eval(value, ctx, vrf);
            } else {
                MVEL.setProperty(ctx, parm, MVEL.eval(value, ctx, vrf));
            }
        }
    }

    public static void checkNameSafety(String name) {
        if (isReservedWord(name)) {
            throw new CompileException("illegal use of reserved word: " + name);
        } else if (isDigit(name.charAt(0))) {
            throw new CompileException("not an identifier");
        }
    }

    public static FileWriter getDebugFileWriter() throws IOException {
        return new FileWriter(new File(getDebuggingOutputFileName()), true);
    }

    public static boolean isPrimitiveWrapper(Class clazz) {
        return clazz == Integer.class || clazz == Boolean.class || clazz == Long.class || clazz == Double.class
                || clazz == Float.class || clazz == Short.class || clazz == Byte.class || clazz == Character.class;
    }

    public static Serializable subCompileExpression(char[] expression) {
        return _optimizeTree(new ExpressionCompiler(expression)._compile());
    }

    public static Serializable subCompileExpression(char[] expression, ParserContext ctx) {
        ExpressionCompiler c = new ExpressionCompiler(expression);
        if (ctx != null) c.setPCtx(ctx);
        return _optimizeTree(c._compile());
    }

    public static Serializable subCompileExpression(String expression, ParserContext ctx) {
        ExpressionCompiler c = new ExpressionCompiler(expression);
        c.setPCtx(ctx);

        return _optimizeTree(c._compile());
    }

    public static Serializable optimizeTree(final CompiledExpression compiled) {
        /**
         * If there is only one token, and it's an identifier, we can optimize this as an accessor expression.
         */
        if (!compiled.isImportInjectionRequired() &&
                compiled.getParserContext().isAllowBootstrapBypass() && compiled.isSingleNode()) {

            return _optimizeTree(compiled);
        }

        return compiled;
    }

    private static Serializable _optimizeTree(final CompiledExpression compiled) {
        /**
         * If there is only one token, and it's an identifier, we can optimize this as an accessor expression.
         */
        if (compiled.isSingleNode()) {
            ASTNode tk = compiled.getFirstNode();

            if (tk.isLiteral() && !tk.isThisVal()) {
                return new ExecutableLiteral(tk.getLiteralValue());
            }
            return tk.canSerializeAccessor() ? new ExecutableAccessorSafe(tk, compiled.getKnownEgressType()) :
                    new ExecutableAccessor(tk, compiled.getKnownEgressType());
        }

        return compiled;
    }

    public static boolean isWhitespace(char c) {
        return c < '\u0020' + 1;
    }

    public static String repeatChar(char c, int times) {
        char[] n = new char[times];
        for (int i = 0; i < times; i++) {
            n[i] = c;
        }
        return new String(n);
    }

    public static char[] loadFromFile(File file) throws IOException {
        return loadFromFile(file, null);
    }

    public static char[] loadFromFile(File file, String encoding) throws IOException {
        if (!file.exists())
            throw new CompileException("cannot find file: " + file.getName());

        FileInputStream inStream = null;
        ReadableByteChannel fc = null;
        try {
            fc = (inStream = new FileInputStream(file)).getChannel();
            ByteBuffer buf = allocateDirect(10);

            StringAppender sb = new StringAppender((int) file.length(), encoding);

            int read = 0;
            while (read >= 0) {
                buf.rewind();
                read = fc.read(buf);
                buf.rewind();

                for (; read > 0; read--) {
                    sb.append(buf.get());
                }
            }

            //noinspection unchecked
            return sb.toChars();
        }
        catch (FileNotFoundException e) {
            // this can't be thrown, we check for this explicitly.
        }
        finally {
            if (inStream != null) inStream.close();
            if (fc != null) fc.close();
        }

        return null;
    }

    public static char[] readIn(InputStream inStream, String encoding) throws IOException {
        try {
            byte[] buf = new byte[10];

            StringAppender sb = new StringAppender(10, encoding);

            int bytesRead;
            while ((bytesRead = inStream.read(buf)) > 0) {
                for (int i = 0; i < bytesRead; i++) {
                    sb.append(buf[i]);
                }
            }

            //noinspection unchecked
            return sb.toChars();
        }
        finally {
            if (inStream != null) inStream.close();
        }
    }


}
