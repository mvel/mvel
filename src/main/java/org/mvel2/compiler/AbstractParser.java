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
package org.mvel2.compiler;

import org.mvel2.*;
import static org.mvel2.Operator.*;
import org.mvel2.ast.*;
import static org.mvel2.ast.TypeDescriptor.getClassReference;
import org.mvel2.integration.VariableResolverFactory;
import static org.mvel2.util.ArrayTools.findFirst;
import org.mvel2.util.ExecutionStack;
import static org.mvel2.util.ParseTools.*;
import static org.mvel2.util.PropertyTools.isEmpty;
import org.mvel2.util.StringAppender;

import java.io.Serializable;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Double.parseDouble;
import static java.lang.Runtime.getRuntime;
import static java.lang.System.getProperty;
import static java.lang.Thread.currentThread;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author Christopher Brock
 */
public class AbstractParser implements Serializable {
    protected char[] expr;
    protected int cursor;
    protected int start;
    protected int length;
    protected int fields;

    protected static final int OP_OVERFLOW = -2;
    protected static final int OP_TERMINATE = -1;
    protected static final int OP_RESET_FRAME = 0;
    protected static final int OP_CONTINUE = 1;

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

    protected ExecutionStack stk;
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
        LITERALS.put("true", TRUE);
        LITERALS.put("false", FALSE);

        LITERALS.put("null", null);
        LITERALS.put("nil", null);

        LITERALS.put("empty", BlankLiteral.INSTANCE);

        /**
         * Add System and all the class wrappers from the JCL.
         */
        LITERALS.put("System", System.class);
        LITERALS.put("String", String.class);
        LITERALS.put("CharSequence", CharSequence.class);

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
        LITERALS.put("double", Double.class);

        LITERALS.put("Float", Float.class);
        LITERALS.put("float", Float.class);

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

        if (parseDouble(getProperty("java.version").substring(0, 3)) >= 1.5) {
            try {
                LITERALS.put("StringBuilder", currentThread().getContextClassLoader().loadClass("java.lang.StringBuilder"));
            }
            catch (Exception e) {
                throw new RuntimeException("cannot resolve a built-in literal", e);
            }
        }

