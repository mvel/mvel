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

import org.mvel.*;
import static org.mvel.Operator.*;
import org.mvel.ast.*;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ArrayTools.findFirst;
import org.mvel.util.ExecutionStack;
import static org.mvel.util.ParseTools.*;
import static org.mvel.util.PropertyTools.*;
import org.mvel.util.Stack;
import org.mvel.util.StringAppender;

import java.io.Serializable;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Float.parseFloat;
import static java.lang.Runtime.getRuntime;
import static java.lang.System.getProperty;
import static java.lang.Thread.currentThread;
import static java.util.Collections.synchronizedMap;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author Christopher Brock
 */
public class AbstractParser implements Serializable {
    protected char[] expr;
    protected int cursor;
    protected int length;
    protected int fields;

    protected boolean greedy = true;
    protected boolean lastWasIdentifier = false;
    protected boolean lastWasLineLabel = false;
    protected boolean lastWasComment = false;
    protected boolean literalOnly = true;

    protected boolean debugSymbols = false;

    protected int lastLineStart = -1;
    protected int line = 1;

    protected ASTNode lastNode;

    private static Map<String, char[]> EX_PRECACHE;

    public static final Map<String, Object> LITERALS =
            new HashMap<String, Object>(35 * 2, 0.4f);

    public static final Map<String, Integer> OPERATORS =
            new HashMap<String, Integer>(25 * 2, 0.4f);

    protected Stack stk;
    protected ExecutionStack splitAccumulator = new ExecutionStack();

    protected static ThreadLocal<ParserContext> parserContext;
    protected ParserContext pCtx;
    protected ExecutionStack dStack;
    protected Object ctx;
    protected VariableResolverFactory variableFactory;

    static {
        configureFactory();

        /**
         * Setup the basic literals
         */
        AbstractParser.LITERALS.put("true", TRUE);
        AbstractParser.LITERALS.put("false", FALSE);

        AbstractParser.LITERALS.put("null", null);
        AbstractParser.LITERALS.put("nil", null);

        AbstractParser.LITERALS.put("empty", BlankLiteral.INSTANCE);

        /**
         * Add System and all the class wrappers from the JCL.
         */
        LITERALS.put("System", System.class);

        LITERALS.put("String", String.class);

        LITERALS.put("Integer", Integer.class);
        LITERALS.put("int", Integer.class);

        LITERALS.put("Long", Long.class);
        LITERALS.put("long", Long.class);

        LITERALS.put("Boolean", Boolean.class);
        LITERALS.put("boolean", Boolean.class);

        LITERALS.put("Short", Short.class);
        LITERALS.put("short", Short.class);

        LITERALS.put("Character", Character.class);
        LITERALS.put("char", Character.class);

        LITERALS.put("Double", Double.class);
        LITERALS.put("double", double.class);

        LITERALS.put("Float", Float.class);
        LITERALS.put("float", float.class);

        LITERALS.put("Math", Math.class);
        LITERALS.put("Void", Void.class);
        LITERALS.put("Object", Object.class);

        LITERALS.put("Class", Class.class);
        LITERALS.put("ClassLoader", ClassLoader.class);
        LITERALS.put("Runtime", Runtime.class);
        LITERALS.put("Thread", Thread.class);
        LITERALS.put("Compiler", Compiler.class);
        LITERALS.put("StringBuffer", StringBuffer.class);
        LITERALS.put("ThreadLocal", ThreadLocal.class);
        LITERALS.put("SecurityManager", SecurityManager.class);
        LITERALS.put("StrictMath", StrictMath.class);

        LITERALS.put("Array", java.lang.reflect.Array.class);

        if (parseFloat(getProperty("java.version").substring(0, 2)) >= 1.5) {
            try {
                LITERALS.put("StringBuilder", currentThread().getContextClassLoader().loadClass("java.lang.StringBuilder"));
            }
            catch (Exception e) {
                throw new RuntimeException("cannot resolve a built-in literal", e);
            }
        }

        //LITERALS.putAll(Units.MEASUREMENTS_ALL);

        //loadLanguageFeaturesByLevel(5);
        setLanguageLevel(5);
    }


    public static void configureFactory() {
        if (MVEL.THREAD_SAFE) {
            EX_PRECACHE = synchronizedMap(new WeakHashMap<String, char[]>(10));
        }
        else {
            EX_PRECACHE = new WeakHashMap<String, char[]>(10);
        }
    }


    protected ASTNode nextTokenSkipSymbols() {
        ASTNode n = nextToken();
        if (n != null && n.getFields() == -1) n = nextToken();
        return n;
    }

