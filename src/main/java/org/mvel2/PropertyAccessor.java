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
package org.mvel2;

import static org.mvel2.DataConversion.canConvert;
import static org.mvel2.DataConversion.convert;
import static org.mvel2.MVEL.eval;
import org.mvel2.ast.Function;
import org.mvel2.ast.Proto;
import org.mvel2.ast.TypeDescriptor;
import static org.mvel2.ast.TypeDescriptor.getClassReference;
import static org.mvel2.compiler.AbstractParser.LITERALS;
import static org.mvel2.compiler.AbstractParser.getCurrentThreadParserContext;
import org.mvel2.integration.GlobalListenerFactory;
import static org.mvel2.integration.GlobalListenerFactory.notifySetListeners;
import static org.mvel2.integration.PropertyHandlerFactory.*;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.util.MethodStub;
import org.mvel2.util.ParseTools;
import static org.mvel2.util.ParseTools.*;
import static org.mvel2.util.PropertyTools.getFieldOrAccessor;
import static org.mvel2.util.PropertyTools.getFieldOrWriteAccessor;
import org.mvel2.util.StringAppender;

import static java.lang.Character.isJavaIdentifierPart;
import static java.lang.Thread.currentThread;

import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import static java.lang.reflect.Array.getLength;
import java.util.*;


@SuppressWarnings({"unchecked"})
/**
 * The property accessor class is used for extracting properties from objects instances.
 */
public class PropertyAccessor {
    private int start = 0;
    private int cursor = 0;

    private char[] property;
    private int length;

    private Object thisReference;
    private Object ctx;
    private Object curr;

    private boolean first = true;
    private boolean nullHandle = false;

    private VariableResolverFactory variableFactory;

    private static final int DONE = -1;
    private static final int NORM = 0;
    private static final int METH = 1;
    private static final int COL = 2;
    private static final int WITH = 3;

    private static final Object[] EMPTYARG = new Object[0];

    private static final WeakHashMap<Class, WeakHashMap<Integer, WeakReference<Member>>> READ_PROPERTY_RESOLVER_CACHE;
    private static final WeakHashMap<Class, WeakHashMap<Integer, WeakReference<Member>>> WRITE_PROPERTY_RESOLVER_CACHE;
    private static final WeakHashMap<Class, WeakHashMap<Integer, WeakReference<Object[]>>> METHOD_RESOLVER_CACHE;
    private static final WeakHashMap<Member, WeakReference<Class[]>> METHOD_PARMTYPES_CACHE;

    static {
        READ_PROPERTY_RESOLVER_CACHE = (new WeakHashMap<Class, WeakHashMap<Integer, WeakReference<Member>>>(10));
        WRITE_PROPERTY_RESOLVER_CACHE = (new WeakHashMap<Class, WeakHashMap<Integer, WeakReference<Member>>>(10));
        METHOD_RESOLVER_CACHE = (new WeakHashMap<Class, WeakHashMap<Integer, WeakReference<Object[]>>>(10));
        METHOD_PARMTYPES_CACHE = new WeakHashMap<Member, WeakReference<Class[]>>(10);
    }


    public PropertyAccessor(char[] property, Object ctx) {
        this.property = property;
        this.length = property.length;
        this.ctx = ctx;
    }

    public PropertyAccessor(char[] property, Object ctx, VariableResolverFactory resolver, Object thisReference) {
        this.length = (this.property = property).length;
        this.ctx = ctx;
        this.variableFactory = resolver;
        this.thisReference = thisReference;
    }

    public PropertyAccessor(char[] property, Object ctx, Object thisRef, VariableResolverFactory resolver, Object thisReference) {
        this.length = (this.property = property).length;
        this.ctx = ctx;
        this.thisReference = thisRef;
        this.variableFactory = resolver;
        this.thisReference = thisReference;
    }

    public PropertyAccessor(VariableResolverFactory resolver, Object thisReference) {
        this.variableFactory = resolver;
        this.thisReference = thisReference;
    }


    public PropertyAccessor(char[] property, int offset, int end, Object ctx, VariableResolverFactory resolver) {
        this.property = property;
        this.cursor = offset;
        this.length = end;
        this.ctx = ctx;
        this.variableFactory = resolver;
    }

