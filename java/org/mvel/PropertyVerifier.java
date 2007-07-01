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
package org.mvel;

import static org.mvel.util.ParseTools.parseParameterList;
import org.mvel.util.ParseTools;
import org.mvel.util.ReflectionUtil;
import org.mvel.util.PropertyTools;
import org.mvel.util.StringAppender;
import org.mvel.optimizers.impl.refl.*;
import org.mvel.optimizers.AbstractOptimizer;

import static java.lang.Character.isJavaIdentifierPart;
import static java.lang.Character.isWhitespace;
import java.lang.reflect.Member;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Array;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class PropertyVerifier extends AbstractOptimizer {


    private static final int DONE = -1;
    private static final int NORM = 0;
    private static final int METH = 1;
    private static final int COL = 2;

    private ParserContext parserContext;
    private List<String> inputs = new LinkedList<String>();
    private boolean first = true;

    private static final Class[] EMPTYCLS = new Class[0];

    public PropertyVerifier(char[] property, ParserContext parserContext) {
        this.expr = property;
        this.length = property.length;
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
                return parserContext.getVarOrInputType(property);
            }
            else if (AbstractParser.LITERALS.containsKey(property)) {
                return (Class) AbstractParser.LITERALS.get(property);
            }
            else {
                if (parserContext.isStrictTypeEnforcement()) {
                    throw new CompileException("unqualified type for '" + property + "' in strict-mode");
                }
                return Object.class;
            }
        }

        start = cursor;

        Member member = ctx != null ? PropertyTools.getFieldOrAccessor(ctx, property) : null;

        if (member instanceof Field) {
            FieldAccessor accessor = new FieldAccessor();
            accessor.setField((Field) member);

            return ((Field) member).getType();
        }
        else if (member != null) {
            return ((Method) member).getReturnType();
        }
        else if (AbstractParser.LITERALS.containsKey(property)) {
            return (Class) AbstractParser.LITERALS.get(property);
        }
        else {
            Object tryStaticMethodRef = tryStaticAccess();

            if (tryStaticMethodRef != null) {
                if (tryStaticMethodRef instanceof Class) {
                    return tryStaticMethodRef.getClass();
                }
                else {

                    try {
                        return ((Field) tryStaticMethodRef).get(null).getClass();
                    }
                    catch (Exception e) {

                        throw new CompileException("in verifier: ", e);
                    }
                }

            }
            else if (ctx.getClass() == Class.class) {
                for (Method m : ctx.getMethods()) {
                    if (property.equals(m.getName())) {
                        return m.getReturnType();
                    }
                }
            }

            if (parserContext.isStrictTypeEnforcement()) {
                throw new PropertyAccessException("unqualified type in strict-mode: ('" + property + "')");
            }
            else {
                return Object.class;
            }
        }


    }

    private Class getCollectionProperty(Class ctx, String prop) {
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


        ExpressionCompiler compiler = new ExpressionCompiler(item);
        compiler.compile();

        ++cursor;

        return compiler.getReturnType();
    }


    private Class getMethod(Class ctx, String name) {
//        if (first && variableFactory.isResolveable(name)) {
//            Method m = (Method) variableFactory.getVariableResolver(name).getValue();
//            ctx = m.getDeclaringClass();
//            name = m.getName();
//            first = false;
//        }


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

        ExpressionCompiler verifCompiler;
        if (tk.length() > 0) {
            String[] subtokens = parseParameterList(tk.toCharArray(), 0, -1);
            for (String token : subtokens) {
                verifCompiler = new ExpressionCompiler(token);
                verifCompiler.compile();

                inputs.addAll(verifCompiler.getInputs());
            }
        }

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
                ExpressionCompiler compiler = new ExpressionCompiler(subtokens[i]);
                compiler.setVerifying(true);
                ExecutableStatement stmt = compiler.compile();

                es[i] = stmt;
                args[i] = compiler.getReturnType();
            }
        }

        /**
         * If the target object is an instance of java.lang.Class itself then do not
         * adjust the Class scope target.
         */

        //    Integer signature = ;

        Method m;
        Class[] parameterTypes = null;

        /**
         * If we have not cached the method then we need to go ahead and try to resolve it.
         */
        /**
         * Try to find an instance method from the class target.
         */

        if ((m = ParseTools.getBestCandidate(args, name, ctx.getMethods())) != null) {
            parameterTypes = m.getParameterTypes();
        }

        if (m == null) {
            /**
             * If we didn't find anything, maybe we're looking for the actual java.lang.Class methods.
             */
            if ((m = ParseTools.getBestCandidate(args, name, ctx.getDeclaredMethods())) != null) {
                parameterTypes = m.getParameterTypes();
            }
        }


        if (m == null) {
            StringAppender errorBuild = new StringAppender();
            for (int i = 0; i < args.length; i++) {
                errorBuild.append(args[i] != null ? args[i].getClass().getName() : null);
                if (i < args.length - 1) errorBuild.append(", ");
            }

            if ("size".equals(name) && args.length == 0 && ctx.isArray()) {
                return Integer.class;
            }

            if (parserContext.isStrictTypeEnforcement()) {
                throw new PropertyAccessException("unable to resolve method: " + ctx.getName() + "." + name + "(" + errorBuild.toString() + ") [arglength=" + args.length + "]");
            }
            else {
                return Object.class;
            }
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

            /**
             * Invoke the target method and return the response.
             */
            return m.getReturnType();
        }

        // HERE


    }
}
