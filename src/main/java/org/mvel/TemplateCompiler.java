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

import static org.mvel.NodeType.*;
import static org.mvel.util.ParseTools.*;
import org.mvel.util.StringAppender;

import static java.lang.Boolean.getBoolean;
import static java.lang.Character.isWhitespace;
import static java.lang.String.copyValueOf;
import static java.lang.System.arraycopy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TemplateCompiler {
    private static final boolean DEFAULT_DEBUG = getBoolean("mvflex.expression.debug");

    private char[] expressionArray;
    private int length = 0;
    private int cursor = 0;
    private boolean debug = DEFAULT_DEBUG;
    private int maxDepth = 10;

    public TemplateCompiler(Interpreter interpreter) {
        this.expressionArray = interpreter.getExpression();
        this.length = expressionArray.length;
    }

    public Node[] compileExpression() {
        Node[] expressions;
        List<Node> aList = new ArrayList<Node>(10);

        int depth = 0, literalRange = 0, length = expressionArray.length;

        char[] exStr;

        String token;

        Node ex;
        int node = 0;
        for (; cursor < length; cursor++) {
            if ((expressionArray[cursor] == '$' || expressionArray[cursor] == '@')) {
                if (literalRange != 0) {
                    aList.add(new Node(node++, LITERAL, cursor - literalRange, literalRange, node));
                    literalRange = 0;
                }

                ex = new Node(cursor);

                if ((token = captureTo('{')) == null) {
                    literalRange++;
                    continue;
                }

                exStr = structuredCaptureArray(1);

                if (token.length() != 0) {
                    if ("if".equals(token)) {
                        depth++;
                        ex.setToken(IF);
                    }
                    else if ("elseif".equals(token)) {
                        aList.add(new Node(node++, GOTO, -1));
                        ex.setToken(ELSEIF);
                    }
                    else if ("else".equals(token)) {
                        aList.add(new Node(node++, GOTO, -1));
                        ex.setToken(ELSE);
                    }
                    else if ("foreach".equals(token)) {
                        depth++;
                        ex.setToken(FOREACH);
                    }
                    else if ("end".equals(token)) {
                        depth--;
                        ex.setToken(END);
                        ex.setRegister(exStr);
                    }
                    else if ("includeByRef".equals(token)) {
                        ex.setToken(INCLUDE_BY_REF);
                        ex.setRegister(buildIncludeRef(exStr));
                        System.out.println(ex.getRegister());
                    }
                    else {
                        throw new CompileException("unknown token: " + token);
                    }
                }

                if (ex.getNodeType() == FOREACH) {
                    ex.setAlias("item");
                    ex.setName(new String(exStr));

                    int capture = -1;
                    for (int i = 0; i < exStr.length; i++) {
                        switch (exStr[i]) {
                            case' ':
                                if (capture == -1 && ((i + 3) < exStr.length)
                                        && exStr[i + 1] == 'a' && exStr[i + 2] == 's'
                                        && exStr[i + 3] == ' ') {

                                    ex.setName(new String(exStr, 0, i));

                                    capture = i += 4;

                                    /**
                                     * Scan to skip any excess whitespace.
                                     */
                                    //noinspection StatementWithEmptyBody
                                    while (i < exStr.length && !isWhitespace(exStr[i++])) ;

                                    /**
                                     * Scan a second time to capture the token.
                                     */
                                    //noinspection StatementWithEmptyBody
                                    while (i < exStr.length && !isWhitespace(exStr[i++])) ;

                                    ex.setAlias(copyValueOf(exStr, capture, i - capture));

                                    /**
                                     * Scan to skip any excess whitespace.
                                     */
                                    //noinspection StatementWithEmptyBody
                                    while (i < exStr.length && !isWhitespace(exStr[i++])) ;
                                }
                                break;
                        }
                    }
                }
                else
                    ex.setEndPos(cursor);

                ex.setLength((cursor + 1) - ex.getStartPos());

                if (depth > maxDepth) maxDepth = depth;

                aList.add(ex.setNode(node++));
            }
            else {
                literalRange++;
            }
        }
        if (literalRange != 0) {
            aList.add(new Node(node++, LITERAL, cursor - literalRange, literalRange, node));
        }

        if (depth == 1 && aList.size() == 1
                && aList.get(0).getStartPos() == 0
                && aList.get(0).getEndPos() == length) {

        }
        else if (depth > 0) {
            throw new CompileException("unbalanced operators: expected $end{}");
        }
        else if (depth < 0) {
            throw new CompileException("unexpected $end{} encountered");
        }

        aList.add(new Node(node, TERMINUS));

        arraycopy(aList.toArray(), 0, expressions = new Node[aList.size()], 0, expressions.length);
        ArrayList<Node> stk = new ArrayList<Node>(10);

        for (int i = 0; i < expressions.length; i++) {
            switch (expressions[i].getToken()) {
                case GOTO:
                case LITERAL:
                case PROPERTY_EX:
                case ELSE:
                case ELSEIF:
                case INCLUDE_BY_REF:
                    break;

                case END:
                    Node e = stk.remove(stk.size() - 1);
                    e.setEndNode(i);

                    int last = -1;
                    if (e.getToken() == IF) {
                        for (int x = i; x >= e.getNode(); x--) {
                            switch (expressions[x].getToken()) {
                                case GOTO:
                                    expressions[x].setEndNode(i + 1);
                                    break;

                                case IF:
                                case ELSEIF:
                                case ELSE:
                                    if (last == -1) {
                                        expressions[x].setEndNode(i);
                                    }
                                    else {
                                        expressions[x].setEndNode(last);
                                    }
                                    last = x;
                                    break;
                            }
                        }
                    }
                    else if (e.getToken() == FOREACH) {
                        char[] props = (char[]) expressions[i].getRegister();
                        if (props != null && props.length > 0) {
                            int j = 0;
                            // skip white space
                            while (j < props.length && isWhitespace(props[j])) {
                                j++;
                            }
                            ;
                            if (props[j] != '"') { //&& props[i+2] !='\"') {
                                throw new CompileException("seperator is not correctly specified \"" + props + "\"");
                            }

                            int k = props.length - 1;
                            while (k < props.length && props[k] != '"') {
                                k--;
                            }
                            ;
                            if (props[k] != '"') { //&& props[i+2] !='\"') {
                                throw new CompileException("seperator is not correctly specified \"" + props + "\"");
                            }

                            e.setRegister(new ForeachContext(new String(props, j + 1, k - j - 1)));
                        }
                        else {
                            e.setRegister(new ForeachContext(""));
                        }
                    }

                    break;
                default:
                    stk.add(expressions[i]);
            }
        }


        if (debug) {
            System.out.println("Expression:\n");
            System.out.println(copyValueOf(expressionArray));
            System.out.println("\n------------------------------------------");

            System.out.println("Outputting Expression Tree");
            System.out.println("--------------------------------------------");

            depth = 0;
            for (Node e : expressions) {
                switch (e.getToken()) {
                    case END:
                        depth--;
                }

                System.out.println(indent(depth) + " + Node (" + e.getNode() + ") [" + getNodeTypeName(e.getNodeType()) + "] " + e.getToken()
                        + " {" + e.getStartPos() + "," + e.getEndPos() + "} --> " + e.getEndNode());

                switch (e.getToken()) {
                    case IF:
                    case FOREACH:
                        depth++;
                        break;
                }

            }

            System.out.println("--------------------------------------------");

        }

        return expressions;
    }

    private static String getNodeTypeName(int node) {
        switch (node) {
            case NodeType.ELSE:
                return "ELSE";
            case NodeType.ELSEIF:
                return "ELSE_IF";
            case NodeType.END:
                return "END";
            case NodeType.FOREACH:
                return "FOREACH";
            case NodeType.GOTO:
                return "GOTO";
            case NodeType.IF:
                return "IF";
            case NodeType.INCLUDE_BY_REF:
                return "INCLUDE_BY_REF";
            case NodeType.LITERAL:
                return "LITERAL";
            case NodeType.OPERATOR:
                return "OPERATOR";
            case NodeType.PROPERTY_EX:
                return "EXPRESSION";
            case NodeType.TERMINUS:
                return "TERMINUS";
        }
        return "UNKNOWN";
    }

    private IncludeRef buildIncludeRef(char[] text) {
        int i = 0;

        // skip leading white spaces
        while (isWhitespace(text[i])) i++;

        int start = i;

        while (text[i] != '(') i++;

        int end = i;

        String name = new String(text, start, end - start);

        Map<String, String> parmVars = parseParameters(subset(text, end, balancedCapture(text, i, ')') - end));
        List<IncludeRefParam> params = new ArrayList<IncludeRefParam>();

        for (String k : parmVars.keySet()) {
            params.add(new IncludeRefParam(k, parmVars.get(k)));
        }

        return new IncludeRef(name, params.toArray(new IncludeRefParam[params.size()]));
    }

    public static class IncludeRef {
        private String name;
        private IncludeRefParam[] params;

        public IncludeRef(String name,
                          IncludeRefParam[] params) {
            this.name = name;
            this.params = params;
        }

        public String getName() {
            return name;
        }

        public IncludeRefParam[] getParams() {
            return params;
        }
    }

    public static class IncludeRefParam {
        private String identifier;
        private String value;

        public IncludeRefParam(String identifier, String value) {
            this.identifier = identifier;
            this.value = value;
        }

        public String getIdentifier() {
            return identifier;
        }

        public String getValue() {
            return value;
        }
    }

    private static String indent(final int depth) {
        final StringAppender sb = new StringAppender();
        for (int i = depth; i >= 0; i--) {
            sb.append("    ");
        }
        return sb.toString();
    }

    private String captureTo(char c) {
        int start = cursor + 1;
        if (lookahead(c)) {
            return new String(expressionArray, start, cursor - start);
        }
        else
            return null;

    }

    private boolean lookahead(char c) {
        int start = cursor;
        for (; cursor < length; cursor++) {
            if (expressionArray[cursor] == c) {
                return true;
            }
        }
        cursor = start;
        return false;
    }

    /**
     * @return array
     */
    private char[] structuredCaptureArray(int depth) {
        int start = cursor++ + 1;
        // int depth = 1;

        //  cursor++;
        while (cursor < (length) && depth != 0) {
            switch (expressionArray[cursor++]) {
                case'@':
                case'$':
                    if (expressionArray[cursor] == '{') {
                        cursor++;
                        depth++;
                    }
                    break;
                case'}':
                    depth--;
                    break;
                case'{':
                    depth++;
                    break;
            }
        }

        if (depth > 0) {
            throw new CompileException("unbalanced braces near: " + showCodeNearError() + " (" + depth + ")");
        }

        char[] array = new char[--cursor - start];
        arraycopy(expressionArray, start, array, 0, cursor - start);

        return array;
    }

    private CharSequence showCodeNearError() {
        int start = cursor - 10;
        int end = (cursor + 20);

        if (start < 0) {
            start = 0;
        }
        if (end > length) {
            end = length - 1;
        }
        return "'" + copyValueOf(expressionArray, start, end - start) + "'";
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

}
