package org.mvel;

import static org.mvel.Operator.*;
import org.mvel.ast.*;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ArrayTools.findFirst;
import org.mvel.util.ExecutionStack;
import static org.mvel.util.ParseTools.*;
import static org.mvel.util.PropertyTools.*;
import org.mvel.util.Stack;
import org.mvel.util.StringAppender;
import org.mvel.util.ThisLiteral;

import java.io.Serializable;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Character.isWhitespace;
import static java.lang.Float.parseFloat;
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

    protected boolean greedy = true;
    protected boolean lastWasIdentifier = false;
    protected boolean lastWasLineLabel = false;
    protected boolean lastWasComment = false;
    protected boolean literalOnly = true;

    protected boolean debugSymbols = false;

    private int line = 1;

    protected ASTNode lastNode;

    protected static final int OP_TERMINATE = -1;
    protected static final int OP_RESET_FRAME = 0;
    protected static final int OP_CONTINUE = 1;

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

        AbstractParser.LITERALS.put("this", ThisLiteral.class);

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

        if (parseFloat(getProperty("java.version").substring(0, 2)) >= 1.5) {
            try {
                LITERALS.put("StringBuilder", currentThread().getContextClassLoader().loadClass("java.lang.StringBuilder"));
            }
            catch (Exception e) {
                throw new RuntimeException("cannot resolve a built-in literal", e);
            }
        }

        _loadLanguageFeaturesByLevel(5);
    }


    static void configureFactory() {
        EX_PRECACHE = new WeakHashMap<String, char[]>(10);
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

            int brace;
            start = cursor;

            /**
             * Because of parser recursion for sub-expression parsing, we sometimes need to remain
             * certain field states.  We do not reset for assignments, boolean mode, list creation or
             * a capture only mode.
             */
            fields = fields & (ASTNode.INLINE_COLLECTION | ASTNode.COMPILE_IMMEDIATE);

            boolean capture = false, union = false;

            if (debugSymbols) {
                if (!lastWasLineLabel) {
                    if (getParserContext().getSourceFile() == null) {
                        throw new CompileException("unable to produce debugging symbols: source name must be provided.");
                    }

                    ParserContext pCtx = getParserContext();
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
                                ImportNode importNode = new ImportNode(subArray(start, cursor));
                                if (importNode.isPackageImport()) {
                                    getParserContext().addPackageImport(importNode.getPackageImport());
                                }
                                else {
                                    getParserContext().addImport(getSimpleClassName(importNode.getImportClass()), importNode.getImportClass());
                                }
                                return importNode;

                            case IMPORT_STATIC:
                                start = cursor = trimRight(cursor);
                                captureToEOS();
                                return lastNode = new StaticImportNode(subArray(start, cursor), fields);
                        }
                    }


                    skipWhitespace();

                    /**
                     * If we *were* capturing a token, and we just hit a non-identifier
                     * character, we stop and figure out what to do.
                     */
                    if (cursor != length && expr[cursor] == '(') {
                        fields |= ASTNode.METHOD;

                        if ((cursor = balancedCapture(expr, cursor, '(')) == -1) {
                            throw new CompileException("unbalanced braces", expr, cursor);
                        }

                        cursor++;
                    }

                    /**
                     * If we encounter any of the following cases, we are still dealing with
                     * a contiguous token.
                     */
                    String name;
                    if (cursor != length) {
                        switch (expr[cursor]) {
                            case '+':
                                switch (lookAhead()) {
                                    case '+':
                                        lastNode = new PostFixIncNode(subArray(start, cursor), fields);
                                        cursor += 2;
                                        return lastNode;

                                    case '=':
                                        name = new String(expr, start, trimLeft(cursor) - start);
                                        start = cursor += 2;
                                        captureToEOS();

                                        if (union) {
                                            return lastNode = new DeepAssignmentNode(subArray(start, cursor), fields, Operator.ADD, t);
                                        }
                                        else {
                                            return lastNode = new AssignmentNode(subArray(start, cursor), fields, Operator.ADD, name);
                                        }
                                }

                                break;

                            case '-':
                                switch (lookAhead()) {
                                    case '-':
                                        lastNode = new PostFixDecNode(subArray(start, cursor), fields);
                                        cursor += 2;
                                        return lastNode;

                                    case '=':
                                        name = new String(expr, start, trimLeft(cursor) - start);
                                        start = cursor += 2;
                                        captureToEOS();
                                        return lastNode = new AssignSub(subArray(start, cursor), fields, name);
                                }
                                break;

                            case '*':
                                if (isNext('=')) {
                                    name = new String(expr, start, trimLeft(cursor) - start);
                                    start = cursor += 2;
                                    captureToEOS();
                                    return lastNode = new AssignMult(subArray(start, cursor), fields, name);
                                }
                                break;

                            case '/':
                                if (isNext('=')) {
                                    name = new String(expr, start, trimLeft(cursor) - start);
                                    start = cursor += 2;
                                    captureToEOS();
                                    return lastNode = new AssignDiv(subArray(start, cursor), fields, name);
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
                                    captureToEOT();
                                    return lastNode = new RegExMatch(stmt, fields, subArray(start, cursor));
                                }
                                break;

                            case '=':
                                if (isNext('+')) {
                                    name = new String(expr, start, trimLeft(cursor) - start);
                                    start = cursor += 2;
                                    captureToEOS();
                                    return lastNode = new AssignAdd(subArray(start, cursor), fields, name);
                                }
                                else if (isNext('-')) {
                                    name = new String(expr, start, trimLeft(cursor) - start);
                                    start = cursor += 2;
                                    captureToEOS();
                                    return lastNode = new AssignSub(subArray(start, cursor), fields, name);

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
                                            if (getParserContext().hasImport((String) lastNode.getLiteralValue())) {
                                                lastNode.setLiteralValue(getParserContext().getImport((String) lastNode.getLiteralValue()));
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

                                        throw new ParseException("unknown class: " + lastNode.getLiteralValue());
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
                else
                    switch (expr[cursor]) {
                        case '@': {
                            start++;
                            captureToEOT();

                            String interceptorName = new String(expr, start, cursor - start);

                            if (getParserContext().getInterceptors() == null || !getParserContext().getInterceptors().
                                    containsKey(interceptorName)) {
                                throw new CompileException("reference to undefined interceptor: " + interceptorName, expr, cursor);
                            }

                            return lastNode = new InterceptorWrapper(getParserContext().getInterceptors().get(interceptorName), nextToken());
                        }

                        case '=':
                            return createOperator(expr, start, (cursor += 2), fields);

                        case '-':
                            if (isNext('-')) {
                                start = cursor += 2;
                                captureToEOT();
                                return lastNode = new PreFixDecNode(subArray(start, cursor), fields);
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
                                return lastNode = new PreFixIncNode(subArray(start, cursor), fields);
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
                                String tokenStr = new String(_subset = subset(expr, st = trimRight(start + 1), trimLeft(cursor - 1) - st));

                                if (getParserContext().hasImport(tokenStr)) {
                                    start = cursor;
                                    captureToEOS();
                                    return lastNode = new TypeCast(expr, start, cursor, fields, getParserContext().getImport(tokenStr));
                                }
                                else {
                                    int rewind = cursor;
                                    try {
                                        /**
                                         *
                                         *  take a stab in the dark and try and load the class
                                         */
                                        captureToEOS();
                                        return lastNode = new TypeCast(expr, rewind, cursor, fields, createClass(tokenStr));

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
                            if ((cursor - 1 != 0 || !isIdentifierPart(lookBehind()))
                                    && isDigit(expr[cursor + 1])) {

                                fields |= ASTNode.INVERT;
                                start++;
                                cursor++;
                                break;
                            }
                            else if (expr[cursor + 1] == '(') {
                                fields |= ASTNode.INVERT;
                                start = ++cursor;
                                continue;
                            }
                            else {
                                if (expr[cursor + 1] == '=') cursor++;
                                return createOperator(expr, start, ++cursor, fields);
                            }

                        case '!': {
                            ++cursor;
                            if (isIdentifierPart(expr[cursor = nextNonBlack()]) || expr[cursor] == '(') {
                                start = cursor;
                                fields |= ASTNode.NEGATION;
                                continue;
                            }
                            else if (expr[cursor] != '=')
                                throw new CompileException("unexpected operator '!'", expr, cursor, null);
                            else {
                                return createOperator(expr, start, ++cursor, fields);
                            }
                        }

                        case '[':
                        case '{':
                            if ((cursor = balancedCapture(expr, cursor, expr[cursor])) == -1) {
                                if (cursor >= length) cursor--;
                                throw new CompileException("unbalanced brace: in inline map/list/array creation", expr, cursor);
                            }

                            cursor++;

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

            if (start == cursor) return null;
            return createPropertyToken(start, cursor);
        }
        catch (CompileException e) {
            throw new CompileException(e.getMessage(), expr, cursor, e.getCursor() == 0);
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

    public ASTNode handleSubstatement(Substatement stmt) {
        return stmt.getStatement() != null && stmt.getStatement().isLiteralOnly() ?
                new LiteralNode(stmt.getStatement().getValue(null, null, null), fields)
                : stmt;
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
        if (parserContext != null && parserContext.get() != null && parserContext.get().hasImports()) {
            char[] _subset = subset(expr, start, cursor - start);
            int offset;

            if ((offset = findFirst('.', _subset)) != -1) {
                String iStr = new String(_subset, 0, offset);
                if ("this".equals(iStr)) {
                    return lastNode = new ThisValDeepPropertyNode(subset(_subset, offset + 1, _subset.length - offset - 1), fields);
                }
                else if (getParserContext().hasImport(iStr)) {
                    return lastNode = new LiteralDeepPropertyNode(subset(_subset, offset + 1, _subset.length - offset - 1), fields, getParserContext().getImport(iStr));
                }
            }
            else {
                String iStr = new String(_subset);
                if (getParserContext().hasImport(iStr)) {
                    return lastNode = new LiteralNode(getParserContext().getImport(iStr), Class.class);
                }

                lastWasIdentifier = (lastNode = new ASTNode(_subset, 0, _subset.length, fields)).isIdentifier();
                return lastNode;
            }
        }
        else if ((fields & ASTNode.METHOD) != 0) {
            return lastNode = new ASTNode(expr, start, end, fields);
        }

        return lastNode = new PropertyASTNode(expr, start, end, fields);
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
                        skipToNextTokenJunction();
                        skipWhitespace();
                        skipCommentBlock();

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
                while (blockContinues());
                return first;
            }

            default: // either BLOCK_WITH or BLOCK_FOREACH

                skipToNextTokenJunction();
                if (debugSymbols) {
                    skipWhitespaceWithLineAccounting();
                }
                else {
                    skipWhitespace();
                }
                return _captureBlock(null, expr, true, type);
        }
    }

    private ASTNode _captureBlock(ASTNode node, final char[] expr, boolean cond, int type) {
        skipWhitespace();
        int startCond = 0;
        int endCond = 0;

        if (cond) {
            if (debugSymbols) {
                int[] cap = balancedCaptureWithLineAccounting(expr, startCond = cursor, '(');

                endCond = cursor = cap[0];

                startCond++;
                cursor++;

                getParserContext().setLineCount(line = getParserContext().getLineCount() + cap[1]);
            }
            else {
                endCond = cursor = balancedCapture(expr, startCond = cursor, '(');
                startCond++;
                cursor++;
            }
        }

        int blockStart;
        int blockEnd;

        skipWhitespace();
        skipCommentBlock();

        if (cursor >= length) {
            throw new CompileException("unbalanced braces", expr, cursor);
        }
        else if (expr[cursor] == '{') {
            blockStart = cursor;

            if (debugSymbols) {

                int[] cap = balancedCaptureWithLineAccounting(expr, cursor, '{');
                if (cap[0] == -1) {
                    throw new CompileException("unbalanced braces { }", expr, cursor);
                }

                blockEnd = cursor = cap[0];

                getParserContext().setLineCount((line = getParserContext().getLineCount() + cap[1]));
            }
            else if ((blockEnd = cursor = balancedCapture(expr, cursor, '{')) == -1) {
                throw new CompileException("unbalanced braces { }", expr, cursor);
            }

        }
        else {
            blockStart = cursor - 1;
            captureToEOLorOF();
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

    protected boolean blockContinues() {
        if ((cursor + 4) < length) {
            if (expr[cursor] != ';') cursor--;
            skipWhitespace();
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
            while (cursor != length && (expr[cursor] != '\n')) cursor++;

            if (debugSymbols) {
                pCtx = getParserContext();

                line = pCtx.getLineCount();
                skipWhitespaceWithLineAccounting();

                if (lastNode instanceof LineLabel) {
                    pCtx.getLastLineLabel().setLineNumber(line);
                    pCtx.addKnownLine(line);
                }

                pCtx.setLineCount(line);

            }
            else {
                skipWhitespace();
            }

            lastWasComment = true;

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
            if (debugSymbols) {
                pCtx = getParserContext();
                line = pCtx.getLineCount();
            }
            while (true) {
                cursor++;
                /**
                 * Since multi-line comments may cross lines, we must keep track of any line-break
                 * we encounter.
                 */

                if (debugSymbols) {
                    skipWhitespaceWithLineAccounting();
                }
                else {
                    skipWhitespace();
                }

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

            if (debugSymbols) {
                pCtx = getParserContext();
                pCtx.setLineCount(line);
                if (lastNode instanceof LineLabel) {
                    pCtx.getLastLineLabel().setLineNumber(line);
                    pCtx.addKnownLine(line);
                }
            }

            lastWasComment = true;

            return OP_RESET_FRAME;
        }

        return OP_CONTINUE;
    }


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

    protected void captureToEOS() {
        while (cursor != length && expr[cursor] != ';') {
            cursor++;
        }
    }

    protected void captureToEOLorOF() {
        while (cursor != length && (expr[cursor] != '\n' && expr[cursor] != '\r' && expr[cursor] != ';')) {
            cursor++;
        }
    }


    protected void captureToEOT() {
        skipWhitespace();
        while (cursor != length) {
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
            cursor++;
        }
    }

    protected int trimLeft(int pos) {
        while (pos != 0 && isWhitespace(expr[pos - 1])) pos--;
        return pos;
    }

    protected int trimRight(int pos) {
        while (pos != length && isWhitespace(expr[pos])) pos++;
        return pos;
    }

    protected void skipWhitespace() {
        while (cursor != length && isWhitespace(expr[cursor])) cursor++;
    }


    protected void skipWhitespaceWithLineAccounting() {
        while (cursor != length && isWhitespace(expr[cursor])) {
            switch (expr[cursor]) {
                case '\n':
                    line++;
                case '\r':
                    cursor++;
                    continue;
            }
            cursor++;
        }
    }

    protected void skipToNextTokenJunction() {
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

    protected void trimWhitespace() {
        while (cursor != 0 && isWhitespace(expr[cursor - 1])) cursor--;
    }

    protected ASTNode captureTokenToEOS() {
        int start = cursor;
        captureToEOS();
        return lastNode = new ASTNode(expr, start, cursor, 0);
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

//    private boolean isFlag(int bit) {
//        return (fields & bit) != 0;
//    }

    public static boolean isReservedWord(String name) {
        return LITERALS.containsKey(name) || OPERATORS.containsKey(name);
    }

    protected char lookBehind() {
        if (cursor == 0) return 0;
        return expr[cursor - 1];
    }

    protected char lookBehind(int range) {
        if ((cursor - range) <= 0) return 0;
        else {
            return expr[cursor - range];
        }
    }

    protected char lookAhead() {
        int tmp = cursor + 1;
        if (tmp != length) return expr[tmp];
        return 0;
    }

    protected char lookAhead(int range) {
        if ((cursor + range) >= length) return 0;
        else {
            return expr[cursor + range];
        }
    }

    public int nextNonBlack() {
        if ((cursor + 1) >= length) {
            return -1;
        }
        int i = cursor;
        while (i != length && isWhitespace(expr[i])) i++;
        return i;
    }

    protected boolean isStatementManuallyTerminated() {
        int c = cursor;
        while (c != length && isWhitespace(expr[c])) c++;
        return (c != length && expr[c] == ';');
    }

    protected boolean isRemain(int range) {
        return (cursor + range) != length;
    }

    protected boolean isNext(char c) {
        return lookAhead() == c;
    }

    protected boolean isAt(char c, int range) {
        return lookAhead(range) == c;
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

    protected void newContext() {
        contextControl(SET, new ParserContext(), this);
    }

    protected void newContext(ParserContext pCtx) {
        contextControl(SET, pCtx, this);
    }

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
        _loadLanguageFeaturesByLevel(level);
    }

    private static void _loadLanguageFeaturesByLevel(int languageLevel) {
        switch (languageLevel) {
            case 5:  // control flow operations
                OPERATORS.put("if", IF);
                OPERATORS.put("else", ELSE);
                OPERATORS.put("?", Operator.TERNARY);
                //   OPERATORS.put("switch", SWITCH);

            case 4: // assignment
                OPERATORS.put("=", Operator.ASSIGN);
                OPERATORS.put("var", TYPED_VAR);
                OPERATORS.put("+=", ASSIGN_ADD);
                OPERATORS.put("-=", ASSIGN_SUB);

            case 3: // iteration
                OPERATORS.put("foreach", FOREACH);
                //   OPERATORS.put("while", WHILE);
                //    OPERATORS.put("for", FOR);
                //   OPERATORS.put("do", DO);

            case 2: // multi-statement
                OPERATORS.put("return", RETURN);
                OPERATORS.put(";", END_OF_STMT);

            case 1: // boolean, math ops, projection, assertion, objection creation, block setters, imports
                OPERATORS.put("+", ADD);
                OPERATORS.put("-", SUB);
                OPERATORS.put("*", MULT);
                OPERATORS.put("**", POWER);
                OPERATORS.put("/", DIV);
                OPERATORS.put("%", MOD);
                OPERATORS.put("==", EQUAL);
                OPERATORS.put("!=", NEQUAL);
                OPERATORS.put(">", GTHAN);
                OPERATORS.put(">=", GETHAN);
                OPERATORS.put("<", LTHAN);
                OPERATORS.put("<=", LETHAN);
                OPERATORS.put("&&", AND);
                OPERATORS.put("and", AND);
                OPERATORS.put("||", OR);
                OPERATORS.put("or", CHOR);
                OPERATORS.put("~=", REGEX);
                OPERATORS.put("instanceof", INSTANCEOF);
                OPERATORS.put("is", INSTANCEOF);
                OPERATORS.put("contains", CONTAINS);
                OPERATORS.put("soundslike", SOUNDEX);
                OPERATORS.put("strsim", SIMILARITY);
                OPERATORS.put("convertable_to", CONVERTABLE_TO);

                OPERATORS.put("#", STR_APPEND);

                OPERATORS.put("&", BW_AND);
                OPERATORS.put("|", BW_OR);
                OPERATORS.put("^", BW_XOR);
                OPERATORS.put("<<", BW_SHIFT_LEFT);
                OPERATORS.put("<<<", BW_USHIFT_LEFT);
                OPERATORS.put(">>", BW_SHIFT_RIGHT);
                OPERATORS.put(">>>", BW_USHIFT_RIGHT);

                OPERATORS.put("new", Operator.NEW);
                OPERATORS.put("in", PROJECTION);

                OPERATORS.put("with", WITH);

                OPERATORS.put("assert", ASSERT);
                OPERATORS.put("import", IMPORT);
                OPERATORS.put("import_static", IMPORT_STATIC);

                OPERATORS.put("++", INC);
                OPERATORS.put("--", DEC);

            case 0: // Property access and inline collections
                OPERATORS.put(":", TERNARY_ELSE);
        }
    }

    public static void resetParserContext() {
        contextControl(REMOVE, null, null);
    }

    protected static boolean isArithmeticOperator(int operator) {
        //  return operator == ADD || operator == MULT || operator == SUB || operator == DIV;
        return operator < 6;
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
        if ((tk = nextToken()) != null && tk.isOperator()) {
            if (isArithmeticOperator(operator2 = tk.getOperator()) && PTABLE[operator2] > PTABLE[operator]) {
                xswap();
                /**
                 * The current arith. operator is of higher precedence the last.
                 */
                dStack.push(operator = operator2, nextToken().getReducedValue(ctx, ctx, variableFactory));

                while (true) {
                    // look ahead again
                    if ((tk = nextToken()) != null && PTABLE[operator2 = tk.getOperator()] > PTABLE[operator]) {
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
                    else if (tk != null) {
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
                            while (stk.size() != 1 && PTABLE[((Integer) stk.peek2())] >= PTABLE[operator]) {
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
                                if (!((Boolean) stk.peek())) return -2;
                                else {
                                    splitAccumulator.add(tk);
                                    return AND;
                                }
                            }
                            case OR: {
                                if (((Boolean) stk.peek())) return -2;
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

        return -1;
    }

    private void dreduce() {
        stk.push(dStack.pop(), dStack.pop());

        // reduce the top of the stack
        reduce();
    }

    private void dreduce2() {
        Object o1, o2;
        boolean x = false;

        do {
            if (x = !x) {
                o1 = dStack.pop();
                o2 = dStack.pop();
                if (!dStack.isEmpty()) stk.push(dStack.pop());
            }
            else {
                o2 = dStack.pop();
                o1 = dStack.pop();
            }

            stk.push(o1);
            stk.push(o2);

            reduce();
        }
        while (dStack.size() > 1);
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