    /**
     * Retrieve the next token in the expression.
     *
     * @return -
     */
    protected ASTNode nextToken() {
        try {
            /**
             * If the cursor is at the end of the expression, we have nothing more to do:
             * return null.
             */
            if (cursor >= length) {
                return null;
            }
            else if (!splitAccumulator.isEmpty()) {
                return lastNode = (ASTNode) splitAccumulator.pop();
            }

            int brace, start = cursor, idx;

            /**
             * Because of parser recursion for sub-expression parsing, we sometimes need to remain
             * certain field states.  We do not reset for assignments, boolean mode, list creation or
             * a capture only mode.
             */
            fields = fields & (ASTNode.INLINE_COLLECTION | ASTNode.COMPILE_IMMEDIATE);

            boolean capture = false, union = false;

            pCtx = getParserContext();

            if (debugSymbols) {
                if (!lastWasLineLabel) {
                    if (pCtx.getSourceFile() == null) {
                        throw new CompileException("unable to produce debugging symbols: source name must be provided.");
                    }

                    line = pCtx.getLineCount();

                    skipWhitespaceWithLineAccounting();

                    if (!pCtx.isKnownLine(pCtx.getSourceFile(), pCtx.setLineCount(line)) && !pCtx.isBlockSymbols()) {
                        lastWasLineLabel = true;
                        pCtx.setLineAndOffset(line, cursor);
                        return lastNode = pCtx.setLastLineLabel(new LineLabel(pCtx.getSourceFile(), line));
                    }
                }
                else {
                    lastWasComment = lastWasLineLabel = false;
                }
            }

            /**
             * Skip any whitespace currently under the starting point.
             */
            while (start != length && isWhitespace(expr[start])) start++;

            /**
             * From here to the end of the method is the core MVEL parsing code.  Fiddling around here is asking for
             * trouble unless you really know what you're doing.
             */
            for (cursor = start; cursor != length;) {
                if (isIdentifierPart(expr[cursor])) {
                    /**
                     * If the current character under the cursor is a valid
                     * part of an identifier, we keep capturing.
                     */
                    capture = true;
                    cursor++;
                }
                else if (capture) {
                    String t;
                    if (OPERATORS.containsKey(t = new String(expr, start, cursor - start))) {
                        switch (OPERATORS.get(t)) {
                            case NEW:
                                start = cursor = trimRight(cursor);
                                captureToEOT();
                                return lastNode = new NewObjectNode(subArray(start, cursor), fields);

                            case ASSERT:
                                start = cursor = trimRight(cursor);
                                captureToEOS();
                                return lastNode = new AssertNode(subArray(start, cursor--), fields);

                            case RETURN:
                                start = cursor = trimRight(cursor);
                                captureToEOS();
                                return lastNode = new ReturnNode(subArray(start, cursor), fields);

                            case IF:
                                return captureCodeBlock(ASTNode.BLOCK_IF);

                            case FOREACH:
                                return captureCodeBlock(ASTNode.BLOCK_FOREACH);

                            case WHILE:
                                return captureCodeBlock(ASTNode.BLOCK_WHILE);

                            case WITH:
                                return captureCodeBlock(ASTNode.BLOCK_WITH);

                            case IMPORT:
                                start = cursor = trimRight(cursor);
                                captureToEOS();
                                ImportNode importNode = new ImportNode(subArray(start, cursor--));
                                if (importNode.isPackageImport()) {
                                    pCtx.addPackageImport(importNode.getPackageImport());
                                    cursor++;
                                }
                                else {
                                    pCtx.addImport(getSimpleClassName(importNode.getImportClass()), importNode.getImportClass());
                                }
                                return importNode;

                            case IMPORT_STATIC:
                                start = cursor = trimRight(cursor);
                                captureToEOS();
                                return lastNode = new StaticImportNode(subArray(start, cursor--));

                            case FUNCTION:
                                Function function = (Function) captureCodeBlock(FUNCTION);
                                capture = false;
                                start = cursor + 1;
                                return function;

                            case UNTYPED_VAR:
                                start = cursor + 1;
                                captureToEOT();
                                int end = cursor;

                                skipWhitespace();
                                if (expr[cursor] == '=') {
                                    cursor = start;
                                    continue;
                                }
                                else {
                                    String name = new String(subArray(start, end));
                                    if ((idx = pCtx.variableIndexOf(name)) != -1) {
                                        return lastNode = new IndexedDeclTypedVarNode(idx, Object.class);
                                    }
                                    else {
                                        return lastNode = new DeclTypedVarNode(name, Object.class, fields);
                                    }
                                }
                        }
                    }


                    skipWhitespace();

                    /**
                     * If we *were* capturing a token, and we just hit a non-identifier
                     * character, we stop and figure out what to do.
                     */
                    if (cursor != length && expr[cursor] == '(') {
                        cursor = balancedCapture(expr, cursor, '(') + 1;
                    }

                    /**
                     * If we encounter any of the following cases, we are still dealing with
                     * a contiguous token.
                     */
                    String name;
                    if (cursor != length) {
                        switch (expr[cursor]) {
                            case '?':
                                if (lookToLast() == '.') {
                                    capture = true;
                                    cursor++;
                                    continue;
                                }
                            case '+':
                                switch (lookAhead()) {
                                    case '+':
                                        if ((idx = pCtx.variableIndexOf(name = new String(subArray(start, cursor)))) != -1) {
                                            lastNode = new IndexedPostFixIncNode(idx);
                                        }
                                        else {
                                            lastNode = new PostFixIncNode(name);
                                        }

                                        cursor += 2;

                                        return lastNode;

                                    case '=':
                                        name = createStringTrimmed(expr, start, cursor - start);
                                        start = cursor += 2;
                                        captureToEOS();

                                        if (union) {
                                            return lastNode = new DeepAssignmentNode(subArray(start, cursor), fields, Operator.ADD, t);
                                        }
                                        else if ((idx = pCtx.variableIndexOf(name)) != -1) {
                                            return lastNode = new IndexedAssignmentNode(subArray(start, cursor), fields, Operator.ADD, name, idx);
                                        }
                                        else {
                                            return lastNode = new AssignmentNode(subArray(start, cursor), fields, Operator.ADD, name);
                                        }
                                }

                                break;

                            case '-':
                                switch (lookAhead()) {
                                    case '-':
                                        if ((idx = pCtx.variableIndexOf(name = new String(subArray(start, cursor)))) != -1) {
                                            lastNode = new IndexedPostFixDecNode(idx);
                                        }
                                        else {
                                            lastNode = new PostFixDecNode(name);
                                        }
                                        cursor += 2;

                                        return lastNode;

                                    case '=':
                                        name = new String(expr, start, trimLeft(cursor) - start);
                                        start = cursor += 2;

                                        captureToEOS();

                                        if ((idx = pCtx.variableIndexOf(name)) != -1) {
                                            return lastNode = new IndexedOperativeAssign(subArray(start, cursor), Operator.SUB, idx, fields);
                                        }
                                        else {
                                            return lastNode = new OperativeAssign(name, subArray(start, cursor), Operator.SUB, fields);
                                        }
                                }
                                break;

                            case '*':
                                if (isNext('=')) {
                                    name = new String(expr, start, trimLeft(cursor) - start);

                                    start = cursor += 2;
                                    captureToEOS();

                                    if ((idx = pCtx.variableIndexOf(name)) != -1) {
                                        return lastNode = new IndexedOperativeAssign(subArray(start, cursor), Operator.MULT, idx, fields);
                                    }
                                    else {
                                        return lastNode = new OperativeAssign(name, subArray(start, cursor), Operator.MULT, fields);
                                    }

                                }
                                break;

                            case '/':
                                if (isNext('=')) {
                                    name = new String(expr, start, trimLeft(cursor) - start);

                                    start = cursor += 2;
                                    captureToEOS();

                                    if ((idx = pCtx.variableIndexOf(name)) != -1) {
                                        return lastNode = new IndexedOperativeAssign(subArray(start, cursor), Operator.DIV, idx, fields);
                                    }
                                    else {
                                        return lastNode = new OperativeAssign(name, subArray(start, cursor), Operator.DIV, fields);
                                    }
                                }
                                break;

                            case ']':
                            case '[':
                                cursor = balancedCapture(expr, cursor, '[') + 1;
                                continue;
                            case '.':
                                union = true;
                                cursor++;
                                continue;

                            case '~':
                                if (isNext('=')) {
                                    char[] stmt = subArray(start, trimLeft(cursor));

                                    start = cursor += 2;
                                    skipWhitespace();

                                    return lastNode = new RegExMatch(stmt, fields, subArray(start, (cursor = balancedCapture(expr, cursor, expr[cursor]) + 1)));
                                }
                                break;

                            case '=':
                                if (isNext('+')) {
                                    name = new String(expr, start, trimLeft(cursor) - start);

                                    start = cursor += 2;
                                    captureToEOS();

                                    if ((idx = pCtx.variableIndexOf(name)) != -1) {
                                        return lastNode = new IndexedOperativeAssign(subArray(start, cursor), Operator.ADD, idx, fields);
                                    }
                                    else {
                                        return lastNode = new OperativeAssign(name, subArray(start, cursor), Operator.ADD, fields);
                                    }
                                }

                                if (greedy && !isNext('=')) {
                                    cursor++;

                                    captureToEOS();

                                    if (union) {
                                        return lastNode = new DeepAssignmentNode(subArray(start, cursor), fields | ASTNode.ASSIGN);
                                    }
                                    else if (lastWasIdentifier) {

                                        /**
                                         * Check for typing information.
                                         */
                                        if (lastNode.getLiteralValue() instanceof String) {
                                            if (pCtx.hasImport((String) lastNode.getLiteralValue())) {
                                                lastNode.setLiteralValue(pCtx.getImport((String) lastNode.getLiteralValue()));
                                                lastNode.setAsLiteral();
                                                lastNode.discard();
                                            }
                                            else if (stk != null && stk.peek() instanceof Class) {
                                                lastNode.setLiteralValue(stk.pop());
                                                lastNode.setAsLiteral();
                                                lastNode.discard();
                                            }
                                            else {
                                                try {
                                                    /**
                                                     *  take a stab in the dark and try and load the class
                                                     */
                                                    lastNode.setLiteralValue(createClass((String) lastNode.getLiteralValue()));
                                                    lastNode.setAsLiteral();
                                                    lastNode.discard();
                                                }
                                                catch (ClassNotFoundException e) {
                                                    /**
                                                     * Just fail through.
                                                     */
                                                }
                                            }
                                        }

                                        if (lastNode.isLiteral() && lastNode.getLiteralValue() instanceof Class) {
                                            lastNode.discard();

                                            captureToEOS();
                                            return new TypedVarNode(subArray(start, cursor), fields | ASTNode.ASSIGN, (Class)
                                                    lastNode.getLiteralValue());
                                        }

                                        throw new CompileException("unknown class or illegal statement: " + lastNode.getLiteralValue(), expr, cursor);
                                    }
                                    else
                                    if (pCtx != null && ((idx = pCtx.variableIndexOf(t)) != -1 || (pCtx.isIndexAllocation()))) {
                                        IndexedAssignmentNode ian = new IndexedAssignmentNode(subArray(start, cursor), ASTNode.ASSIGN, idx);

                                        if (idx == -1) {
                                            pCtx.addIndexedVariable(t = ian.getAssignmentVar());
                                            ian.setRegister(idx = pCtx.variableIndexOf(t));
                                        }
                                        return lastNode = ian;
                                    }
                                    else {
                                        return lastNode = new AssignmentNode(subArray(start, cursor), fields | ASTNode.ASSIGN);
                                    }
                                }
                        }
                    }

                    /**
                     * Produce the token.
                     */
                    trimWhitespace();

                    return createPropertyToken(start, cursor);
                }
                else {
                    String name;

                    switch (expr[cursor]) {
                        case '@': {
                            start++;
                            captureToEOT();

                            name = new String(expr, start, cursor - start);

                            if (pCtx.getInterceptors() == null || !pCtx.getInterceptors().
                                    containsKey(name)) {
                                throw new CompileException("reference to undefined interceptor: " + name, expr, cursor);
                            }

                            return lastNode = new InterceptorWrapper(pCtx.getInterceptors().get(name), nextToken());
                        }

                        case '=':
                            return createOperator(expr, start, (cursor += 2), fields);

                        case '-':
                            if (isNext('-')) {
                                start = cursor += 2;
                                captureToEOT();

                                if ((idx = pCtx.variableIndexOf(name = new String(subArray(start, cursor)))) != -1) {
                                    return lastNode = new IndexedPreFixDecNode(idx);
                                }
                                else {
                                    return lastNode = new PreFixDecNode(name);
                                }
                            }
                            else if ((cursor != 0 && !isWhitespace(lookBehind())) || !isDigit(lookAhead())) {
                                return createOperator(expr, start, cursor++ + 1, fields);
                            }
                            else if ((cursor - 1) != 0 || (!isDigit(lookBehind())) && isDigit(lookAhead())) {
                                cursor++;
                                break;
                            }

                        case '+':
                            if (isNext('+')) {
                                start = cursor += 2;
                                captureToEOT();

                                if ((idx = pCtx.variableIndexOf(name = new String(subArray(start, cursor)))) != -1) {
                                    return lastNode = new IndexedPreFixIncNode(idx);
                                }
                                else {
                                    return lastNode = new PreFixIncNode(name);
                                }
                            }
                            return createOperator(expr, start, cursor++ + 1, fields);

                        case '*':
                            if (isNext('*')) {
                                cursor++;
                            }
                            return createOperator(expr, start, cursor++ + 1, fields);

                        case ';':
                            cursor++;
                            lastWasIdentifier = false;
                            return lastNode = new EndOfStatement();

                        case '#':
                        case '/':
                            if (isNext(expr[cursor])) {
                                /**
                                 * Handle single line comments.
                                 */

                                captureToEOL();

                                line = pCtx.getLineCount();

                                skipWhitespaceWithLineAccounting();

                                if (lastNode instanceof LineLabel) {
                                    pCtx.getLastLineLabel().setLineNumber(line);
                                    pCtx.addKnownLine(line);
                                }

                                lastWasComment = true;

                                pCtx.setLineCount(line);

                                if ((start = cursor) >= length) return null;

                                continue;
                            }
                            else if (expr[cursor] == '/' && isNext('*')) {
                                /**
                                 * Handle multi-line comments.
                                 */
                                int len = length - 1;

                                /**
                                 * This probably seems highly redundant, but sub-compilations within the same
                                 * source will spawn a new compiler, and we need to sync this with the
                                 * parser context;
                                 */
                                line = pCtx.getLineCount();

                                while (true) {
                                    cursor++;
                                    /**
                                     * Since multi-line comments may cross lines, we must keep track of any line-break
                                     * we encounter.
                                     */
                                    skipWhitespaceWithLineAccounting();

                                    if (cursor == len) {
                                        throw new CompileException("unterminated block comment", expr, cursor);
                                    }
                                    if (expr[cursor] == '*' && isNext('/')) {
                                        if ((cursor += 2) >= length) return null;
                                        skipWhitespaceWithLineAccounting();
                                        start = cursor;
                                        break;
                                    }
                                }

                                pCtx.setLineCount(line);

                                if (lastNode instanceof LineLabel) {
                                    pCtx.getLastLineLabel().setLineNumber(line);
                                    pCtx.addKnownLine(line);
                                }

                                lastWasComment = true;

                                continue;
                            }

                        case '?':
                        case ':':
                        case '^':
                        case '%': {
                            return createOperator(expr, start, cursor++ + 1, fields);
                        }

                        case '(': {
                            cursor++;

                            boolean singleToken = true;
                            boolean lastWS = false;

                            skipWhitespace();
                            for (brace = 1; cursor != length && brace != 0; cursor++) {
                                switch (expr[cursor]) {
                                    case '(':
                                        brace++;
                                        break;
                                    case ')':
                                        brace--;
                                        break;
                                    case '\'':
                                        cursor = captureStringLiteral('\'', expr, cursor, length);
                                        break;
                                    case '"':
                                        cursor = captureStringLiteral('"', expr, cursor, length);
                                        break;

                                    case 'i':
                                        if (isNext('n') && isWhitespace(lookAhead(2)) && !isIdentifierPart(lookBehind())) {
                                            fields |= ASTNode.FOLD;
                                            for (int level = brace; cursor != length; cursor++) {
                                                switch (expr[cursor]) {
                                                    case '(':
                                                        brace++;
                                                        break;
                                                    case ')':
                                                        if (--brace != level) {
                                                            if (lookAhead() == '.') {
                                                                lastNode = createToken(expr, trimRight(start + 1), (start = cursor++), ASTNode.FOLD);
                                                                captureToEOT();
                                                                return lastNode = new Union(expr, trimRight(start + 2), cursor, fields, lastNode);
                                                            }
                                                            else {
                                                                return createToken(expr, trimRight(start + 1), cursor++, ASTNode.FOLD);
                                                            }
                                                        }
                                                        break;
                                                    case '\'':
                                                        cursor = captureStringLiteral('\'', expr, cursor, length);
                                                        break;
                                                    case '"':
                                                        cursor = captureStringLiteral('\'', expr, cursor, length);
                                                        break;
                                                }
                                            }
                                        }
                                        break;

                                    default:
                                        /**
                                         * Check to see if we should disqualify this current token as a potential
                                         * type-cast candidate.
                                         */
                                        if ((lastWS && expr[cursor] != '.') || !(isIdentifierPart(expr[cursor]) || expr[cursor] == '.')) {
                                            singleToken = false;
                                        }
                                        else if (isWhitespace(expr[cursor])) {
                                            lastWS = true;
                                            skipWhitespace();
                                            cursor--;
                                        }
                                }
                            }

                            if (brace != 0) {
                                throw new CompileException("unbalanced braces in expression: (" + brace + "):", expr, cursor);
                            }

                            char[] _subset = null;
                            if (singleToken) {
                                int st;

                                //String tokenStr = new String(_subset = subset(expr, st = trimRight(start + 1), trimLeft(cursor - 1) - st));

                                name = new String(_subset = subset(expr, st = trimRight(start + 1), trimLeft(cursor - 1) - st));

                                if (pCtx.hasImport(name)) {
                                    start = cursor;
                                    captureToEOS();
                                    return lastNode = new TypeCast(subset(expr, start, cursor - start), pCtx.getImport(name), fields);
                                }
                                else {
                                    int rewind = cursor;
                                    try {
                                        /**
                                         *
                                         *  take a stab in the dark and try and load the class
                                         */
                                        captureToEOS();
                                        return lastNode = new TypeCast(subset(expr, rewind, cursor - rewind), createClass(name), fields);

                                    }
                                    catch (ClassNotFoundException e) {
                                        /**
                                         * Just fail through.
                                         */
                                        cursor = rewind;
                                    }
                                }
                            }

                            if (_subset != null) {
                                return handleUnion(handleSubstatement(new Substatement(_subset, fields)));
                            }
                            else {
                                return handleUnion(handleSubstatement(new Substatement(subset(expr, start = trimRight(start + 1), trimLeft(cursor - 1) - start), fields)));
                            }
                        }

                        case '}':
                        case ']':
                        case ')': {
                            throw new ParseException("unbalanced braces", expr, cursor);
                        }

                        case '>': {
                            if (expr[cursor + 1] == '>') {
                                if (expr[cursor += 2] == '>') cursor++;
                                return createOperator(expr, start, cursor, fields);
                            }
                            else if (expr[cursor + 1] == '=') {
                                return createOperator(expr, start, cursor += 2, fields);
                            }
                            else {
                                return createOperator(expr, start, ++cursor, fields);
                            }
                        }

                        case '<': {
                            if (expr[++cursor] == '<') {
                                if (expr[++cursor] == '<') cursor++;
                                return createOperator(expr, start, cursor, fields);
                            }
                            else if (expr[cursor] == '=') {
                                return createOperator(expr, start, ++cursor, fields);
                            }
                            else {
                                return createOperator(expr, start, cursor, fields);
                            }
                        }

                        case '\'':
                        case '"':
                            lastNode = new LiteralNode(
                                    handleStringEscapes(
                                            subset(expr, start + 1, (cursor = captureStringLiteral(expr[cursor], expr, cursor, length)) - start - 1))
                                    , String.class);

                            cursor++;

                            if (tokenContinues()) {
                                return lastNode = handleUnion(lastNode);
                            }

                            return lastNode;

                        case '&': {
                            if (expr[cursor++ + 1] == '&') {
                                return createOperator(expr, start, ++cursor, fields);
                            }
                            else {
                                return createOperator(expr, start, cursor, fields);
                            }
                        }

                        case '|': {
                            if (expr[cursor++ + 1] == '|') {
                                return new OperatorNode(OPERATORS.get(new String(expr, start, ++cursor - start)));
                            }
                            else {
                                return createOperator(expr, start, cursor, fields);
                            }
                        }

                        case '~':
                            if ((cursor++ - 1 != 0 || !isIdentifierPart(lookBehind()))
                                    && isDigit(expr[cursor])) {
                                start = cursor;
                                captureToEOT();
                                return lastNode = new Invert(subset(expr, start, cursor - start), fields);
                            }
                            else if (expr[cursor] == '(') {
                                start = cursor--;
                                captureToEOT();
                                return lastNode = new Invert(subset(expr, start, cursor - start), fields);
                            }
                            else {
                                if (expr[cursor] == '=') cursor++;
                                return createOperator(expr, start, cursor, fields);
                            }

                        case '!': {
                            if (isIdentifierPart(expr[++cursor])) {
                                start = cursor;
                                captureToEOT();
                                return lastNode = new Negation(subset(expr, start, cursor - start), fields);
                            }
                            else if (expr[cursor] == '(') {
                                start = cursor--;
                                captureToEOT();
                                return lastNode = new Negation(subset(expr, start, cursor - start), fields);
                            }
                            else if (expr[cursor] != '=')
                                throw new CompileException("unexpected operator '!'", expr, cursor, null);
                            else {
                                return createOperator(expr, start, ++cursor, fields);
                                //   return new OperatorNode(OPERATORS.get("!="));
                            }
                        }

                        case '[':
                        case '{':
                            cursor = balancedCapture(expr, cursor, expr[cursor]) + 1;
                            if (tokenContinues()) {
                                //   if (lookAhead(1) == '.') {
                                lastNode = new InlineCollectionNode(expr, start, start = cursor, fields);
                                captureToEOT();
                                return lastNode = new Union(expr, start + 1, cursor, fields, lastNode);
                            }
                            else {
                                return lastNode = new InlineCollectionNode(expr, start, cursor, fields);
                            }

                        default:
                            cursor++;
                    }
                }
            }

            if (start == cursor) return null;
            return createPropertyToken(start, cursor);
        }
        catch (CompileException e) {
            CompileException c = new CompileException(e.getMessage(), expr, cursor, e.getCursor() == 0, e);
            c.setLineNumber(line);
            c.setColumn(cursor - lastLineStart);
            throw c;
        }
    }