    public PropertyAccessor(String property, Object ctx) {
        this.length = (this.property = property.toCharArray()).length;
        this.ctx = ctx;
    }

    public static Object get(String property, Object ctx) {
        return new PropertyAccessor(property, ctx).get();
    }

    public static Object get(char[] property, Object ctx, VariableResolverFactory resolver, Object thisReference) {
        return new PropertyAccessor(property, ctx, resolver, thisReference).get();
    }

    public static Object get(char[] property, int offset, int end, Object ctx, VariableResolverFactory resolver) {
        return new PropertyAccessor(property, offset, end, ctx, resolver).get();
    }

    public static Object get(String property, Object ctx, VariableResolverFactory resolver, Object thisReference) {
        return new PropertyAccessor(property.toCharArray(), ctx, resolver, thisReference).get();
    }

    public static void set(Object ctx, String property, Object value) {
        new PropertyAccessor(property, ctx).set(value);
    }

    public static void set(Object ctx, VariableResolverFactory resolver, String property, Object value) {
        new PropertyAccessor(property.toCharArray(), ctx, resolver, null).set(value);
    }

    private Object get() {
        curr = ctx;
        try {
            if (!MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING) {
                while (cursor < length) {
                    switch (nextToken()) {
                        case NORM:
                            curr = getBeanProperty(curr, capture());
                            break;
                        case METH:
                            curr = getMethod(curr, capture());
                            break;
                        case COL:
                            curr = getCollectionProperty(curr, capture());
                            break;
                        case WITH:
                            curr = getWithProperty(curr);
                            break;

                        case DONE:
                    }

                    if (nullHandle) {
                        if (curr == null) {
                            return null;
                        }
                        else {
                            nullHandle = false;
                        }
                    }

                    first = false;
                }
                return curr;
            }
            else {
                while (cursor < length) {
                    switch (nextToken()) {
                        case NORM:
                            if ((curr = getBeanPropertyAO(curr, capture())) == null && hasNullPropertyHandler()) {
                                curr = getNullPropertyHandler().getProperty(capture(), ctx, variableFactory);
                            }
                            break;
                        case METH:
                            if ((curr = getMethod(curr, capture())) == null && hasNullMethodHandler()) {
                                curr = getNullMethodHandler().getProperty(capture(), ctx, variableFactory);
                            }
                            break;
                        case COL:
                            curr = getCollectionPropertyAO(curr, capture());
                            break;
                        case WITH:
                            curr = getWithProperty(curr);
                            break;

                        case DONE:
                    }

                    if (nullHandle) {
                        if (curr == null) {
                            return null;
                        }
                        else {
                            nullHandle = false;
                        }
                    }

                    first = false;
                }
                return curr;
            }


        }
        catch (InvocationTargetException e) {
            throw new PropertyAccessException("could not access property", e);
        }
        catch (IllegalAccessException e) {
            throw new PropertyAccessException("could not access property", e);
        }
        catch (IndexOutOfBoundsException e) {
            throw new PropertyAccessException("array or collections index out of bounds in property: " + new String(property), e);
        }
        catch (PropertyAccessException e) {
            throw new PropertyAccessException("failed to access property: " + new String(property) + ": " + e.getMessage(), e);
        }
        catch (CompileException e) {
            throw e;
        }
        catch (NullPointerException e) {
            throw new PropertyAccessException("null pointer exception in property: " + new String(property), e);
        }
        catch (Exception e) {
            throw new PropertyAccessException("unknown exception in expression: " + new String(property), e);
        }
    }