        setLanguageLevel(5);
    }

    public static void configureFactory() {
        EX_PRECACHE = (new WeakHashMap<String, char[]>(15));
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

            int brace, idx;
            start = cursor;

            char[] tmp;
            String name;

            /**
             * Because of parser recursion for sub-expression parsing, we sometimes need to remain
             * certain field states.  We do not reset for assignments, boolean mode, list creation or
             * a capture only mode.
             */

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
                                if (!isIdentifierPart(expr[start = cursor = trimRight(cursor)])) {
                                    throw new CompileException("unexpected character (expected identifier): "
                                            + expr[cursor], expr, cursor);
                                }

                                /**
                                 * Capture the beginning part of the token.
                                 */
                                do {
                                    captureToNextTokenJunction();
                                    skipWhitespaceWithLineAccounting();
                                }
                                while (cursor < length && expr[cursor] == '[');

                                /**
                                 * If it's not a dimentioned array, continue capturing if necessary.
                                 */
                                if (cursor < length && !lastNonWhite(']')) captureToEOT();

                                lastNode = new NewObjectNode(subArray(start, cursor), fields);

                                skipWhitespaceWithLineAccounting();
                                if (cursor != length && expr[cursor] == '{') {
                                    start = cursor;
                                    Class egressType = ((NewObjectNode) lastNode).getEgressType();

                                    if (egressType == null) {
                                        try {
                                            egressType = TypeDescriptor.getClassReference(pCtx, ((NewObjectNode) lastNode).getTypeDescr());
                                        }
                                        catch (ClassNotFoundException e) {
                                            throw new CompileException("could not instantiate class", e);
                                        }
                                    }

                                    cursor = balancedCapture(expr, cursor, expr[cursor]) + 1;
                                    if (tokenContinues()) {
                                        lastNode = new InlineCollectionNode(expr, start, start = cursor, fields,
                                                egressType);
                                        captureToEOT();
                                        return lastNode = new Union(expr, start + 1, cursor, fields, lastNode);
                                    }
                                    else {
                                        return lastNode = new InlineCollectionNode(expr, start, cursor, fields,
                                                egressType);
                                    }
                                }
                                return lastNode;

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

                            case ELSE:
                                throw new CompileException("else without if", cursor);

                            case FOREACH:
                                return captureCodeBlock(ASTNode.BLOCK_FOREACH);

                            case WHILE:
                                return captureCodeBlock(ASTNode.BLOCK_WHILE);

                            case UNTIL:
                                return captureCodeBlock(ASTNode.BLOCK_UNTIL);

                            case FOR:
                                return captureCodeBlock(ASTNode.BLOCK_FOR);

                            case WITH:
                                return captureCodeBlock(ASTNode.BLOCK_WITH);

                            case DO:
                                return captureCodeBlock(ASTNode.BLOCK_DO);

                            case ISDEF:
                                start = cursor = trimRight(cursor);
                                captureToEOS();
                                return lastNode = new IsDef(subArray(start, cursor));

                            case IMPORT:
                                start = cursor = trimRight(cursor);
                                captureToEOS();
                                ImportNode importNode = new ImportNode(subArray(start, cursor));
                                if (importNode.isPackageImport()) {
                                    pCtx.addPackageImport(importNode.getPackageImport());
                                }
                                else {
                                    pCtx.addImport(getSimpleClassName(importNode.getImportClass()), importNode.getImportClass());
                                }
                                return lastNode = importNode;

                            case IMPORT_STATIC:
                                start = cursor = trimRight(cursor);
                                captureToEOS();
                                return lastNode = new StaticImportNode(subArray(start, cursor));

                            case FUNCTION:
                                lastNode = (Function) captureCodeBlock(FUNCTION);
                                capture = false;
                                start = cursor + 1;
                                return lastNode;

                            case UNTYPED_VAR:
                                start = cursor + 1;
                                captureToEOT();
                                int end = cursor;

                                skipWhitespace();

                                if (expr[cursor] == '=') {
                                    if (end == (cursor = start))
                                        throw new CompileException("illegal use of reserved word: var");
                                    continue;
                                }
                                else {
                                    if ((idx = pCtx.variableIndexOf(name = new String(subArray(start, end)))) != -1) {
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
                                        if ((idx = pCtx.variableIndexOf(name = new String(subArray(start, trimLeft(cursor))))) != -1) {
                                            lastNode = new IndexedPostFixIncNode(idx);
                                        }
                                        else {
                                            lastNode = new PostFixIncNode(name);
                                        }

                                        cursor += 2;

                                        expectEOS();

                                        return lastNode;

                                    case '=':
                                        name = createStringTrimmed(expr, start, cursor - start);
                                        start = cursor += 2;

                                        captureToEOS();

                                        if (union) {
                                            return lastNode = new DeepAssignmentNode(subArray(start, cursor), fields, ADD, t);
                                        }
                                        else if ((idx = pCtx.variableIndexOf(name)) != -1) {
                                            return lastNode = new IndexedAssignmentNode(subArray(start, cursor), fields, ADD, name, idx);
                                        }
                                        else {
                                            return lastNode = new AssignmentNode(subArray(start, cursor), fields, ADD, name);
                                        }
                                }

                                if (isDigit(lookAhead()) &&
                                        cursor > 1 && (expr[cursor - 1] == 'E' || expr[cursor - 1] == 'e')
                                        && isDigit(expr[cursor - 2])) {
                                    cursor++;
                                    capture = true;
                                    continue;

                                }
                                break;

                            case '-':
                                switch (lookAhead()) {
                                    case '-':
                                        if ((idx = pCtx.variableIndexOf(name = new String(subArray(start, trimLeft(cursor))))) != -1) {
                                            lastNode = new IndexedPostFixDecNode(idx);
                                        }
                                        else {
                                            lastNode = new PostFixDecNode(name);
                                        }
                                        cursor += 2;

                                        expectEOS();

                                        return lastNode;

                                    case '=':
                                        name = new String(expr, start, trimLeft(cursor) - start);
                                        start = cursor += 2;

                                        captureToEOS();

                                        if (union) {
                                            return lastNode = new DeepAssignmentNode(subArray(start, cursor), fields, SUB, t);
                                        }
                                        else if ((idx = pCtx.variableIndexOf(name)) != -1) {
                                            return lastNode = new IndexedOperativeAssign(subArray(start, cursor), Operator.SUB, idx, fields);
                                        }
                                        else {
                                            return lastNode = new OperativeAssign(name, subArray(start, cursor), Operator.SUB, fields);
                                        }
                                }

                                if (isDigit(lookAhead()) &&
                                        cursor > 1 && (expr[cursor - 1] == 'E' || expr[cursor - 1] == 'e')
                                        && isDigit(expr[cursor - 2])) {
                                    cursor++;
                                    capture = true;
                                    continue;

                                }

                                break;


                            case '\u00AB': // special compact code for recursive parses
                            case '\u00BB':
                            case '\u00AC':
                            case '&':
                            case '^':
                            case '|':
                            case '*':
                            case '/':
                            case '%':
                                char op = expr[cursor];
                                if (lookAhead() == '=') {
                                    name = new String(expr, start, trimLeft(cursor) - start);

                                    start = cursor += 2;
                                    captureToEOS();

                                    if (union) {
                                        return lastNode = new DeepAssignmentNode(subArray(start, cursor), fields, _bwOpLookup(op), t);
                                    }
                                    else if ((idx = pCtx.variableIndexOf(name)) != -1) {
                                        return lastNode = new IndexedOperativeAssign(subArray(start, cursor), _bwOpLookup(op), idx, fields);
                                    }
                                    else {
                                        return lastNode = new OperativeAssign(name, subArray(start, cursor), _bwOpLookup(op), fields);
                                    }
                                }


                            case '<':
                                if ((lookAhead() == '<' && lookAhead(2) == '=')) {
                                    name = new String(expr, start, trimLeft(cursor) - start);

                                    start = cursor += 3;
                                    captureToEOS();

                                    if (union) {
                                        return lastNode = new DeepAssignmentNode(subArray(start, cursor), fields, BW_SHIFT_LEFT, t);
                                    }
                                    else if ((idx = pCtx.variableIndexOf(name)) != -1) {
                                        return lastNode = new IndexedOperativeAssign(subArray(start, cursor), BW_SHIFT_LEFT, idx, fields);
                                    }
                                    else {
                                        return lastNode = new OperativeAssign(name, subArray(start, cursor), BW_SHIFT_LEFT, fields);
                                    }
                                }
                                break;


                            case '>':
                                if (lookAhead() == '>') {
                                    if (lookAhead(2) == '=') {
                                        name = new String(expr, start, trimLeft(cursor) - start);

                                        start = cursor += 3;
                                        captureToEOS();

                                        if (union) {
                                            return lastNode = new DeepAssignmentNode(subArray(start, cursor), fields, BW_SHIFT_RIGHT, t);
                                        }
                                        else if ((idx = pCtx.variableIndexOf(name)) != -1) {
                                            return lastNode = new IndexedOperativeAssign(subArray(start, cursor), BW_SHIFT_RIGHT, idx, fields);
                                        }
                                        else {
                                            return lastNode = new OperativeAssign(name, subArray(start, cursor), BW_SHIFT_RIGHT, fields);
                                        }
                                    }
                                    else if ((lookAhead(2) == '>' && lookAhead(3) == '=')) {
                                        name = new String(expr, start, trimLeft(cursor) - start);

                                        start = cursor += 4;
                                        captureToEOS();

                                        if (union) {
                                            return lastNode = new DeepAssignmentNode(subArray(start, cursor), fields, BW_USHIFT_RIGHT, t);
                                        }
                                        else if ((idx = pCtx.variableIndexOf(name)) != -1) {
                                            return lastNode = new IndexedOperativeAssign(subArray(start, cursor), BW_USHIFT_RIGHT, idx, fields);
                                        }
                                        else {
                                            return lastNode = new OperativeAssign(name, subArray(start, cursor), BW_USHIFT_RIGHT, fields);
                                        }
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
                                skipWhitespaceWithLineAccounting();
                                continue;

                            case '{':
                                if (!union) break;
                                cursor = balancedCapture(expr, cursor, '{') + 1;
                                continue;


                            case '~':
                                if (lookAhead() == '=') {
                                    tmp = subArray(start, trimLeft(cursor));

                                    start = cursor += 2;

                                    if (!isNextIdentifierOrLiteral()) {
                                        throw new CompileException("unexpected symbol '" + expr[cursor] + "'", expr, cursor);
                                    }

                                    captureToEOT();

                                    return lastNode = new RegExMatch(tmp, fields, subArray(start, cursor));
                                }
                                break;

                            case '=':
                                if (lookAhead() == '+') {
                                    name = new String(expr, start, trimLeft(cursor) - start);

                                    start = cursor += 2;

                                    if (!isNextIdentifierOrLiteral()) {
                                        throw new CompileException("unexpected symbol '" + expr[cursor] + "'", expr, cursor);
                                    }

                                    captureToEOS();

                                    if ((idx = pCtx.variableIndexOf(name)) != -1) {
                                        return lastNode = new IndexedOperativeAssign(subArray(start, cursor), ADD, idx, fields);
                                    }
                                    else {
                                        return lastNode = new OperativeAssign(name, subArray(start, cursor), ADD, fields);
                                    }
                                }
                                else if (lookAhead() == '-') {
                                    name = new String(expr, start, trimLeft(cursor) - start);

                                    start = cursor += 2;

                                    if (!isNextIdentifierOrLiteral()) {
                                        throw new CompileException("unexpected symbol '" + expr[cursor] + "'", expr, cursor);
                                    }

                                    captureToEOS();

                                    if ((idx = pCtx.variableIndexOf(name)) != -1) {
                                        return lastNode = new IndexedOperativeAssign(subArray(start, cursor), SUB, idx, fields);
                                    }
                                    else {
                                        return lastNode = new OperativeAssign(name, subArray(start, cursor), SUB, fields);
                                    }
                                }

                                if (greedy && lookAhead() != '=') {
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
                                            TypeDescriptor tDescr = new TypeDescriptor(((String) lastNode.getLiteralValue()).toCharArray(), 0);

                                            try {
                                                lastNode.setLiteralValue(getClassReference(pCtx, tDescr));
                                                lastNode.discard();
                                            }
                                            catch (Exception e) {
                                                // fall through;
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
                                    else if (pCtx != null
                                            && ((idx = pCtx.variableIndexOf(t)) != -1
                                            || (pCtx.isIndexAllocation()))) {

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
                    switch (expr[cursor]) {
                        case '.': {
                            cursor++;
                            if (isDigit(expr[cursor])) {
                                capture = true;
                                continue;
                            }
                            expectNextChar_IW('{');

                            tmp = subArray(start, cursor - 1);
                            cursor = balancedCapture(expr, start = cursor, '{') + 1;

                            return lastNode = new ThisWithNode(tmp, subArray(start + 1, cursor - 1), fields);
                        }

                        case '@': {
                            start++;
                            captureToEOT();

                            if (pCtx.getInterceptors() == null || !pCtx.getInterceptors().
                                    containsKey(name = new String(expr, start, cursor - start))) {
                                throw new CompileException("reference to undefined interceptor: " + new String(expr, start, cursor - start), expr, cursor);
                            }

                            return lastNode = new InterceptorWrapper(pCtx.getInterceptors().get(name), nextToken());
                        }

                        case '=':
                            return createOperator(expr, start, (cursor += 2));

                        case '-':
                            if (lookAhead() == '-') {
                                cursor += 2;
                                skipWhitespace();
                                start = cursor;
                                captureIdentifier();

                                if ((idx = pCtx.variableIndexOf(name = new String(subArray(start, cursor)))) != -1) {
                                    return lastNode = new IndexedPreFixDecNode(idx);
                                }
                                else {
                                    return lastNode = new PreFixDecNode(name);
                                }
                            }
                            else if ((cursor != 0 && !isWhitespace(lookBehind())) || !isDigit(lookAhead())) {
                                return createOperator(expr, start, cursor++ + 1);
                            }
                            else if ((cursor - 1) != 0 || (!isDigit(lookBehind())) && isDigit(lookAhead())) {
                                cursor++;
                                break;
                            }

                        case '+':
                            if (lookAhead() == '+') {
                                cursor += 2;
                                skipWhitespace();
                                start = cursor;
                                captureIdentifier();

                                if ((idx = pCtx.variableIndexOf(name = new String(subArray(start, cursor)))) != -1) {
                                    return lastNode = new IndexedPreFixIncNode(idx);
                                }
                                else {
                                    return lastNode = new PreFixIncNode(name);
                                }
                            }
                            return createOperator(expr, start, cursor++ + 1);

                        case '*':
                            if (lookAhead() == '*') {
                                cursor++;
                            }
                            return createOperator(expr, start, cursor++ + 1);

                        case ';':
                            cursor++;
                            lastWasIdentifier = false;
                            return lastNode = new EndOfStatement();

                        case '#':
                        case '/':
                            switch (skipCommentBlock()) {
                                case OP_TERMINATE:
                                    return null;
                                case OP_RESET_FRAME:
                                    continue;
                            }

                        case '?':
                        case ':':
                        case '^':
                        case '%': {
                            return createOperator(expr, start, cursor++ + 1);
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
                                        if (lookAhead() == 'n' && isWhitespace(lookAhead(2))) {

                                            for (int level = brace; cursor != length; cursor++) {
                                                switch (expr[cursor]) {
                                                    case '(':
                                                        brace++;
                                                        break;
                                                    case ')':
                                                        if (--brace < level) {
                                                            cursor++;
                                                            if (tokenContinues()) {
                                                                lastNode = new Fold(subset(expr, trimRight(start + 1), cursor - start - 2), fields);
                                                                start = cursor;
                                                                if (expr[start] == '.') start++;
                                                                captureToEOT();
                                                                return lastNode = new Union(expr, trimRight(start), cursor, fields, lastNode);
                                                            }
                                                            else {
                                                                return lastNode = new Fold(subset(expr, trimRight(start + 1), cursor - start - 2), fields);
                                                            }

                                                        }
                                                        break;
                                                    case '\'':
                                                        cursor = captureStringLiteral('\'', expr, cursor, length);
                                                        break;
                                                    case '"':
                                                        cursor = captureStringLiteral('\"', expr, cursor, length);
                                                        break;
                                                }
                                            }

                                            throw new CompileException("unterminated projection; closing parathesis required", expr, cursor);
                                        }
                                        break;

                                    default:
                                        /**
                                         * Check to see if we should disqualify this current token as a potential
                                         * type-cast candidate.
                                         */

                                        if (lastWS && expr[cursor] != '.') {
                                            switch (expr[cursor]) {
                                                case '[':
                                                case ']':
                                                    break;

                                                default:
                                                    if (!(isIdentifierPart(expr[cursor]) || expr[cursor] == '.')) {
                                                        singleToken = false;
                                                    }
                                            }
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

                            //todo: support typecast to array types
                            tmp = null;
                            if (singleToken) {
                                int st;
                                TypeDescriptor tDescr = new TypeDescriptor(tmp = subset(expr, st = trimRight(start + 1), trimLeft(cursor - 1) - st), fields);

                                Class cls;
                                if (tDescr.getClassName() != null) {
                                    try {
                                        cls = getClassReference(pCtx, tDescr);

                                        start = cursor;
                                        captureToEOS();

                                        return lastNode = new TypeCast(subset(expr, start, cursor - start), cls, fields);
                                    }
                                    catch (Exception e) {
                                        // fallthrough
                                    }
                                }
                            }

                            if (tmp != null) {
                                return handleUnion(handleSubstatement(new Substatement(tmp, fields)));
                            }
                            else {
                                return handleUnion(handleSubstatement(new Substatement(subset(expr, start = trimRight(start + 1), trimLeft(cursor - 1) - start), fields)));
                            }
                        }

                        case '}':
                        case ']':
                        case ')': {
                            throw new CompileException("unbalanced braces", expr, cursor);
                        }

                        case '>': {
                            if (expr[cursor + 1] == '>') {
                                if (expr[cursor += 2] == '>') cursor++;
                                return createOperator(expr, start, cursor);
                            }
                            else if (expr[cursor + 1] == '=') {
                                return createOperator(expr, start, cursor += 2);
                            }
                            else {
                                return createOperator(expr, start, ++cursor);
                            }
                        }

                        case '<': {
                            if (expr[++cursor] == '<') {
                                if (expr[++cursor] == '<') cursor++;
                                return createOperator(expr, start, cursor);
                            }
                            else if (expr[cursor] == '=') {
                                return createOperator(expr, start, ++cursor);
                            }
                            else {
                                return createOperator(expr, start, cursor);
                            }
                        }

                        case '\'':
                        case '"':
                            lastNode = new LiteralNode(handleStringEscapes(subset(expr, start + 1,
                                    (cursor = captureStringLiteral(expr[cursor], expr, cursor, length)) - start - 1))
                                    , String.class);

                            cursor++;

                            if (tokenContinues()) {
                                return lastNode = handleUnion(lastNode);
                            }

                            return lastNode;

                        case '&': {
                            if (expr[cursor++ + 1] == '&') {
                                return createOperator(expr, start, ++cursor);
                            }
                            else {
                                return createOperator(expr, start, cursor);
                            }
                        }

                        case '|': {
                            if (expr[cursor++ + 1] == '|') {
                                return new OperatorNode(OPERATORS.get(new String(expr, start, ++cursor - start)));
                            }
                            else {
                                return createOperator(expr, start, cursor);
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
                                return createOperator(expr, start, cursor);
                            }

                        case '!': {
                            ++cursor;
                            if (isNextIdentifier()) {
                                if (lastNode != null && !lastNode.isOperator()) {
                                    throw new CompileException("unexpected operator '!'", expr, cursor);
                                }

                                start = cursor;
                                captureToEOT();
                                if ("new".equals(name = new String(expr, start, cursor - start))
                                        || "isdef".equals(name)) {
                                    captureToEOT();
                                    return lastNode = new Negation(subset(expr, start, cursor - start), fields);
                                }
                                else {
                                    return lastNode = new Negation(name.toCharArray(), fields);
                                }
                            }
                            else if (expr[cursor] == '(') {
                                start = cursor--;
                                captureToEOT();
                                return lastNode = new Negation(subset(expr, start, cursor - start), fields);
                            }
                            else if (expr[cursor] != '=')
                                throw new CompileException("unexpected operator '!'", expr, cursor, null);
                            else {
                                return createOperator(expr, start, ++cursor);
                            }
                        }

                        case '[':
                        case '{':
                            cursor = balancedCapture(expr, cursor, expr[cursor]) + 1;
                            if (tokenContinues()) {
                                lastNode = new InlineCollectionNode(expr, start, start = cursor, fields);
                                captureToEOT();
                                if (expr[start] == '.') start++;
                                return lastNode = new Union(expr, start, cursor, fields, lastNode);
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
        catch (NumberFormatException e) {
            CompileException c = new CompileException("badly formatted number: " + e.getMessage(), expr, cursor, e);
            c.setLineNumber(line);
            c.setColumn(cursor - lastLineStart);
            throw c;
        }
        catch (StringIndexOutOfBoundsException e) {
            CompileException c = new CompileException("unexpected end of statement", expr, cursor, e);
            c.setLineNumber(line);
            c.setColumn(cursor - lastLineStart);
            throw c;
        }
        catch (ArrayIndexOutOfBoundsException e) {
            CompileException c = new CompileException("unexpected end of statement", expr, cursor, e);
            c.setLineNumber(line);
            c.setColumn(cursor - lastLineStart);
            throw c;
        }
        catch (CompileException e) {
            e.setExpr(expr);
            e.setLineNumber(line);
            e.setColumn(cursor - lastLineStart);
            throw e;
        }
    }

    public ASTNode handleSubstatement(Substatement stmt) {
        if (stmt.getStatement() != null && stmt.getStatement().isLiteralOnly()) {
            return new LiteralNode(stmt.getStatement().getValue(null, null, null));
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
        }
        return lastNode = node;
    }

    private ASTNode createOperator(final char[] expr, final int start, final int end) {
        lastWasIdentifier = false;
        return lastNode = new OperatorNode(OPERATORS.get(new String(expr, start, end - start)));
    }

    private char[] subArray(final int start, final int end) {
        if (start >= end) return new char[0];

        char[] newA = new char[end - start];
        for (int i = 0; i != newA.length; i++) {
            newA[i] = expr[i + start];
        }

        return newA;
    }

    //todo: improve performance of this method
    private ASTNode createPropertyToken(int start, int end) {
        lastWasIdentifier = true;
        String tmp;
        if (parserContext != null && parserContext.get() != null && parserContext.get().hasImports()) {
            char[] _subset = subset(expr, start, cursor - start);
            int offset;

            if ((offset = findFirst('.', _subset)) != -1) {
                String iStr = new String(_subset, 0, offset);
                if (pCtx.hasImport(iStr)) {
                    return lastNode = new LiteralDeepPropertyNode(subset(_subset, offset + 1, _subset.length - offset - 1),
                            fields, pCtx.getImport(iStr));
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
            }
        }

        if ((fields & ASTNode.METHOD) != 0) {
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

        if (isStatementNotManuallyTerminated()) {
            splitAccumulator.add(new EndOfStatement());
        }

        switch (type) {
            case ASTNode.BLOCK_IF:
                return new IfNode(subArray(condStart, condEnd), subArray(blockStart, blockEnd), fields);
            case ASTNode.BLOCK_FOR:
                for (int i = condStart; i < condEnd; i++) {
                    if (expr[i] == ';')
                        return new ForNode(subArray(condStart, condEnd), subArray(blockStart, blockEnd), fields);
                    else if (expr[i] == ':')
                        break;
                }

            case ASTNode.BLOCK_FOREACH:
                return new ForEachNode(subArray(condStart, condEnd), subArray(blockStart, blockEnd), fields);
            case ASTNode.BLOCK_WHILE:
                return new WhileNode(subArray(condStart, condEnd), subArray(blockStart, blockEnd), fields);
            case ASTNode.BLOCK_UNTIL:
                return new UntilNode(subArray(condStart, condEnd), subArray(blockStart, blockEnd), fields);
            case ASTNode.BLOCK_DO:
                return new DoNode(subArray(condStart, condEnd), subArray(blockStart, blockEnd), fields);
            case ASTNode.BLOCK_DO_UNTIL:
                return new DoUntilNode(subArray(condStart, condEnd), subArray(blockStart, blockEnd));
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
                while (ifThenElseBlockContinues());

                return first;
            }

            case ASTNode.BLOCK_DO:
                skipWhitespaceWithLineAccounting();
                return _captureBlock(null, expr, false, type);

            default: // either BLOCK_WITH or BLOCK_FOREACH
                captureToNextTokenJunction();
                skipWhitespaceWithLineAccounting();
                return _captureBlock(null, expr, true, type);
        }
    }

    private ASTNode _captureBlock(ASTNode node, final char[] expr, boolean cond, int type) {
        skipWhitespace();
        int startCond = 0;
        int endCond = 0;

        int blockStart;
        int blockEnd;

        String name;

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
             * Check to see if the name is legal.
             */
            if (isReservedWord(name = createStringTrimmed(expr, start, (startCond = cursor) - start))
                    || isNotValidNameorLabel(name))
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
                    throw new CompileException("incomplete statement", expr, cursor);
                }
                else if (expr[cursor] == '{') {
                    blockEnd = cursor = balancedCapture(expr, blockStart = cursor, '{');
                }
                else {
                    blockStart = cursor - 1;
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
                    blockEnd = cursor = balancedCapture(expr, blockStart = cursor, '{');
                }
                else {
                    /**
                     * This is a single statement function declaration.  We only capture the statement.
                     */
                    blockStart = cursor - 1;
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
            if (isStatementNotManuallyTerminated()) {
                /**
                 * Add an EndOfStatement to the split accumulator in the parser.
                 */
                splitAccumulator.add(new EndOfStatement());
            }

            /**
             * Produce the funciton node.
             */
            return new Function(name, subArray(startCond, endCond), subArray(blockStart, blockEnd));
        }
        else if (cond) {
            if (expr[cursor] != '(') {
                throw new CompileException("expected '(' but encountered: " + expr[cursor]);
            }

            /**
             * This block is an: IF, FOREACH or WHILE node.
             */
            int[] cap = balancedCaptureWithLineAccounting(expr, startCond = cursor, '(');

            endCond = cursor = cap[0];

            startCond++;
            cursor++;

            pCtx.incrementLineCount(cap[1]);
        }

        skipWhitespace();

        if (cursor >= length) {
            throw new CompileException("unbalanced braces", expr, cursor);
        }
        else if (expr[cursor] == '{') {
            int[] cap = balancedCaptureWithLineAccounting(expr, blockStart = cursor, '{');
            blockEnd = cursor = cap[0];
            pCtx.incrementLineCount(cap[1]);
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
        else if (type == ASTNode.BLOCK_DO) {
            cursor++;
            skipWhitespaceWithLineAccounting();
            start = cursor;
            captureToNextTokenJunction();

            if ("while".equals(name = new String(expr, start, cursor - start))) {
                skipWhitespaceWithLineAccounting();
                startCond = cursor + 1;
                int[] cap = balancedCaptureWithLineAccounting(expr, cursor, '(');
                endCond = cursor = cap[0];
                pCtx.incrementLineCount(cap[1]);
                return createBlockToken(startCond, endCond, trimRight(blockStart + 1), trimLeft(blockEnd), type);
            }
            else if ("until".equals(name)) {
                skipWhitespaceWithLineAccounting();
                startCond = cursor + 1;
                int[] cap = balancedCaptureWithLineAccounting(expr, cursor, '(');
                endCond = cursor = cap[0];
                pCtx.incrementLineCount(cap[1]);
                return createBlockToken(startCond, endCond, trimRight(blockStart + 1), trimLeft(blockEnd), ASTNode.BLOCK_DO_UNTIL);
            }
            else {
                throw new CompileException("expected 'while' or 'until' but encountered: " + name, expr, cursor);
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
    protected boolean ifThenElseBlockContinues() {
        if ((cursor + 4) < length) {
            if (expr[cursor] != ';') cursor--;
            skipWhitespaceWithLineAccounting();
            skipCommentBlock();

            return expr[cursor] == 'e' && expr[cursor + 1] == 'l' && expr[cursor + 2] == 's' && expr[cursor + 3] == 'e'
                    && (isWhitespace(expr[cursor + 4]) || expr[cursor + 4] == '{');
        }
        return false;
    }

    protected int skipCommentBlock() {
        if (lookAhead() == expr[cursor]) {
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

            if ((start = cursor) >= length) return OP_TERMINATE;

            return OP_RESET_FRAME;
        }
        else if (expr[cursor] == '/' && lookAhead() == '*') {
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
                if (expr[cursor] == '*' && lookAhead() == '/') {
                    if ((cursor += 2) >= length) return OP_RESET_FRAME;
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

            return OP_RESET_FRAME;
        }

        return OP_CONTINUE;
    }

    /**
     * Checking from the current cursor position, check to see if we're inside a contiguous identifier.
     *
     * @return
     */
    protected boolean tokenContinues() {
        if (cursor == length) return false;
        else if (expr[cursor] == '.' || expr[cursor] == '[') return true;
        else if (isWhitespace(expr[cursor])) {
            int markCurrent = cursor;
            skipWhitespace();
            if (cursor != length && (expr[cursor] == '.' || expr[cursor] == '[')) return true;
            cursor = markCurrent;
        }
        return false;
    }


    protected void expectEOS() {
        skipWhitespace();
        if (cursor != length && expr[cursor] != ';') {
            switch (expr[cursor]) {
                case '&':
                    if (lookAhead() == '&') return;
                    else break;
                case '|':
                    if (lookAhead() == '|') return;
                    else break;
                case '!':
                    if (lookAhead() == '=') return;
                    else break;

                case '<':
                case '>':
                    return;

                case '=': {
                    switch (lookAhead()) {
                        case '=':
                        case '+':
                        case '-':
                        case '*':
                            return;
                    }
                    break;
                }

                case '+':
                case '-':
                case '/':
                case '*':
                    if (lookAhead() == '=') return;
                    else break;
            }

            throw new CompileException("expected end of statement but encountered: "
                    + (cursor == length ? "<end of stream>" : expr[cursor]), expr, cursor);
        }
    }

    protected boolean isNextIdentifier() {
        while (cursor != length && isWhitespace(expr[cursor])) cursor++;
        return cursor != length && isIdentifierPart(expr[cursor]);
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
                    if (cursor >= length) return;
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

    protected void captureIdentifier() {
        boolean captured = false;
        if (cursor == length) throw new CompileException("unexpected end of statement: EOF", expr, cursor);
        while (cursor != length) {
            switch (expr[cursor]) {
                case ';':
                    return;

                default: {
                    if (!isIdentifierPart(expr[cursor])) {
                        if (captured) return;
                        throw new CompileException("unexpected symbol (was expecting an identifier): " + expr[cursor], expr, cursor);
                    }
                    else {
                        captured = true;
                    }
                }
            }
            cursor++;
        }
    }

    /**
     * From the current cursor position, capture to the end of the current token.
     */
    protected void captureToEOT() {
        skipWhitespace();
        do {
            switch (expr[cursor]) {
                case '(':
                case '[':
                case '{':
                    if ((cursor = balancedCapture(expr, cursor, expr[cursor])) == -1) {
                        throw new CompileException("unbalanced braces", expr, cursor);
                    }
                    break;

                case '=':
                case '&':
                case '|':
                case ';':
                    return;

                case '.':
                    skipWhitespace();
                    break;

                case '\'':
                    cursor = captureStringLiteral('\'', expr, cursor, length);
                    break;
                case '"':
                    cursor = captureStringLiteral('"', expr, cursor, length);
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
        while (++cursor != length);
    }

    protected void captureToEOTNEW() {
        skipWhitespace();
        boolean dims = false;
        do {
            switch (expr[cursor]) {
                case '{':
                    if (dims) return;

                case '[':
                    dims = true;
                    if ((cursor = balancedCapture(expr, cursor, expr[cursor])) == -1) {
                        throw new CompileException("unbalanced braces", expr, cursor);
                    }
                    break;
                case '(':
                    if ((cursor = balancedCapture(expr, cursor, expr[cursor])) == -1) {
                        throw new CompileException("unbalanced braces", expr, cursor);
                    }
                    break;

                case '=':
                case '&':
                case '|':
                case ';':
                    return;

                case '.':
                    skipWhitespace();
                    break;

                case '\'':
                    cursor = captureStringLiteral('\'', expr, cursor, length);
                    break;
                case '"':
                    cursor = captureStringLiteral('"', expr, cursor, length);
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
        while (++cursor != length);
    }


    protected boolean lastNonWhite(char c) {
        int i = cursor - 1;
        while (isWhitespace(expr[i])) i--;
        return c == expr[i];
    }

    /**
     * From the specified cursor position, trim out any whitespace between the current position and the end of the
     * last non-whitespace character.
     *
     * @param pos - current position
     * @return new position.
     */
    protected int trimLeft(int pos) {
        while (pos != 0 && pos != start && isWhitespace(expr[pos - 1])) pos--;
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
                case '[':
                    int[] c = balancedCaptureWithLineAccounting(expr, cursor, '[');
                    cursor = c[0] + 1;
                    line += c[1];
                    continue;

                default:
                    if (isWhitespace(expr[cursor])) {
                        return;
                    }
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
    public static boolean isNotValidNameorLabel(String name) {
        for (char c : name.toCharArray()) {
            if (c == '.') return true;
            else if (!isIdentifierPart(c)) return true;
        }
        return false;
    }

    protected void setExpression(String expression) {
        if (expression != null && !"".equals(expression)) {
            if ((this.expr = EX_PRECACHE.get(expression)) == null) {
                synchronized (EX_PRECACHE) {
                    length = (this.expr = expression.toCharArray()).length;

                    // trim any whitespace.
                    while (length != 0 && isWhitespace(this.expr[length - 1])) length--;

                    char[] e = new char[length];

                    for (int i = 0; i != e.length; i++)
                        e[i] = expr[i];

                    EX_PRECACHE.put(expression, e);
                }
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
        if (cursor < length) return expr[cursor + 1];
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


    protected boolean isNextIdentifierOrLiteral() {
        int tmp = cursor;
        if (tmp == length) return false;
        else {
            while (tmp != length && isWhitespace(expr[tmp])) tmp++;
            if (tmp == length) return false;
            char n = expr[tmp];
            return isIdentifierPart(n) || isDigit(n) || n == '\'' || n == '"';
        }
    }

    public int nextNonBlank() {
        if ((cursor + 1) >= length) {
            return -1;
        }
        int i = cursor;
        while (i != length && isWhitespace(expr[i])) i++;
        return i;
    }

    public void expectNextChar_IW(char c) {
        nextNonBlank();
        if (cursor == length) throw new CompileException("unexpected end of statement", expr, cursor);
        if (expr[cursor] != c)
            throw new CompileException("unexpected character ('" + expr[cursor] + "'); was expecting: " + c);
    }


    /**
     * NOTE: This method assumes that the current position of the cursor is at the end of a logical statement, to
     * begin with.
     * <p/>
     * Determines whether or not the logical statement is manually terminated with a statement separator (';').
     *
     * @return
     */
    protected boolean isStatementNotManuallyTerminated() {
        if (cursor >= length) return false;
        int c = cursor;
        while (c != length && isWhitespace(expr[c])) c++;
        return !(c != length && expr[c] == ';');
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
                    return pCtx;

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
                operatorsTable.put("isdef", ISDEF);

            case 4: // assignment
                operatorsTable.put("=", ASSIGN);
                operatorsTable.put("var", UNTYPED_VAR);
                operatorsTable.put("+=", ASSIGN_ADD);
                operatorsTable.put("-=", ASSIGN_SUB);
                operatorsTable.put("/=", ASSIGN_DIV);
                operatorsTable.put("%=", ASSIGN_MOD);

            case 3: // iteration
                operatorsTable.put("foreach", FOREACH);
                operatorsTable.put("while", WHILE);
                operatorsTable.put("until", UNTIL);
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
        return operator != -1 && operator < 6;
    }

    protected int arithmeticFunctionReduction(int operator) {
        ASTNode tk;
        int operator2;

        boolean x = false;
        int y = 0;

        /**
         * If the next token is an operator, we check to see if it has a higher
         * precdence.
         */
        if ((tk = nextToken()) != null) {
            if (isArithmeticOperator(operator2 = tk.getOperator()) && PTABLE[operator2] > PTABLE[operator]) {
                xswap();
                /**
                 * The current arith. operator is of higher precedence the last.
                 */
                dStack.push(operator = operator2, nextToken().getReducedValue(ctx, ctx, variableFactory));

                while (true) {
                    // look ahead again

                    if ((tk = nextToken()) != null && (operator2 = tk.getOperator()) != -1
                            && operator2 != 37 && PTABLE[operator2] > PTABLE[operator]) {
                        // if we have back to back operations on the stack, we don't xswap
                        if (x) {
                            xswap();
                        }
                        /**
                         * This operator is of higher precedence, or the same level precedence.  push to the RHS.
                         */
                        dStack.push(operator = operator2, nextToken().getReducedValue(ctx, ctx, variableFactory));
                        y = 1;
                        continue;
                    }
                    else if (tk != null && operator2 != -1 && operator2 != 37) {
                        if (PTABLE[operator2] == PTABLE[operator]) {
                            // if we have back to back operations on the stack, we don't xswap             
                            if (x) {
                                xswap();
                            }

                            /**
                             * Reduce any operations waiting now.
                             */
                            while (!dStack.isEmpty()) {
                                dreduce();
                            }

                            /**
                             * This operator is of the same level precedence.  push to the RHS.
                             */
                            dStack.push(operator = operator2, nextToken().getReducedValue(ctx, ctx, variableFactory));

                            y++;
                            continue;
                        }
                        else {
                            /**
                             * The operator doesn't have higher precedence. Therfore reduce the LHS.
                             */
                            if (!dStack.isEmpty()) {
                                do {
                                    if (y == 1) {
                                        dreduce2();
                                        y = 0;
                                    }
                                    else {
                                        dreduce();
                                    }
                                }
                                while (dStack.size() > 1);
                            }

                            if (!dStack.isEmpty()) {
                                stk.push(dStack.pop());
                                xswap();
                            }

                            operator = tk.getOperator();
                            // Reduce the lesser or equal precedence operations.
                            while (stk.size() != 1 && stk.peek2() instanceof Integer &&
                                    ((operator2 = (Integer) stk.peek2()) < PTABLE.length) &&
                                    PTABLE[operator2] >= PTABLE[operator]) {
                                xswap();
                                reduce();
                            }

                            y = 0;
                        }
                    }
                    else {
                        /**
                         * There are no more tokens.
                         */
                        x = false;

                        if (dStack.size() > 1) {
                            do {
                                if (y == 1) {
                                    dreduce2();
                                    y = 0;
                                }
                                else {
                                    dreduce();
                                }
                            }
                            while (dStack.size() > 1);

                            x = true;
                        }

                        if (!dStack.isEmpty()) {
                            stk.push(dStack.pop());
                        }
                        else if (x) {
                            xswap();
                        }

                        y = 0;
                        break;
                    }

                    if (tk != null && (tk = nextToken()) != null) {
                        switch (operator) {
                            case AND: {
                                if (!((Boolean) stk.peek())) return OP_TERMINATE;
                                else {
                                    splitAccumulator.add(tk);
                                    return AND;
                                }
                            }
                            case OR: {
                                if (((Boolean) stk.peek())) return OP_TERMINATE;
                                else {
                                    splitAccumulator.add(tk);
                                    return OR;
                                }
                            }

                            default:
                                stk.push(tk.getReducedValue(ctx, ctx, variableFactory), operator);
                        }
                    }

                    x = true;
                    y = 0;
                }
            }
            else if (!tk.isOperator()) {
                throw new CompileException("unexpected token: " + tk.getName());
            }
            else {
                reduce();
                splitAccumulator.push(tk);
            }
        }

        // while any values remain on the stack
        // keep XSWAPing and reducing, until there is nothing left.
        while (stk.size() > 1) {
            reduce();
            if (stk.size() > 1) xswap();
        }

        return OP_RESET_FRAME;
    }

    private void dreduce() {
        stk.push(dStack.pop(), dStack.pop());

        // reduce the top of the stack
        reduce();
    }

    private void dreduce2() {
        Object o1, o2;

        o1 = dStack.pop();
        o2 = dStack.pop();

        if (!dStack.isEmpty()) stk.push(dStack.pop());

        stk.push(o1);
        stk.push(o2);

        reduce();
    }

    /**
     * XSWAP.
     */
    private void xswap() {
        stk.push(stk.pop(), stk.pop());
    }

    /**
     * This method is called when we reach the point where we must subEval a trinary operation in the expression.
     * (ie. val1 op val2).  This is not the same as a binary operation, although binary operations would appear
     * to have 3 structures as well.  A binary structure (or also a junction in the expression) compares the
     * current state against 2 downrange structures (usually an op and a val).
     */
    protected void reduce() {
        Object v1, v2;
        int operator;
        try {
            switch (operator = (Integer) stk.pop()) {
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
                    stk.push(doOperations(stk.peek2(), operator, stk.pop2()));
                    break;

                case AND:
                    v1 = stk.pop();
                    stk.push(((Boolean) stk.pop()) && ((Boolean) v1));
                    break;

                case OR:
                    v1 = stk.pop();
                    stk.push(((Boolean) stk.pop()) || ((Boolean) v1));
                    break;

                case CHOR:
                    v1 = stk.pop();
                    if (!isEmpty(v2 = stk.pop()) || !isEmpty(v1)) {
                        stk.clear();
                        stk.push(!isEmpty(v2) ? v2 : v1);
                        return;
                    }
                    else stk.push(null);
                    break;

                case REGEX:
                    stk.push(java.util.regex.Pattern.compile(java.lang.String.valueOf(stk.pop())).matcher(java.lang.String.valueOf(stk.pop())).matches());
                    break;

                case INSTANCEOF:
                    if ((v1 = stk.pop()) instanceof Class)
                        stk.push(((Class) v1).isInstance(stk.pop()));
                    else
                        stk.push(currentThread().getContextClassLoader().loadClass(java.lang.String.valueOf(v1)).isInstance(stk.pop()));

                    break;

                case CONVERTABLE_TO:
                    if ((v1 = stk.pop()) instanceof Class)
                        stk.push(org.mvel2.DataConversion.canConvert(stk.pop().getClass(), (Class) v1));
                    else
                        stk.push(org.mvel2.DataConversion.canConvert(stk.pop().getClass(), currentThread().getContextClassLoader().loadClass(java.lang.String.valueOf(v1))));
                    break;

                case CONTAINS:
                    stk.push(containsCheck(stk.peek2(), stk.pop2()));
                    break;

                case BW_AND:
                    stk.push(asInt(stk.peek2()) & asInt(stk.pop2()));
                    break;

                case BW_OR:
                    stk.push(asInt(stk.peek2()) | asInt(stk.pop2()));
                    break;

                case BW_XOR:
                    stk.push(asInt(stk.peek2()) ^ asInt(stk.pop2()));
                    break;

                case BW_SHIFT_LEFT:
                    stk.push(asInt(stk.peek2()) << asInt(stk.pop2()));
                    break;

                case BW_USHIFT_LEFT:
                    int iv2 = asInt(stk.peek2());
                    if (iv2 < 0) iv2 *= -1;
                    stk.push(iv2 << asInt(stk.pop2()));
                    break;

                case BW_SHIFT_RIGHT:
                    stk.push(asInt(stk.peek2()) >> asInt(stk.pop2()));
                    break;

                case BW_USHIFT_RIGHT:
                    stk.push(asInt(stk.peek2()) >>> asInt(stk.pop2()));
                    break;

                case STR_APPEND:
                    stk.push(new StringAppender(java.lang.String.valueOf(stk.peek2()))
                            .append(java.lang.String.valueOf(stk.pop2())).toString());
                    break;

                case SOUNDEX:
                    stk.push(Soundex.soundex(java.lang.String.valueOf(stk.pop()))
                            .equals(Soundex.soundex(java.lang.String.valueOf(stk.pop()))));
                    break;

                case SIMILARITY:
                    stk.push(similarity(java.lang.String.valueOf(stk.pop()), java.lang.String.valueOf(stk.pop())));
                    break;
            }
        }
        catch (ClassCastException e) {
            throw new CompileException("syntax error or incompatable types", expr, cursor, e);
        }
        catch (ArithmeticException e) {
            throw new CompileException("arithmetic error: " + e.getMessage(), e);
        }
        catch (Exception e) {
            throw new CompileException("failed to subEval expression", e);
        }
    }

    private static int _bwOpLookup(char c) {
        switch (c) {
            case '|':
                return Operator.BW_OR;
            case '&':
                return Operator.BW_AND;
            case '^':
                return Operator.BW_XOR;
            case '*':
                return Operator.MULT;
            case '/':
                return Operator.DIV;
            case '+':
                return Operator.ADD;
            case '%':
                return Operator.MOD;
            case '\u00AB':
                return Operator.BW_SHIFT_LEFT;
            case '\u00BB':
                return Operator.BW_SHIFT_RIGHT;
            case '\u00AC':
                return Operator.BW_USHIFT_RIGHT;
        }
        return -1;
    }

    private static int asInt(final Object o) {
        return (Integer) o;
    }
}