    public ASTNode handleSubstatement(Substatement stmt) {
        if (stmt.getStatement() != null && stmt.getStatement().isLiteralOnly()) {
            return new LiteralNode(stmt.getStatement().getValue(null, null, null), fields);
        }
        else {
            return stmt;
        }
    }

    protected ASTNode handleUnion(ASTNode node) {
        if (cursor != length) {
            skipWhitespace();
            if (expr[cursor] == '.') {
                int union = cursor + 1;
                captureToEOT();
                return lastNode = new Union(expr, union, cursor, fields, node);
            }
            else if (expr[cursor] == '[') {
                captureToEOT();
                return lastNode = new Union(expr, cursor, cursor, fields, node);
            }
        }
        return lastNode = node;
    }


    /**
     * Most of this method should be self-explanatory.
     *
     * @param expr   -
     * @param start  -
     * @param end    -
     * @param fields -
     * @return -
     */
    private ASTNode createToken(final char[] expr, final int start, final int end, int fields) {
        lastWasIdentifier = (lastNode = new ASTNode(expr, start, end, fields)).isIdentifier();
        return lastNode;
    }

    private ASTNode createOperator(final char[] expr, final int start, final int end, int fields) {
        char[] e = subset(expr, start, end - start);
        lastWasIdentifier = false;
        return lastNode = new OperatorNode(OPERATORS.get(new String(e)));
    }

