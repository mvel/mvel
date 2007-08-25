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
import org.mvel.optimizers.impl.refl.FieldAccessor;
import static org.mvel.util.ParseTools.getBestCandidate;
import static org.mvel.util.ParseTools.parseParameterList;
import org.mvel.util.PropertyTools;
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
                    ctx = getCollectionProperty();
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

    private Class getCollectionProperty() {

        int start = ++cursor;

        whiteSpaceSkip();

        if (cursor == length)
            throw new PropertyAccessException("unterminated '['");

        if (!scanTo(']')) {
            addFatalError("unterminated [ in token");
        }

        ExpressionCompiler compiler = new ExpressionCompiler(new String(expr, start, cursor - start));
        compiler._compile();

        ++cursor;

        return compiler.getReturnType() == null ? Object.class : compiler.getReturnType();
    }


    private Class getMethod(Class ctx, String name) {
        if (first && parserContext.hasImport(name)) {
            Method m = parserContext.getStaticImport(name);
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

        ExpressionCompiler verifCompiler;
        if (tk.length() > 0) {
            String[] subtokens = parseParameterList(tk.toCharArray(), 0, -1);
            for (String token : subtokens) {
                verifCompiler = new ExpressionCompiler(token);
                verifCompiler._compile();
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
                ExpressionCompiler compiler = new ExpressionCompiler(subtokens[i]);
                compiler.setVerifying(true);
                compiler._compile();
                args[i] = compiler.getReturnType();
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

        if ((m = getBestCandidate(args, name, ctx.getMethods())) == null) {
            if ((m = getBestCandidate(args, name, ctx.getDeclaredMethods())) == null) {
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
}
