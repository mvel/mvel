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
package org.mvel2.optimizers;

import org.mvel2.CompileException;
import org.mvel2.compiler.AbstractParser;
import static org.mvel2.util.ParseTools.*;

import static java.lang.Thread.currentThread;
import java.lang.reflect.Method;

/**
 * @author Christopher Brock
 */
public class AbstractOptimizer extends AbstractParser {
    protected static final int BEAN = 0;
    protected static final int METH = 1;
    protected static final int COL = 2;
    protected static final int WITH = 3;

    protected int start = 0;
    protected boolean collection = false;
    protected boolean nullSafe = false;
    protected Class currType = null;


    /**
     * Try static access of the property, and return an instance of the Field, Method of Class if successful.
     *
     * @return - Field, Method or Class instance.
     */
    protected Object tryStaticAccess() {
        int begin = cursor;
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
            int last = length;
            for (int i = length - 1; i > 0; i--) {
                switch (expr[i]) {
                    case '.':
                        if (!meth) {
                            try {
                                return Class.forName(new String(expr, 0, cursor = last), true, currentThread().getContextClassLoader());
                            }
                            catch (ClassNotFoundException e) {
                                Class cls = Class.forName(new String(expr, 0, i), true, currentThread().getContextClassLoader());
                                String name = new String(expr, i + 1, expr.length - i - 1);
                                try {
                                    return cls.getField(name);
                                }
                                catch (NoSuchFieldException nfe) {
                                    for (Method m : cls.getMethods()) {
                                        if (name.equals(m.getName())) return m;
                                    }
                                    return null;
                                }
                            }
                        }

                        meth = false;
                        last = i;
                        break;

                    case '}':
                        i--;
                        for (int d = 1; i > 0 && d != 0; i--) {
                            switch (expr[i]) {
                                case '}':
                                    d++;
                                    break;
                                case '{':
                                    d--;
                                    break;
                                case '"':
                                case '\'':
                                    char s = expr[i];
                                    while (i > 0 && (expr[i] != s && expr[i - 1] != '\\')) i--;
                            }
                        }
                        break;

                    case ')':
                        i--;

                        for (int d = 1; i > 0 && d != 0; i--) {
                            switch (expr[i]) {
                                case ')':
                                    d++;
                                    break;
                                case '(':
                                    d--;
                                    break;
                                case '"':
                                case '\'':
                                    char s = expr[i];
                                    while (i > 0 && (expr[i] != s && expr[i - 1] != '\\')) i--;
                            }
                        }

                        meth = true;
                        last = i++;
                        break;


                    case '\'':
                        while (--i > 0) {
                            if (expr[i] == '\'' && expr[i - 1] != '\\') {
                                break;
                            }
                        }
                        break;

                    case '"':
                        while (--i > 0) {
                            if (expr[i] == '"' && expr[i - 1] != '\\') {
                                break;
                            }
                        }
                        break;
                }
            }
        }
        catch (Exception cnfe) {
            cursor = begin;
        }

        return null;
    }

    protected int nextSubToken() {

        skipWhitespaceWithLineAccounting();
        nullSafe = false;

        switch (expr[start = cursor]) {
            case '[':
                return COL;
            case '.':
                if ((start + 1) != length) {
                    switch (expr[cursor = ++start]) {
                        case '?':
                            skipWhitespaceWithLineAccounting();
                            if ((cursor = ++start) == length) {
                                throw new CompileException("unexpected end of statement");
                            }
                            nullSafe = true;

                            fields = -1;
                            break;
                        case '{':
                            return WITH;
                        default:
                            if (isWhitespace(expr[start])) {
                                skipWhitespaceWithLineAccounting();
                                start = cursor;
                            }
                    }
                }
                else {
                    throw new CompileException("unexpected end of statement");
                }
                break;
        }

        //noinspection StatementWithEmptyBody
        while (++cursor < length && isIdentifierPart(expr[cursor])) ;

        if (cursor < length) {
            skipWhitespaceWithLineAccounting();
            switch (expr[cursor]) {
                case '[':
                    return COL;
                case '(':
                    return METH;
                default:
                    return BEAN;
            }
        }

        return 0;
    }

    protected String capture() {
        /**
         * Trim off any whitespace.
         */
        return new String(expr, start = trimRight(start), trimLeft(cursor) - start);
    }

    /**
     * Skip to the next non-whitespace position.
     */
    protected void whiteSpaceSkip() {
        if (cursor < length)
            //noinspection StatementWithEmptyBody
            while (isWhitespace(expr[cursor]) && ++cursor != length) ;
    }

    /**
     * @param c - character to scan to.
     * @return - returns true is end of statement is hit, false if the scan scar is countered.
     */
    protected boolean scanTo(char c) {
        for (; cursor < length; cursor++) {
            switch (expr[cursor]) {
                case '\'':
                case '"':
                    cursor = captureStringLiteral(expr[cursor], expr, cursor, expr.length);
                default:
                    if (expr[cursor] == c) {
                        return false;
                    }
            }

        }
        return true;
    }


    protected int findLastUnion() {
        int split = -1;
        int depth = 0;

        for (int i = expr.length - 1; i != 0; i--) {
            switch (expr[i]) {
                case '}':
                case ']':
                    depth++;
                    break;

                case '{':
                case '[':
                    if (--depth == 0) {
                        split = i;
                        collection = true;
                    }
                    break;
                case '.':
                    if (depth == 0) {
                        split = i;
                    }
                    break;
            }
            if (split != -1) break;
        }

        return split;
    }


}