    private char[] subArray(final int start, final int end) {
        if (start >= end) return new char[0];

        char[] newA = new char[end - start];
        for (int i = 0; i != newA.length; i++)
            newA[i] = expr[i + start];

        return newA;
    }

    private ASTNode createPropertyToken(int start, int end) {
        lastWasIdentifier = true;
        String tmp;
        if (parserContext != null && parserContext.get() != null && parserContext.get().hasImports()) {
            char[] _subset = subset(expr, start, cursor - start);
            int offset;

            if ((offset = findFirst('.', _subset)) != -1) {
                String iStr = new String(_subset, 0, offset);
                if (pCtx.hasImport(iStr)) {
                    return lastNode = new LiteralDeepPropertyNode(subset(_subset, offset + 1, _subset.length - offset - 1), fields, pCtx.getImport(iStr));
                }
            }
            else {
                if (pCtx.hasImport(tmp = new String(_subset))) {
                    Object i = pCtx.getStaticOrClassImport(tmp);

                    if (i instanceof Class) {
                        return lastNode = new LiteralNode(i, Class.class);
                    }
                }

                lastWasIdentifier = true;
                return lastNode = new ASTNode(_subset, 0, _subset.length, fields);
            }
        }
        else if ((fields & ASTNode.METHOD) != 0) {
            return lastNode = new ASTNode(expr, start, end, fields);
        }
        else if (LITERALS.containsKey(tmp = new String(expr, start, end - start))) {
            return lastNode = new LiteralNode(LITERALS.get(tmp));
        }
        else if (OPERATORS.containsKey(tmp)) {
            return lastNode = new OperatorNode(OPERATORS.get(tmp));
        }

        return lastNode = new ASTNode(expr, start, end, fields);
    }