    private void set(Object value) {
        curr = ctx;

        try {
            int oLength = length;

            length = findAbsoluteLast(property);

            if ((curr = get()) == null)
                throw new PropertyAccessException("cannot bind to null context: " + new String(property));

            length = oLength;

            if (nextToken() == COL) {
                int start = ++cursor;

                whiteSpaceSkip();

                if (cursor == length || scanTo(']'))
                    throw new PropertyAccessException("unterminated '['");

                String ex = new String(property, start, cursor - start);

                if (!MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING) {
                    if (curr instanceof Map) {
                        //noinspection unchecked
                        ((Map) curr).put(eval(ex, this.ctx, this.variableFactory), value);
                    }
                    else if (curr instanceof List) {
                        //noinspection unchecked
                        ((List) curr).set(eval(ex, this.ctx, this.variableFactory, Integer.class), value);
                    }
                    else if (hasPropertyHandler(curr.getClass())) {
                        getPropertyHandler(curr.getClass()).setProperty(ex, ctx, variableFactory, value);
                    }
                    else if (curr.getClass().isArray()) {
                        Array.set(curr, eval(ex, this.ctx, this.variableFactory, Integer.class), convert(value, getBaseComponentType(curr.getClass())));
                    }

                    else {
                        throw new PropertyAccessException("cannot bind to collection property: " + new String(property) + ": not a recognized collection type: " + ctx.getClass());
                    }

                    return;
                }
                else {
                    notifySetListeners(ctx, ex, variableFactory, value);

                    if (curr instanceof Map) {
                        //noinspection unchecked
                        if (hasPropertyHandler(Map.class))
                            getPropertyHandler(Map.class).setProperty(ex, curr, variableFactory, value);
                        else
                            ((Map) curr).put(eval(ex, this.ctx, this.variableFactory), value);
                    }
                    else if (curr instanceof List) {
                        //noinspection unchecked
                        if (hasPropertyHandler(List.class))
                            getPropertyHandler(List.class).setProperty(ex, curr, variableFactory, value);
                        else
                            ((List) curr).set(eval(ex, this.ctx, this.variableFactory, Integer.class), value);
                    }
                    else if (curr.getClass().isArray()) {
                        if (hasPropertyHandler(Array.class))
                            getPropertyHandler(Array.class).setProperty(ex, curr, variableFactory, value);
                        else
                            Array.set(curr, eval(ex, this.ctx, this.variableFactory, Integer.class), convert(value, getBaseComponentType(curr.getClass())));
                    }
                    else if (hasPropertyHandler(curr.getClass())) {
                        getPropertyHandler(curr.getClass()).setProperty(ex, curr, variableFactory, value);
                    }
                    else {
                        throw new PropertyAccessException("cannot bind to collection property: " + new String(property) + ": not a recognized collection type: " + ctx.getClass());
                    }

                    return;
                }
            }
            else if (MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING && hasPropertyHandler(curr.getClass())) {
                getPropertyHandler(curr.getClass()).setProperty(capture(), curr, variableFactory, value);
                return;
            }

            String tk = capture();

            Member member = checkWriteCache(curr.getClass(), tk == null ? 0 : tk.hashCode());
            if (member == null) {
                addWriteCache(curr.getClass(), tk.hashCode(),
                        (member = value != null ? getFieldOrWriteAccessor(curr.getClass(), tk, value.getClass()) : getFieldOrWriteAccessor(curr.getClass(), tk)));
            }

            if (member instanceof Method) {
                Method meth = (Method) member;

                Class[] paramaterTypes = checkParmTypesCache(meth);

                if (value != null && !paramaterTypes[0].isAssignableFrom(value.getClass())) {
                    if (!canConvert(paramaterTypes[0], value.getClass())) {
                        throw new ConversionException("cannot convert type: "
                                + value.getClass() + ": to " + meth.getParameterTypes()[0]);
                    }
                    meth.invoke(curr, convert(value, paramaterTypes[0]));
                }
                else {
                    meth.invoke(curr, value);
                }
            }
            else if (member != null) {
                Field fld = (Field) member;

                if (value != null && !fld.getType().isAssignableFrom(value.getClass())) {
                    if (!canConvert(fld.getType(), value.getClass())) {
                        throw new ConversionException("cannot convert type: "
                                + value.getClass() + ": to " + fld.getType());
                    }

                    fld.set(curr, convert(value, fld.getType()));
                }
                else {
                    fld.set(curr, value);
                }
            }
            else if (curr instanceof Map) {
                //noinspection unchecked
                ((Map) curr).put(eval(tk, this.ctx, this.variableFactory), value);
            }
            else {
                throw new PropertyAccessException("could not access/write property (" + tk + ") in: "
                        + (curr == null ? "Unknown" : curr.getClass().getName()));
            }
        }
        catch (InvocationTargetException e) {
            throw new PropertyAccessException("could not access property", e);
        }
        catch (IllegalAccessException e) {
            throw new PropertyAccessException("could not access property", e);
        }
    }


