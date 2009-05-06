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

import static java.lang.Character.isJavaIdentifierPart;
import static java.lang.Character.isWhitespace;
import java.util.LinkedList;
import java.util.List;

public class PropertyVerifier {
    private int start = 0;
    private int cursor = 0;

    private char[] property;
    private int length;

    private static final int DONE = -1;
    private static final int NORM = 0;
    private static final int METH = 1;
    private static final int COL = 2;


    public PropertyVerifier(char[] property) {
        this.property = property;

        this.length = property.length;
    }

    public PropertyVerifier(String property) {
        this.length = (this.property = property.toCharArray()).length;
    }

    private List<String> inputs = new LinkedList<String>();


    public List<String> getInputs() {
        return inputs;
    }

    public void setInputs(List<String> inputs) {
        this.inputs = inputs;
    }

    public void analyze() {
        while (cursor < length) {
            switch (nextToken()) {
                case NORM:
                    getBeanProperty(capture());
                    break;
                case METH:
                    getMethod(capture());
                    break;
                case COL:
                    getCollectionProperty(capture());
                    break;
                case DONE:
                    break;
            }
        }


    }

    private int nextToken() {
        switch (property[start = cursor]) {
            case'[':
                return COL;
            case'.':
                cursor = ++start;
        }

        //noinspection StatementWithEmptyBody
        while (++cursor < length && isJavaIdentifierPart(property[cursor])) ;


        if (cursor < length) {
            switch (property[cursor]) {
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
        return new String(property, start, cursor - start);
    }


    private void getBeanProperty(String property) {
    }

    private void whiteSpaceSkip() {
        if (cursor < length)
            //noinspection StatementWithEmptyBody
            while (isWhitespace(property[cursor]) && ++cursor < length) ;
    }

    private boolean scanTo(char c) {
        for (; cursor < length; cursor++) {
            if (property[cursor] == c) {
                return true;
            }
        }
        return false;
    }

    private int containsStringLiteralTermination() {
        int pos = cursor;
        for (pos--; pos > 0; pos--) {
            if (property[pos] == '\'' || property[pos] == '"') return pos;
            else if (!isWhitespace(property[pos])) return pos;
        }
        return -1;
    }


    private void getCollectionProperty(String prop) {

        if (prop.length() > 0) getBeanProperty(prop);

        int start = ++cursor;

        whiteSpaceSkip();

        if (cursor == length)
            throw new PropertyAccessException("unterminated '['");

        String item;

        if (property[cursor] == '\'' || property[cursor] == '"') {
            start++;

            int end;

            if (!scanTo(']'))
                throw new PropertyAccessException("unterminated '['");

            if ((end = containsStringLiteralTermination()) == -1)
                throw new PropertyAccessException("unterminated string literal in collections accessor");

            item = new String(property, start, end - start);
        }
        else {
            if (!scanTo(']'))
                throw new PropertyAccessException("unterminated '['");

            item = new String(property, start, cursor - start);
        }


        ExpressionCompiler compiler = new ExpressionCompiler(item);
        compiler.compile();

        ++cursor;
    }


    private void getMethod(String name) {

        int st = cursor;

        int depth = 1;

        while (cursor++ < length - 1 && depth != 0) {
            switch (property[cursor]) {
                case'(':
                    depth++;
                    continue;
                case')':
                    depth--;

            }
        }
        cursor--;

        String tk = (cursor - st) > 1 ? new String(property, st + 1, cursor - st - 1) : "";


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
    }
}
