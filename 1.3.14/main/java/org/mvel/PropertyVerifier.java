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

import org.mvel.optimizers.AbstractOptimizer;
import org.mvel.util.ParseTools;
import static org.mvel.util.ParseTools.getBestCandidate;
import static org.mvel.util.ParseTools.parseParameterList;
import org.mvel.util.PropertyTools;
import static org.mvel.util.PropertyTools.getSubComponentType;
import org.mvel.util.StringAppender;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

public class PropertyVerifier extends AbstractOptimizer {
    private static final int DONE = -1;
    private static final int NORM = 0;
    private static final int METH = 1;
    private static final int COL = 2;

    private ParserContext parserContext;
    private List<String> inputs = new LinkedList<String>();
    private boolean first = true;
    private boolean resolvedExternally;


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
                return parserContext.getVarOrInputType(property);
            }
            else if (parserContext.hasImport(property)) {
                resolvedExternally = false;
                return parserContext.getImport(property);
            }
            else {
                return Object.class;
            }
        }

        start = cursor;

        Member member = ctx != null ? PropertyTools.getFieldOrAccessor(ctx, property) : null;

        if (member instanceof Field) {
            return ((Field) member).getType();
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
        //     if (first) {
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
        //     }

        //   int start = ++cursor;

        ++cursor;

        whiteSpaceSkip();

        if (cursor == length)
            throw new PropertyAccessException("unterminated '['");

        if (!scanTo(']')) {
            addFatalError("unterminated [ in token");
        }

//        ExpressionCompiler compiler = new ExpressionCompiler(new String(expr, start, cursor - start));
//        compiler._compile();

        ++cursor;

        return ctx;
    }


    private Class getMethod(Class ctx, String name) {
        if (first && parserContext.hasImport(name)) {
            Method m = parserContext.getStaticImport(name).getMethod();
            ctx = m.getDeclaringClass();
            name = m.getName();
            first = false;
        }

        int st = cursor;

        String tk = ((cursor = ParseTools.balancedCapture(expr, cursor, '(')) - st) > 1 ? new String(expr, st + 1, cursor - st - 1) : "";

        cursor++;

        if (tk.length() > 0) {
            for (String token : parseParameterList(tk.toCharArray(), 0, -1)) {
                new ExpressionCompiler(token)._compile();
            }
        }

        Class[] args;

        if (tk.length() == 0) {
            args = new Class[0];
        }
        else {
            String[] subtokens = parseParameterList(tk.toCharArray(), 0, -1);
            args = new Class[subtokens.length];
            for (int i = 0; i < subtokens.length; i++) {
                ExpressionCompiler compiler = new ExpressionCompiler(subtokens[i], true);
                compiler._compile();
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
        /**
         * Try to find an instance method from the class target.
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

        return m.getReturnType();
    }


    public boolean isResolvedExternally() {
        return resolvedExternally;
    }
}