    private int nextToken() {
        switch (property[start = cursor]) {
            case '[':
                return COL;
            case '.':
                // ++cursor;
                while (cursor < length && isWhitespace(property[cursor])) cursor++;
                if ((start + 1) != length) {
                    switch (property[cursor = ++start]) {
                        case '?':
                            cursor = ++start;
                            nullHandle = true;
                            break;
                        case '{':
                            return WITH;
                    }

                }
        }

        while (cursor < length && isWhitespace(property[cursor])) cursor++;
        start = cursor;

        //noinspection StatementWithEmptyBody
        while (++cursor < length && isJavaIdentifierPart(property[cursor])) ;

        if (cursor < length) {
            while (isWhitespace(property[cursor])) cursor++;
            switch (property[cursor]) {
                case '[':
                    return COL;
                case '(':
                    return METH;
                default:
                    return 0;
            }
        }
        return 0;
    }

    private String capture() {
        return new String(property, start, trimLeft(cursor) - start);
    }

    protected int trimLeft(int pos) {
        while (pos > 0 && isWhitespace(property[pos - 1])) pos--;
        return pos;
    }

    public static void clearPropertyResolverCache() {
        READ_PROPERTY_RESOLVER_CACHE.clear();
        WRITE_PROPERTY_RESOLVER_CACHE.clear();
        METHOD_RESOLVER_CACHE.clear();
    }

    public static void reportCacheSizes() {
        System.out.println("read property cache: " + READ_PROPERTY_RESOLVER_CACHE.size());
        for (Class cls : READ_PROPERTY_RESOLVER_CACHE.keySet()) {
            System.out.println(" [" + cls.getName() + "]: " + READ_PROPERTY_RESOLVER_CACHE.get(cls).size() + " entries.");
        }
        System.out.println("write property cache: " + WRITE_PROPERTY_RESOLVER_CACHE.size());
        for (Class cls : WRITE_PROPERTY_RESOLVER_CACHE.keySet()) {
            System.out.println(" [" + cls.getName() + "]: " + WRITE_PROPERTY_RESOLVER_CACHE.get(cls).size() + " entries.");
        }
        System.out.println("method cache: " + METHOD_RESOLVER_CACHE.size());
        for (Class cls : METHOD_RESOLVER_CACHE.keySet()) {
            System.out.println(" [" + cls.getName() + "]: " + METHOD_RESOLVER_CACHE.get(cls).size() + " entries.");
        }
    }

    private static void addReadCache(Class cls, Integer property, Member member) {
        synchronized (READ_PROPERTY_RESOLVER_CACHE) {
            WeakHashMap<Integer, WeakReference<Member>> nestedMap = READ_PROPERTY_RESOLVER_CACHE.get(cls);

            if (nestedMap == null) {
                READ_PROPERTY_RESOLVER_CACHE.put(cls, nestedMap = new WeakHashMap<Integer, WeakReference<Member>>());
            }

            nestedMap.put(property, new WeakReference<Member>(member));
        }
    }

    private static Member checkReadCache(Class cls, Integer property) {
        WeakHashMap<Integer, WeakReference<Member>> map = READ_PROPERTY_RESOLVER_CACHE.get(cls);
        if (map != null) {
            WeakReference<Member> member = map.get(property);
            if (member != null) return member.get();
        }
        return null;
    }

    private static void addWriteCache(Class cls, Integer property, Member member) {
        synchronized (WRITE_PROPERTY_RESOLVER_CACHE) {
            WeakHashMap<Integer, WeakReference<Member>> map = WRITE_PROPERTY_RESOLVER_CACHE.get(cls);
            if (map == null) {
                WRITE_PROPERTY_RESOLVER_CACHE.put(cls, map = new WeakHashMap<Integer, WeakReference<Member>>());
            }
            map.put(property, new WeakReference<Member>(member));
        }
    }

    private static Member checkWriteCache(Class cls, Integer property) {
        Map<Integer, WeakReference<Member>> map = WRITE_PROPERTY_RESOLVER_CACHE.get(cls);
        if (map != null) {
            WeakReference<Member> member = map.get(property);
            if (member != null) return member.get();
        }
        return null;
    }