    private ASTNode createBlockToken(final int condStart,
                                     final int condEnd, final int blockStart, final int blockEnd, int type) {

        lastWasIdentifier = false;

        cursor++;

        if (!isStatementManuallyTerminated()) {
            splitAccumulator.add(new EndOfStatement());
        }

        switch (type) {
            case ASTNode.BLOCK_IF:
                return new IfNode(subArray(condStart, condEnd), subArray(blockStart, blockEnd), fields);
            case ASTNode.BLOCK_FOREACH:
                return new ForEachNode(subArray(condStart, condEnd), subArray(blockStart, blockEnd), fields);
            case ASTNode.BLOCK_WHILE:
                return new WhileNode(subArray(condStart, condEnd), subArray(blockStart, blockEnd), fields);
            default:
                return new WithNode(subArray(condStart, condEnd), subArray(blockStart, blockEnd), fields);
        }
    }

    private ASTNode captureCodeBlock(int type) {
        boolean cond = true;

        ASTNode first = null;
        ASTNode tk = null;

        switch (type) {
            case ASTNode.BLOCK_IF: {
                do {
                    if (tk != null) {
                        captureToNextTokenJunction();
                        skipWhitespace();

                        cond = expr[cursor] != '{' && expr[cursor] == 'i' && expr[++cursor] == 'f'
                                && (isWhitespace(expr[++cursor]) || expr[cursor] == '(');
                    }

                    if (((IfNode) (tk = _captureBlock(tk, expr, cond, type))).getElseBlock() != null) {
                        cursor++;
                        return first;
                    }

                    if (first == null) first = tk;

                    if (cursor != length && expr[cursor] != ';') {
                        cursor++;
                    }
                }
                while (ifThenElseblockContinues());
                return first;
            }

            default: // either BLOCK_WITH or BLOCK_FOREACH
                captureToNextTokenJunction();
                //         if (debugSymbols) {
                skipWhitespaceWithLineAccounting();
                //             }
//                else {
//                    skipWhitespace();
//                }
                return _captureBlock(null, expr, true, type);
        }

    }

    private ASTNode _captureBlock(ASTNode node, final char[] expr, boolean cond, int type) {
        skipWhitespace();
        int startCond = 0;
        int endCond = 0;

        int blockStart;
        int blockEnd;

        /**
         * Functions are a special case we handle differently from the rest of block parsing
         */
        if (type == FUNCTION) {
            int start = cursor;

            captureToNextTokenJunction();

            if (cursor == length) {
                throw new CompileException("unexpected end of statement", expr, start);
            }

            /**
             * Grabe the function name.
             */
            String functionName = createStringTrimmed(expr, start, (startCond = cursor) - start);

            /**
             * Check to see if the name is legal.
             */
            if (isReservedWord(functionName) || !isValidNameorLabel(functionName))
                throw new CompileException("illegal function name or use of reserved word", expr, cursor);


            if (expr[cursor] == '(') {
                /**
                 * If we discover an opening bracket after the function name, we check to see
                 * if this function accepts parameters.
                 */

                endCond = cursor = balancedCapture(expr, startCond = cursor, '(');
                startCond++;
                cursor++;

                skipWhitespace();

                if (cursor >= length) {
                    throw new CompileException("unbalanced braces", expr, cursor);
                }
                else if (expr[cursor] == '{') {
                    blockStart = cursor;
                    blockEnd = cursor = balancedCapture(expr, cursor, '{');
                }
                else {
                    blockStart = cursor;
                    captureToEOS();
                    blockEnd = cursor;
                }
            }
            else {
                /**
                 * This function has not parameters.
                 */
                if (expr[cursor] == '{') {
                    /**
                     * This function is bracketed.  We capture the entire range in the brackets.
                     */
                    blockStart = cursor;
                    blockEnd = cursor = balancedCapture(expr, cursor, '{');
                }
                else {
                    /**
                     * This is a single statement function declaration.  We only capture the statement.
                     */
                    blockStart = cursor;
                    captureToEOS();
                    blockEnd = cursor;
                }
            }

            /**
             * Trim any whitespace from the captured block range.
             */
            blockStart = trimRight(blockStart + 1);
            blockEnd = trimLeft(blockEnd);

            cursor++;

            /**
             * Check if the function is manually terminated.
             */
            if (!isStatementManuallyTerminated()) {
                /**
                 * Add an EndOfStatement to the split accumulator in the parser.
                 */
                splitAccumulator.add(new EndOfStatement());
            }

            /**
             * Produce the funciton node.
             */
            return new Function(functionName, subArray(startCond, endCond), subArray(blockStart, blockEnd));
        }
        else if (cond) {
            /**
             * This block is an: IF, FOREACH or WHILE node.
             */

//            if (debugSymbols) {
            int[] cap = balancedCaptureWithLineAccounting(expr, startCond = cursor, '(');

            endCond = cursor = cap[0];

            startCond++;
            cursor++;

            pCtx.incrementLineCount(cap[1]);

            //getParserContext().setLineCount(line = getParserContext().getLineCount() + cap[1]);
//            }
//            else {
//                endCond = cursor = balancedCapture(expr, startCond = cursor, '(');
//                startCond++;
//                cursor++;
//            }
        }

        skipWhitespace();

        if (cursor >= length) {
            throw new CompileException("unbalanced braces", expr, cursor);
        }
        else if (expr[cursor] == '{') {
            blockStart = cursor;

//            if (debugSymbols) {
            int[] cap = balancedCaptureWithLineAccounting(expr, cursor, '{');
            blockEnd = cursor = cap[0];

            pCtx.incrementLineCount(cap[1]);

            //   getParserContext().setLineCount((line = getParserContext().getLineCount() + cap[1]));

            //           }
//            else {
//                blockEnd = cursor = balancedCapture(expr, cursor, '{');
//            }
        }
        else {
            blockStart = cursor - 1;
            captureToEOSorEOL();
            blockEnd = cursor + 1;
        }

        if (type == ASTNode.BLOCK_IF) {
            IfNode ifNode = (IfNode) node;

            if (node != null) {
                if (!cond) {
                    return ifNode.setElseBlock(subArray(trimRight(blockStart + 1), trimLeft(blockEnd - 1)));
                }
                else {
                    return ifNode.setElseIf((IfNode) createBlockToken(startCond, endCond, trimRight(blockStart + 1),
                            trimLeft(blockEnd), type));
                }
            }
            else {
                return createBlockToken(startCond, endCond, blockStart + 1, blockEnd, type);
            }
        }
        // DON"T REMOVE THIS COMMENT!
        // else if (isFlag(ASTNode.BLOCK_FOREACH) || isFlag(ASTNode.BLOCK_WITH)) {
        else {
            return createBlockToken(startCond, endCond, trimRight(blockStart + 1), trimLeft(blockEnd), type);
        }
    }


