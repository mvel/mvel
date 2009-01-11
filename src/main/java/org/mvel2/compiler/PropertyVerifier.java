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

package org.mvel2.compiler;

import org.mvel2.CompileException;
import org.mvel2.ParserContext;
import org.mvel2.PropertyAccessException;
import org.mvel2.ast.Function;
import org.mvel2.optimizers.AbstractOptimizer;
import org.mvel2.optimizers.impl.refl.nodes.WithAccessor;
import static org.mvel2.util.ParseTools.*;
import static org.mvel2.util.PropertyTools.getFieldOrAccessor;
import org.mvel2.util.StringAppender;

import java.lang.reflect.*;
import java.util.*;

/**
 * This verifier is used by the compiler to enforce rules such as type strictness.  It is, as side-effect, also
 * responsible for extracting type information.
 *
 * @author Mike Brock
 * @author Dhanji Prasanna
 */
public class PropertyVerifier extends AbstractOptimizer {
    private static final int DONE = -1;
    private static final int NORM = 0;
    private static final int METH = 1;
    private static final int COL = 2;
    private static final int WITH = 3;

    private List<String> inputs = new LinkedList<String>();
    private boolean first = false;
    private boolean resolvedExternally;
    private Map<String, Class> paramTypes;

    private Class ctx = null;


    public PropertyVerifier(char[] property, ParserContext parserContext) {
        this.length = (this.expr = property).length;
        this.pCtx = parserContext;
    }

    public PropertyVerifier(String property, ParserContext parserContext) {
        this.length = (this.expr = property.toCharArray()).length;
        this.pCtx = parserContext;
    }

    public PropertyVerifier(String property, ParserContext parserContext, Class root) {
        this.length = (this.expr = property.toCharArray()).length;
        this.pCtx = parserContext;
        this.ctx = root;
    }

    public List<String> getInputs() {
        return inputs;
    }

    public void setInputs(List<String> inputs) {
        this.inputs = inputs;
    }

    /**
     * Analyze the statement and return the known egress type.
     *
     * @return known engress type
     */
    public Class analyze() {
        resolvedExternally = true;
        if (ctx == null) {
            ctx = Object.class;
            first = true;
        }

        while (cursor < length) {
            switch (nextSubToken()) {
                case NORM:
                    ctx = getBeanProperty(ctx, capture());
                    break;
                case METH:
                    ctx = getMethod(ctx, capture());
                    break;
                case COL:
                    ctx = getCollectionProperty(ctx, capture());
                    break;
                case WITH:
                    ctx = getWithProperty(ctx);
                    break;

                case DONE:
                    break;
            }

            first = false;
        }
        return ctx;
    }