    public static Class[] checkParmTypesCache(Method member) {
        WeakReference<Class[]> pt = METHOD_PARMTYPES_CACHE.get(member);
        Class[] ret;
        if (pt == null || (ret = pt.get()) == null) {
            METHOD_PARMTYPES_CACHE.put(member, pt = new WeakReference<Class[]>(ret = member.getParameterTypes()));
        }
        return ret;
    }


    private static void addMethodCache(Class cls, Integer property, Method member) {
        synchronized (METHOD_RESOLVER_CACHE) {
            WeakHashMap<Integer, WeakReference<Object[]>> map = METHOD_RESOLVER_CACHE.get(cls);
            if (map == null) {
                METHOD_RESOLVER_CACHE.put(cls, map = new WeakHashMap<Integer, WeakReference<Object[]>>());
            }
            map.put(property, new WeakReference<Object[]>(new Object[]{member, member.getParameterTypes()}));
        }
    }

    private static Object[] checkMethodCache(Class cls, Integer property) {
        Map<Integer, WeakReference<Object[]>> map = METHOD_RESOLVER_CACHE.get(cls);
        if (map != null) {
            WeakReference<Object[]> ref =  map.get(property);
            if (ref != null) return ref.get();
        }
        return null;
    }

    private Object getBeanPropertyAO(Object ctx, String property)
            throws IllegalAccessException, InvocationTargetException {
        if (ctx != null && hasPropertyHandler(ctx.getClass()))
            return getPropertyHandler(ctx.getClass()).getProperty(property, ctx, variableFactory);

        GlobalListenerFactory.notifyGetListeners(ctx, property, variableFactory);

        return getBeanProperty(ctx, property);
    }

    private Object getBeanProperty(Object ctx, String property)
            throws IllegalAccessException, InvocationTargetException {

        if (first) {
            if ("this".equals(property)) {
                return this.ctx;
            }
            else if (LITERALS.containsKey(property)) {
                return LITERALS.get(property);
            }
            else if (variableFactory != null && variableFactory.isResolveable(property)) {
                return variableFactory.getVariableResolver(property).getValue();
            }
        }

        if (ctx != null) {
            Class cls;
            Member member = checkReadCache(cls = (ctx instanceof Class ? ((Class) ctx) : ctx.getClass()), property.hashCode());

            if (member == null) {
                addReadCache(cls, property.hashCode(), member = getFieldOrAccessor(cls, property));
            }

            if (member instanceof Method) {
                try {
                    return ((Method) member).invoke(ctx, EMPTYARG);
                }
                catch (IllegalAccessException e) {
                    synchronized (member) {
                        try {
                            ((Method) member).setAccessible(true);
                            return ((Method) member).invoke(ctx, EMPTYARG);
                        }
                        finally {
                            ((Method) member).setAccessible(false);
                        }
                    }
                }
            }
            else if (member != null) {
                return ((Field) member).get(ctx);
            }
            else if (ctx instanceof Map && ((Map) ctx).containsKey(property)) {
                if (ctx instanceof Proto.ProtoInstance) {
                    return ((Proto.ProtoInstance) ctx).get(property).call(null, thisReference, variableFactory, EMPTY_OBJ_ARR);
                }
                return ((Map) ctx).get(property);
            }
            else if ("length".equals(property) && ctx.getClass().isArray()) {
                return getLength(ctx);
            }
            else if (ctx instanceof Class) {
                Class c = (Class) ctx;
                for (Method m : c.getMethods()) {
                    if (property.equals(m.getName())) {
                        if (MVEL.COMPILER_OPT_ALLOW_NAKED_METH_CALL) {
                            return m.invoke(ctx, EMPTY_OBJ_ARR);
                        }
                        return m;
                    }
                }

                try {
                    return findClass(variableFactory, c.getName() + "$" + property, null);
                }
                catch (ClassNotFoundException cnfe) {
                    // fall through.
                }
            }
            else if (hasPropertyHandler(cls)) {
                return getPropertyHandler(cls).getProperty(property, ctx, variableFactory);
            }
        }

        Object tryStatic = tryStaticAccess();

        if (tryStatic != null) {
            if (tryStatic instanceof Class || tryStatic instanceof Method) return tryStatic;
            else {
                return ((Field) tryStatic).get(null);
            }
        }
        else if (MVEL.COMPILER_OPT_ALLOW_NAKED_METH_CALL) {
            return getMethod(ctx, property);
        }

        throw new PropertyAccessException("could not access property: " + property);
    }