    /**
     * Checking from the current cursor position, check to see if the if-then-else block continues.
     *
     * @return boolean value
     */
    protected boolean ifThenElseblockContinues() {
        if ((cursor + 4) < length) {
            if (expr[cursor] != ';') cursor--;
            skipWhitespace();
            return expr[cursor] == 'e' && expr[cursor + 1] == 'l' && expr[cursor + 2] == 's' && expr[cursor + 3] == 'e'
                    && (isWhitespace(expr[cursor + 4]) || expr[cursor + 4] == '{');
        }
        return false;
    }

    /**
     * Checking from the current cursor position, check to see if we're inside a contiguous identifier.
     *
     * @return
     */
    protected boolean tokenContinues() {
        if (cursor >= length) return false;
        else if (expr[cursor] == '.' || expr[cursor] == '[') return true;
        else if (isWhitespace(expr[cursor])) {
            int markCurrent = cursor;
            skipWhitespace();
            if (cursor != length && (expr[cursor] == '.' || expr[cursor] == '[')) return true;
            cursor = markCurrent;
        }
        return false;
    }

    /**
     * Capture from the current cursor position, to the end of the statement.
     */
    protected void captureToEOS() {
        while (cursor != length) {
            switch (expr[cursor]) {
                case '(':
                case '[':
                case '{':
                    cursor = balancedCapture(expr, cursor, expr[cursor]);
                    break;

                case ';':
                case '}':
                    return;

            }
            cursor++;
        }
    }

    /**
     * From the current cursor position, capture to the end of statement, or the end of line, whichever comes first.
     */
    protected void captureToEOSorEOL() {
        while (cursor != length && (expr[cursor] != '\n' && expr[cursor] != '\r' && expr[cursor] != ';')) {
            cursor++;
        }
    }

    /**
     * From the current cursor position, capture to the end of the line.
     */
    protected void captureToEOL() {
        while (cursor != length && (expr[cursor] != '\n')) cursor++;
    }

    /**
     * From the current cursor position, capture to the end of the current token.
     */
    protected void captureToEOT() {
        skipWhitespace();
        while (++cursor != length) {
            switch (expr[cursor]) {
                case '(':
                case '[':
                case '{':
                    cursor = balancedCapture(expr, cursor, expr[cursor]);
                    break;

                case '=':
                case '&':
                case '|':
                case ';':
                    return;

                case '.':
                    skipWhitespace();
                    break;

                default:
                    if (isWhitespace(expr[cursor])) {
                        skipWhitespace();

                        if (expr[cursor] == '.') {
                            if (cursor != length) cursor++;
                            skipWhitespace();
                            break;
                        }
                        else {
                            trimWhitespace();
                            return;
                        }
                    }
            }
        }
    }

    /**
     * From the specified cursor position, trim out any whitespace between the current position and the end of the
     * last non-whitespace character.
     *
     * @param pos - current position
     * @return new position.
     */
    protected int trimLeft(int pos) {
        while (pos != 0 && isWhitespace(expr[pos - 1])) pos--;
        return pos;
    }

    /**
     * From the specified cursor position, trim out any whitespace between the current position and beginning of the
     * first non-whitespace character.
     *
     * @param pos
     * @return
     */
    protected int trimRight(int pos) {
        while (pos != length && isWhitespace(expr[pos])) pos++;
        return pos;
    }

    /**
     * If the cursor is currently pointing to whitespace, move the cursor forward to the first non-whitespace
     * character.
     */
    protected void skipWhitespace() {
        while (cursor != length && isWhitespace(expr[cursor])) cursor++;
    }

    /**
     * If the cursor is currently pointing to whitespace, move the cursor forward to the first non-whitespace
     * character, but account for carraige returns in the script (updates parser field: line).
     */
    protected void skipWhitespaceWithLineAccounting() {
        while (cursor != length && isWhitespace(expr[cursor])) {
            switch (expr[cursor]) {
                case '\n':
                    line++;
                    lastLineStart = cursor;
                case '\r':
                    cursor++;
                    continue;
            }
            cursor++;
        }
    }

    /**
     * From the current cursor position, capture to the end of the next token junction.
     */
    protected void captureToNextTokenJunction() {
        while (cursor != length) {
            switch (expr[cursor]) {
                case '{':
                case '(':
                    return;
                default:
                    if (isWhitespace(expr[cursor])) return;
                    cursor++;
            }
        }
    }

    /**
     * From the current cursor position, trim backward over any whitespace to the first non-whitespace character.
     */
    protected void trimWhitespace() {
        while (cursor != 0 && isWhitespace(expr[cursor - 1])) cursor--;
    }


    /**
     * Check if the specified string is a reserved word in the parser.
     *
     * @param name
     * @return
     */
    public static boolean isReservedWord(String name) {
        return LITERALS.containsKey(name) || OPERATORS.containsKey(name);
    }

    /**
     * Check if the specfied string represents a valid name of label.
     *
     * @param name
     * @return
     */
    public static boolean isValidNameorLabel(String name) {
        for (char c : name.toCharArray()) {
            if (c == '.') return false;
            else if (!isIdentifierPart(c)) return false;
        }
        return true;
    }

    protected void setExpression(String expression) {
        if (expression != null && !"".equals(expression)) {
            this.expr = EX_PRECACHE.get(expression);
            if (this.expr == null) {
                length = (this.expr = expression.toCharArray()).length;

                // trim any whitespace.
                while (length != 0 && isWhitespace(this.expr[length - 1])) length--;

                char[] e = new char[length];
                //arraycopy(this.expr, 0, e, 0, length);
                for (int i = 0; i != e.length; i++)
                    e[i] = expr[i];

                EX_PRECACHE.put(expression, e);
            }
            else {
                length = this.expr.length;
            }
        }
    }


    protected void setExpression(char[] expression) {
        length = (this.expr = expression).length;
        while (length != 0 && isWhitespace(this.expr[length - 1])) length--;
    }


    /**
     * Return the previous non-whitespace character.
     *
     * @return
     */
    protected char lookToLast() {
        if (cursor == 0) return 0;
        int temp = cursor;
        while (temp != 0 && isWhitespace(expr[--temp])) ;
        return expr[temp];
    }

    /**
     * Return the last character (delta -1 of cursor position).
     *
     * @return
     */
    protected char lookBehind() {
        if (cursor == 0) return 0;
        return expr[cursor - 1];
    }

    /**
     * Return the next character (delta 1 of cursor position).
     *
     * @return
     */
    protected char lookAhead() {
        int tmp = cursor + 1;
        if (tmp != length) return expr[tmp];
        return 0;
    }

    /**
     * Return the character, forward of the currrent cursor position based on the specified range delta.
     *
     * @param range
     * @return
     */
    protected char lookAhead(int range) {
        if ((cursor + range) >= length) return 0;
        else {
            return expr[cursor + range];
        }
    }

