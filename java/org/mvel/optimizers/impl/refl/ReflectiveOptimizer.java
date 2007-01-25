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
import static org.mvel.ExpressionParser.compileExpression;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.AccessorOptimizer;
import org.mvel.optimizers.impl.refl.collection.ArrayCreator;
import org.mvel.optimizers.impl.refl.collection.ListCreator;
import org.mvel.optimizers.impl.refl.collection.MapCreator;
import org.mvel.optimizers.impl.refl.collection.ExprValueAccessor;
import static org.mvel.util.ParseTools.parseParameterList;
import static org.mvel.util.ParseTools.subset;
import org.mvel.util.*;

import static java.lang.Character.isWhitespace;
import static java.lang.Class.forName;
import static java.lang.Integer.parseInt;
import java.lang.reflect.*;
import java.util.*;

public class ReflectiveOptimizer extends AbstractParser implements AccessorOptimizer {
    private int start = 0;

    private AccessorNode rootNode;
    private AccessorNode currNode;

    private Object ctx;
    private Object thisRef;
    private Object val;

    private VariableResolverFactory variableFactory;

    private static final int DONE = -1;
    private static final int BEAN = 0;
    private static final int METH = 1;
    private static final int COL = 2;

    private static final Object[] EMPTYARG = new Object[0];

    private boolean first = true;


    public ReflectiveOptimizer() {
    }

    public ReflectiveOptimizer(char[] property, Object ctx) {
        this.expr = property;
        this.length = property.length;
        this.ctx = ctx;
    }

    public ReflectiveOptimizer(char[] property, Object ctx, VariableResolverFactory variableFactory) {
        this.expr = property;
        this.length = property != null ? property.length : 0;
        this.thisRef = this.ctx = ctx;
        this.variableFactory = variableFactory;
    }

    public ReflectiveOptimizer(char[] property, Object ctx, Object thisRef, VariableResolverFactory variableFactory) {
        this.expr = property;
        this.length = property != null ? property.length : 0;
        this.ctx = ctx;
        this.variableFactory = variableFactory;
        this.thisRef = thisRef;
    }


    public ReflectiveOptimizer(String property, Object ctx) {
        this.length = (this.expr = property.toCharArray()).length;
        this.ctx = ctx;
    }


    public Accessor optimize(char[] property, Object ctx, Object thisRef, VariableResolverFactory factory, boolean root) {
        this.rootNode = this.currNode = null;
        this.start = this.cursor = 0;
        this.first = true;

        this.length = (this.expr = property).length;
        this.ctx = ctx;
        this.thisRef = thisRef;
        this.variableFactory = factory;

        if (root) currNode = rootNode = new ThisValueAccessor();

        return compileGetChain();
    }