    private void whiteSpaceSkip() {
        if (cursor < length)
            //noinspection StatementWithEmptyBody
            while (isWhitespace(property[cursor]) && ++cursor < length) ;
    }

    /**
     * @param c - character to scan to.
     * @return - returns true is end of statement is hit, false if the scan scar is countered.
     */
    private boolean scanTo(char c) {
        for (; cursor < length; cursor++) {
            switch (property[cursor]) {
                case '\'':
                case '"':
                    cursor = captureStringLiteral(property[cursor], property, cursor, property.length);
                default:
                    if (property[cursor] == c) {
                        return false;
                    }
            }

        }
        return true;
    }

    private Object getWithProperty(Object ctx) {
        parseWithExpressions(new String(property, 0, cursor - 1).trim(), property, cursor + 1,
                cursor = balancedCaptureWithLineAccounting(property, cursor, '{', getCurrentThreadParserContext()), ctx, variableFactory);
        cursor++;
        return ctx;
    }

    /**
     * Handle accessing a property embedded in a collections, map, or array
     *
     * @param ctx  -
     * @param prop -
     * @return -
     * @throws Exception -
     */
    private Object getCollectionProperty(Object ctx, String prop) throws Exception {
        if (prop.length() != 0) {
            ctx = getBeanProperty(ctx, prop);
        }

        int start = ++cursor;
        whiteSpaceSkip();

        if (cursor == length || scanTo(']'))
            throw new PropertyAccessException("unterminated '['");

        prop = new String(property, start, cursor++ - start);

        if (ctx instanceof Map) {
            return ((Map) ctx).get(eval(prop, ctx, variableFactory));
        }
        else if (ctx instanceof List) {
            return ((List) ctx).get((Integer) eval(prop, ctx, variableFactory));
        }
        else if (ctx instanceof Collection) {
            int count = (Integer) eval(prop, ctx, variableFactory);
            if (count > ((Collection) ctx).size())
                throw new PropertyAccessException("index [" + count + "] out of bounds on collections");

            Iterator iter = ((Collection) ctx).iterator();
            for (int i = 0; i < count; i++) iter.next();
            return iter.next();
        }
        else if (ctx.getClass().isArray()) {
            return Array.get(ctx, (Integer) eval(prop, ctx, variableFactory));
        }
        else if (ctx instanceof CharSequence) {
            return ((CharSequence) ctx).charAt((Integer) eval(prop, ctx, variableFactory));
        }
        else {
            //     TypeDescriptor td = new TypeDescriptor(property, 0);
            try {
                return getClassReference(getCurrentThreadParserContext(), (Class) ctx, new TypeDescriptor(property, 0));
            }
            catch (Exception e) {
                throw new PropertyAccessException("illegal use of []: unknown type: " + (ctx == null ? null : ctx.getClass().getName()), e);
            }
        }
    }

    private Object getCollectionPropertyAO(Object ctx, String prop) throws Exception {
        if (prop.length() != 0) {
            ctx = getBeanProperty(ctx, prop);
        }

        int start = ++cursor;
        whiteSpaceSkip();

        if (cursor == length || scanTo(']'))
            throw new PropertyAccessException("unterminated '['");

        prop = new String(property, start, cursor++ - start);

        if (ctx instanceof Map) {
            if (hasPropertyHandler(Map.class))
                return getPropertyHandler(Map.class).getProperty(prop, ctx, variableFactory);
            else
                return ((Map) ctx).get(eval(prop, ctx, variableFactory));
        }
        else if (ctx instanceof List) {
            if (hasPropertyHandler(List.class))
                return getPropertyHandler(List.class).getProperty(prop, ctx, variableFactory);
            else
                return ((List) ctx).get((Integer) eval(prop, ctx, variableFactory));
        }
        else if (ctx instanceof Collection) {
            if (hasPropertyHandler(Collection.class))
                return getPropertyHandler(Collection.class).getProperty(prop, ctx, variableFactory);
            else {
                int count = (Integer) eval(prop, ctx, variableFactory);
                if (count > ((Collection) ctx).size())
                    throw new PropertyAccessException("index [" + count + "] out of bounds on collections");

                Iterator iter = ((Collection) ctx).iterator();
                for (int i = 0; i < count; i++) iter.next();
                return iter.next();
            }
        }
        else if (ctx.getClass().isArray()) {
            if (hasPropertyHandler(Array.class))
                return getPropertyHandler(Array.class).getProperty(prop, ctx, variableFactory);

            return Array.get(ctx, (Integer) eval(prop, ctx, variableFactory));
        }
        else if (ctx instanceof CharSequence) {
            if (hasPropertyHandler(CharSequence.class))
                return getPropertyHandler(CharSequence.class).getProperty(prop, ctx, variableFactory);
            else
                return ((CharSequence) ctx).charAt((Integer) eval(prop, ctx, variableFactory));
        }
        else {
            try {
                return getClassReference(getCurrentThreadParserContext(), (Class) ctx, new TypeDescriptor(property, 0));
            }
            catch (Exception e) {
                throw new PropertyAccessException("illegal use of []: unknown type: " + (ctx == null ? null : ctx.getClass().getName()));
            }
        }
    }


