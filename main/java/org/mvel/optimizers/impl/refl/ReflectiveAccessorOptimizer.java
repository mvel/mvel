/**
 * MVEL (The MVFLEX Expression Language)
 *
 * Copyright (C) 2007 Christopher Brock, MVFLEX/Valhalla Project and the Codehaus
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
 *
 */

package org.mvel.optimizers.impl.refl;

import org.mvel.*;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.AbstractOptimizer;
import org.mvel.optimizers.AccessorOptimizer;
import org.mvel.optimizers.impl.refl.collection.ArrayCreator;
import org.mvel.optimizers.impl.refl.collection.ExprValueAccessor;
import org.mvel.optimizers.impl.refl.collection.ListCreator;
import org.mvel.optimizers.impl.refl.collection.MapCreator;
import org.mvel.util.*;
import static org.mvel.util.ParseTools.*;

import static java.lang.Integer.parseInt;
import java.lang.reflect.*;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class ReflectiveAccessorOptimizer extends AbstractOptimizer implements AccessorOptimizer {

    private AccessorNode rootNode;
    private AccessorNode currNode;

    private Object ctx;
    private Object thisRef;
    private Object val;

    private VariableResolverFactory variableFactory;

    private static final int DONE = -1;

    private static final Object[] EMPTYARG = new Object[0];
    private static final Class[] EMPTYCLS = new Class[0];

    private boolean first = true;

    private static final Map<String, Accessor> REFLECTIVE_ACCESSOR_CACHE =
            new WeakHashMap<String, Accessor>();

    private Class returnType;

    public ReflectiveAccessorOptimizer() {
    }

    private ReflectiveAccessorOptimizer(char[] property, Object ctx, Object thisRef, VariableResolverFactory variableFactory) {
        this.expr = property;
        this.length = property != null ? property.length : 0;
        this.ctx = ctx;
        this.variableFactory = variableFactory;
        this.thisRef = thisRef;
    }


    public static Object get(String expression, Object ctx) {
        if (REFLECTIVE_ACCESSOR_CACHE.containsKey(expression)) {
            return REFLECTIVE_ACCESSOR_CACHE.get(expression).getValue(ctx, null, null);
        }
        else {
            Accessor accessor = new ReflectiveAccessorOptimizer().optimizeAccessor(expression.toCharArray(), ctx, null, null, false);
            REFLECTIVE_ACCESSOR_CACHE.put(expression, accessor);
            return accessor.getValue(ctx, null, null);
        }
    }


    public Accessor optimizeAccessor(char[] property, Object ctx, Object thisRef, VariableResolverFactory factory, boolean root) {
        this.rootNode = this.currNode = null;
        this.start = this.cursor = 0;
        this.first = true;

        this.length = (this.expr = property).length;
        this.ctx = ctx;
        this.thisRef = thisRef;
        this.variableFactory = factory;

        return compileGetChain();
    }

    private Accessor compileGetChain() {
        Object curr = ctx;

        try {
            while (cursor < length) {
                switch (nextSubToken()) {
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

                first = false;

                if (curr != null) returnType = curr.getClass();
            }

            val = curr;

            return rootNode;
        }
        catch (InvocationTargetException e) {
            throw new PropertyAccessException("could not access property", e);
        }
        catch (IllegalAccessException e) {
            throw new PropertyAccessException("could not access property", e);
        }
        catch (IndexOutOfBoundsException e) {
            throw new PropertyAccessException("array or collections index out of bounds (property: " + new String(expr) + ")", e);
        }
        catch (PropertyAccessException e) {
            throw new PropertyAccessException("failed to access property: <<" + new String(expr) + ">> in: " + (ctx != null ? ctx.getClass() : null), e);
        }
        catch (CompileException e) {
            throw e;
        }
        catch (NullPointerException e) {
            throw new PropertyAccessException("null pointer exception in property: " + new String(expr), e);
        }
        catch (Exception e) {
            throw new PropertyAccessException("unknown exception in expression: " + new String(expr), e);
        }
    }

    private void addAccessorNode(AccessorNode an) {
        if (rootNode == null)
            rootNode = currNode = an;
        else {
            currNode = currNode.setNextNode(an);
        }
    }


    private Object getBeanProperty(Object ctx, String property)
            throws IllegalAccessException, InvocationTargetException {


        if (first && variableFactory != null && variableFactory.isResolveable(property)) {
            VariableAccessor accessor = new VariableAccessor(property, variableFactory);

            addAccessorNode(accessor);

            return variableFactory.getVariableResolver(property).getValue();
        }

        Class cls = (ctx instanceof Class ? ((Class) ctx) : ctx != null ? ctx.getClass() : null);
        Member member = cls != null ? PropertyTools.getFieldOrAccessor(cls, property) : null;


        if (member instanceof Field) {
            FieldAccessor accessor = new FieldAccessor();
            accessor.setField((Field) member);

            addAccessorNode(accessor);

            return ((Field) member).get(ctx);
        }
        else if (member != null) {
            Object o;

            try {
                o = ((Method) member).invoke(ctx, EMPTYARG);

                GetterAccessor accessor = new GetterAccessor((Method) member);
                addAccessorNode(accessor);
            }
            catch (IllegalAccessException e) {
                Method iFaceMeth = ParseTools.determineActualTargetMethod((Method) member);
                GetterAccessor accessor = new GetterAccessor(iFaceMeth);
                addAccessorNode(accessor);

                o = iFaceMeth.invoke(ctx, EMPTYARG);
            }
            return o;
        }
        else if (ctx instanceof Map && ((Map) ctx).containsKey(property)) {
            MapAccessor accessor = new MapAccessor();
            accessor.setProperty(property);

            addAccessorNode(accessor);

            return ((Map) ctx).get(property);
        }
        else if ("this".equals(property)) {
            ThisValueAccessor accessor = new ThisValueAccessor();

            addAccessorNode(accessor);

            return this.thisRef;
        }
        else if (LITERALS.containsKey(property)) {
            StaticReferenceAccessor accessor = new StaticReferenceAccessor();
            accessor.setLiteral(LITERALS.get(property));

            addAccessorNode(accessor);

            return accessor.getLiteral();
        }
        else {
            Object tryStaticMethodRef = tryStaticAccess();

            if (tryStaticMethodRef != null) {
                if (tryStaticMethodRef instanceof Class) {
                    StaticReferenceAccessor accessor = new StaticReferenceAccessor();
                    accessor.setLiteral(tryStaticMethodRef);
                    addAccessorNode(accessor);
                    return tryStaticMethodRef;
                }
                else {
                    StaticVarAccessor accessor = new StaticVarAccessor((Field) tryStaticMethodRef);
                    addAccessorNode(accessor);
                    return ((Field) tryStaticMethodRef).get(null);
                }

            }
            else if (ctx instanceof Class) {
                Class c = (Class) ctx;
                for (Method m : c.getMethods()) {
                    if (property.equals(m.getName())) {
                        StaticReferenceAccessor accessor = new StaticReferenceAccessor();
                        accessor.setLiteral(m);
                        addAccessorNode(accessor);

                        return m;
                    }
                }
            }


            throw new PropertyAccessException("could not access property ('" + property + "')");
        }
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
        if (prop.length() > 0) ctx = getBeanProperty(ctx, prop);

        int start = ++cursor;

        whiteSpaceSkip();

        if (cursor == length)
            throw new PropertyAccessException("unterminated '['");

        String item;


        if (!scanTo(']'))
            throw new PropertyAccessException("unterminated '['");

        item = new String(expr, start, cursor - start);

        boolean itemSubExpr = true;

        Object idx = null;

        try {
            idx = parseInt(item);
            itemSubExpr = false;
        }
        catch (Exception e) {
            // not a number;
        }

        ExecutableStatement itemStmt = null;
        if (itemSubExpr) {
            itemStmt = (ExecutableStatement) MVEL.compileExpression(item);
            idx = itemStmt.getValue(ctx, thisRef, variableFactory);
        }

        ++cursor;

        if (ctx instanceof Map) {
            if (itemSubExpr) {
                MapAccessorNest accessor = new MapAccessorNest();
                accessor.setProperty(itemStmt);
                addAccessorNode(accessor);
            }
            else {
                MapAccessor accessor = new MapAccessor();
                accessor.setProperty(parseInt(item));
                addAccessorNode(accessor);
            }

            return ((Map) ctx).get(idx);
        }
        else if (ctx instanceof List) {
            if (itemSubExpr) {
                ListAccessorNest accessor = new ListAccessorNest();
                accessor.setIndex(itemStmt);
                addAccessorNode(accessor);
            }
            else {
                ListAccessor accessor = new ListAccessor();
                accessor.setIndex(parseInt(item));
                addAccessorNode(accessor);
            }

            return ((List) ctx).get((Integer) idx);
        }
//        else if (ctx instanceof Collection) {
//            int count = parseInt(item);
//            if (count > ((Collection) ctx).size())
//                throw new PropertyAccessException("index [" + count + "] out of bounds on collections");
//
//            Iterator iter = ((Collection) ctx).iterator();
//            for (int i = 0; i < count; i++) iter.next();
//            return iter.next();
//        }
        else if (ctx instanceof Object[]) {
            if (itemSubExpr) {
                ArrayAccessorNest accessor = new ArrayAccessorNest();
                accessor.setIndex(itemStmt);
                addAccessorNode(accessor);
            }
            else {
                ArrayAccessor accessor = new ArrayAccessor();
                accessor.setIndex(parseInt(item));
                addAccessorNode(accessor);
            }

            return ((Object[]) ctx)[(Integer) idx];
        }
        else if (ctx instanceof CharSequence) {
            if (itemSubExpr) {
                IndexedCharSeqAccessorNest accessor = new IndexedCharSeqAccessorNest();
                accessor.setIndex(itemStmt);
                addAccessorNode(accessor);
            }
            else {
                IndexedCharSeqAccessor accessor = new IndexedCharSeqAccessor();
                accessor.setIndex(parseInt(item));
                addAccessorNode(accessor);
            }

            return ((CharSequence) ctx).charAt((Integer) idx);
        }
        else {
            throw new PropertyAccessException("illegal use of []: unknown type: " + (ctx == null ? null : ctx.getClass().getName()));
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
    private Object getMethod(Object ctx, String name) throws Exception {
        if (first && variableFactory.isResolveable(name)) {
            Method m = (Method) variableFactory.getVariableResolver(name).getValue();
            ctx = m.getDeclaringClass();
            name = m.getName();
            first = false;
        }


        int st = cursor;

        int depth = 1;

        while (cursor++ < length - 1 && depth != 0) {
            switch (expr[cursor]) {
                case'(':
                    depth++;
                    continue;
                case')':
                    depth--;

            }
        }
        cursor--;

        String tk = (cursor - st) > 1 ? new String(expr, st + 1, cursor - st - 1) : "";

        cursor++;

        Object[] args;
        ExecutableStatement[] es;

        if (tk.length() == 0) {
            args = ParseTools.EMPTY_OBJ_ARR;
            es = null;
        }
        else {
            String[] subtokens = parseParameterList(tk.toCharArray(), 0, -1);
            es = new ExecutableStatement[subtokens.length];
            args = new Object[subtokens.length];
            for (int i = 0; i < subtokens.length; i++) {
                args[i] = (es[i] = (ExecutableStatement) MVEL.compileExpression(subtokens[i])).getValue(this.ctx, variableFactory);
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

        if ((m = ParseTools.getBestCandidate(args, name, cls.getMethods())) != null) {
            parameterTypes = m.getParameterTypes();
        }

        if (m == null) {
            /**
             * If we didn't find anything, maybe we're looking for the actual java.lang.Class methods.
             */
            if ((m = ParseTools.getBestCandidate(args, name, cls.getClass().getDeclaredMethods())) != null) {
                parameterTypes = m.getParameterTypes();
            }
        }


        if (m == null) {
            StringAppender errorBuild = new StringAppender();
            for (int i = 0; i < args.length; i++) {
                errorBuild.append(args[i] != null ? args[i].getClass().getName() : null);
                if (i < args.length - 1) errorBuild.append(", ");
            }

            if ("size".equals(name) && args.length == 0 && cls.isArray()) {
                addAccessorNode(new ArrayLength());
                return Array.getLength(ctx);
            }

            throw new PropertyAccessException("unable to resolve method: " + cls.getName() + "." + name + "(" + errorBuild.toString() + ") [arglength=" + args.length + "]");
        }
        else {
            if (es != null) {
                ExecutableStatement cExpr;
                for (int i = 0; i < es.length; i++) {
                    cExpr = es[i];
                    if (cExpr.getKnownIngressType() == null) {
                        cExpr.setKnownIngressType(parameterTypes[i]);
                        cExpr.computeTypeConversionRule();
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
            access.setMethod(ParseTools.getWidenedTarget(m));
            access.setParms(es);

            addAccessorNode(access);

            /**
             * Invoke the target method and return the response.
             */
            return m.invoke(ctx, args);
        }
    }


    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) throws Exception {
        return rootNode.getValue(ctx, elCtx, variableFactory);
    }


    public static void main(String[] args) {
        new ReflectiveAccessorOptimizer().optimizeCollection("[test, foo, bar, {1,2,3}]".toCharArray(), null, null, null);
    }


    private Accessor _getAccessor(Object o) {
        if (o instanceof List) {
            Accessor[] a = new Accessor[((List) o).size()];
            int i = 0;

            for (Object item : (List) o) {
                a[i++] = _getAccessor(item);
            }

            return new ListCreator(a);
        }
        else if (o instanceof Map) {
            Accessor[] k = new Accessor[((Map) o).size()];
            Accessor[] v = new Accessor[k.length];
            int i = 0;

            for (Object item : ((Map) o).keySet()) {
                k[i] = _getAccessor(item); // key
                v[i++] = _getAccessor(((Map) o).get(item)); // value
            }

            return new MapCreator(k, v);
        }
        else if (o instanceof Object[]) {
            Accessor[] a = new Accessor[((Object[]) o).length];
            int i = 0;

            for (Object item : (Object[]) o) {
                a[i++] = _getAccessor(item); // item
            }

            return new ArrayCreator(a);
        }
        else {
            return new ExprValueAccessor((String) o);
        }

    }

    public Accessor optimizeCollection(char[] property, Object ctx, Object thisRef, VariableResolverFactory factory) {
        this.ctx = ctx;
        this.thisRef = thisRef;
        this.variableFactory = factory;

        CollectionParser parser = new CollectionParser();
        Object o = ((List) parser.parseCollection(property)).get(0);


        Accessor root = _getAccessor(o);
        int end = parser.getEnd() + 2;

        if (end < property.length) {
            return new Union(root, subset(property, end));
        }
        else {
            return root;
        }
    }


    public Accessor optimizeObjectCreation(char[] property, Object ctx, Object thisRef, VariableResolverFactory factory) {
        this.length = (this.expr = property).length;
        this.cursor = 0;
        try {
            Accessor contructor = compileConstructor(property, ctx, factory);
            val = contructor.getValue(property, thisRef, factory);
            return contructor;
        }
        catch (Exception e) {
            throw new CompileException("could not create constructor", e);
        }
    }


    public Accessor optimizeFold(char[] property, Object ctx, Object thisRef, VariableResolverFactory factory) {
        this.length = (this.expr = property).length;
        this.cursor = 0;
        greedy = false; // don't be greedy!

        if (expr[cursor] == '(') {
            balancedCapture('(');
            length = cursor;
            cursor = 1;
        }

        ASTNode var = nextToken();

        if (!nextToken().isOperator(Operator.PROJECTION)) {
            throw new CompileException("expected fold operator");
        }

        greedy = true;

        Fold fold = new Fold(var.getNameAsArray(), new ExprValueAccessor(nextToken().getName()));

        if (length < property.length - 1) {
            cursor += 2;
            Accessor union = new Union(fold, subset(property, cursor));
            val = union.getValue(ctx, thisRef, factory);
            return union;
        }
        else {
            val = fold.getValue(ctx, thisRef, factory);
            return fold;
        }
    }

    private void setRootNode(AccessorNode rootNode) {
        this.rootNode = this.currNode = rootNode;
    }

    private AccessorNode getRootNode() {
        return rootNode;
    }


    public Object getResultOptPass() {
        return val;
    }

    @SuppressWarnings({"WeakerAccess"})
    public static AccessorNode compileConstructor(char[] expression, Object ctx, VariableResolverFactory vars) throws
            InstantiationException, IllegalAccessException, InvocationTargetException,
            ClassNotFoundException, NoSuchMethodException {


        String[] cnsRes = captureContructorAndResidual(expression);

        String[] constructorParms = parseMethodOrConstructor(cnsRes[0].toCharArray());

        if (constructorParms != null) {
            String s = new String(subset(expression, 0, ArrayTools.findFirst('(', expression)));
            Class cls = ParseTools.findClass(vars, s);

            ExecutableStatement[] cStmts = new ExecutableStatement[constructorParms.length];

            for (int i = 0; i < constructorParms.length; i++) {
                cStmts[i] = (ExecutableStatement) MVEL.compileExpression(constructorParms[i]);
            }

            Object[] parms = new Object[constructorParms.length];
            for (int i = 0; i < constructorParms.length; i++) {
                parms[i] = cStmts[i].getValue(ctx, vars);
            }

            Constructor cns = getBestConstructorCanadidate(parms, cls);

            if (cns == null)
                throw new CompileException("unable to find constructor for: " + cls.getName());

            for (int i = 0; i < parms.length; i++) {
                //noinspection unchecked
                parms[i] = DataConversion.convert(parms[i], cns.getParameterTypes()[i]);
            }

            AccessorNode ca = new ConstructorAccessor(cns, cStmts);

            if (cnsRes.length > 1) {
                ReflectiveAccessorOptimizer compiledOptimizer
                        = new ReflectiveAccessorOptimizer(cnsRes[1].toCharArray(), cns.newInstance(parms), ctx, vars);
                compiledOptimizer.setRootNode(ca);
                compiledOptimizer.compileGetChain();
                ca = compiledOptimizer.getRootNode();
            }

            return ca;
        }
        else {
            Constructor cns = Class.forName(new String(expression)).getConstructor(EMPTYCLS);
            AccessorNode ca = new ConstructorAccessor(cns, null);

            if (cnsRes.length > 1) {
                ReflectiveAccessorOptimizer compiledOptimizer
                        = new ReflectiveAccessorOptimizer(cnsRes[1].toCharArray(), cns.newInstance(null), ctx, vars);
                compiledOptimizer.setRootNode(ca);
                compiledOptimizer.compileGetChain();
                ca = compiledOptimizer.getRootNode();
            }

            return ca;
        }
    }

    public Accessor optimizeReturn(char[] property, Object ctx, Object thisRef, VariableResolverFactory factory) {
        ExecutableStatement stmt = (ExecutableStatement) MVEL.compileExpression(property);
        Return ret = new Return(stmt);
        val = stmt.getValue(ctx, thisRef, factory);

        return ret;
    }

    public Class getEgressType() {
        return returnType;
    }

}
