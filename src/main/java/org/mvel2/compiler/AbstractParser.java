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
import org.mvel2.ErrorDetail;
import org.mvel2.Operator;
import static org.mvel2.Operator.*;
import org.mvel2.ParserContext;
import org.mvel2.ast.*;
import static org.mvel2.ast.TypeDescriptor.getClassReference;
import org.mvel2.integration.VariableResolverFactory;
import static org.mvel2.util.ArrayTools.findFirst;
import static org.mvel2.util.ArrayTools.isLiteralOnly;
import org.mvel2.util.ExecutionStack;
import org.mvel2.util.FunctionParser;
import static org.mvel2.util.ParseTools.*;
import static org.mvel2.util.PropertyTools.isEmpty;
import org.mvel2.util.ProtoParser;
import org.mvel2.util.Soundex;

import java.io.Serializable;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Double.parseDouble;
import static java.lang.Runtime.getRuntime;
import static java.lang.System.getProperty;
import static java.lang.Thread.currentThread;
import java.util.HashMap;
import java.util.WeakHashMap;

/**
 * This is the core parser that the subparsers extend.
 *
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
    protected boolean compileMode = false;

    protected int literalOnly = -1;

    protected int lastLineStart = 0;
    protected int line = 0;

    protected ASTNode lastNode;

    private static final WeakHashMap<String, char[]> EX_PRECACHE = new WeakHashMap<String, char[]>(15);

    public static HashMap<String, Object> LITERALS;
    public static HashMap<String, Integer> OPERATORS;

    protected ExecutionStack stk;
    protected ExecutionStack splitAccumulator = new ExecutionStack();

    protected static ThreadLocal<ParserContext> parserContext;
    protected ParserContext pCtx;
    protected ExecutionStack dStack;
    protected Object ctx;
    protected VariableResolverFactory variableFactory;

    protected boolean debugSymbols = false;

    static {
        setupParser();
    }

    public static void setupParser() {
        if (LITERALS == null || LITERALS.isEmpty()) {
            LITERALS = new HashMap<String, Object>();
            OPERATORS = new HashMap<String, Integer>();

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

            LITERALS.put("Byte", Byte.class);
            LITERALS.put("byte", Byte.class);

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

            setLanguageLevel(Boolean.getBoolean("mvel.future.lang.support") ? 6 : 5);
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

            if (!splitAccumulator.isEmpty()) {
                lastNode = (ASTNode) splitAccumulator.pop();
                if (cursor >= length && lastNode instanceof EndOfStatement) {
                    return nextToken();
                }
                else {
                    return lastNode;
                }
            }
            else if (cursor >= length) {
                return null;
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

            if ((fields & ASTNode.COMPILE_IMMEDIATE) != 0 && pCtx == null) {
                debugSymbols = (pCtx = getParserContext()).isDebugSymbols();
            }

            if (debugSymbols) {
                if (!lastWasLineLabel) {
                    if (pCtx.getSourceFile() == null) {
                        throw new CompileException("unable to produce debugging symbols: source name must be provided.");
                    }

                    line = pCtx.getLineCount();

                    skipWhitespace();

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
            skipWhitespace();

            /**
             * From here to the end of the method is the core MVEL parsing code.  Fiddling around here is asking for
             * trouble unless you really know what you're doing.
             */

            start = cursor;

            Mainloop:
            while (cursor != length) {
                if (isIdentifierPart(expr[cursor])) {
                    capture = true;
                    cursor++;

                    while (cursor != length && isIdentifierPart(expr[cursor])) cursor++;
                }

                /**
                 * If the current character under the cursor is a valid
                 * part of an identifier, we keep capturing.
                 */

                if (capture) {
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
                                    skipWhitespace();
                                }
                                while (cursor < length && expr[cursor] == '[');

                                /**
                                 * If it's not a dimentioned array, continue capturing if necessary.
                                 */
                                if (cursor < length && !lastNonWhite(']')) captureToEOT();

                                TypeDescriptor descr = new TypeDescriptor(subArray(start, cursor), fields);

                                if (pCtx == null) pCtx = getParserContext();

                                if (pCtx.hasProtoImport(descr.getClassName())) {
                                    return lastNode = new NewPrototypeNode(descr);
                                }

                                lastNode = new NewObjectNode(descr, fields, pCtx);

                                skipWhitespace();
                                if (cursor != length && expr[cursor] == '{') {
                                    if (!((NewObjectNode) lastNode).getTypeDescr().isUndimensionedArray()) {
                                        throw new CompileException(
                                                "conflicting syntax: dimensioned array with initializer block", expr, cursor);
                                    }

                                    start = cursor;
                                    Class egressType = lastNode.getEgressType();

                                    if (egressType == null) {
                                        try {
                                            egressType = getClassReference(pCtx, descr);
                                        }
                                        catch (ClassNotFoundException e) {
                                            throw new CompileException("could not instantiate class", e);
                                        }
                                    }

                                    cursor = balancedCaptureWithLineAccounting(expr, cursor, expr[cursor], pCtx) + 1;
                                    if (tokenContinues()) {
                                        lastNode = new InlineCollectionNode(expr, start, start = cursor, fields,
                                                egressType, pCtx);
                                        captureToEOT();
                                        return lastNode = new Union(expr, start + 1, cursor, fields, lastNode);
                                    }
                                    else {
                                        return lastNode = new InlineCollectionNode(expr, start, cursor, fields,
                                                egressType, pCtx);
                                    }
                                }
                                else if (((NewObjectNode) lastNode).getTypeDescr().isUndimensionedArray()) {
                                    throw new CompileException("array initializer expected", expr, cursor);
                                }

                                return lastNode;

                            case ASSERT:
                                start = cursor = trimRight(cursor);
                                captureToEOS();
                                return lastNode = new AssertNode(subArray(start, cursor--), fields, pCtx);

                            case RETURN:
                                start = cursor = trimRight(cursor);
                                captureToEOS();
                                return lastNode = new ReturnNode(subArray(start, cursor), fields, pCtx);

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

                            case PROTO:
                                return captureCodeBlock(PROTO);

                            case ISDEF:
                                start = cursor = trimRight(cursor);
                                captureToNextTokenJunction();
                                return lastNode = new IsDef(subArray(start, cursor));

                            case IMPORT:
                                start = cursor = trimRight(cursor);
                                captureToEOS();
                                ImportNode importNode = new ImportNode(subArray(start, cursor));

                                if (pCtx == null) pCtx = getParserContext();

                                if (importNode.isPackageImport()) {
                                    pCtx.addPackageImport(importNode.getPackageImport());
                                }
                                else {
                                    pCtx.addImport(importNode.getImportClass().getSimpleName(), importNode.getImportClass());
                                }
                                return lastNode = importNode;

                            case IMPORT_STATIC:
                                start = cursor = trimRight(cursor);
                                captureToEOS();
                                return lastNode = new StaticImportNode(subArray(start, cursor));

                            case FUNCTION:
                                lastNode = captureCodeBlock(FUNCTION);
                                start = cursor + 1;
                                return lastNode;

                            case UNTYPED_VAR:
                                int end;
                                start = cursor + 1;

                                while (true) {
                                    captureToEOT();
                                    end = cursor;
                                    skipWhitespace();

                                    if (cursor != length && expr[cursor] == '=') {
                                        if (end == (cursor = start))
                                            throw new CompileException("illegal use of reserved word: var");

                                        continue Mainloop;
                                    }
                                    else {
                                        name = new String(subArray(start, end));
                                        if (pCtx != null && (idx = pCtx.variableIndexOf(name)) != -1) {
                                            splitAccumulator.add(lastNode = new IndexedDeclTypedVarNode(idx, Object.class));
                                        }
                                        else {
                                            splitAccumulator.add(lastNode = new DeclTypedVarNode(name, Object.class, fields, pCtx));
                                        }
                                    }

                                    if (cursor == length || expr[cursor] != ',') break;
                                    else {
                                        cursor++;
                                        skipWhitespace();
                                        start = cursor;
                                    }
                                }

                                return (ASTNode) splitAccumulator.pop();
                        }
                    }

                    skipWhitespace();

                    /**
                     * If we *were* capturing a token, and we just hit a non-identifier
                     * character, we stop and figure out what to do.
                     */
                    if (cursor != length && expr[cursor] == '(') {
                        cursor = balancedCaptureWithLineAccounting(expr, cursor, '(', pCtx) + 1;
                    }

                    /**
                     * If we encounter any of the following cases, we are still dealing with
                     * a contiguous token.
                     */
                    CaptureLoop:
                    while (cursor != length) {
                        switch (expr[cursor]) {
                            case '.':
                                union = true;
                                cursor++;
                                skipWhitespace();

                                continue;

                            case '?':
                                if (lookToLast() == '.') {
                                    union = true;
                                    cursor++;
                                    continue;
                                }
                                else {
                                    break CaptureLoop;
                                }

                            case '+':
                                switch (lookAhead()) {
                                    case '+':
                                        name = new String(subArray(start, trimLeft(cursor)));
                                        if (pCtx != null && (idx = pCtx.variableIndexOf(name)) != -1) {
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
                                            return lastNode = new DeepAssignmentNode(subArray(start, cursor), fields,
                                                    ADD, name, pCtx);
                                        }
                                        else if (pCtx != null && (idx = pCtx.variableIndexOf(name)) != -1) {
                                            return lastNode = new IndexedAssignmentNode(subArray(start, cursor), fields,
                                                    ADD, name, idx, pCtx);
                                        }
                                        else {
                                            return lastNode = new OperativeAssign(name, subArray(start, cursor),
                                                    ADD, fields, pCtx);
                                        }
                                }

                                if (isDigit(lookAhead()) &&
                                        cursor > 1 && (expr[cursor - 1] == 'E' || expr[cursor - 1] == 'e')
                                        && isDigit(expr[cursor - 2])) {
                                    cursor++;
                                    //     capture = true;
                                    continue Mainloop;
                                }
                                break CaptureLoop;

                            case '-':
                                switch (lookAhead()) {
                                    case '-':
                                        name = new String(subArray(start, trimLeft(cursor)));
                                        if (pCtx != null && (idx = pCtx.variableIndexOf(name)) != -1) {
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
                                            return lastNode = new DeepAssignmentNode(subArray(start, cursor), fields,
                                                    SUB, t, pCtx);
                                        }
                                        else if (pCtx != null && (idx = pCtx.variableIndexOf(name)) != -1) {
                                            return lastNode = new IndexedOperativeAssign(subArray(start, cursor),
                                                    SUB, idx, fields, pCtx);
                                        }
                                        else {
                                            return lastNode = new OperativeAssign(name, subArray(start, cursor),
                                                    SUB, fields, pCtx);
                                        }
                                }

                                if (isDigit(lookAhead()) &&
                                        cursor > 1 && (expr[cursor - 1] == 'E' || expr[cursor - 1] == 'e')
                                        && isDigit(expr[cursor - 2])) {
                                    cursor++;
                                    capture = true;
                                    continue Mainloop;
                                }
                                break CaptureLoop;

                            /**
                             * Exit immediately for any of these cases.
                             */
                            case '!':
                            case ',':
                            case '"':
                            case '\'':
                            case ';':
                            case ':':
                                break CaptureLoop;

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
                                        return lastNode = new DeepAssignmentNode(subArray(start, cursor), fields,
                                                opLookup(op), t, pCtx);
                                    }
                                    else if (pCtx != null && (idx = pCtx.variableIndexOf(name)) != -1) {
                                        return lastNode = new IndexedOperativeAssign(subArray(start, cursor),
                                                opLookup(op), idx, fields, pCtx);
                                    }
                                    else {
                                        return lastNode = new OperativeAssign(name, subArray(start, cursor),
                                                opLookup(op), fields, pCtx);
                                    }
                                }
                                break CaptureLoop;


                            case '<':
                                if ((lookAhead() == '<' && lookAhead(2) == '=')) {
                                    name = new String(expr, start, trimLeft(cursor) - start);

                                    start = cursor += 3;
                                    captureToEOS();

                                    if (union) {
                                        return lastNode = new DeepAssignmentNode(subArray(start, cursor), fields,
                                                BW_SHIFT_LEFT, t, pCtx);
                                    }
                                    else if (pCtx != null && (idx = pCtx.variableIndexOf(name)) != -1) {
                                        return lastNode = new IndexedOperativeAssign(subArray(start, cursor),
                                                BW_SHIFT_LEFT, idx, fields, pCtx);
                                    }
                                    else {
                                        return lastNode = new OperativeAssign(name, subArray(start, cursor),
                                                BW_SHIFT_LEFT, fields, pCtx);
                                    }
                                }
                                break CaptureLoop;


                            case '>':
                                if (lookAhead() == '>') {
                                    if (lookAhead(2) == '=') {
                                        name = new String(expr, start, trimLeft(cursor) - start);

                                        start = cursor += 3;
                                        captureToEOS();

                                        if (union) {
                                            return lastNode = new DeepAssignmentNode(subArray(start, cursor), fields,
                                                    BW_SHIFT_RIGHT, t, pCtx);
                                        }
                                        else if (pCtx != null && (idx = pCtx.variableIndexOf(name)) != -1) {
                                            return lastNode = new IndexedOperativeAssign(subArray(start, cursor),
                                                    BW_SHIFT_RIGHT, idx, fields, pCtx);
                                        }
                                        else {
                                            return lastNode = new OperativeAssign(name, subArray(start, cursor),
                                                    BW_SHIFT_RIGHT, fields, pCtx);
                                        }
                                    }
                                    else if ((lookAhead(2) == '>' && lookAhead(3) == '=')) {
                                        name = new String(expr, start, trimLeft(cursor) - start);

                                        start = cursor += 4;
                                        captureToEOS();

                                        if (union) {
                                            return lastNode = new DeepAssignmentNode(subArray(start, cursor), fields,
                                                    BW_USHIFT_RIGHT, t, pCtx);
                                        }
                                        else if (pCtx != null && (idx = pCtx.variableIndexOf(name)) != -1) {
                                            return lastNode = new IndexedOperativeAssign(subArray(start, cursor),
                                                    BW_USHIFT_RIGHT, idx, fields, pCtx);
                                        }
                                        else {
                                            return lastNode = new OperativeAssign(name, subArray(start, cursor),
                                                    BW_USHIFT_RIGHT, fields, pCtx);
                                        }
                                    }
                                }
                                break CaptureLoop;

                            case '(':
                                cursor = balancedCaptureWithLineAccounting(expr, cursor, '(', pCtx) + 1;
                                continue;

                            case '[':
                                cursor = balancedCaptureWithLineAccounting(expr, cursor, '[', pCtx) + 1;
                                continue;


                            case '{':
                                if (!union) break CaptureLoop;
                                cursor = balancedCaptureWithLineAccounting(expr, cursor, '{', pCtx) + 1;
                                continue;

                            case '~':
                                if (lookAhead() == '=') {
                                    tmp = subArray(start, trimLeft(cursor));

                                    start = cursor += 2;

                                    if (!isNextIdentifierOrLiteral()) {
                                        throw new CompileException("unexpected symbol '" + expr[cursor] + "'", expr, cursor);
                                    }

                                    captureToEOT();

                                    return lastNode = new RegExMatch(tmp, fields, subArray(start, cursor), pCtx);
                                }
                                break CaptureLoop;


                            case '=':
                                if (lookAhead() == '+') {
                                    name = new String(expr, start, trimLeft(cursor) - start);

                                    start = cursor += 2;

                                    if (!isNextIdentifierOrLiteral()) {
                                        throw new CompileException("unexpected symbol '" + expr[cursor] + "'", expr, cursor);
                                    }

                                    captureToEOS();

                                    if (pCtx != null && (idx = pCtx.variableIndexOf(name)) != -1) {
                                        return lastNode = new IndexedOperativeAssign(subArray(start, cursor), ADD, idx, fields, pCtx);
                                    }
                                    else {
                                        return lastNode = new OperativeAssign(name, subArray(start, cursor), ADD, fields, pCtx);
                                    }
                                }
                                else if (lookAhead() == '-') {
                                    name = new String(expr, start, trimLeft(cursor) - start);

                                    start = cursor += 2;

                                    if (!isNextIdentifierOrLiteral()) {
                                        throw new CompileException("unexpected symbol '" + expr[cursor] + "'", expr, cursor);
                                    }

                                    captureToEOS();

                                    if (pCtx != null && (idx = pCtx.variableIndexOf(name)) != -1) {
                                        return lastNode = new IndexedOperativeAssign(subArray(start, cursor), SUB, idx, fields, pCtx);
                                    }
                                    else {
                                        return lastNode = new OperativeAssign(name, subArray(start, cursor), SUB, fields, pCtx);
                                    }
                                }
                                if (greedy && lookAhead() != '=') {
                                    cursor++;

                                    if (union) {
                                        captureToEOS();

                                        return lastNode = new DeepAssignmentNode(subArray(start, cursor), fields | ASTNode.ASSIGN, pCtx);
                                    }
                                    else if (lastWasIdentifier) {
                                        return procTypedNode(false);
                                    }
                                    else if (pCtx != null && ((idx = pCtx.variableIndexOf(t)) != -1
                                            || (pCtx.isIndexAllocation()))) {
                                        captureToEOS();

                                        IndexedAssignmentNode ian = new IndexedAssignmentNode(subArray(start, cursor), ASTNode.ASSIGN, idx, pCtx);

                                        if (idx == -1) {
                                            pCtx.addIndexedVariable(t = ian.getAssignmentVar());
                                            ian.setRegister(pCtx.variableIndexOf(t));
                                        }
                                        return lastNode = ian;
                                    }
                                    else {
                                        captureToEOS();

                                        return lastNode = new AssignmentNode(subArray(start, cursor), fields | ASTNode.ASSIGN, pCtx);
                                    }
                                }
                                break CaptureLoop;

                            default:
                                if (cursor != length) {
                                    if (isIdentifierPart(expr[cursor])) {
                                        if (!union) {
                                            break CaptureLoop;
                                        }
                                        cursor++;
                                        while (cursor != length && isIdentifierPart(expr[cursor])) cursor++;
                                    }
                                    else if ((cursor + 1) != length && isIdentifierPart(expr[cursor + 1])) {
                                        break CaptureLoop;
                                    }
                                    else {
                                        cursor++;
                                    }
                                }
                                else {
                                    break CaptureLoop;
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

                            return lastNode = new ThisWithNode(subArray(start, cursor - 1),
                                    subArray(cursor + 1,
                                            (cursor = balancedCaptureWithLineAccounting(expr,
                                                    start = cursor, '{', pCtx) + 1) - 1), fields, pCtx);
                        }

                        case '@': {
                            start++;
                            captureToEOT();

                            if (pCtx == null || (pCtx.getInterceptors() == null || !pCtx.getInterceptors().
                                    containsKey(name = new String(expr, start, cursor - start)))) {
                                throw new CompileException("reference to undefined interceptor: "
                                        + new String(expr, start, cursor - start), expr, cursor);
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

                                name = new String(subArray(start, cursor));
                                if (pCtx != null && (idx = pCtx.variableIndexOf(name)) != -1) {
                                    return lastNode = new IndexedPreFixDecNode(idx);
                                }
                                else {
                                    return lastNode = new PreFixDecNode(name);
                                }
                            }
                            else if ((cursor == 0 || (lastNode != null &&
                                    (lastNode instanceof BooleanNode || lastNode.isOperator())))
                                    && !isDigit(lookAhead())) {

                                captureToEOT();
                                return new Sign(expr, start, cursor, fields, pCtx);
                            }
                            else if ((cursor != 0 && !isWhitespace(expr[cursor - 1]) && (
                                    !(lastNode != null && (lastNode instanceof BooleanNode || lastNode.isOperator()))))
                                    || !isDigit(lookAhead())) {

                                return createOperator(expr, start, cursor++ + 1);
                            }
                            else if ((cursor - 1) != 0 || (!isDigit(expr[cursor - 1])) && isDigit(lookAhead())) {
                                cursor++;
                                break;
                            }
                            else {
                                throw new CompileException("not a statement", expr, cursor);
                            }


                        case '+':
                            if (lookAhead() == '+') {
                                cursor += 2;
                                skipWhitespace();
                                start = cursor;
                                captureIdentifier();

                                name = new String(subArray(start, cursor));
                                if (pCtx != null && (idx = pCtx.variableIndexOf(name)) != -1) {
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
                                        if (brace == 1 && lookAhead() == 'n' && isWhitespace(lookAhead(2))) {

                                            for (int level = brace; cursor != length; cursor++) {
                                                switch (expr[cursor]) {
                                                    case '(':
                                                        brace++;
                                                        break;
                                                    case ')':
                                                        if (--brace < level) {
                                                            cursor++;
                                                            if (tokenContinues()) {
                                                                lastNode = new Fold(subset(expr, trimRight(start + 1),
                                                                        cursor - start - 2), fields, pCtx);
                                                                if (expr[start = cursor] == '.') start++;
                                                                captureToEOT();
                                                                return lastNode = new Union(expr, trimRight(start),
                                                                        cursor, fields, lastNode);
                                                            }
                                                            else {
                                                                return lastNode = new Fold(subset(expr, trimRight(start + 1),
                                                                        cursor - start - 2), fields, pCtx);
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

                                            throw new CompileException("unterminated projection; closing parathesis required",
                                                    expr, cursor);
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
                                try {

                                    if (tDescr.isClass() && (cls = getClassReference(pCtx, tDescr)) != null) {

                                        start = cursor;
                                        captureToEOS();

                                        return lastNode = new TypeCast(subset(expr, start, cursor - start), cls, fields, pCtx);
                                    }
                                }
                                catch (ClassNotFoundException e) {
                                    // fallthrough
                                }

                            }

                            if (tmp != null) {
                                return handleUnion(handleSubstatement(new Substatement(tmp, fields, pCtx)));
                            }
                            else {
                                return handleUnion(
                                        handleSubstatement(
                                                new Substatement(
                                                        subset(expr, start = trimRight(start + 1),
                                                                trimLeft(cursor - 1) - start), fields, pCtx)));
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
                                return createOperator(expr, start, ++cursor);
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
                                return lastNode = new Invert(subset(expr, start, cursor - start), fields, pCtx);
                            }
                            else if (expr[cursor] == '(') {
                                start = cursor--;
                                captureToEOT();
                                return lastNode = new Invert(subset(expr, start, cursor - start), fields, pCtx);
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
                                    return lastNode = new Negation(subset(expr, start, cursor - start), fields, pCtx);
                                }
                                else {
                                    return lastNode = new Negation(name.toCharArray(), fields, pCtx);
                                }
                            }
                            else if (expr[cursor] == '(') {
                                start = cursor--;
                                captureToEOT();
                                return lastNode = new Negation(subset(expr, start, cursor - start), fields, pCtx);
                            }
                            else if (expr[cursor] != '=')
                                throw new CompileException("unexpected operator '!'", expr, cursor, null);
                            else {
                                return createOperator(expr, start, ++cursor);
                            }
                        }

                        case '[':
                        case '{':
                            cursor = balancedCaptureWithLineAccounting(expr, cursor, expr[cursor], pCtx) + 1;
                            if (tokenContinues()) {
                                lastNode = new InlineCollectionNode(expr, start, start = cursor, fields, pCtx);
                                captureToEOT();
                                if (expr[start] == '.') start++;
                                return lastNode = new Union(expr, start, cursor, fields, lastNode);
                            }
                            else {
                                return lastNode = new InlineCollectionNode(expr, start, cursor, fields, pCtx);
                            }

                        default:
                            cursor++;
                    }
                }
            }

            if (start == cursor)
                return null;
            else
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
            e.setLineNumber(pCtx == null ? 1 : pCtx.getLineCount());
            e.setCursor(cursor);
            e.setColumn(cursor - (pCtx == null ? 0 : pCtx.getLineOffset()));
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
            int union = -1;
            switch (expr[cursor]) {
                case '.':
                    union = cursor + 1;
                    break;
                case '[':
                    union = cursor;
            }

            if (union != -1) {
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
        String tmp;

        if (isLiteralOnly(expr, start, end)) {
            if (pCtx != null && pCtx.hasImports()) {

                char[] _subset = subset(expr, start, cursor - start);
                int offset;

                if ((offset = findFirst('.', _subset)) != -1) {
                    String iStr = new String(_subset, 0, offset);
                    if (pCtx.hasImport(iStr)) {
                        lastWasIdentifier = true;
                        return lastNode = new LiteralDeepPropertyNode(subset(_subset, offset + 1, _subset.length
                                - offset - 1),
                                fields, pCtx.getImport(iStr));
                    }
                }
                else {
                    if (pCtx.hasImport(tmp = new String(_subset))) {
                        lastWasIdentifier = true;
                        return lastNode = new LiteralNode(pCtx.getStaticOrClassImport(tmp));
                    }
                }
            }

            if (LITERALS.containsKey(tmp = new String(expr, start, end - start))) {
                lastWasIdentifier = true;
                return lastNode = new LiteralNode(LITERALS.get(tmp));
            }
            else if (OPERATORS.containsKey(tmp)) {
                lastWasIdentifier = false;
                return lastNode = new OperatorNode(OPERATORS.get(tmp));
            }
            else if (lastWasIdentifier) {
                return procTypedNode(true);
            }

        }

        lastWasIdentifier = true;

        return lastNode = new ASTNode(expr, start, end, fields);
    }

    private ASTNode procTypedNode(boolean decl) {

        while (true) {
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

                if (decl) {
                    splitAccumulator.add(new DeclTypedVarNode(new String(expr, start, cursor - start),
                            (Class) lastNode.getLiteralValue(), fields | ASTNode.ASSIGN, pCtx));
                }
                else {
                    captureToEOS();
                    splitAccumulator.add(new TypedVarNode(subArray(start, cursor), fields | ASTNode.ASSIGN, (Class)
                            lastNode.getLiteralValue(), pCtx));
                }
            }
             else if (lastNode instanceof Proto) {
                    captureToEOS();
                    if (decl) {
                        splitAccumulator.add(new DeclProtoVarNode(new String(expr, start, cursor - start),
                                (Proto) lastNode, fields | ASTNode.ASSIGN, pCtx));
                    }
                    else {
                        splitAccumulator.add(new ProtoVarNode(subArray(start, cursor), fields | ASTNode.ASSIGN, (Proto)
                                lastNode, pCtx));
                    }
                }

            // this redundant looking code is needed to work with the interpreter and MVELSH properly.
            else if ((fields & ASTNode.COMPILE_IMMEDIATE) == 0) {
                if (stk.peek() instanceof Class) {
                    captureToEOS();
                    if (decl) {
                        splitAccumulator.add(new DeclTypedVarNode(new String(expr, start, cursor - start),
                                (Class) stk.pop(), fields | ASTNode.ASSIGN, pCtx));
                    }
                    else {
                        splitAccumulator.add(new TypedVarNode(subArray(start, cursor), fields | ASTNode.ASSIGN, (Class)
                                stk.pop(), pCtx));
                    }
                }
                else if (stk.peek() instanceof Proto) {
                    captureToEOS();
                    if (decl) {
                        splitAccumulator.add(new DeclProtoVarNode(new String(expr, start, cursor - start),
                                (Proto) stk.pop(), fields | ASTNode.ASSIGN, pCtx));
                    }
                    else {
                        splitAccumulator.add(new ProtoVarNode(subArray(start, cursor), fields | ASTNode.ASSIGN, (Proto)
                                stk.pop(), pCtx));
                    }
                }
                else {
                    throw new CompileException("unknown class or illegal statement: " + lastNode.getLiteralValue(), expr, cursor);
                }
            }
            else {
                throw new CompileException("unknown class or illegal statement: " + lastNode.getLiteralValue(), expr, cursor);
            }

            skipWhitespace();
            if (cursor < length && expr[cursor] == ',') {
                start = ++cursor;
                splitAccumulator.add(new EndOfStatement());
            }
            else {
                return (ASTNode) splitAccumulator.pop();
            }
        }

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
                return new IfNode(subArray(condStart, condEnd), subArray(blockStart, blockEnd), fields, pCtx);
            case ASTNode.BLOCK_FOR:
                for (int i = condStart; i < condEnd; i++) {
                    if (expr[i] == ';')
                        return new ForNode(subArray(condStart, condEnd), subArray(blockStart, blockEnd), fields, pCtx);
                    else if (expr[i] == ':')
                        break;
                }
            case ASTNode.BLOCK_FOREACH:
                return new ForEachNode(subArray(condStart, condEnd), subArray(blockStart, blockEnd), fields, pCtx);
            case ASTNode.BLOCK_WHILE:
                return new WhileNode(subArray(condStart, condEnd), subArray(blockStart, blockEnd), fields, pCtx);
            case ASTNode.BLOCK_UNTIL:
                return new UntilNode(subArray(condStart, condEnd), subArray(blockStart, blockEnd), fields, pCtx);
            case ASTNode.BLOCK_DO:
                return new DoNode(subArray(condStart, condEnd), subArray(blockStart, blockEnd), fields, pCtx);
            case ASTNode.BLOCK_DO_UNTIL:
                return new DoUntilNode(subArray(condStart, condEnd), subArray(blockStart, blockEnd), pCtx);
            default:
                return new WithNode(subArray(condStart, condEnd), subArray(blockStart, blockEnd), fields, pCtx);
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
                                && expr[cursor = incNextNonBlank()] == '(';
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
                skipWhitespace();
                return _captureBlock(null, expr, false, type);

            default: // either BLOCK_WITH or BLOCK_FOREACH
                captureToNextTokenJunction();
                skipWhitespace();
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
        switch (type) {
            case FUNCTION: {
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

                if (pCtx == null) pCtx = getParserContext();

                FunctionParser parser = new FunctionParser(name, cursor, expr.length, expr, pCtx, splitAccumulator);

                Function function = parser.parse();

                cursor = parser.getCursor();

                return lastNode = function;
            }
            case PROTO:
                if (ProtoParser.isUnresolvedWaiting()) {
                    if (pCtx == null) pCtx = getParserContext();
                    ProtoParser.checkForPossibleUnresolvedViolations(expr, cursor, pCtx);
                }

                int start = cursor;
                captureToNextTokenJunction();

                if (isReservedWord(name = createStringTrimmed(expr, start, (startCond = cursor) - start))
                        || isNotValidNameorLabel(name))
                    throw new CompileException("illegal prototype name or use of reserved word", expr, cursor);

                if (expr[cursor = nextNonBlank()] != '{') {
                    throw new CompileException("expected '{' but found: " + expr[cursor]);
                }

                cursor = balancedCaptureWithLineAccounting(expr, start = cursor + 1, '{', pCtx);

                if (pCtx == null) pCtx = getParserContext();

                ProtoParser parser = new ProtoParser(expr, start, cursor, name, pCtx, fields, splitAccumulator);

                Proto proto = parser.parse();

                if (pCtx == null) pCtx = getParserContext();

                pCtx.addImport(proto);

                proto.setCursorPosition(start, cursor);
                cursor = parser.getCursor();

                ProtoParser.notifyForLateResolution(proto);

                return lastNode = proto;

            default:
                if (cond) {
                    if (expr[cursor] != '(') {
                        throw new CompileException("expected '(' but encountered: " + expr[cursor]);
                    }

                    /**
                     * This block is an: IF, FOREACH or WHILE node.
                     */

                    endCond = cursor = balancedCaptureWithLineAccounting(expr, startCond = cursor, '(', pCtx);

                    startCond++;
                    cursor++;
                }
        }

        skipWhitespace();

        if (cursor >= length) {
            throw new CompileException("unexpected end of statement", expr, cursor);
        }
        else if (expr[cursor] == '{') {
            blockEnd = cursor = balancedCaptureWithLineAccounting(expr, blockStart = cursor, '{', pCtx);
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
            skipWhitespace();
            start = cursor;
            captureToNextTokenJunction();

            if ("while".equals(name = new String(expr, start, cursor - start))) {
                skipWhitespace();
                startCond = cursor + 1;
                endCond = cursor = balancedCaptureWithLineAccounting(expr, cursor, '(', pCtx);
                return createBlockToken(startCond, endCond, trimRight(blockStart + 1), trimLeft(blockEnd), type);
            }
            else if ("until".equals(name)) {
                skipWhitespace();
                startCond = cursor + 1;
                endCond = cursor = balancedCaptureWithLineAccounting(expr, cursor, '(', pCtx);
                return createBlockToken(startCond, endCond, trimRight(blockStart + 1), trimLeft(blockEnd),
                        ASTNode.BLOCK_DO_UNTIL);
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
            skipWhitespace();

            return expr[cursor] == 'e' && expr[cursor + 1] == 'l' && expr[cursor + 2] == 's' && expr[cursor + 3] == 'e'
                    && (isWhitespace(expr[cursor + 4]) || expr[cursor + 4] == '{');
        }
        return false;
    }

    /**
     * Checking from the current cursor position, check to see if we're inside a contiguous identifier.
     *
     * @return -
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
                    if ((cursor = balancedCaptureWithLineAccounting(expr, cursor, expr[cursor], pCtx)) >= length)
                        return;
                    break;

                case '"':
                case '\'':
                    cursor = captureStringLiteral(expr[cursor], expr, cursor, length);
                    break;

                case ',':
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
                        throw new CompileException("unexpected symbol (was expecting an identifier): " + expr[cursor],
                                expr, cursor);
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
                    if ((cursor = balancedCaptureWithLineAccounting(expr, cursor, expr[cursor], pCtx)) == -1) {
                        throw new CompileException("unbalanced braces", expr, cursor);
                    }
                    break;

                case ',':
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

                        if (cursor < length && expr[cursor] == '.') {
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
        if (pos > length) pos = length;
        while (pos > 0 && pos >= start && isWhitespace(expr[pos - 1])) pos--;
        return pos;
    }

    /**
     * From the specified cursor position, trim out any whitespace between the current position and beginning of the
     * first non-whitespace character.
     *
     * @param pos -
     * @return -
     */
    protected int trimRight(int pos) {
        while (pos != length && isWhitespace(expr[pos])) pos++;
        return pos;
    }

    /**
     * If the cursor is currently pointing to whitespace, move the cursor forward to the first non-whitespace
     * character, but account for carraige returns in the script (updates parser field: line).
     */
    protected void skipWhitespace() {
        Skip:
        while (cursor != length) {
            switch (expr[cursor]) {
                case '\n':
                    line++;
                    lastLineStart = cursor;
                case '\r':
                    cursor++;
                    continue;
                case '/':
                    if (cursor + 1 != length) {
                        switch (expr[cursor + 1]) {
                            case '/':
                                expr[cursor++] = ' ';
                                while (cursor != length && expr[cursor] != '\n') expr[cursor++] = ' ';
                                if (cursor != length) expr[cursor++] = ' ';

                                line++;
                                lastLineStart = cursor;

                                continue;

                            case '*':
                                int len = length - 1;
                                expr[cursor++] = ' ';
                                while (cursor != len && !(expr[cursor] == '*' && expr[cursor + 1] == '/')) {
                                    if (expr[cursor] == '\n') {
                                        line++;
                                        lastLineStart = cursor;
                                    }

                                    expr[cursor++] = ' ';
                                }
                                if (cursor != len) expr[cursor++] = expr[cursor++] = ' ';
                                continue;

                            default:
                                break Skip;

                        }
                    }
                default:
                    if (!isWhitespace(expr[cursor])) break Skip;

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
                    cursor = balancedCaptureWithLineAccounting(expr, cursor, '[', pCtx) + 1;
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

    protected void setExpression(String expression) {
        if (expression != null && expression.length() != 0) {
            synchronized (EX_PRECACHE) {
                if ((this.expr = EX_PRECACHE.get(expression)) == null) {
                    length = (this.expr = expression.toCharArray()).length;

                    // trim any whitespace.
                    while (length != 0 && isWhitespace(this.expr[length - 1])) length--;

                    char[] e = new char[length];

                    for (int i = 0; i != e.length; i++)
                        e[i] = expr[i];

                    EX_PRECACHE.put(expression, e);
                }
                else {
                    length = this.expr.length;
                }
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
     * @return -
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
     * @return -
     */
    protected char lookBehind() {
        if (cursor == 0) return 0;
        else return expr[cursor - 1];
    }

    /**
     * Return the next character (delta 1 of cursor position).
     *
     * @return -
     */
    protected char lookAhead() {
        if (cursor + 1 < length) {
            return expr[cursor + 1];
        }
        else {
            return 0;
        }
    }

    /**
     * Return the character, forward of the currrent cursor position based on the specified range delta.
     *
     * @param range -
     * @return -
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

    public int incNextNonBlank() {
        cursor++;
        return nextNonBlank();
    }

    public int nextNonBlank() {
        if ((cursor + 1) >= length) {
            throw new CompileException("unexpected end of statement", expr, cursor);
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
     * @return -
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
    public void newContext() {
        contextControl(SET, new ParserContext(), this);
    }

    /**
     * Create a new ParserContext in the current thread, using the one specified.
     *
     * @param pCtx -
     */
    public void newContext(ParserContext pCtx) {
        contextControl(SET, pCtx, this);
    }

    /**
     * Remove the current ParserContext from the thread.
     */
    public void removeContext() {
        contextControl(REMOVE, null, this);
    }

    public static ParserContext contextControl(int operation, ParserContext pCtx, AbstractParser parser) {
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

    protected static String getCurrentSourceFileName() {
        if (parserContext != null && parserContext.get() != null) {
            return parserContext.get().getSourceFile();
        }
        return null;
    }

    protected void addFatalError(String message) {
        getParserContext().addError(new ErrorDetail(getParserContext().getLineCount(),
                cursor - getParserContext().getLineOffset(), true, message));
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

    public static HashMap<String, Integer> loadLanguageFeaturesByLevel(int languageLevel) {
        HashMap<String, Integer> operatorsTable = new HashMap<String, Integer>();
        switch (languageLevel) {
            case 6:  // prototype definition
                operatorsTable.put("proto", PROTO);

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
                operatorsTable.put("isdef", ISDEF);

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

        /**
         * If the next token is an operator, we check to see if it has a higher
         * precdence.
         */
        if ((tk = nextToken()) != null) {
            if (isArithmeticOperator(operator2 = tk.getOperator()) && PTABLE[operator2] > PTABLE[operator]) {
                stk.xswap();
                /**
                 * The current arith. operator is of higher precedence the last.
                 */

                tk = nextToken();

                /**
                 * Check to see if we're compiling or executing interpretively.  If we're compiling, we really
                 * need to stop if this is not a literal.
                 */
                if (compileMode && !tk.isLiteral()) {

                    // BAIL OUT!
                    splitAccumulator.push(tk);
                    splitAccumulator.push(new OperatorNode(operator2));
                    return tk instanceof Substatement ? -2 : OP_TERMINATE;
                }

                dStack.push(operator = operator2, tk.getReducedValue(ctx, ctx, variableFactory));

                while (true) {
                    // look ahead again
                    if ((tk = nextToken()) != null && (operator2 = tk.getOperator()) != -1
                            && operator2 != 37 && PTABLE[operator2] > PTABLE[operator]) {
                        // if we have back to back operations on the stack, we don't xswap

                        if (dStack.isReduceable()) {
                            stk.copyx2(dStack);
                        }

                        /**
                         * This operator is of higher precedence, or the same level precedence.  push to the RHS.
                         */
                        dStack.push(operator = operator2, nextToken().getReducedValue(ctx, ctx, variableFactory));

                        continue;
                    }
                    else if (tk != null && operator2 != -1 && operator2 != 37) {
                        if (PTABLE[operator2] == PTABLE[operator]) {
                            if (!dStack.isEmpty()) dreduce();
                            else {
                                while (stk.isReduceable()) {
                                    stk.xswap_op();
                                }
                            }

                            /**
                             * This operator is of the same level precedence.  push to the RHS.
                             */

                            dStack.push(operator = operator2, nextToken().getReducedValue(ctx, ctx, variableFactory));

                            continue;
                        }
                        else {
                            /**
                             * The operator doesn't have higher precedence. Therfore reduce the LHS.
                             */
                            while (dStack.size() > 1) {
                                dreduce();
                            }

                            operator = tk.getOperator();
                            // Reduce the lesser or equal precedence operations.
                            while (stk.size() != 1 && stk.peek2() instanceof Integer &&
                                    ((operator2 = (Integer) stk.peek2()) < PTABLE.length) &&
                                    PTABLE[operator2] >= PTABLE[operator]) {
                                stk.xswap_op();
                            }
                        }
                    }
                    else {
                        /**
                         * There are no more tokens.
                         */

                        if (dStack.size() > 1) {
                            dreduce();
                        }

                        if (stk.isReduceable()) stk.xswap();

                        break;
                    }

                    if ((tk = nextToken()) != null) {
                        switch (operator) {
                            case AND: {
                                if (!(stk.peekBoolean())) return OP_TERMINATE;
                                else {
                                    splitAccumulator.add(tk);
                                    return AND;
                                }
                            }
                            case OR: {
                                if ((stk.peekBoolean())) return OP_TERMINATE;
                                else {
                                    splitAccumulator.add(tk);
                                    return OR;
                                }
                            }

                            default:
                                stk.push(operator, tk.getReducedValue(ctx, ctx, variableFactory));
                        }
                    }
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
        while (stk.isReduceable()) {
            reduce();
            if (stk.isReduceable()) stk.xswap();
        }

        return OP_RESET_FRAME;
    }

    private void dreduce() {
        stk.copy2(dStack);
        stk.op();
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
                    stk.op(operator);
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
                    stk.push(java.util.regex.Pattern.compile(java.lang.String.valueOf(stk.pop()))
                            .matcher(java.lang.String.valueOf(stk.pop())).matches());
                    break;

                case INSTANCEOF:
                    stk.push(((Class) stk.pop()).isInstance(stk.pop()));
                    break;

                case CONVERTABLE_TO:
                    stk.push(org.mvel2.DataConversion.canConvert(stk.peek2().getClass(), (Class) stk.pop2()));
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

    private static int asInt(final Object o) {
        return (Integer) o;
    }

    public ParserContext getPCtx() {
        return pCtx;
    }

    public void setPCtx(ParserContext pCtx) {
        this.debugSymbols = (this.pCtx = pCtx).isDebugSymbols();
    }
}