    /**
     * Find an appropriate method, execute it, and return it's response.
     *
     * @param ctx  -
     * @param name -
     * @return -
     * @throws Exception -
     */
    @SuppressWarnings({"unchecked"})
    private Object getMethod(Object ctx, String name) {
        int st = cursor;

        String tk = cursor != length
                && property[cursor] == '(' && ((cursor = balancedCapture(property, cursor, '(')) - st) > 1 ?
                new String(property, st + 1, cursor - st - 1) : "";

        cursor++;

        Object[] args;
        if (tk.length() == 0) {
            args = ParseTools.EMPTY_OBJ_ARR;
        }
        else {
            String[] subtokens = parseParameterList(tk.toCharArray(), 0, -1);
            args = new Object[subtokens.length];
            for (int i = 0; i < subtokens.length; i++) {
                args[i] = eval(subtokens[i], thisReference, variableFactory);
            }
        }

        if (first && variableFactory != null && variableFactory.isResolveable(name)) {
            Object ptr = variableFactory.getVariableResolver(name).getValue();
            if (ptr instanceof Method) {
                ctx = ((Method) ptr).getDeclaringClass();
                name = ((Method) ptr).getName();
            }
            else if (ptr instanceof MethodStub) {
                ctx = ((MethodStub) ptr).getClassReference();
                name = ((MethodStub) ptr).getMethodName();
            }
            else if (ptr instanceof Function) {
                ((Function) ptr).checkArgumentCount(args.length);
                return ((Function) ptr).call(null, thisReference, variableFactory, args);
            }
            else {
                throw new OptimizationFailure("attempt to optimize a method call for a reference that does not point to a method: "
                        + name + " (reference is type: " + (ctx != null ? ctx.getClass().getName() : null) + ")");
            }

            first = false;
        }

        if (ctx == null) throw new CompileException("no such method or function: " + name);

        /**
         * If the target object is an instance of java.lang.Class itself then do not
         * adjust the Class scope target.
         */
        Class cls = (ctx instanceof Class ? (Class) ctx : ctx.getClass());

        if (cls == Proto.ProtoInstance.class) {
            return ((Proto.ProtoInstance) ctx).get(name).call(null, thisReference, variableFactory, args);
        }

        /**
         * Check to see if we have already cached this method;
         */
        Object[] cache = checkMethodCache(cls, createSignature(name, tk));

        Method m;
        Class[] parameterTypes;

        if (cache != null) {
            m = (Method) cache[0];
            parameterTypes = (Class[]) cache[1];
        }
        else {
            m = null;
            parameterTypes = null;
        }

        /**
         * If we have not cached the method then we need to go ahead and try to resolve it.
         */
        if (m == null) {
            /**
             * Try to find an instance method from the class target.
             */
            if ((m = getBestCandidate(args, name, cls, cls.getMethods(), false)) != null) {
                addMethodCache(cls, createSignature(name, tk), m);
                parameterTypes = m.getParameterTypes();
            }

            if (m == null) {
                /**
                 * If we didn't find anything, maybe we're looking for the actual java.lang.Class methods.
                 */
                if ((m = getBestCandidate(args, name, cls, cls.getClass().getDeclaredMethods(), false)) != null) {
                    addMethodCache(cls, createSignature(name, tk), m);
                    parameterTypes = m.getParameterTypes();
                }
            }
        }

        if (m == null) {
            StringAppender errorBuild = new StringAppender();
            for (int i = 0; i < args.length; i++) {
                errorBuild.append(args[i] != null ? args[i].getClass().getName() : null);
                if (i < args.length - 1) errorBuild.append(", ");
            }

            if ("size".equals(name) && args.length == 0 && cls.isArray()) {
                return getLength(ctx);
            }


            throw new PropertyAccessException("unable to resolve method: " + cls.getName() + "." + name + "(" + errorBuild.toString() + ") [arglength=" + args.length + "]");
        }
        else {
            for (int i = 0; i < args.length; i++) {
                args[i] = convert(args[i], parameterTypes[i]);
            }

            /**
             * Invoke the target method and return the response.
             */
            try {
                return m.invoke(ctx, args);
            }
            catch (IllegalAccessException e) {
                try {
                    addMethodCache(cls, createSignature(name, tk), (m = getWidenedTarget(m)));

                    return m.invoke(ctx, args);
                }
                catch (Exception e2) {
                    throw new PropertyAccessException("unable to invoke method: " + name, e2);
                }
            }
            catch (Exception e) {
                throw new PropertyAccessException("unable to invoke method: " + name, e);
            }
        }
    }

