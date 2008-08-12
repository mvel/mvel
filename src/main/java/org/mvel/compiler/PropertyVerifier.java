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
package org.mvel.compiler;

import org.mvel.CompileException;
import org.mvel.ParserContext;
import org.mvel.PropertyAccessException;
import org.mvel.ast.Function;
import org.mvel.optimizers.AbstractOptimizer;
import static org.mvel.util.ParseTools.*;
import static org.mvel.util.PropertyTools.getFieldOrAccessor;
import static org.mvel.util.PropertyTools.getSubComponentType;
import org.mvel.util.StringAppender;

import java.lang.reflect.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class PropertyVerifier extends AbstractOptimizer {
    private static final int DONE = -1;
    private static final int NORM = 0;
    private static final int METH = 1;
    private static final int COL = 2;

    private ParserContext parserContext;
    private List<String> inputs = new LinkedList<String>();
    private boolean first = true;
    private boolean resolvedExternally;
    private Map<String, Class> paramTypes;


    public PropertyVerifier(char[] property, ParserContext parserContext) {
        this.length = (this.expr = property).length;
        this.parserContext = parserContext;
    }

    public PropertyVerifier(String property, ParserContext parserContext) {
        this.length = (this.expr = property.toCharArray()).length;
        this.parserContext = parserContext;
    }

    public List<String> getInputs() {
        return inputs;
    }

    public void setInputs(List<String> inputs) {
        this.inputs = inputs;
    }

    public Class analyze() {
        Class ctx = Object.class;
        resolvedExternally = true;

        first = true;
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
                case DONE:
                    break;
            }

            first = false;
        }
        return ctx;
    }

    private Class getBeanProperty(Class ctx, String property) {
        if (first) {
            if (parserContext.hasVarOrInput(property)) {
                if (parserContext.isStrictTypeEnforcement()) {
                    paramTypes = parserContext.getTypeParameters(property);
                    parserContext.setLastTypeParameters(parserContext.getTypeParametersAsArray(property));
                }

                return parserContext.getVarOrInputType(property);
            }
            else if (parserContext.hasImport(property)) {
                resolvedExternally = false;
                return parserContext.getImport(property);
            }
            if (!parserContext.isStrongTyping()) {
                return Object.class;
            }
        }

        start = cursor;

        Member member = ctx != null ? getFieldOrAccessor(ctx, property) : null;

        if (member instanceof Field) {
            if (parserContext.isStrictTypeEnforcement()) {
                Field f = ((Field) member);

                if (f.getGenericType() != null && f.getGenericType() instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType) f.getGenericType();
                    parserContext.setLastTypeParameters(pt.getActualTypeArguments());

                    Type[] gpt = pt.getActualTypeArguments();
                    Type[] classArgs = ((Class) pt.getRawType()).getTypeParameters();
                    //   ParameterizedType pt;

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
            return ((Method) member).getReturnType();
        }
        else if (parserContext.hasImport(property)) {
            return parserContext.getImport(property);
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

            if (parserContext.isStrictTypeEnforcement()) {
                addFatalError("unqualified type in strict mode for: " + property);
            }
            return Object.class;
        }
    }

    private Class getCollectionProperty(Class ctx, String property) {
        if (parserContext.hasVarOrInput(property)) {
            ctx = getSubComponentType(parserContext.getVarOrInputType(property));
        }
        else if (parserContext.hasImport(property)) {
            resolvedExternally = false;
            ctx = getSubComponentType(parserContext.getImport(property));
        }
        else {
            ctx = Object.class;
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


    private Class getMethod(Class ctx, String name) {
        if (first) {
            if (parserContext.hasImport(name)) {
                Method m = parserContext.getStaticImport(name).getMethod();
                ctx = m.getDeclaringClass();
                name = m.getName();
                first = false;
            }
            else if (parserContext.hasFunction(name)) {
                resolvedExternally = false;
                return parserContext.getFunction(name).getEgressType();
            }
        }

        int st = cursor;

        String tk = ((cursor = balancedCapture(expr, cursor, '(')) - st) > 1 ? new String(expr, st + 1, cursor - st - 1) : "";

        cursor++;

        if (tk.length() > 0) {
            for (String token : parseParameterList(tk.toCharArray(), 0, -1)) {
                new ExpressionCompiler(token)._compile();
            }
        }

        Class[] args;
        String[] subtokens;

        if (tk.length() == 0) {
            args = new Class[0];
            subtokens = new String[0];
        }
        else {
            subtokens = parseParameterList(tk.toCharArray(), 0, -1);
            args = new Class[subtokens.length];

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

        if ((m = getBestCandidate(args, name, ctx, ctx.getMethods())) == null) {
            if ((m = getBestCandidate(args, name, ctx, ctx.getDeclaredMethods())) == null) {
                StringAppender errorBuild = new StringAppender();
                for (int i = 0; i < args.length; i++) {
                    errorBuild.append(args[i] != null ? args[i].getClass().getName() : null);
                    if (i < args.length - 1) errorBuild.append(", ");
                }

                if ("size".equals(name) && args.length == 0 && ctx.isArray()) {
                    return Integer.class;
                }

                if (parserContext.isStrictTypeEnforcement()) {
                    addFatalError("unable to resolve method using strict-mode: " + ctx.getName() + "." + name + "(...)");
                }
                return Object.class;
            }
        }

        if (parserContext.isStrictTypeEnforcement() && m.getGenericReturnType() != null) {
            Map<String, Class> typeArgs = new HashMap<String, Class>();

            Type[] gpt = m.getGenericParameterTypes();
            Class z;
            ParameterizedType pt;

            for (int i = 0; i < gpt.length; i++) {
                if (gpt[i] instanceof ParameterizedType) {
                    pt = (ParameterizedType) gpt[i];
                    if ((z = parserContext.getImport(subtokens[i])) != null) {
                        if (pt.getRawType().equals(Class.class)) {
                            typeArgs.put(pt.getActualTypeArguments()[0].toString(), z);
                        }
                        else {
                            typeArgs.put(gpt[i].toString(), z);
                        }
                    }
                }
            }

            String returnTypeArg = m.getGenericReturnType().toString();

            if (paramTypes != null && paramTypes.containsKey(returnTypeArg)) {
                return paramTypes.get(returnTypeArg);
            }
            else if (typeArgs.containsKey(returnTypeArg)) {
                return typeArgs.get(returnTypeArg);
            }
        }

        return m.getReturnType();
    }

    public boolean isResolvedExternally() {
        return resolvedExternally;
    }
}
