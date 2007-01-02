package org.mvel;

import org.mvel.util.ParseTools;
import org.mvel.util.PropertyTools;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.*;

public class CompiledAccessor {
    private int start = 0;
    private int cursor = 0;

    private char[] property;
    private int length;

    private AccessorNode rootNode;
    private AccessorNode currNode;

    private Object ctx;

    private Map variables;

    private static final int DONE = -1;
    private static final int BEAN = 0;
    private static final int METH = 1;
    private static final int COL = 2;

    private static final Object[] EMPTYARG = new Object[0];


    public CompiledAccessor(char[] property, Object ctx) {
        this.property = property;
        this.length = property.length;
        this.ctx = ctx;
    }

    public CompiledAccessor(char[] property, Object ctx, Map variables) {
        this.property = property;
        this.length = property != null ? property.length : 0;
        this.ctx = ctx;
        this.variables = variables;
    }

    public CompiledAccessor(Map variables) {
        this.variables = variables;
    }


    public CompiledAccessor(char[] property, int offset, int end, Object ctx, Map variables) {
        this.property = property;
        this.cursor = offset;
        this.length = end;
        this.ctx = ctx;
        this.variables = variables;
    }

    public CompiledAccessor(String property, Object ctx) {
        this.length = (this.property = property.toCharArray()).length;
        this.ctx = ctx;
    }