    /**
     * NOTE: This method assumes that the current position of the cursor is at the end of a logical statement, to
     * begin with.
     * <p/>
     * Determines whether or not the logical statement is manually terminated with a statement separator (';').
     *
     * @return
     */
    protected boolean isStatementManuallyTerminated() {
        if (cursor >= length) return true;
        int c = cursor;
        while (c != length && isWhitespace(expr[c])) c++;
        return (c != length && expr[c] == ';');
    }

    /**
     * Returns true of if the detal 1 of the cursor matches the specified character.
     *
     * @param c
     * @return
     */
    protected boolean isNext(char c) {
        return lookAhead() == c;
    }

    protected ParserContext getParserContext() {
        if (parserContext == null || parserContext.get() == null) {
            newContext();
        }
        return parserContext.get();
    }

    public static ParserContext getCurrentThreadParserContext() {
        return contextControl(GET_OR_CREATE, null, null);
    }

    public static void setCurrentThreadParserContext(ParserContext pCtx) {
        contextControl(SET, pCtx, null);
    }

    /**
     * Create a new ParserContext in the current thread.
     */
    protected void newContext() {
        contextControl(SET, new ParserContext(), this);
    }

    /**
     * Create a new ParserContext in the current thread, using the one specified.
     *
     * @param pCtx
     */
    protected void newContext(ParserContext pCtx) {
        contextControl(SET, pCtx, this);

    }

    /**
     * Remove the current ParserContext from the thread.
     */
    protected void removeContext() {
        contextControl(REMOVE, null, this);
    }

    protected static ParserContext contextControl(int operation, ParserContext pCtx, AbstractParser parser) {
        synchronized (getRuntime()) {
            if (parserContext == null) parserContext = new ThreadLocal<ParserContext>();

            switch (operation) {
                case SET:
                    pCtx.setRootParser(parser);
                    parserContext.set(pCtx);
                    return null;

                case REMOVE:
                    parserContext.set(null);
                    return null;

                case GET_OR_CREATE:
                    if (parserContext.get() == null) {
                        parserContext.set(new ParserContext(parser));
                    }

                case GET:
                    return parserContext.get();
            }
        }

        return null;
    }

    protected static final int SET = 0;
    protected static final int REMOVE = 1;
    protected static final int GET = 2;
    protected static final int GET_OR_CREATE = 3;

    public boolean isDebugSymbols() {
        return debugSymbols;
    }

    public void setDebugSymbols(boolean debugSymbols) {
        this.debugSymbols = debugSymbols;
    }

    protected static String getCurrentSourceFileName() {
        if (parserContext != null && parserContext.get() != null) {
            return parserContext.get().getSourceFile();
        }
        return null;
    }

    protected void addFatalError(String message) {
        getParserContext().addError(new ErrorDetail(getParserContext().getLineCount(), cursor - getParserContext().getLineOffset(), true, message));
    }

    protected void addFatalError(String message, int row, int cols) {
        getParserContext().addError(new ErrorDetail(row, cols, true, message));
    }

    protected void addWarning(String message) {
        getParserContext().addError(new ErrorDetail(message, false));
    }

    public static final int LEVEL_5_CONTROL_FLOW = 5;
    public static final int LEVEL_4_ASSIGNMENT = 4;
    public static final int LEVEL_3_ITERATION = 3;
    public static final int LEVEL_2_MULTI_STATEMENT = 2;
    public static final int LEVEL_1_BASIC_LANG = 1;
    public static final int LEVEL_0_PROPERTY_ONLY = 0;

    public static void setLanguageLevel(int level) {
        OPERATORS.clear();
        OPERATORS.putAll(loadLanguageFeaturesByLevel(level));
    }

    public static Map<String, Integer> loadLanguageFeaturesByLevel(int languageLevel) {

        Map<String, Integer> operatorsTable = new HashMap<String, Integer>();

        switch (languageLevel) {
            case 5:  // control flow operations
                operatorsTable.put("if", IF);
                operatorsTable.put("else", ELSE);
                operatorsTable.put("?", TERNARY);
                operatorsTable.put("switch", SWITCH);
                operatorsTable.put("function", FUNCTION);
                operatorsTable.put("def", FUNCTION);

            case 4: // assignment
                operatorsTable.put("=", ASSIGN);
                operatorsTable.put("var", UNTYPED_VAR);
                operatorsTable.put("+=", ASSIGN_ADD);
                operatorsTable.put("-=", ASSIGN_SUB);

            case 3: // iteration
                operatorsTable.put("foreach", FOREACH);
                operatorsTable.put("while", WHILE);
                operatorsTable.put("for", FOR);
                operatorsTable.put("do", DO);

            case 2: // multi-statement
                operatorsTable.put("return", RETURN);
                operatorsTable.put(";", END_OF_STMT);

            case 1: // boolean, math ops, projection, assertion, objection creation, block setters, imports
                operatorsTable.put("+", ADD);
                operatorsTable.put("-", SUB);
                operatorsTable.put("*", MULT);
                operatorsTable.put("**", POWER);
                operatorsTable.put("/", DIV);
                operatorsTable.put("%", MOD);
                operatorsTable.put("==", EQUAL);
                operatorsTable.put("!=", NEQUAL);
                operatorsTable.put(">", GTHAN);
                operatorsTable.put(">=", GETHAN);
                operatorsTable.put("<", LTHAN);
                operatorsTable.put("<=", LETHAN);
                operatorsTable.put("&&", AND);
                operatorsTable.put("and", AND);
                operatorsTable.put("||", OR);
                operatorsTable.put("or", CHOR);
                operatorsTable.put("~=", REGEX);
                operatorsTable.put("instanceof", INSTANCEOF);
                operatorsTable.put("is", INSTANCEOF);
                operatorsTable.put("contains", CONTAINS);
                operatorsTable.put("soundslike", SOUNDEX);
                operatorsTable.put("strsim", SIMILARITY);
                operatorsTable.put("convertable_to", CONVERTABLE_TO);

                operatorsTable.put("#", STR_APPEND);

                operatorsTable.put("&", BW_AND);
                operatorsTable.put("|", BW_OR);
                operatorsTable.put("^", BW_XOR);
                operatorsTable.put("<<", BW_SHIFT_LEFT);
                operatorsTable.put("<<<", BW_USHIFT_LEFT);
                operatorsTable.put(">>", BW_SHIFT_RIGHT);
                operatorsTable.put(">>>", BW_USHIFT_RIGHT);

                operatorsTable.put("new", Operator.NEW);
                operatorsTable.put("in", PROJECTION);

                operatorsTable.put("with", WITH);

                operatorsTable.put("assert", ASSERT);
                operatorsTable.put("import", IMPORT);
                operatorsTable.put("import_static", IMPORT_STATIC);

                operatorsTable.put("++", INC);
                operatorsTable.put("--", DEC);

            case 0: // Property access and inline collections
                operatorsTable.put(":", TERNARY_ELSE);
        }

        return operatorsTable;
    }

    /**
     * Remove the current parser context from the thread.
     */
    public static void resetParserContext() {
        contextControl(REMOVE, null, null);
    }

    protected static boolean isArithmeticOperator(int operator) {
        //  return operator == ADD || operator == MULT || operator == SUB || operator == DIV;
        return operator < 6;
    }

