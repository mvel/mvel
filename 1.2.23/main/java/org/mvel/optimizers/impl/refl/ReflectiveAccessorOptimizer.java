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
import static org.mvel.DataConversion.canConvert;
import static org.mvel.MVEL.eval;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.AbstractOptimizer;
import org.mvel.optimizers.AccessorOptimizer;
import org.mvel.optimizers.impl.refl.collection.ArrayCreator;
import org.mvel.optimizers.impl.refl.collection.ExprValueAccessor;
import org.mvel.optimizers.impl.refl.collection.ListCreator;
import org.mvel.optimizers.impl.refl.collection.MapCreator;
import org.mvel.util.*;
import static org.mvel.util.ParseTools.*;
import static org.mvel.util.PropertyTools.getBaseComponentType;
import static org.mvel.util.PropertyTools.getFieldOrWriteAccessor;

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
    private boolean literal = false;

    private static final Map<Integer, Accessor> REFLECTIVE_ACCESSOR_CACHE =
            new WeakHashMap<Integer, Accessor>();

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


    private static int createSignatureHash(String expr, Object ctx) {
        if (ctx == null) {
            return expr.hashCode();
        }
        else {
            return expr.hashCode() + ctx.getClass().hashCode();
        }
    }

    public static Object get(String expression, Object ctx) {
        int hash = createSignatureHash(expression, ctx);
        Accessor accessor = REFLECTIVE_ACCESSOR_CACHE.get(hash);
        if (accessor != null) {
            return accessor.getValue(ctx, null, null);
        }
        else {
            accessor = new ReflectiveAccessorOptimizer().optimizeAccessor(expression.toCharArray(), ctx, null, null, false);
            REFLECTIVE_ACCESSOR_CACHE.put(hash, accessor);
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


    public SetAccessor optimizeSetAccessor(char[] property, Object ctx, Object thisRef, VariableResolverFactory factory, boolean rootThisRef, Object value) {
        this.rootNode = this.currNode = null;
        this.start = this.cursor = 0;
        this.first = true;

        this.length = (this.expr = property).length;
        this.ctx = ctx;
        this.thisRef = thisRef;
        this.variableFactory = factory;

        char[] root = null;
        boolean col = false;

        int split = -1;
        for (int i = property.length - 1; i != 0; i--) {
            switch (property[i]) {
                case '[':
                    split = i;
                    col = true;
                    break;
                case '.':
                    split = i;
                    break;
            }
            if (split != -1) break;
        }

        if (split != -1) {
            root = subset(property, 0, split++);
            property = subset(property, split, property.length - split);
        }

        Accessor rootAccessor = null;

        if (root != null) {
            this.length = (this.expr = root).length;

            rootAccessor = compileGetChain();
            ctx = this.val;
        }

        try {
            this.length = (this.expr = property).length;
            this.cursor = this.start = 0;

            whiteSpaceSkip();

            if (col) {
                int start = cursor;
                whiteSpaceSkip();

                if (cursor == length)
                    throw new PropertyAccessException("unterminated '['");

                if (!scanTo(']'))
                    throw new PropertyAccessException("unterminated '['");

                String ex = new String(property, start, cursor - start);

                if (ctx instanceof Map) {
                    //noinspection unchecked
                    ((Map) ctx).put(eval(ex, ctx, variableFactory), value);
                    return new SetAccessor(rootAccessor, new MapAccessorNest(ex));
                }
                else if (ctx instanceof List) {
                    //noinspection unchecked
                    ((List) ctx).set(eval(ex, ctx, variableFactory, Integer.class), value);
                    return new SetAccessor(rootAccessor, new ListAccessorNest(ex));
                }
                else if (ctx.getClass().isArray()) {
                    //noinspection unchecked
                    Array.set(ctx, eval(ex, ctx, variableFactory, Integer.class), DataConversion.convert(value, getBaseComponentType(ctx.getClass())));
                    return new SetAccessor(rootAccessor, new ArrayAccessorNest(ex));
                }
                else {
                    throw new PropertyAccessException("cannot bind to collection property: " + new String(property) + ": not a recognized collection type: " + ctx.getClass());
                }
            }

            String tk = new String(property);

            Member member = getFieldOrWriteAccessor(ctx.getClass(), tk);

            if (member instanceof Field) {
                Field fld = (Field) member;

                if (value != null && !fld.getType().isAssignableFrom(value.getClass())) {
                    if (!canConvert(fld.getType(), value.getClass())) {
                        throw new ConversionException("cannot convert type: "
                                + value.getClass() + ": to " + fld.getType());
                    }

                    fld.set(ctx, DataConversion.convert(value, fld.getType()));
                    return new SetAccessor(rootAccessor, new DynamicFieldAccessor(fld));
                }
                else {
                    fld.set(ctx, value);
                    return new SetAccessor(rootAccessor, new FieldAccessor(fld));
                }
            }
            else if (member != null) {
                Method meth = (Method) member;

                if (value != null && !meth.getParameterTypes()[0].isAssignableFrom(value.getClass())) {
                    if (!canConvert(meth.getParameterTypes()[0], value.getClass())) {
                        throw new ConversionException("cannot convert type: "
                                + value.getClass() + ": to " + meth.getParameterTypes()[0]);
                    }

                    meth.invoke(ctx, DataConversion.convert(value, meth.getParameterTypes()[0]));
                }
                else {
                    meth.invoke(ctx, value);
                }

                return new SetAccessor(rootAccessor, new SetterAccessor(meth));

            }
            else if (ctx instanceof Map) {
                //noinspection unchecked
                ((Map) ctx).put(tk, value);
                return new SetAccessor(rootAccessor, new MapAccessor(tk));

            }
            else {
                throw new PropertyAccessException("could not access property (" + tk + ") in: " + ctx.getClass().getName());
            }
        }
        catch (InvocationTargetException e) {
            throw new PropertyAccessException("could not access property", e);
        }
        catch (IllegalAccessException e) {
            throw new PropertyAccessException("could not access property", e);
        }

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
            throw new PropertyAccessException(new String(expr), e);
        }
        catch (IllegalAccessException e) {
            throw new PropertyAccessException(new String(expr), e);
        }
        catch (IndexOutOfBoundsException e) {
            throw new PropertyAccessException(new String(expr), e);
        }
        catch (PropertyAccessException e) {
            throw new CompileException(e.getMessage(), e);
        }
        catch (CompileException e) {
            throw e;
        }
        catch (NullPointerException e) {
            throw new PropertyAccessException(new String(expr), e);
        }
        catch (Exception e) {
            throw new CompileException(e.getMessage(), e);
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

        if (first) {
            if ("this".equals(property)) {
                addAccessorNode(new ThisValueAccessor());
                return this.thisRef;
            }
            else if (variableFactory != null && variableFactory.isResolveable(property)) {
                addAccessorNode(new VariableAccessor(property, variableFactory));
                return variableFactory.getVariableResolver(property).getValue();
            }
        }

        //noinspection unchecked
        Class<? extends Object> cls = (ctx instanceof Class ? ((Class<? extends Object>) ctx) : ctx != null ? ctx.getClass() : null);
        Member member = cls != null ? PropertyTools.getFieldOrAccessor(cls, property) : null;

        if (member instanceof Field) {
            addAccessorNode(new FieldAccessor((Field) member));
            return ((Field) member).get(ctx);
        }
        else if (member != null) {
            Object o;

            try {
                o = ((Method) member).invoke(ctx, EMPTYARG);
                addAccessorNode(new GetterAccessor((Method) member));
            }
            catch (IllegalAccessException e) {
                Method iFaceMeth = ParseTools.determineActualTargetMethod((Method) member);
                addAccessorNode(new GetterAccessor(iFaceMeth));
                o = iFaceMeth.invoke(ctx, EMPTYARG);
            }
            return o;
        }
        else if (ctx instanceof Map && ((Map) ctx).containsKey(property)) {
            addAccessorNode(new MapAccessor(property));
            return ((Map) ctx).get(property);
        }

        else if ("length".equals(property) && ctx.getClass().isArray()) {
            addAccessorNode(new ArrayLength());
            return Array.getLength(ctx);
        }
        else if (LITERALS.containsKey(property)) {
            addAccessorNode(new StaticReferenceAccessor(ctx = LITERALS.get(property)));
            return ctx;
        }
        else {
            Object tryStaticMethodRef = tryStaticAccess();

            if (tryStaticMethodRef != null) {
                if (tryStaticMethodRef instanceof Class) {
                    addAccessorNode(new StaticReferenceAccessor(tryStaticMethodRef));
                    return tryStaticMethodRef;
                }
                else {
                    addAccessorNode(new StaticVarAccessor((Field) tryStaticMethodRef));
                    return ((Field) tryStaticMethodRef).get(null);
                }

            }
            else if (ctx instanceof Class) {
                Class c = (Class) ctx;
                for (Method m : c.getMethods()) {
                    if (property.equals(m.getName())) {
                        addAccessorNode(new StaticReferenceAccessor(m));
                        return m;
                    }
                }
            }

            throw new PropertyAccessException(property);
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
            throw new CompileException("unterminated '['");

        String item;

        if (!scanTo(']'))
            throw new CompileException("unterminated '['");

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
            idx = (itemStmt = (ExecutableStatement) subCompileExpression(item)).getValue(ctx, thisRef, variableFactory);
        }

        ++cursor;

        if (ctx instanceof Map) {
            if (itemSubExpr) {
                addAccessorNode(new MapAccessorNest(itemStmt));
            }
            else {
                addAccessorNode(new MapAccessor(parseInt(item)));
            }

            return ((Map) ctx).get(idx);
        }
        else if (ctx instanceof List) {
            if (itemSubExpr) {
                addAccessorNode(new ListAccessorNest(itemStmt));
            }
            else {
                addAccessorNode(new ListAccessor(parseInt(item)));
            }

            return ((List) ctx).get((Integer) idx);
        }
        else if (ctx instanceof Object[]) {
            if (itemSubExpr) {
                addAccessorNode(new ArrayAccessorNest(itemStmt));
            }
            else {
                addAccessorNode(new ArrayAccessor(parseInt(item)));
            }

            return ((Object[]) ctx)[(Integer) idx];
        }
        else if (ctx instanceof CharSequence) {
            if (itemSubExpr) {
                addAccessorNode(new IndexedCharSeqAccessorNest(itemStmt));
            }
            else {
                addAccessorNode(new IndexedCharSeqAccessor(parseInt(item)));
            }

            return ((CharSequence) ctx).charAt((Integer) idx);
        }
        else {
            throw new CompileException("illegal use of []: unknown type: " + (ctx == null ? null : ctx.getClass().getName()));
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
            else {
                throw new OptimizationFailure("attempt to optimize a method call for a reference that does not point to a method: "
                        + name + " (reference is type: " + (ctx != null ? ctx.getClass().getName() : null) + ")");
            }

            first = false;
        }

        int st = cursor;

        String tk = ((cursor = ParseTools.balancedCapture(expr, cursor, '(')) - st) > 1 ? new String(expr, st + 1, cursor - st - 1) : "";

        cursor++;

        Object[] args;
        Accessor[] es;

        if (tk.length() == 0) {
            args = ParseTools.EMPTY_OBJ_ARR;
            es = null;
        }
        else {
            String[] subtokens = parseParameterList(tk.toCharArray(), 0, -1);
            es = new ExecutableStatement[subtokens.length];
            args = new Object[subtokens.length];
            for (int i = 0; i < subtokens.length; i++) {
                args[i] = (es[i] = (ExecutableStatement) subCompileExpression(subtokens[i])).getValue(this.ctx, thisRef, variableFactory);
            }
        }

        /**
         * If the target object is an instance of java.lang.Class itself then do not
         * adjust the Class scope target.
         */
        Class<? extends Object> cls = ctx instanceof Class ? (Class<? extends Object>) ctx : ctx.getClass();

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
                    cExpr = (ExecutableStatement) es[i];
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


            addAccessorNode(new MethodAccessor(ParseTools.getWidenedTarget(m), (ExecutableStatement[]) es));

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

            returnType = List.class;

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

            returnType = Map.class;

            return new MapCreator(k, v);
        }
        else if (o instanceof Object[]) {
            Accessor[] a = new Accessor[((Object[]) o).length];
            int i = 0;

            for (Object item : (Object[]) o) {
                a[i++] = _getAccessor(item); // item
            }

            returnType = Object[].class;

            return new ArrayCreator(a);
        }
        else {

            returnType = Object.class;

            return new ExprValueAccessor((String) o);
        }

    }

    public Accessor optimizeCollection(char[] property, Object ctx, Object thisRef, VariableResolverFactory factory) {
        CollectionParser parser = new CollectionParser();
        ctx = ((List) parser.parseCollection(property)).get(0);

        Accessor root = _getAccessor(ctx);
        int end = parser.getCursor() + 2;

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
            //   Accessor contructor = compileConstructor(property, ctx, factory);
            //       val = contructor.getValue(property, thisRef, factory);
            return compileConstructor(property, ctx, factory);
        }
        catch (CompileException e) {
            throw e;
        }
        catch (Exception e) {
            throw new CompileException("could not create constructor: " + e.getMessage(), e);
        }
    }


    public Accessor optimizeFold(char[] property, Object ctx, Object thisRef, VariableResolverFactory factory) {
        this.length = (this.expr = property).length;
        this.cursor = 0;
        greedy = false; // don't be greedy!

        if (expr[cursor] == '(') {
            //balancedCapture('(');
            length = cursor = balancedCapture(expr, cursor, '(');
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
    public AccessorNode compileConstructor(char[] expression, Object ctx, VariableResolverFactory vars) throws
            InstantiationException, IllegalAccessException, InvocationTargetException,
            ClassNotFoundException, NoSuchMethodException {

        String[] cnsRes = captureContructorAndResidual(expression);

        String[] constructorParms = parseMethodOrConstructor(cnsRes[0].toCharArray());

        if (constructorParms != null) {
            String s = new String(subset(expression, 0, ArrayTools.findFirst('(', expression)));
            Class cls = ParseTools.findClass(vars, s);

            ExecutableStatement[] cStmts = new ExecutableStatement[constructorParms.length];

            for (int i = 0; i < constructorParms.length; i++) {
                cStmts[i] = (ExecutableStatement) subCompileExpression(constructorParms[i]);
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

                this.val = compiledOptimizer.getResultOptPass();
            }

            return ca;
        }
        else {
            Constructor<?> cns = Thread.currentThread().getContextClassLoader().loadClass(new String(expression)).getConstructor(EMPTYCLS);
            AccessorNode ca = new ConstructorAccessor(cns, null);

            if (cnsRes.length > 1) {
                //noinspection NullArgumentToVariableArgMethod
                ReflectiveAccessorOptimizer compiledOptimizer
                        = new ReflectiveAccessorOptimizer(cnsRes[1].toCharArray(), cns.newInstance(null), ctx, vars);
                compiledOptimizer.setRootNode(ca);
                compiledOptimizer.compileGetChain();
                ca = compiledOptimizer.getRootNode();

                this.val = compiledOptimizer.getResultOptPass();
            }

            return ca;
        }
    }


    public Class getEgressType() {
        return returnType;
    }

    public boolean isLiteralOnly() {
        return literal;
    }
}