    public Accessor compileGetChain() {
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


    private int nextSubToken() {
        switch (expr[start = cursor]) {
            case'[':
                return COL;
            case'.':
                cursor = ++start;
        }

        //noinspection StatementWithEmptyBody
        while (++cursor < length && Character.isJavaIdentifierPart(expr[cursor])) ;


        if (cursor < length) {
            switch (expr[cursor]) {
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
        return new String(expr, start, cursor - start);
    }

    public void addAccessorNode(AccessorNode an) {
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
            GetterAccessor accessor = new GetterAccessor((Method) member);
            addAccessorNode(accessor);

            return ((Method) member).invoke(ctx, EMPTYARG);
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
        else if (Token.LITERALS.containsKey(property)) {
            StaticReferenceAccessor accessor = new StaticReferenceAccessor();
            accessor.setLiteral(Token.LITERALS.get(property));

            addAccessorNode(accessor);

            return accessor.getLiteral();
        }
        else {
            Class tryStaticMethodRef = tryStaticAccess();

            if (tryStaticMethodRef != null) {
                StaticReferenceAccessor accessor = new StaticReferenceAccessor();
                accessor.setLiteral(tryStaticMethodRef);

                addAccessorNode(accessor);

                return tryStaticMethodRef;
            }
            else
                throw new PropertyAccessException("could not access property ('" + property + "')");
        }
    }

    private void whiteSpaceSkip() {
        if (cursor < length)
            //noinspection StatementWithEmptyBody
            while (isWhitespace(expr[cursor]) && ++cursor < length) ;
    }

    private boolean scanTo(char c) {
        for (; cursor < length; cursor++) {
            if (expr[cursor] == c) {
                return true;
            }
        }
        return false;
    }

    private int containsStringLiteralTermination() {
        int pos = cursor;
        for (pos--; pos > 0; pos--) {
            if (expr[pos] == '\'' || expr[pos] == '"') return pos;
            else if (!isWhitespace(expr[pos])) return pos;
        }
        return -1;
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

        if (expr[cursor] == '\'' || expr[cursor] == '"') {
            start++;

            int end;

            if (!scanTo(']'))
                throw new PropertyAccessException("unterminated '['");
            if ((end = containsStringLiteralTermination()) == -1)
                throw new PropertyAccessException("unterminated string literal in collections accessor");

            item = new String(expr, start, end - start);
        }
        else {
            if (!scanTo(']'))
                throw new PropertyAccessException("unterminated '['");

            item = new String(expr, start, cursor - start);
        }

        ++cursor;

        if (ctx instanceof Map) {
            MapAccessor accessor = new MapAccessor();
            accessor.setProperty(item);

            addAccessorNode(accessor);

            return ((Map) ctx).get(item);
        }
        else if (ctx instanceof List) {
            ListAccessor accessor = new ListAccessor();
            accessor.setIndex(parseInt(item));

            addAccessorNode(accessor);

            return ((List) ctx).get(accessor.getIndex());
        }
        else if (ctx instanceof Collection) {
            int count = parseInt(item);
            if (count > ((Collection) ctx).size())
                throw new PropertyAccessException("index [" + count + "] out of bounds on collections");

            Iterator iter = ((Collection) ctx).iterator();
            for (int i = 0; i < count; i++) iter.next();
            return iter.next();
        }
        else if (ctx instanceof Object[]) {
            ArrayAccessor accessor = new ArrayAccessor();
            accessor.setIndex(parseInt(item));

            addAccessorNode(accessor);

            return ((Object[]) ctx)[accessor.getIndex()];
        }
        else if (ctx instanceof CharSequence) {
            IndexedCharSeqAccessor accessor = new IndexedCharSeqAccessor();
            accessor.setIndex(parseInt(item));

            addAccessorNode(accessor);

            return ((CharSequence) ctx).charAt(accessor.getIndex());
        }
        else {
            throw new PropertyAccessException("illegal use of []: unknown type: " + (ctx == null ? null : ctx.getClass().getName()));
        }
    }

    private static final Map<String, ExecutableStatement[]> SUBEXPRESSION_CACHE
            = new WeakHashMap<String, ExecutableStatement[]>();

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
            args = new Object[0];
            es = null;
        }
        else {
            if (SUBEXPRESSION_CACHE.containsKey(tk)) {
                es = SUBEXPRESSION_CACHE.get(tk);
                args = new Object[es.length];
                for (int i = 0; i < es.length; i++) {
                    args[i] = es[i].getValue(this.ctx, variableFactory);
                }
            }
            else {
                String[] subtokens = parseParameterList(tk.toCharArray(), 0, -1);
                es = new ExecutableStatement[subtokens.length];
                args = new Object[subtokens.length];
                for (int i = 0; i < subtokens.length; i++) {
                    args[i] = (es[i] = (ExecutableStatement) compileExpression(subtokens[i])).getValue(this.ctx, variableFactory);
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
            StringAppender errorBuild = new StringAppender();
            for (int i = 0; i < args.length; i++) {
                errorBuild.append(args[i] != null ? args[i].getClass().getName() : null);
                if (i < args.length - 1) errorBuild.append(", ");
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
            access.setMethod(m);
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

    private Class tryStaticAccess() {
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
            int depth = 0;
            int last = expr.length;
            for (int i = expr.length - 1; i > 0; i--) {
                switch (expr[i]) {
                    case'.':
                        if (!meth) {
                            return forName(new String(expr, 0, last));
                        }

                        meth = false;
                        last = i;
                        break;
                    case')':
                        if (depth++ == 0)
                            meth = true;
                        break;
                    case'(':
                        depth--;
                        break;
                }
            }
        }
        catch (Exception cnfe) {
            // do nothing.
        }

        return null;
    }


    public static void main(String[] args) {
        new ReflectiveOptimizer().optimizeCollection("[test, foo, bar, {1,2,3}]".toCharArray(), null, null, null);
    }

    public Accessor optimizeCollection(char[] property, Object ctx, Object thisRef, VariableResolverFactory factory) {
        CollectionParser parser = new CollectionParser();
        Object o = ((List) parser.parseCollection(property)).get(0);


        Accessor root = get(o);
        int end = parser.getEnd() + 2;

        if (end < property.length) {
            return new Union(root, subset(property, end));
        }
        else {
            return root;
        }
    }

    public Accessor get(Object o) {
        if (o instanceof List) {
            Accessor[] a = new Accessor[((List) o).size()];
            int i = 0;

            for (Object item : (List) o) {
                a[i++] = get(item);
            }

            return new ListCreator(a);
        }
        else if (o instanceof Map) {
            Accessor[] k = new Accessor[((Map) o).size()];
            Accessor[] v = new Accessor[k.length];
            int i = 0;

            for (Object item : ((Map) o).keySet()) {
                k[i] = get(item); // key
                v[i++] = get(((Map) o).get(item)); // value
            }

            return new MapCreator(k, v);
        }
        else if (o instanceof Object[]) {
            Accessor[] a = new Accessor[((Object[]) o).length];
            int i = 0;

            for (Object item : (Object[]) o) {
                a[i++] = get(item); // item
            }

            return new ArrayCreator(a);
        }
        else {
            return new ExprValueAccessor((String) o);
        }

    }


    public Accessor optimizeAssignment(char[] property, Object ctx, Object thisRef, VariableResolverFactory factory) {
        this.length = (this.expr = property).length;
        this.cursor = 0;

        greedy = false; // don't do a greedy capture.
        Token var = nextToken();

        if (!nextToken().isOperator(Operator.ASSIGN))
            throw new CompileException("expected assignment operator");

        greedy = true; // turn greedy back on.

        Token expr = nextToken();

        if (expr.isLiteral()) {
            assert ParseTools.debug("ASSIGN_LITERAL '" + expr.getName() + "'");
            Literal lit = new Literal(expr.getName());
            val = lit.getValue(ctx, thisRef, factory);
            return new Assignment(var.getName(), lit);
        }
        else {
            assert ParseTools.debug("ASSIGN_EXPR '" + expr.getName() + "'");
            ExprValueAccessor valAcc = new ExprValueAccessor(expr.getName());
            val = valAcc.getValue(ctx, thisRef, factory);
            return new Assignment(var.getName(), valAcc);
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


        Token var = nextToken();

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

    public void setRootNode(AccessorNode rootNode) {
        this.rootNode = this.currNode = rootNode;
    }

    public AccessorNode getRootNode() {
        return rootNode;
    }


    public Object getResultOptPass() {
        return val;
    }

    public static AccessorNode compileConstructor(char[] expression, Object ctx, VariableResolverFactory vars) throws
            InstantiationException, IllegalAccessException, InvocationTargetException,
            ClassNotFoundException, NoSuchMethodException {


        String[] cnsRes = ParseTools.captureContructorAndResidual(expression);

        String[] constructorParms = ParseTools.parseMethodOrConstructor(cnsRes[0].toCharArray());

        if (constructorParms != null) {
            String s;

            Class cls = Token.LITERALS.containsKey(s = new String(subset(expression, 0, ArrayTools.findFirst('(', expression)))) ?
                    ((Class) Token.LITERALS.get(s)) : ParseTools.createClass(s);

            ExecutableStatement[] cStmts = new ExecutableStatement[constructorParms.length];

            for (int i = 0; i < constructorParms.length; i++) {
                cStmts[i] = (ExecutableStatement) compileExpression(constructorParms[i]);
            }

            Object[] parms = new Object[constructorParms.length];
            for (int i = 0; i < constructorParms.length; i++) {
                parms[i] = cStmts[i].getValue(ctx, vars);
            }

            Constructor cns = ParseTools.getBestConstructorCanadidate(parms, cls);

            if (cns == null)
                throw new CompileException("unable to find constructor for: " + cls.getName());

            for (int i = 0; i < parms.length; i++) {
                //noinspection unchecked
                parms[i] = DataConversion.convert(parms[i], cns.getParameterTypes()[i]);
            }

            AccessorNode ca = new ConstructorAccessor(cns, cStmts);

            if (cnsRes.length > 1) {
                ReflectiveOptimizer compiledOptimizer
                        = new ReflectiveOptimizer(cnsRes[1].toCharArray(), cns.newInstance(parms), ctx, vars);
                compiledOptimizer.setRootNode(ca);
                compiledOptimizer.compileGetChain();
                ca = compiledOptimizer.getRootNode();
            }

            return ca;
        }
        else {
            Constructor cns = Class.forName(new String(expression)).getConstructor();
            AccessorNode ca = new ConstructorAccessor(cns, null);

            if (cnsRes.length > 1) {
                ReflectiveOptimizer compiledOptimizer
                        = new ReflectiveOptimizer(cnsRes[1].toCharArray(), cns.newInstance(), ctx, vars);
                compiledOptimizer.setRootNode(ca);
                compiledOptimizer.compileGetChain();
                ca = compiledOptimizer.getRootNode();
            }

            return ca;
        }
    }
}