    private static int createSignature(String name, String args) {
        return name.hashCode() + args.hashCode();
    }

    public int getCursorPosition() {
        return cursor;
    }


    /**
     * Try static access of the property, and return an instance of the Field, Method of Class if successful.
     *
     * @return - Field, Method or Class instance.
     */
    protected Object tryStaticAccess() {
        int begin = cursor;
        try {
            /**
             * Try to resolve this *smartly* as a static class reference.
             *
             * This starts at the end of the token and starts to step backwards to figure out whether
             * or not this may be a static class reference.  We search for method calls simply by
             * inspecting for ()'s.  The first union area we come to where no brackets are present is our
             * test-point for a class reference.  If we find a class, we pass the reference to the
             * property accessor along  with trailing methods (if any).
             *
             */
            boolean meth = false;
            int last = length;
            for (int i = length - 1; i > 0; i--) {
                switch (property[i]) {
                    case '.':
                        if (!meth) {
                            try {
                                return currentThread().getContextClassLoader().loadClass(new String(property, 0, cursor = last));
                            }
                            catch (ClassNotFoundException e) {
                                Class cls = currentThread().getContextClassLoader().loadClass(new String(property, 0, i));
                                String name = new String(property, i + 1, property.length - i - 1);
                                try {
                                    return cls.getField(name);
                                }
                                catch (NoSuchFieldException nfe) {
                                    for (Method m : cls.getMethods()) {
                                        if (name.equals(m.getName())) return m;
                                    }
                                    return null;
                                }
                            }
                        }

                        meth = false;
                        last = i;
                        break;

                    case '}':
                        i--;
                        for (int d = 1; i > 0 && d != 0; i--) {
                            switch (property[i]) {
                                case '}':
                                    d++;
                                    break;
                                case '{':
                                    d--;
                                    break;
                                case '"':
                                case '\'':
                                    char s = property[i];
                                    while (i > 0 && (property[i] != s && property[i - 1] != '\\')) i--;
                            }
                        }
                        break;

                    case ')':
                        i--;

                        for (int d = 1; i > 0 && d != 0; i--) {
                            switch (property[i]) {
                                case ')':
                                    d++;
                                    break;
                                case '(':
                                    d--;
                                    break;
                                case '"':
                                case '\'':
                                    char s = property[i];
                                    while (i > 0 && (property[i] != s && property[i - 1] != '\\')) i--;
                            }
                        }

                        meth = true;

                        last = i++;

                        break;


                    case '\'':
                        while (--i > 0) {
                            if (property[i] == '\'' && property[i - 1] != '\\') {
                                break;
                            }
                        }
                        break;

                    case '"':
                        while (--i > 0) {
                            if (property[i] == '"' && property[i - 1] != '\\') {
                                break;
                            }
                        }
                        break;
                }
            }
        }
        catch (Exception cnfe) {
            cursor = begin;
        }

        return null;
    }
}
