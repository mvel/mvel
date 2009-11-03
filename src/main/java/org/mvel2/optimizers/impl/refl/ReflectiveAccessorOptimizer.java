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
package org.mvel2.optimizers.impl.refl;

import org.mvel2.*;
import static org.mvel2.DataConversion.canConvert;
import static org.mvel2.DataConversion.convert;
import static org.mvel2.MVEL.eval;
import org.mvel2.ast.Function;
import org.mvel2.ast.TypeDescriptor;
import static org.mvel2.ast.TypeDescriptor.getClassReference;
import org.mvel2.compiler.Accessor;
import org.mvel2.compiler.AccessorNode;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.compiler.PropertyVerifier;
import org.mvel2.integration.GlobalListenerFactory;
import static org.mvel2.integration.GlobalListenerFactory.*;
import org.mvel2.integration.PropertyHandler;
import static org.mvel2.integration.PropertyHandlerFactory.*;
import org.mvel2.integration.VariableResolver;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.optimizers.AbstractOptimizer;
import org.mvel2.optimizers.AccessorOptimizer;
import org.mvel2.optimizers.impl.refl.collection.ArrayCreator;
import org.mvel2.optimizers.impl.refl.collection.ExprValueAccessor;
import org.mvel2.optimizers.impl.refl.collection.ListCreator;
import org.mvel2.optimizers.impl.refl.collection.MapCreator;
import org.mvel2.optimizers.impl.refl.nodes.*;
import org.mvel2.util.ArrayTools;
import static org.mvel2.util.CompilerTools.expectType;
import org.mvel2.util.MethodStub;
import org.mvel2.util.ParseTools;
import static org.mvel2.util.ParseTools.*;
import static org.mvel2.util.PropertyTools.getFieldOrAccessor;
import static org.mvel2.util.PropertyTools.getFieldOrWriteAccessor;
import org.mvel2.util.StringAppender;