    /**
     * Process bean property
     *
     * @param ctx      - the ingress type
     * @param property - the property component
     * @return known egress type.
     */
    private Class getBeanProperty(Class ctx, String property) {
        if (first) {
            if (pCtx.hasVarOrInput(property)) {
                if (pCtx.isStrictTypeEnforcement()) {
                    paramTypes = pCtx.getTypeParameters(property);
                    pCtx.setLastTypeParameters(pCtx.getTypeParametersAsArray(property));
                }
                return pCtx.getVarOrInputType(property);
            }
            else if (pCtx.hasImport(property)) {
                resolvedExternally = false;
                return pCtx.getImport(property);
            }
            if (!pCtx.isStrongTyping()) {
                return Object.class;
            }
        }

        start = cursor;

        Member member = ctx != null ? getFieldOrAccessor(ctx, property) : null;

        if (member instanceof Field) {
            if (pCtx.isStrictTypeEnforcement()) {
                Field f = ((Field) member);

                if (f.getGenericType() != null && f.getGenericType() instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType) f.getGenericType();
                    pCtx.setLastTypeParameters(pt.getActualTypeArguments());

                    Type[] gpt = pt.getActualTypeArguments();
                    Type[] classArgs = ((Class) pt.getRawType()).getTypeParameters();

                    if (gpt.length > 0 && paramTypes == null) paramTypes = new HashMap<String, Class>();
                    for (int i = 0; i < gpt.length; i++) {
                        paramTypes.put(classArgs[i].toString(), (Class) gpt[i]);
                    }

                }

                return f.getType();
            }
            else {
                return ((Field) member).getType();
            }
        }
        else if (member != null) {
            Method method = (Method) member;

            if (pCtx.isStrictTypeEnforcement()) {
                //if not a field, then this is a property getter
                Type parametricReturnType = method.getGenericReturnType();

                //push return type parameters onto parser context, only if this is a parametric type
                if (parametricReturnType instanceof ParameterizedType) {
                    pCtx.setLastTypeParameters(((ParameterizedType) parametricReturnType).getActualTypeArguments());
                }

            }
            return method.getReturnType();
        }
        else if (pCtx != null && pCtx.hasImport(property)) {
            return pCtx.getImport(property);
        }
        else {
            Object tryStaticMethodRef = tryStaticAccess();

            if (tryStaticMethodRef != null) {
                if (tryStaticMethodRef instanceof Class) {
                    return (Class) tryStaticMethodRef;
                }
                else if (tryStaticMethodRef instanceof Field) {
                    try {
                        return ((Field) tryStaticMethodRef).get(null).getClass();
                    }
                    catch (Exception e) {
                        throw new CompileException("in verifier: ", e);
                    }
                }
                else {
                    try {
                        return ((Method) tryStaticMethodRef).getReturnType();
                    }
                    catch (Exception e) {
                        throw new CompileException("in verifier: ", e);
                    }
                }

            }
            else if (ctx != null && ctx.getClass() == Class.class) {
                for (Method m : ctx.getMethods()) {
                    if (property.equals(m.getName())) {
                        return m.getReturnType();
                    }
                }
            }

            if (pCtx.isStrictTypeEnforcement()) {
                addFatalError("unqualified type in strict mode for: " + property);
            }
            return Object.class;
        }
    }

    /**
     * Process collection property
     *
     * @param ctx      - the ingress type
     * @param property - the property component
     * @return known egress type
     */
    private Class getCollectionProperty(Class ctx, String property) {
        if (first) {
            if (pCtx.hasVarOrInput(property)) {
                ctx = getSubComponentType(pCtx.getVarOrInputType(property));
            }
            else if (pCtx.hasImport(property)) {
                resolvedExternally = false;
                ctx = getSubComponentType(pCtx.getImport(property));
            }
            else {
                ctx = Object.class;
            }
        }
        else if (pCtx.isStrongTyping()) {
            if (Map.class.isAssignableFrom(ctx = getBeanProperty(ctx, property))) {
                ctx = (Class) pCtx.getLastTypeParameters()[1];
            }
            else if (Collection.class.isAssignableFrom(ctx)) {
                ctx = (Class) pCtx.getLastTypeParameters()[0];
            }
            else if (ctx.isArray()) {
                ctx = getBaseComponentType(ctx);
            }
            else {
                throw new CompileException("unknown collection type");
            }
        }

        ++cursor;

        whiteSpaceSkip();

        if (cursor == length)
            throw new PropertyAccessException("unterminated '['");

        if (scanTo(']')) {
            addFatalError("unterminated [ in token");
        }

        ++cursor;

        return ctx;
    }


    /**
     * Process method
     *
     * @param ctx  - the ingress type
     * @param name - the property component
     * @return known egress type.
     */
    private Class getMethod(Class ctx, String name) {
        int st = cursor;

        /**
         * Check to see if this is the first element in the statement.
         */
        if (first) {
            first = false;

            /**
             * It's the first element in the statement, therefore we check to see if there is a static import of a
             * native Java method or an MVEL function.
             */
            if (pCtx.hasImport(name)) {
                Method m = pCtx.getStaticImport(name).getMethod();

                /**
                 * Replace the method parameters.
                 */
                ctx = m.getDeclaringClass();
                name = m.getName();
            }
            else if (pCtx.hasFunction(name)) {
                resolvedExternally = false;
                String tk = ((cursor = balancedCapture(expr, cursor, '(')) - st) > 1 ? new String(expr, st + 1, cursor - st - 1) : "";
                Function f = pCtx.getFunction(name);
                f.checkArgumentCount(parseParameterList(tk.toCharArray(), 0, -1).length);
                return f.getEgressType();
            }
        }

        /**
         * Get the arguments for the method.
         */
        String tk = ((cursor = balancedCapture(expr, cursor, '(')) - st) > 1 ? new String(expr, st + 1, cursor - st - 1) : "";

        cursor++;

        /**
         * Parse out the arguments list.
         */
        Class[] args;
        String[] subtokens = parseParameterList(tk.toCharArray(), 0, -1);

        if (subtokens.length == 0) {
            args = new Class[0];
            subtokens = new String[0];
        }
        else {
            args = new Class[subtokens.length];

            /**
             *  Subcompile all the arguments to determine their known types.
             */
            ExpressionCompiler compiler;
            for (int i = 0; i < subtokens.length; i++) {
                (compiler = new ExpressionCompiler(subtokens[i], true))._compile();
                args[i] = compiler.getReturnType() != null ? compiler.getReturnType() : Object.class;
            }
        }

        /**
         * If the target object is an instance of java.lang.Class itself then do not
         * adjust the Class scope target.
         */

        Method m;

        /**
         * If we have not cached the method then we need to go ahead and try to resolve it.
         */

        if ((m = getBestCandidate(args, name, ctx, ctx.getMethods(), pCtx.isStrongTyping())) == null) {
            if ((m = getBestCandidate(args, name, ctx, ctx.getDeclaredMethods(), pCtx.isStrongTyping())) == null) {
                StringAppender errorBuild = new StringAppender();
                for (int i = 0; i < args.length; i++) {
                    errorBuild.append(args[i] != null ? args[i].getClass().getName() : null);
                    if (i < args.length - 1) errorBuild.append(", ");
                }

                if ("size".equals(name) && args.length == 0 && ctx.isArray()) {
                    return Integer.class;
                }

                if (pCtx.isStrictTypeEnforcement()) {
                    addFatalError("unable to resolve method using strict-mode: " + ctx.getName() + "." + name + "(...)");
                }
                return Object.class;
            }
        }

        /**
         * If we're in strict mode, we look for generic type information.
         */
        if (pCtx.isStrictTypeEnforcement() && m.getGenericReturnType() != null) {
            Map<String, Class> typeArgs = new LinkedHashMap<String, Class>();

            Type[] gpt = m.getGenericParameterTypes();
            Class z;
            ParameterizedType pt;

            for (int i = 0; i < gpt.length; i++) {
                if (gpt[i] instanceof ParameterizedType) {
                    pt = (ParameterizedType) gpt[i];
                    if ((z = pCtx.getImport(subtokens[i])) != null) {
                        /**
                         * We record the value of the type parameter to our typeArgs Map.
                         */
                        if (pt.getRawType().equals(Class.class)) {
                            /**
                             * If this is an instance of Class, we deal with the special parameterization case.
                             */
                            typeArgs.put(pt.getActualTypeArguments()[0].toString(), z);
                        }
                        else {
                            typeArgs.put(gpt[i].toString(), z);
                        }
                    }
                }
            }

            /**
             * Get the return type argument
             */
            Type parametricReturnType = m.getGenericReturnType();
            String returnTypeArg = parametricReturnType.toString();

            //push return type parameters onto parser context, only if this is a parametric type
            if (parametricReturnType instanceof ParameterizedType) {
                pCtx.setLastTypeParameters(((ParameterizedType) parametricReturnType).getActualTypeArguments());
            }

            if (paramTypes != null && paramTypes.containsKey(returnTypeArg)) {
                /**
                 * If the paramTypes Map contains the known type, return that type.
                 */
                return paramTypes.get(returnTypeArg);
            }
            else if (typeArgs.containsKey(returnTypeArg)) {
                /**
                 * If the generic type was declared as part of the method, it will be in this
                 * Map.
                 */
                return typeArgs.get(returnTypeArg);
            }
        }

        return m.getReturnType();
    }

    private Class getWithProperty(Class ctx) {
        String root = new String(expr, 0, cursor - 1).trim();

        int start = cursor + 1;
        int[] res = balancedCaptureWithLineAccounting(expr, cursor, '{');
        cursor = res[0];
        getParserContext().incrementLineCount(res[1]);

        new WithAccessor(root, subset(expr, start, cursor++ - start), ctx, pCtx.isStrictTypeEnforcement());

        return ctx;
    }

    public boolean isResolvedExternally() {
        return resolvedExternally;
    }

    public Class getCtx() {
        return ctx;
    }

    public void setCtx(Class ctx) {
        this.ctx = ctx;
    }
}