    public Object compileGetChain() {
        Object curr = ctx;

        try {
            while (cursor < length) {
//                if (curr == null) {
//                    throw new PropertyAccessException("null pointer exception in property: " + new String(property) + " (" + capture() + " is null)");
//                }

                switch (nextToken()) {
                    case BEAN:
                        curr = getBeanProperty(curr, capture());
                        break;
                    case METH:
                        curr = getMethod(curr, capture());
                        break;
                    case COL:
                        curr = getCollectionProperty(curr, capture());
                        break;
                    case DONE:
                        break;
                }
            }

            return curr;
        }
        catch (InvocationTargetException e) {
            throw new PropertyAccessException("could not access property", e);
        }
        catch (IllegalAccessException e) {
            throw new PropertyAccessException("could not access property", e);
        }
        catch (IndexOutOfBoundsException e) {
            throw new PropertyAccessException("array or collection index out of bounds (property: " + new String(property) + ")", e);
        }
        catch (PropertyAccessException e) {
            throw new PropertyAccessException("failed to access property: <<" + new String(property) + ">> in: " + (ctx != null ? ctx.getClass() : null), e);
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

//    private void set(Object value) {
//        curr = ctx;
//
//        try {
//            String tk = null;
//            while (cursor < length) {
//                tk = captureNext();
//                if (!hasMore()) break;
//                curr = getBeanProperty(curr, tk);
//            }
//
//            Member member = checkWriteCache(curr.getClass(), tk == null ? 0 : tk.hashCode());
//            if (member == null) {
//                addWriteCache(curr.getClass(), tk == null ? 0 : tk.hashCode(), (member = PropertyTools.getFieldOrWriteAccessor(curr.getClass(), tk)));
//            }
//
//            if (member instanceof Field) {
//                Field fld = (Field) member;
//
//                if (value != null && !fld.getType().isAssignableFrom(value.getClass())) {
//                    if (!DataConversion.canConvert(fld.getType(), value.getClass())) {
//                        throw new ConversionException("cannot convert type: "
//                                + value.getClass() + ": to " + fld.getType());
//                    }
//
//                    fld.set(curr, DataConversion.convert(value, fld.getType()));
//                }
//                else
//                    fld.set(curr, value);
//            }
//            else if (member != null) {
//                Method meth = (Method) member;
//
//                if (value != null && !meth.getParameterTypes()[0].isAssignableFrom(value.getClass())) {
//                    if (!DataConversion.canConvert(meth.getParameterTypes()[0], value.getClass())) {
//                        throw new ConversionException("cannot convert type: "
//                                + value.getClass() + ": to " + meth.getParameterTypes()[0]);
//                    }
//
//                    meth.invoke(curr, DataConversion.convert(value, meth.getParameterTypes()[0]));
//                }
//                else {
//                    meth.invoke(curr, value);
//                }
//            }
//            else {
//                throw new PropertyAccessException("could not access property (" + property + ") in: " + ctx.getClass().getName());
//            }
//        }
//        catch (InvocationTargetException e) {
//            throw new PropertyAccessException("could not access property", e);
//        }
//        catch (IllegalAccessException e) {
//            throw new PropertyAccessException("could not access property", e);
//        }
//
//    }


    private int nextToken() {
        switch (property[start = cursor]) {
            case'[':
                return COL;
            case'.':
                cursor = ++start;
        }

        //noinspection StatementWithEmptyBody
        while (++cursor < length && Character.isJavaIdentifierPart(property[cursor])) ;


        if (cursor < length) {
            switch (property[cursor]) {
                case'[':
                    return COL;
                case'(':
                    return METH;
                default:
                    return 0;
            }
        }
        return 0;
    }

    private String capture() {
        return new String(property, start, cursor - start);
    }

    public void addAccessorNode(AccessorNode an) {
        if (currNode == null)
            rootNode = currNode = an;
        else {
            currNode = currNode.setNextNode(an);
        }
    }


    private Object getBeanProperty(Object ctx, String property)
            throws IllegalAccessException, InvocationTargetException {

        Class cls = (ctx instanceof Class ? ((Class) ctx) : ctx != null ? ctx.getClass() : null);
        Member member = cls != null ? PropertyTools.getFieldOrAccessor(cls, property) : null;

        if (member instanceof Field) {
            FieldAccessor accessor = new FieldAccessor();
            accessor.setField((Field) member);

            addAccessorNode(accessor);

            return ((Field) member).get(ctx);
        }
        else if (member != null) {
            GetterAccessor accessor = new GetterAccessor();
            accessor.setMethod((Method) member);

            addAccessorNode(accessor);

            return ((Method) member).invoke(ctx, EMPTYARG);
        }
        else if (ctx instanceof Map && ((Map) ctx).containsKey(property)) {
            return ((Map) ctx).get(property);
        }
        else if ("this".equals(property)) {
            return ctx;
        }
        else if (variables != null && variables.containsKey(property)) {
            DefaultPropertyMapAccessor accessor = new DefaultPropertyMapAccessor();
            accessor.setProperty(property);

            addAccessorNode(accessor);

            return variables.get(property);
        }
        else {
            throw new PropertyAccessException("could not access property (" + property + ")");
        }
    }

    private void whiteSpaceSkip() {
        if (cursor < length)
            //noinspection StatementWithEmptyBody
            while (Character.isWhitespace(property[cursor]) && ++cursor < length) ;
    }

    private boolean scanTo(char c) {
        for (; cursor < length; cursor++) {
            if (property[cursor] == c) {
                return true;
            }
        }
        return false;
    }

    private int containsStringLiteralTermination() {
        int pos = cursor;
        for (pos--; pos > 0; pos--) {
            if (property[pos] == '\'' || property[pos] == '"') return pos;
            else if (!Character.isWhitespace(property[pos])) return pos;
        }
        return -1;
    }


    /**
     * Handle accessing a property embedded in a collection, map, or array
     *
     * @param ctx  -
     * @param prop -
     * @return -
     * @throws Exception -
     */
    private Object getCollectionProperty(Object ctx, String prop) throws Exception {
        if (prop.length() > 0) ctx = getBeanProperty(ctx, prop);

        int start = ++cursor;

        whiteSpaceSkip();

        if (cursor == length)
            throw new PropertyAccessException("unterminated '['");

        String item;

        if (property[cursor] == '\'' || property[cursor] == '"') {
            start++;

            int end;

            if (!scanTo(']'))
                throw new PropertyAccessException("unterminated '['");
            if ((end = containsStringLiteralTermination()) == -1)
                throw new PropertyAccessException("unterminated string literal in collection accessor");

            item = new String(property, start, end - start);
        }
        else {
            if (!scanTo(']'))
                throw new PropertyAccessException("unterminated '['");

            item = new String(property, start, cursor - start);
        }

        ++cursor;

        if (ctx instanceof Map) {
            MapAccessor accessor = new MapAccessor();
            accessor.setProperty(item);

            addAccessorNode(accessor);

            return ((Map) ctx).get(item);
        }
        else if (ctx instanceof List) {
            return ((List) ctx).get(Integer.parseInt(item));
        }
        else if (ctx instanceof Collection) {
            int count = Integer.parseInt(item);
            if (count > ((Collection) ctx).size())
                throw new PropertyAccessException("index [" + count + "] out of bounds on collection");

            Iterator iter = ((Collection) ctx).iterator();
            for (int i = 0; i < count; i++) iter.next();
            return iter.next();
        }
        else if (ctx instanceof Object[]) {
            return ((Object[]) ctx)[Integer.parseInt(item)];
        }
        else if (ctx instanceof CharSequence) {
            return ((CharSequence) ctx).charAt(Integer.parseInt(item));
        }
        else {
            throw new PropertyAccessException("illegal use of []: unknown type: " + (ctx == null ? null : ctx.getClass().getName()));
        }
    }

    private static final Map<String, Serializable[]> SUBEXPRESSION_CACHE = new WeakHashMap<String, Serializable[]>();

    /**
     * Find an appropriate method, execute it, and return it's response.
     *
     * @param ctx  -
     * @param name -
     * @return -
     * @throws Exception -
     */
    @SuppressWarnings({"unchecked"})
    private Object getMethod(Object ctx, String name) throws Exception {
        int st = cursor;

        int depth = 1;

        while (cursor++ < length - 1 && depth != 0) {
            switch (property[cursor]) {
                case'(':
                    depth++;
                    continue;
                case')':
                    depth--;

            }
        }
        cursor--;

        String tk = (cursor - st) > 1 ? new String(property, st + 1, cursor - st - 1) : "";

        cursor++;

        Object[] args;
        Serializable[] es;

        if (tk.length() == 0) {
            args = new Object[0];
            es = null;
        }
        else {
            if (SUBEXPRESSION_CACHE.containsKey(tk)) {
                es = SUBEXPRESSION_CACHE.get(tk);
                args = new Object[es.length];
                for (int i = 0; i < es.length; i++) {
                    args[i] = ExpressionParser.executeExpression(es[i], ctx, variables);
                }

            }
            else {
                String[] subtokens = ParseTools.parseParameterList(tk.toCharArray(), 0, -1);

                es = new Serializable[subtokens.length];
                args = new Object[subtokens.length];
                for (int i = 0; i < subtokens.length; i++) {
                    es[i] = ExpressionParser.compileExpression(subtokens[i]);
                    args[i] = ExpressionParser.executeExpression(es[i], ctx, variables);
                    ((CompiledExpression) es[i]).setKnownEgressType(args[i] != null ? args[i].getClass() : null);
                }

                SUBEXPRESSION_CACHE.put(tk, es);
            }

        }

        /**
         * If the target object is an instance of java.lang.Class itself then do not
         * adjust the Class scope target.
         */
        Class cls = ctx instanceof Class ? (Class) ctx : ctx.getClass();

        //    Integer signature = ;

        Method m;
        Class[] parameterTypes = null;

        /**
         * If we have not cached the method then we need to go ahead and try to resolve it.
         */
        /**
         * Try to find an instance method from the class target.
         */

        if ((m = ParseTools.getBestCanadidate(args, name, cls.getMethods())) != null) {
            parameterTypes = m.getParameterTypes();
        }

        if (m == null) {
            /**
             * If we didn't find anything, maybe we're looking for the actual java.lang.Class methods.
             */
            if ((m = ParseTools.getBestCanadidate(args, name, cls.getClass().getDeclaredMethods())) != null) {
                parameterTypes = m.getParameterTypes();
            }
        }


        if (m == null) {
            StringBuilder errorBuild = new StringBuilder();
            for (int i = 0; i < args.length; i++) {
                errorBuild.append(args[i] != null ? args[i].getClass().getName() : null);
                if (i < args.length - 1) errorBuild.append(", ");
            }

            throw new PropertyAccessException("unable to resolve method: " + cls.getName() + "." + name + "(" + errorBuild.toString() + ") [arglength=" + args.length + "]");
        }
        else {
            if (es != null) {
                CompiledExpression cExpr;
                for (int i = 0; i < es.length; i++) {
                    cExpr = ((CompiledExpression) es[i]);
                    if (cExpr.getKnownIngressType() == null) {
                        cExpr.setKnownIngressType(parameterTypes[i]);
                        cExpr.pack();
                    }
                    if (!cExpr.isConvertableIngressEgress()) {
                        args[i] = DataConversion.convert(args[i], parameterTypes[i]);
                    }
                }
            }
            else {
                /**
                 * Coerce any types if required.
                 */
                for (int i = 0; i < args.length; i++)
                    args[i] = DataConversion.convert(args[i], parameterTypes[i]);
            }


            MethodAccessor access = new MethodAccessor();
            access.setMethod(m);
            access.setCompiledParameters(es);

            addAccessorNode(access);

            /**
             * Invoke the target method and return the response.
             */
            return m.invoke(ctx, args);
        }
    }


    public void setVariables(Map variables) {
        this.variables = variables;
    }

    public Object getValue(Object ctx, Object elCtx, Map vars) throws Exception {
        return rootNode.getValue(ctx, elCtx, vars);
    }
}