import static java.lang.Integer.parseInt;
import java.lang.reflect.*;
import static java.lang.reflect.Array.getLength;
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

    private Class ingressType;
    private Class returnType;

    public ReflectiveAccessorOptimizer() {
    }

    public void init() {
    }

    private ReflectiveAccessorOptimizer(ParserContext pCtx, char[] property, Object ctx, Object thisRef,
                                        VariableResolverFactory variableFactory) {
        this.pCtx = pCtx;
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
            REFLECTIVE_ACCESSOR_CACHE.put(hash, accessor = new ReflectiveAccessorOptimizer()
                    .optimizeAccessor(getCurrentThreadParserContext(),
                            expression.toCharArray(), ctx, null, null, false, null));
            return accessor.getValue(ctx, null, null);
        }
    }

    public Accessor optimizeAccessor(ParserContext pCtx, char[] property, Object ctx, Object thisRef,
                                     VariableResolverFactory factory, boolean root, Class ingressType) {
        this.rootNode = this.currNode = null;
        this.start = this.cursor = 0;
        this.first = true;

        this.length = (this.expr = property).length;
        this.ctx = ctx;
        this.thisRef = thisRef;
        this.variableFactory = factory;
        this.ingressType = ingressType;

        this.pCtx = pCtx;

        return compileGetChain();
    }

    public Accessor optimizeSetAccessor(ParserContext pCtx, char[] property, Object ctx, Object thisRef,
                                        VariableResolverFactory factory, boolean rootThisRef, Object value, Class ingressType) {
        this.rootNode = this.currNode = null;
        this.start = this.cursor = 0;
        this.first = true;

        this.length = (this.expr = property).length;
        this.ctx = ctx;
        this.thisRef = thisRef;
        this.variableFactory = factory;
        this.ingressType = ingressType;

        char[] root = null;

        int split = findLastUnion();

        PropertyVerifier verifier = new PropertyVerifier(property, this.pCtx = pCtx);

        if (split != -1) {
            root = subset(property, 0, split++);
            //todo: must use the property verifier.
            property = subset(property, split, property.length - split);
        }

        if (root != null) {
            this.length = (this.expr = root).length;

            compileGetChain();
            ctx = this.val;
        }

        try {
            this.length = (this.expr = property).length;
            this.cursor = this.start = 0;

            skipWhitespace();

            if (collection) {
                int start = cursor;

                if (cursor == length)
                    throw new PropertyAccessException("unterminated '['");

                if (scanTo(']'))
                    throw new PropertyAccessException("unterminated '['");

                String ex = new String(property, start, cursor - start);

                if (ctx instanceof Map) {
                    if (MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING && hasPropertyHandler(Map.class)) {
                        propHandlerSet(ex, ctx, Map.class, value);
                    }
                    else {
                        //noinspection unchecked
                        ((Map) ctx).put(eval(ex, ctx, variableFactory), convert(value, returnType = verifier.analyze()));

                        addAccessorNode(new MapAccessorNest(ex, returnType));
                    }

                    return rootNode;
                }
                else if (ctx instanceof List) {
                    if (MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING && hasPropertyHandler(List.class)) {
                        propHandlerSet(ex, ctx, List.class, value);
                    }
                    else {
                        //noinspection unchecked
                        ((List) ctx).set(eval(ex, ctx, variableFactory, Integer.class), convert(value, returnType = verifier.analyze()));

                        addAccessorNode(new ListAccessorNest(ex, returnType));
                    }

                    return rootNode;
                }
                else if (MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING && hasPropertyHandler(ctx.getClass())) {
                    propHandlerSet(ex, ctx, ctx.getClass(), value);
                    return rootNode;
                }
                else if (ctx.getClass().isArray()) {
                    if (MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING && hasPropertyHandler(Array.class)) {
                        propHandlerSet(ex, ctx, Array.class, value);
                    }
                    else {
                        //noinspection unchecked
                        Array.set(ctx, eval(ex, ctx, variableFactory, Integer.class), convert(value, getBaseComponentType(ctx.getClass())));
                        addAccessorNode(new ArrayAccessorNest(ex));
                    }
                    return rootNode;
                }
                else {
                    throw new PropertyAccessException("cannot bind to collection property: " + new String(property) +
                            ": not a recognized collection type: " + ctx.getClass());
                }
            }
            else if (MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING && hasPropertyHandler(ctx.getClass())) {
                propHandlerSet(new String(property), ctx, ctx.getClass(), value);
                return rootNode;
            }

            String tk = new String(property);

            if (hasSetListeners()) {
                notifySetListeners(ctx, tk, variableFactory, value);
                addAccessorNode(new Notify(tk));
            }

            Member member = getFieldOrWriteAccessor(ctx.getClass(), tk, value == null ? null : ingressType);

            if (member instanceof Field) {
                Field fld = (Field) member;

                if (value != null && !fld.getType().isAssignableFrom(value.getClass())) {
                    if (!canConvert(fld.getType(), value.getClass())) {
                        throw new ConversionException("cannot convert type: "
                                + value.getClass() + ": to " + fld.getType());
                    }

                    fld.set(ctx, convert(value, fld.getType()));
                    addAccessorNode(new DynamicFieldAccessor(fld));
                }
                else {
                    fld.set(ctx, value);
                    addAccessorNode(new FieldAccessor(fld));
                }
            }
            else if (member != null) {
                Method meth = (Method) member;

                if (value != null && !meth.getParameterTypes()[0].isAssignableFrom(value.getClass())) {
                    if (!canConvert(meth.getParameterTypes()[0], value.getClass())) {
                        throw new ConversionException("cannot convert type: "
                                + value.getClass() + ": to " + meth.getParameterTypes()[0]);
                    }

                    meth.invoke(ctx, convert(value, meth.getParameterTypes()[0]));
                }
                else {
                    meth.invoke(ctx, value);
                }

                addAccessorNode(new SetterAccessor(meth));
            }
            else if (ctx instanceof Map) {
                //noinspection unchecked
                ((Map) ctx).put(tk, value);

                addAccessorNode(new MapAccessor(tk));
            }
            else {
                throw new PropertyAccessException("could not access property (" + tk + ") in: " + ingressType.getName());
            }
        }
        catch (InvocationTargetException e) {
            throw new PropertyAccessException("could not access property", e);
        }
        catch (IllegalAccessException e) {
            throw new PropertyAccessException("could not access property", e);
        }


        return rootNode;
    }

    private Accessor compileGetChain() {
        Object curr = ctx;

        try {
            if (!MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING) {
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
                        case WITH:
                            curr = getWithProperty(curr);
                            break;
                        case DONE:
                            break;
                    }

                    first = false;
                    if (curr != null) returnType = curr.getClass();
                    if (nullSafe && cursor < length) {
                        if (curr == null) return null;
                        addAccessorNode(new NullSafe());
                    }
                }

            }
            else {
                while (cursor < length) {
                    switch (nextSubToken()) {
                        case BEAN:
                            curr = getBeanPropertyAO(curr, capture());
                            break;
                        case METH:
                            curr = getMethod(curr, capture());
                            break;
                        case COL:
                            curr = getCollectionPropertyAO(curr, capture());
                            break;
                        case WITH:
                            curr = getWithProperty(curr);
                            break;
                        case DONE:
                            break;
                    }

                    first = false;
                    if (curr != null) returnType = curr.getClass();
                    if (nullSafe && cursor < length) {
                        if (curr == null) return null;
                        addAccessorNode(new NullSafe());
                    }
                }
            }

            val = curr;

            if (pCtx.isStrictTypeEnforcement()) {
                this.returnType = new PropertyVerifier(this.expr, pCtx).analyze();
            }

            return rootNode;
        }
        catch (InvocationTargetException e) {
            throw new PropertyAccessException(new String(expr) + ": " + e.getMessage(), e);
        }
        catch (IllegalAccessException e) {
            throw new PropertyAccessException(new String(expr) + ": " + e.getMessage(), e);
        }
        catch (IndexOutOfBoundsException e) {
            throw new PropertyAccessException(new String(expr) + ": array index out of bounds.", e);
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

    private Object getWithProperty(Object ctx) {
        String root = new String(expr, 0, cursor - 1).trim();

        int start = cursor + 1;
        cursor = balancedCaptureWithLineAccounting(expr, cursor, '{', pCtx);

        WithAccessor wa = new WithAccessor(root, subset(expr, start, cursor++ - start), ingressType, false);

        addAccessorNode(wa);

        return wa.getValue(ctx, thisRef, variableFactory);
    }

    private Object getBeanPropertyAO(Object ctx, String property)
            throws Exception {
        if (ctx != null && hasPropertyHandler(ctx.getClass())) return propHandler(property, ctx, ctx.getClass());

        if (GlobalListenerFactory.hasGetListeners()) {
            notifyGetListeners(ctx, property, variableFactory);
            addAccessorNode(new Notify(property));
        }

        return getBeanProperty(ctx, property);
    }

    private Object getBeanProperty(Object ctx, String property) throws Exception {
        if ((currType = !first || pCtx == null ? null : pCtx.getVarOrInputTypeOrNull(property)) == Object.class
                && !pCtx.isStrongTyping()) {
            currType = null;
        } 

        if (first) {
            if ("this".equals(property)) {
                addAccessorNode(new ThisValueAccessor());
                return this.thisRef;
            }
            else if (variableFactory != null && variableFactory.isResolveable(property)) {


                if (variableFactory.isIndexedFactory() && variableFactory.isTarget(property)) {
                    int idx;
                    addAccessorNode(new IndexedVariableAccessor(idx = variableFactory.variableIndexOf(property)));

                    VariableResolver vr = variableFactory.getIndexedVariableResolver(idx);
                    if (vr == null) {
                        variableFactory.setIndexedVariableResolver(idx, variableFactory.getVariableResolver(property));
                    }

                    return variableFactory.getIndexedVariableResolver(idx).getValue();
                }
                else {
                    addAccessorNode(new VariableAccessor(property));

                    return variableFactory.getVariableResolver(property).getValue();
                }
            }
        }

        //noinspection unchecked
        Class<?> cls = (ctx instanceof Class ? ((Class<?>) ctx) : ctx != null ? ctx.getClass() : null);

        if (hasPropertyHandler(cls)) {
            PropertyHandlerAccessor acc = new PropertyHandlerAccessor(property, getPropertyHandler(cls));
            addAccessorNode(acc);
            return acc.getValue(ctx, thisRef, variableFactory);
        }

        Member member = cls != null ? getFieldOrAccessor(cls, property) : null;

        Object o;

        if (member instanceof Method) {
            try {
                o = ((Method) member).invoke(ctx, EMPTYARG);

                if (hasNullPropertyHandler()) {
                    addAccessorNode(new GetterAccessorNH((Method) member, getNullPropertyHandler()));
                    if (o == null) o = getNullPropertyHandler().getProperty(member.getName(), ctx, variableFactory);
                }
                else {
                    addAccessorNode(new GetterAccessor((Method) member));
                }
            }
            catch (IllegalAccessException e) {
                Method iFaceMeth = determineActualTargetMethod((Method) member);

                if (iFaceMeth == null)
                    throw new PropertyAccessException("could not access field: " + cls.getName() + "." + property);

                o = iFaceMeth.invoke(ctx, EMPTYARG);

                if (hasNullPropertyHandler()) {
                    addAccessorNode(new GetterAccessorNH((Method) member, getNullMethodHandler()));
                    if (o == null) o = getNullMethodHandler().getProperty(member.getName(), ctx, variableFactory);
                }
                else {
                    addAccessorNode(new GetterAccessor(iFaceMeth));
                }
            }
            return o;
        }
        else if (member != null) {
            Field f = (Field) member;

            if ((f.getModifiers() & Modifier.STATIC) != 0) {
                o = f.get(null);

                if (hasNullPropertyHandler()) {

                    addAccessorNode(new StaticVarAccessorNH((Field) member, getNullMethodHandler()));
                    if (o == null) o = getNullMethodHandler().getProperty(member.getName(), ctx, variableFactory);
                }
                else {
                    addAccessorNode(new StaticVarAccessor((Field) member));
                }

            }
            else {
                o = f.get(ctx);
                if (hasNullPropertyHandler()) {
                    addAccessorNode(new FieldAccessorNH((Field) member, getNullMethodHandler()));
                    if (o == null) o = getNullMethodHandler().getProperty(member.getName(), ctx, variableFactory);
                }
                else {
                    addAccessorNode(new FieldAccessor((Field) member));
                }
            }
            return o;
        }
        else if (ctx instanceof Map && ((Map) ctx).containsKey(property)) {
            addAccessorNode(new MapAccessor(property));
            return ((Map) ctx).get(property);
        }
        else if (ctx != null && "length".equals(property) && ctx.getClass().isArray()) {
            addAccessorNode(new ArrayLength());
            return getLength(ctx);
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
                else if (tryStaticMethodRef instanceof Field) {
                    addAccessorNode(new StaticVarAccessor((Field) tryStaticMethodRef));
                    return ((Field) tryStaticMethodRef).get(null);
                }
                else {
                    addAccessorNode(new StaticReferenceAccessor(tryStaticMethodRef));
                    return tryStaticMethodRef;
                }
            }
            else if (ctx instanceof Class) {
                Class c = (Class) ctx;
                for (Method m : c.getMethods()) {
                    if (property.equals(m.getName())) {
                        if (MVEL.COMPILER_OPT_ALLOW_NAKED_METH_CALL) {
                            o = m.invoke(null, EMPTY_OBJ_ARR);
                            if (hasNullMethodHandler()) {
                                addAccessorNode(new MethodAccessorNH(m, new ExecutableStatement[0], getNullMethodHandler()));
                                if (o == null)
                                    o = getNullMethodHandler().getProperty(m.getName(), ctx, variableFactory);
                            }
                            else {
                                addAccessorNode(new MethodAccessor(m, new ExecutableStatement[0]));
                            }
                            return o;
                        }
                        else {
                            addAccessorNode(new StaticReferenceAccessor(m));
                            return m;
                        }
                    }
                }

                try {
                    Class subClass = findClass(variableFactory, c.getName() + "$" + property, pCtx);
                    addAccessorNode(new StaticReferenceAccessor(subClass));
                    return subClass;
                }
                catch (ClassNotFoundException cnfe) {
                    // fall through.
                }
            }
            else if (MVEL.COMPILER_OPT_ALLOW_NAKED_METH_CALL) {
                return getMethod(ctx, property);
            }

            throw new PropertyAccessException("could not access: " + property + "; in class: " + (ctx == null ? "null" : ctx.getClass().getName()));
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
        if (prop.length() > 0) {
            ctx = getBeanProperty(ctx, prop);
        }

        int start = ++cursor;

        skipWhitespace();

        if (cursor == length)
            throw new CompileException("unterminated '['");

        String item;

        if (scanTo(']'))
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
            idx = (itemStmt = (ExecutableStatement) subCompileExpression(item.toCharArray(), pCtx)).getValue(ctx, thisRef, variableFactory);
        }

        ++cursor;

        if (ctx instanceof Map) {
            if (itemSubExpr) {
                addAccessorNode(new MapAccessorNest(itemStmt, null));
            }
            else {
                addAccessorNode(new MapAccessor(parseInt(item)));
            }

            return ((Map) ctx).get(idx);
        }
        else if (ctx instanceof List) {
            if (itemSubExpr) {
                addAccessorNode(new ListAccessorNest(itemStmt, null));
            }
            else {
                addAccessorNode(new ListAccessor(parseInt(item)));
            }

            return ((List) ctx).get((Integer) idx);
        }
        else if (ctx.getClass().isArray()) {
            if (itemSubExpr) {
                addAccessorNode(new ArrayAccessorNest(itemStmt));
            }
            else {
                addAccessorNode(new ArrayAccessor(parseInt(item)));
            }

            return Array.get(ctx, (Integer) idx);
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
            TypeDescriptor tDescr = new TypeDescriptor(expr, 0);
            if (tDescr.isArray()) {
                Class cls = getClassReference((Class) ctx, tDescr, variableFactory, pCtx);
                rootNode = new StaticReferenceAccessor(cls);
                return cls;
            }

            throw new CompileException("illegal use of []: unknown type: "
                    + ctx.getClass().getName());
        }
    }


    private Object getCollectionPropertyAO(Object ctx, String prop) throws Exception {
        if (prop.length() > 0) {
            ctx = getBeanPropertyAO(ctx, prop);
        }

        int start = ++cursor;

        skipWhitespace();

        if (cursor == length)
            throw new CompileException("unterminated '['");

        String item;

        if (scanTo(']'))
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
            idx = (itemStmt = (ExecutableStatement) subCompileExpression(item.toCharArray(), pCtx)).getValue(ctx, thisRef, variableFactory);
        }

        ++cursor;

        if (ctx instanceof Map) {
            if (hasPropertyHandler(Map.class)) {
                return propHandler(item, ctx, Map.class);
            }
            else {
                if (itemSubExpr) {
                    addAccessorNode(new MapAccessorNest(itemStmt, null));
                }
                else {
                    addAccessorNode(new MapAccessor(parseInt(item)));
                }

                return ((Map) ctx).get(idx);
            }
        }
        else if (ctx instanceof List) {
            if (hasPropertyHandler(List.class)) {
                return propHandler(item, ctx, List.class);
            }
            else {
                if (itemSubExpr) {
                    addAccessorNode(new ListAccessorNest(itemStmt, null));
                }
                else {
                    addAccessorNode(new ListAccessor(parseInt(item)));
                }

                return ((List) ctx).get((Integer) idx);
            }
        }
        else if (ctx.getClass().isArray()) {
            if (hasPropertyHandler(Array.class)) {
                return propHandler(item, ctx, Array.class);
            }
            else {
                if (itemSubExpr) {
                    addAccessorNode(new ArrayAccessorNest(itemStmt));
                }
                else {
                    addAccessorNode(new ArrayAccessor(parseInt(item)));
                }

                return Array.get(ctx, (Integer) idx);
            }
        }
        else if (ctx instanceof CharSequence) {
            if (hasPropertyHandler(CharSequence.class)) {
                return propHandler(item, ctx, CharSequence.class);
            }
            else {
                if (itemSubExpr) {
                    addAccessorNode(new IndexedCharSeqAccessorNest(itemStmt));
                }
                else {
                    addAccessorNode(new IndexedCharSeqAccessor(parseInt(item)));
                }

                return ((CharSequence) ctx).charAt((Integer) idx);
            }
        }
        else {
            TypeDescriptor tDescr = new TypeDescriptor(expr, 0);
            if (tDescr.isArray()) {
                Class cls = getClassReference((Class) ctx, tDescr, variableFactory, pCtx);
                rootNode = new StaticReferenceAccessor(cls);
                return cls;
            }

            throw new CompileException("illegal use of []: unknown type: "
                    + ctx.getClass().getName());
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
        int st = cursor;
        String tk = cursor != length
                && expr[cursor] == '(' && ((cursor = balancedCapture(expr, cursor, '(')) - st) > 1 ?
                new String(expr, st + 1, cursor - st - 1) : "";
        cursor++;

        Object[] args;
        Class[] argTypes;
        Accessor[] es;

        if (tk.length() == 0) {
            args = ParseTools.EMPTY_OBJ_ARR;
            argTypes = ParseTools.EMPTY_CLS_ARR;
            es = null;
        }
        else {
            String[] subtokens = parseParameterList(tk.toCharArray(), 0, -1);
            es = new ExecutableStatement[subtokens.length];
            args = new Object[subtokens.length];
            argTypes = new Class[subtokens.length];

            for (int i = 0; i < subtokens.length; i++) {
                args[i] = (es[i] = (ExecutableStatement) subCompileExpression(subtokens[i].toCharArray(), pCtx))
                        .getValue(this.ctx, thisRef, variableFactory);
            }

            if (pCtx.isStrictTypeEnforcement()) {
                for (int i = 0; i < args.length; i++) {
                    argTypes[i] = es[i].getKnownEgressType();
                }
            }
            else {
                for (int i = 0; i < args.length; i++) {
                    if (es[i].getKnownEgressType() == Object.class) {
                        argTypes[i] = args[i] == null ? null : args[i].getClass();
                    }
                    else {
                        argTypes[i] = es[i].getKnownEgressType();
                    }
                }
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
                addAccessorNode(new FunctionAccessor((Function) ptr, es));
                return ((Function) ptr).call(ctx, thisRef, variableFactory, args);
            }
            else {
                throw new OptimizationFailure("attempt to optimize a method call for a reference that does not point to a method: "
                        + name + " (reference is type: " + (ctx != null ? ctx.getClass().getName() : null) + ")");
            }

            first = false;
        }

        if (ctx == null) {
            throw new PropertyAccessException("unable to access property (null parent): " + name);
        }


        /**
         * If the target object is an instance of java.lang.Class itself then do not
         * adjust the Class scope target.
         */
        Class<?> cls = currType != null ? currType : (ctx instanceof Class ? (Class<?>) ctx : ctx.getClass());
        currType = null;

        Method m;
        Class[] parameterTypes = null;

        /**
         * If we have not cached the method then we need to go ahead and try to resolve it.
         */
        /**
         * Try to find an instance method from the class target.
         */

        if ((m = getBestCandidate(argTypes, name, cls, cls.getMethods(), false)) != null) {
            parameterTypes = m.getParameterTypes();
        }

        if (m == null) {
            /**
             * If we didn't find anything, maybe we're looking for the actual java.lang.Class methods.
             */
            if ((m = getBestCandidate(argTypes, name, cls, cls.getClass().getDeclaredMethods(), false)) != null) {
                parameterTypes = m.getParameterTypes();
            }
        }

        if (m == null) {
            StringAppender errorBuild = new StringAppender();
            if ("size".equals(name) && args.length == 0 && cls.isArray()) {
                addAccessorNode(new ArrayLength());
                return getLength(ctx);
            }

            for (int i = 0; i < args.length; i++) {
                errorBuild.append(args[i] != null ? args[i].getClass().getName() : null);
                if (i < args.length - 1) errorBuild.append(", ");
            }

            throw new PropertyAccessException("unable to resolve method: " + cls.getName() + "."
                    + name + "(" + errorBuild.toString() + ") [arglength=" + args.length + "]");
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
                        args[i] = convert(args[i], parameterTypes[i]);
                    }
                }
            }
            else {
                /**
                 * Coerce any types if required.
                 */
                for (int i = 0; i < args.length; i++)
                    args[i] = convert(args[i], parameterTypes[i]);
            }

            Object o = getWidenedTarget(m).invoke(ctx, args);

            if (hasNullMethodHandler()) {
                addAccessorNode(new MethodAccessorNH(getWidenedTarget(m), (ExecutableStatement[]) es, getNullMethodHandler()));
                if (o == null) o = getNullMethodHandler().getProperty(m.getName(), ctx, variableFactory);
            }
            else {
                addAccessorNode(new MethodAccessor(getWidenedTarget(m), (ExecutableStatement[]) es));
            }

            /**
             * return the response.
             */
            return o;
        }
    }


    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) throws Exception {
        return rootNode.getValue(ctx, elCtx, variableFactory);
    }

    private Accessor _getAccessor(Object o, Class type) {
        if (o instanceof List) {
            Accessor[] a = new Accessor[((List) o).size()];
            int i = 0;

            for (Object item : (List) o) {
                a[i++] = _getAccessor(item, type);
            }

            returnType = List.class;

            return new ListCreator(a);
        }
        else if (o instanceof Map) {
            Accessor[] k = new Accessor[((Map) o).size()];
            Accessor[] v = new Accessor[k.length];
            int i = 0;

            for (Object item : ((Map) o).keySet()) {
                k[i] = _getAccessor(item, type); // key
                v[i++] = _getAccessor(((Map) o).get(item), type); // value
            }

            returnType = Map.class;

            return new MapCreator(k, v);
        }
        else if (o instanceof Object[]) {
            Accessor[] a = new Accessor[((Object[]) o).length];
            int i = 0;
            int dim = 0;

            if (type != null) {
                String nm = type.getName();
                while (nm.charAt(dim) == '[') dim++;
            }
            else {
                type = Object[].class;
                dim = 1;
            }

            try {
                Class base = getBaseComponentType(type);
                Class cls = dim > 1 ? findClass(null, repeatChar('[', dim - 1) + "L" + base.getName() + ";", pCtx)
                        : type;

                for (Object item : (Object[]) o) {
                    expectType(a[i++] = _getAccessor(item, cls), base, true);
                }

                return new ArrayCreator(a, getSubComponentType(type));
            }
            catch (ClassNotFoundException e) {
                throw new RuntimeException("this error should never throw:" + getBaseComponentType(type).getName(), e);
            }
        }
        else {
            if (returnType == null) returnType = Object.class;
            if (type.isArray()) {
                return new ExprValueAccessor((String) o, type, ctx, variableFactory, pCtx);
            }
            else {
                return new ExprValueAccessor((String) o, Object.class, ctx, variableFactory, pCtx);
            }
        }
    }


    public Accessor optimizeCollection(ParserContext pCtx, Object o, Class type, char[] property, Object ctx, Object thisRef, VariableResolverFactory factory) {
        this.returnType = type;
        this.ctx = ctx;
        this.variableFactory = factory;
        this.pCtx = pCtx;

        Accessor root = _getAccessor(o, returnType);

        if (property != null && property.length > 0) {
            return new Union(root, property);
        }
        else {
            return root;
        }
    }


    public Accessor optimizeObjectCreation(ParserContext pCtx, char[] property, Object ctx, Object thisRef, VariableResolverFactory factory) {
        this.length = (this.expr = property).length;
        this.cursor = 0;
        this.pCtx = pCtx;

        try {
            return compileConstructor(property, ctx, factory);
        }
        catch (CompileException e) {
            throw e;
        }
        catch (Exception e) {
            throw new CompileException("could not create constructor: " + e.getMessage(), e);
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
            Class cls = ParseTools.findClass(vars, s, pCtx);

            ExecutableStatement[] cStmts = new ExecutableStatement[constructorParms.length];

            for (int i = 0; i < constructorParms.length; i++) {
                cStmts[i] = (ExecutableStatement) subCompileExpression(constructorParms[i].toCharArray(), pCtx);
            }

            Object[] parms = new Object[constructorParms.length];
            for (int i = 0; i < constructorParms.length; i++) {
                parms[i] = cStmts[i].getValue(ctx, vars);
            }

            Constructor cns = getBestConstructorCanadidate(parms, cls, pCtx.isStrongTyping());

            if (cns == null) {
                StringBuilder error = new StringBuilder();
                for (int i = 0; i < parms.length; i++) {
                    error.append(parms[i].getClass().getName());
                    if (i + 1 < parms.length) error.append(", ");
                }

                throw new CompileException("unable to find constructor: " + cls.getName() + "(" + error.toString() + ")");
            }
            for (int i = 0; i < parms.length; i++) {
                //noinspection unchecked
                parms[i] = convert(parms[i], cns.getParameterTypes()[i]);
            }

            AccessorNode ca = new ConstructorAccessor(cns, cStmts);

            if (cnsRes.length > 1) {
                ReflectiveAccessorOptimizer compiledOptimizer
                        = new ReflectiveAccessorOptimizer(pCtx, cnsRes[1].toCharArray(), cns.newInstance(parms), ctx, vars);
                compiledOptimizer.ingressType = cns.getDeclaringClass();


                compiledOptimizer.setRootNode(ca);
                compiledOptimizer.compileGetChain();
                ca = compiledOptimizer.getRootNode();

                this.val = compiledOptimizer.getResultOptPass();
            }

            return ca;
        }
        else {
            Constructor<?> cns = Class.forName(new String(expression), true, Thread.currentThread().getContextClassLoader())
                    .getConstructor(EMPTYCLS);

            AccessorNode ca = new ConstructorAccessor(cns, null);

            if (cnsRes.length > 1) {
                //noinspection NullArgumentToVariableArgMethod
                ReflectiveAccessorOptimizer compiledOptimizer
                        = new ReflectiveAccessorOptimizer(getCurrentThreadParserContext(),
                        cnsRes[1].toCharArray(), cns.newInstance(null), ctx, vars);
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

    private Object propHandler(String property, Object ctx, Class handler) {
        PropertyHandler ph = getPropertyHandler(handler);
        addAccessorNode(new PropertyHandlerAccessor(property, ph));
        return ph.getProperty(property, ctx, variableFactory);
    }

    public void propHandlerSet(String property, Object ctx, Class handler, Object value) {
        PropertyHandler ph = getPropertyHandler(handler);
        addAccessorNode(new PropertyHandlerAccessor(property, ph));
        ph.setProperty(property, ctx, variableFactory, value);
    }
}