    protected void arithmeticFunctionReduction(int operator) {
        ASTNode tk2;
        int operator2;

        boolean x = false;

        /**
         * If the next token is an operator, we check to see if it has a higher
         * precdence.
         */

        if ((tk2 = nextToken()) != null && tk2.isOperator()) {
            if (isArithmeticOperator(operator2 = tk2.getOperator()) && PTABLE[operator2] > PTABLE[operator]) {
                do {
                    dStack.push(tk2.getOperator(), nextToken().getReducedValue(ctx, ctx, variableFactory));
                    if (x = !x)
                        reduceRightXSwap(); // reduce from the RHS and XSWAP
                    else
                        reduceRightXXSwap(); // reduce from the RHS and XXSWAP
                }
                while (((tk2 = nextToken()) != null && tk2.isOperator()
                        && isArithmeticOperator(operator2 = tk2.getOperator())
                        && (PTABLE[operator2] > PTABLE[operator])));

                xswap(); // XSWAP the stack.
                reduce(); // reduce the stack.

                // Record the current operator value to the stack.
                if (tk2 != null) stk.push(operator2);

                // Evaluate the next token and push the value to the stack.
                if ((tk2 = nextToken()) != null) {
                    stk.push(tk2.getReducedValue(ctx, ctx, variableFactory));
                }
            }
            else {
                reduce();
                splitAccumulator.push(tk2);
            }
        }
        else if (tk2 != null) {
            reduce();  // reduce the stack.
            operator = tk2.getOperator();

            // if there is another token, then this statement must continue
            // push the values down and reduce.
            if ((tk2 = nextToken()) != null) {
                stk.push(tk2.getReducedValue(ctx, ctx, variableFactory), operator);
                reduce();
            }

            // while any values remain on the stack
            // keep XSWAPing and reducing, until there is nothing left.
            while (stk.size() > 1) {
                xswap();
                reduce();
            }

            /**
             * Push tk2 back into the accumulator.
             */
        }
        else {
            reduce();

            // while any values remain on the stack
            // keep XSWAPing and reducing, until there is nothing left.
            while (stk.size() > 1) {
                xswap();
                reduce();
            }

        }
    }

    /**
     * A more efficient RHS reduction, to avoid the need
     * to XSWAP directly on the stack.
     */
    private void reduceRightXSwap() {
        Object o = stk.pop();
        Object o2 = stk.pop();

        stk.push(o);
        stk.push(dStack.pop());
        stk.push(o2);
        stk.push(dStack.pop());
        reduce();
    }

    /**
     * Same as reduceRightXSwap, except this is an inverted
     * operator, or XXSWAP.
     */
    private void reduceRightXXSwap() {
        Object o = stk.pop();
        Object o2 = stk.pop();

        stk.push(o2);
        stk.push(dStack.pop());
        stk.push(o);
        stk.push(dStack.pop());
        reduce();
    }

    /**
     * XSWAP.
     */
    private void xswap() {
        Object o = stk.pop();
        Object o2 = stk.pop();
        stk.push(o);
        stk.push(o2);
    }

    /**
     * This method is called when we reach the point where we must subEval a trinary operation in the expression.
     * (ie. val1 op val2).  This is not the same as a binary operation, although binary operations would appear
     * to have 3 structures as well.  A binary structure (or also a junction in the expression) compares the
     * current state against 2 downrange structures (usually an op and a val).
     */
    protected void reduce() {
        Object v1 = null, v2 = null;
        Integer operator;
        try {
            operator = (Integer) stk.pop();
            v1 = stk.pop();
            v2 = stk.pop();

            //      System.out.println("reduce [" + v2 + " <" + DebugTools.getOperatorName(operator) + "> " + v1 + "]");

            switch (operator) {
                case ADD:
                case SUB:
                case DIV:
                case MULT:
                case MOD:
                case EQUAL:
                case NEQUAL:
                case GTHAN:
                case LTHAN:
                case GETHAN:
                case LETHAN:
                case POWER:
                    stk.push(doOperations(v2, operator, v1));
                    break;

                case AND:
                    stk.push(((Boolean) v2) && ((Boolean) v1));
                    break;

                case OR:
                    stk.push(((Boolean) v2) || ((Boolean) v1));
                    break;

                case CHOR:
                    if (!isEmpty(v2) || !isEmpty(v1)) {
                        stk.clear();
                        stk.push(!isEmpty(v2) ? v2 : v1);
                        return;
                    }
                    else stk.push(null);
                    break;

                case REGEX:
                    stk.push(java.util.regex.Pattern.compile(java.lang.String.valueOf(v1)).matcher(java.lang.String.valueOf(v2)).matches());
                    break;

                case INSTANCEOF:
                    if (v1 instanceof Class)
                        stk.push(((Class) v1).isInstance(v2));
                    else
                        stk.push(currentThread().getContextClassLoader().loadClass(java.lang.String.valueOf(v1)).isInstance(v2));

                    break;

                case CONVERTABLE_TO:
                    if (v1 instanceof Class)
                        stk.push(org.mvel.DataConversion.canConvert(v2.getClass(), (Class) v1));
                    else
                        stk.push(org.mvel.DataConversion.canConvert(v2.getClass(), currentThread().getContextClassLoader().loadClass(java.lang.String.valueOf(v1))));
                    break;

                case CONTAINS:
                    stk.push(containsCheck(v2, v1));
                    break;

                case BW_AND:
                    stk.push(asInt(v2) & asInt(v1));
                    break;

                case BW_OR:
                    stk.push(asInt(v2) | asInt(v1));
                    break;

                case BW_XOR:
                    stk.push(asInt(v2) ^ asInt(v1));
                    break;

                case BW_SHIFT_LEFT:
                    stk.push(asInt(v2) << asInt(v1));
                    break;

                case BW_USHIFT_LEFT:
                    int iv2 = asInt(v2);
                    if (iv2 < 0) iv2 *= -1;
                    stk.push(iv2 << asInt(v1));
                    break;

                case BW_SHIFT_RIGHT:
                    stk.push(asInt(v2) >> asInt(v1));
                    break;

                case BW_USHIFT_RIGHT:
                    stk.push(asInt(v2) >>> asInt(v1));
                    break;

                case STR_APPEND:
                    stk.push(new StringAppender(java.lang.String.valueOf(v2)).append(java.lang.String.valueOf(v1)).toString());
                    break;

                case SOUNDEX:
                    stk.push(Soundex.soundex(java.lang.String.valueOf(v1)).equals(Soundex.soundex(java.lang.String.valueOf(v2))));
                    break;

                case SIMILARITY:
                    stk.push(similarity(java.lang.String.valueOf(v1), java.lang.String.valueOf(v2)));
                    break;

            }
            //      }
        }
        catch (ClassCastException e) {
            if ((fields & ASTNode.LOOKAHEAD) == 0) {
                /**
                 * This will allow for some developers who like messy expressions to compileAccessor
                 * away with some messy constructs like: a + b < c && e + f > g + q instead
                 * of using brackets like (a + b < c) && (e + f > g + q)
                 */

                fields |= ASTNode.LOOKAHEAD;

                ASTNode tk = nextToken();
                if (tk != null) {
                    stk.push(v1, nextToken(), tk.getOperator());

                    reduce();
                    return;
                }
            }
            throw new CompileException("syntax error or incomptable types (left=" +
                    (v1 != null ? v1.getClass().getName() : "null") + ", right=" +
                    (v2 != null ? v2.getClass().getName() : "null") + ")", expr, cursor, e);

        }
        catch (Exception e) {
            throw new CompileException("failed to subEval expression", e);
        }

    }

    private static int asInt(final Object o) {
        return (Integer) o;
    }
}
