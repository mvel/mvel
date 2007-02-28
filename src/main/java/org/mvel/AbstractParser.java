package org.mvel;

import static org.mvel.Operator.*;
import org.mvel.block.ForEachToken;
import org.mvel.block.IfToken;
import static org.mvel.util.ParseTools.debug;
import static org.mvel.util.ParseTools.handleEscapeSequence;
import static org.mvel.util.PropertyTools.isDigit;
import static org.mvel.util.PropertyTools.isIdentifierPart;
import org.mvel.util.ThisLiteral;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Character.isWhitespace;
import static java.util.Collections.synchronizedMap;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author Christopher Brock
 */
public class AbstractParser {

    protected char[] expr;
    protected int cursor;
    protected int length;
    protected int fields;

    protected boolean greedy = true;

    protected static final int FRAME_END = -1;
    protected static final int FRAME_CONTINUE = 0;
    protected static final int FRAME_NEXT = 1;
    protected static final int FRAME_RETURN = 2;

    private static Map<String, char[]> EX_PRECACHE;

    public static final Map<String, Object> LITERALS =
            new HashMap<String, Object>(35, 0.6f);

    public static final Map<String, Integer> OPERATORS =
            new HashMap<String, Integer>(25 * 2, 0.6f);

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
        LITERALS.put("Long", Long.class);
        LITERALS.put("Boolean", Boolean.class);
        LITERALS.put("Short", Short.class);
        LITERALS.put("Character", Character.class);
        LITERALS.put("Double", Double.class);
        LITERALS.put("Float", Float.class);
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

        float version = Float.parseFloat(System.getProperty("java.version").substring(0, 2));
        if (version >= 1.5) {
            try {
                LITERALS.put("StringBuilder", Class.forName("java.lang.StringBuilder"));
            }
            catch (Exception e) {
                throw new RuntimeException("cannot resolve a built-in literal", e);
            }
        }


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

        OPERATORS.put("?", Operator.TERNARY);
        OPERATORS.put(":", TERNARY_ELSE);

        OPERATORS.put("=", Operator.ASSIGN);

        OPERATORS.put(";", END_OF_STMT);

        OPERATORS.put("new", Operator.NEW);

        OPERATORS.put("in", PROJECTION);

        OPERATORS.put("foreach", FOREACH);
        OPERATORS.put("while", WHILE);
        OPERATORS.put("if", IF);
        OPERATORS.put("else", ELSE);

        OPERATORS.put("return", RETURN);
    }

    static void configureFactory() {
        if (MVEL.THREAD_SAFE) {
            EX_PRECACHE = synchronizedMap(new WeakHashMap<String, char[]>(10));
        }
        else {
            EX_PRECACHE = new WeakHashMap<String, char[]>(10);
        }
    }

    /**
     * Retrieve the next token in the expression.
     *
     * @return -
     */
    protected Token nextToken() {
        /**
         * If the cursor is at the end of the expression, we have nothing more to do:
         * return null.
         */
        if (cursor >= length) {
            return null;
        }

        int brace, start = cursor;

        /**
         * Because of parser recursion for sub-expression parsing, we sometimes need to remain
         * certain field states.  We do not reset for assignments, boolean mode, list creation or
         * a capture only mode.
         */

        fields = fields & (Token.CAPTURE_ONLY | Token.NOCOMPILE | Token.INLINE_COLLECTION | Token.PUSH);

        boolean capture = false;

        /**
         * Skip any whitespace currently under the starting point.
         */
        while (start < length && isWhitespace(expr[start])) start++;

        /**
         * From here to the end of the method is the core MVEL parsing code.  Fiddling around here is asking for
         * trouble unless you really know what you're doing.
         */
        for (cursor = start; cursor < length;) {
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
                            fields |= Token.NEW;
                            start = cursor + 1;
                            capture = false;
                            continue;

                        case RETURN:
                            fields |= Token.RETURN;
                            start = cursor + 1;
                            capture = false;
                            continue;

                        case IF:
                            fields |= Token.BLOCK_IF;
                            return captureConditionalBlock(expr);

                        case FOREACH:
                            fields |= Token.BLOCK_FOREACH;
                            return captureConditionalBlock(expr);
                    }
                }
                else if (isIdentifierPart(expr[cursor])) {
                    capture = true;
                    continue;
                }

                /**
                 * If we *were* capturing a token, and we just hit a non-identifier
                 * character, we stop and figure out what to do.
                 */

                skipWhitespace();

                if (expr[cursor] == '(') {
                    /**
                     * If the current token is a method call or a constructor, we
                     * simply capture the entire parenthesized range and allow
                     * reduction to be dealt with through sub-parsing the property.
                     */
                    cursor++;
                    for (brace = 1; cursor < length && brace > 0;) {
                        switch (expr[cursor++]) {
                            case'(':
                                brace++;
                                break;
                            case')':
                                brace--;
                                break;
                        }
                    }

                    /**
                     * If the brace counter is greater than 0, we know we have
                     * unbalanced braces in the expression.  So we throw a
                     * optimize error now.
                     */
                    if (brace > 0)
                        throw new CompileException("unbalanced braces in expression: (" + brace + "):" + new String(expr));
                }

                /**
                 * If we encounter any of the following cases, we are still dealing with
                 * a contiguous token.
                 */
                if (cursor < length) {


                    switch (expr[cursor]) {
                        case']':
                            if ((fields & (Token.INLINE_COLLECTION)) != 0) {
                                break;
                            }
                        case'[':
                            balancedCapture('[');
                            cursor++;
                            continue;
                        case'.':
                            cursor++;
                            assert debug("GREEDY_CAPTURE_CONTINUE ['" + (cursor < length ? expr[cursor] : "EOF")
                                    + "']");
                            continue;
                        case'=':
                            if (greedy && expr[cursor + 1] != '=') {
                                cursor++;

                                fields |= Token.ASSIGN;

                                skipWhitespace();

                                assert debug("GREEDY_CAPTURE_CONTINUE_FOR_ASSIGNMENT");

                                captureToEOS();

                                break;
                            }
                        case'i': // handle "in" fold operator
                            if (greedy && cursor < (length - 2) && expr[cursor + 1] == 'n' && isWhitespace(expr[cursor + 2])) {
                                cursor += 2;

                                fields |= Token.FOLD;

                                assert debug("GREEDY_CAPTURE_CONTINUE_FOR_FOLD");

                                capture = false;

                                continue;
                            }

                    }

                }

                assert debug("EXIT IDENTIFIER @ '" + (cursor < length ? expr[cursor] : "EOF") + "'");

                /**
                 * Produce the token.
                 */
                trimWhitespace();
                return createToken(expr, start, cursor, fields);
            }
            else
                switch (expr[cursor]) {
                    case'=': {
                        if (expr[cursor + 1] != '=') {
                            return createToken(expr, start, ++cursor, fields |= Token.ASSIGN);
                        }
                        else {
                            return createToken(expr, start, (cursor += 2), fields);
                        }
                    }

                    case'-':
                        if (!isDigit(expr[cursor + 1])) {
                            return createToken(expr, start, cursor++ + 1, fields);
                        }
                        else if ((cursor - 1) < 0 || (!isDigit(expr[cursor - 1])) && isDigit(expr[cursor + 1])) {
                            cursor++;
                            break;
                        }


                    case'*':
                        if (cursor < length && expr[cursor + 1] == '*') {
                            cursor++;
                            return createToken(expr, start, cursor++ + 1, fields);
                        }
                    case';':
                    case'#':
                    case'?':
                    case':':
                    case'^':
                    case'/':
                    case'+':

                    case'%': {
                        return createToken(expr, start, cursor++ + 1, fields);
                    }

                    case'(': {
                        cursor++;

                        for (brace = 1; cursor < length && brace > 0;) {
                            switch (expr[cursor++]) {
                                case'(':
                                    brace++;
                                    break;
                                case')':
                                    brace--;
                                    break;
                                case'i':
                                    if (cursor < length && expr[cursor] == 'n' && isWhitespace(expr[cursor + 1])) {
                                        fields |= Token.FOLD;
                                    }
                                    break;
                            }
                        }
                        if (brace > 0)
                            throw new CompileException("unbalanced braces in expression: (" + brace + "):" + new String(expr));

                        if ((fields & Token.FOLD) != 0) {
                            if (cursor < length && expr[cursor] == '.') {
                                cursor++;
                                continue;
                            }

                            return createToken(expr, start, cursor, Token.FOLD);
                        }
                        else if ((fields & Token.ASSIGN) != 0) {
                            return createToken(expr, start, cursor, fields | Token.SUBEVAL);
                        }
                        else if (cursor < length && (expr[cursor] == '.')) {

                            cursor++;
                            continue;
                        }

                        return createToken(expr, start + 1, cursor - 1, fields |= Token.SUBEVAL);

                    }

                    case'>': {
                        if (expr[cursor + 1] == '>') {
                            if (expr[cursor += 2] == '>') cursor++;
                            return createToken(expr, start, cursor, fields);
                        }
                        else if (expr[cursor + 1] == '=') {
                            return createToken(expr, start, cursor += 2, fields);
                        }
                        else {
                            return createToken(expr, start, ++cursor, fields);
                        }
                    }


                    case'<': {
                        if (expr[++cursor] == '<') {
                            if (expr[++cursor] == '<') cursor++;
                            return createToken(expr, start, cursor, fields);
                        }
                        else if (expr[cursor] == '=') {
                            return createToken(expr, start, ++cursor, fields);
                        }
                        else {
                            return createToken(expr, start, cursor, fields);
                        }
                    }


                    case'\'':
                        while (++cursor < length && expr[cursor] != '\'') {
                            if (expr[cursor] == '\\') handleEscapeSequence(expr[++cursor]);
                        }

                        if (cursor == length || expr[cursor] != '\'') {
                            throw new CompileException("unterminated literal: " + new String(expr));
                        }

                        if ((fields & Token.ASSIGN) != 0) {
                            return createToken(expr, start, ++cursor, Token.ASSIGN);
                        }
                        else {
                            return createToken(expr, start + 1, ++cursor - 1, Token.STR_LITERAL | Token.LITERAL);
                        }


                    case'"':
                        while (++cursor < length && expr[cursor] != '"') {
                            if (expr[cursor] == '\\') handleEscapeSequence(expr[++cursor]);
                        }
                        if (cursor == length || expr[cursor] != '"') {
                            throw new CompileException("unterminated literal: " + new String(expr));
                        }

                        if ((fields & Token.ASSIGN) != 0) {
                            return createToken(expr, start, ++cursor, Token.ASSIGN);
                        }
                        else {
                            return createToken(expr, start + 1, ++cursor - 1, Token.STR_LITERAL | Token.LITERAL);
                        }

                    case'&': {
                        if (expr[cursor++ + 1] == '&') {
                            return createToken(expr, start, ++cursor, fields);
                        }
                        else {
                            return createToken(expr, start, cursor, fields);
                        }
                    }

                    case'|': {
                        if (expr[cursor++ + 1] == '|') {
                            return createToken(expr, start, ++cursor, fields);
                        }
                        else {
                            return createToken(expr, start, cursor, fields);
                        }
                    }

                    case'~':
                        if ((cursor - 1 < 0 || !isIdentifierPart(expr[cursor - 1]))
                                && isDigit(expr[cursor + 1])) {

                            fields |= Token.INVERT;
                            start++;
                            cursor++;
                            break;
                        }
                        else if (expr[cursor + 1] == '(') {
                            fields |= Token.INVERT;
                            start = ++cursor;
                            continue;
                        }
                        else {
                            if (expr[cursor + 1] == '=') cursor++;
                            return createToken(expr, start, ++cursor, fields);
                        }

                    case'!': {
                        if (isIdentifierPart(expr[++cursor]) || expr[cursor] == '(') {
                            start = cursor;
                            fields |= Token.NEGATION;
                            continue;
                        }
                        else if (expr[cursor] != '=')
                            throw new CompileException("unexpected operator '!'", expr, cursor, null);
                        else {
                            return createToken(expr, start, ++cursor, fields);
                        }
                    }

                    case'[':
                        if (capture) {
                            if (balancedCapture(expr[cursor]) == -1) {
                                throw new CompileException("unbalanced '" + expr[cursor] + "'");
                            }
                            cursor++;
                            continue;
                        }

                    case'{':
                        if (balancedCapture(expr[cursor]) == -1) {
                            throw new CompileException("unbalanced '" + expr[cursor] + "': in inline map/list/array creation");
                        }

                        if (cursor < (length - 1) && expr[cursor + 1] == '.') {
                            fields |= Token.INLINE_COLLECTION;
                            cursor++;
                            continue;
                        }

                        return createToken(expr, start, ++cursor, fields | Token.INLINE_COLLECTION);

                    default:
                        cursor++;
                }
        }

        return createToken(expr, start, cursor, fields);
    }


    protected int balancedCapture(char type) {
        int depth = 1;
        char term = type;
        switch (type) {
            case'[':
                term = ']';
                break;
            case'{':
                term = '}';
                break;
            case'(':
                term = ')';
                break;
        }

        if (type == term) {
            for (cursor++; cursor < length; cursor++) {
                if (expr[cursor] == type) {
                    return cursor;
                }

            }
        }
        else {
            for (cursor++; cursor < length; cursor++) {
                if (expr[cursor] == type) {
                    depth++;
                }
                else if (expr[cursor] == term && --depth == 0) {
                    return cursor;
                }
            }
        }

        return -1;
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
    private Token createToken(final char[] expr, final int start, final int end, int fields) {
        assert debug("CAPTURE_TOKEN <<" + new String(expr, start, end - start) + ">>");
        return new Token(expr, start, end, fields);
    }

    private char[] subArray(final int start, final int end) {
        char[] newA = new char[end - start];
        System.arraycopy(expr, start, newA, 0, newA.length);
        return newA;
    }

    private Token createBlockToken(final int condStart,
                                   final int condEnd, final int blockStart, final int blockEnd) {
        if (isFlag(Token.BLOCK_IF)) {
            return new IfToken(subArray(condStart, condEnd), subArray(blockStart, blockEnd), fields);
        }
        else if (isFlag(Token.BLOCK_FOREACH)) {
            return new ForEachToken(subArray(condStart, condEnd), subArray(blockStart, blockEnd), fields);
        }
        else {
            return null;
        }
    }

    private Token captureConditionalBlock(final char[] expr) {

        boolean cond = true;
        Token tk = null;

        if (isFlag(Token.BLOCK_IF)) {

            do {
                if (tk != null) {
                    skipToWhitespace();
                    skipWhitespace();

                    if (expr[cursor] != '{') {
                        if (expr[cursor] == 'i' && expr[++cursor] == 'f'
                                && (isWhitespace(expr[++cursor]) || expr[cursor] == '{')) {
                            cond = true;
                        }
                        else {
                            throw new CompileException("expected 'if'");
                        }
                    }
                    else {
                        cond = false;
                    }
                }

                if (((IfToken) (tk = _captureConditionalBlock(tk, expr, cond))).getElseBlock() != null) {
                    return tk;
                }

                cursor++;
            }
            while (blockContinues());
        }
        else if (isFlag(Token.BLOCK_FOREACH)) {
            skipToWhitespace();
            skipWhitespace();

            return _captureConditionalBlock(null, expr, true);
        }

        return tk;
    }

    private Token _captureConditionalBlock(Token node, final char[] expr, boolean cond) {
        skipWhitespace();
        int startCond = 0;
        int endCond = 0;

        if (cond) {
            startCond = ++cursor;
            endCond = balancedCapture('(');
        }

        int blockStart = ++cursor;
        int blockEnd;

        skipWhitespace();

        if (expr[cursor] == '{') {
            if ((blockEnd = balancedCapture('{')) == -1) {
                throw new CompileException("unbalanced braces { }");
            }
        }
        else {
            captureToEOLorOF();
            blockEnd = cursor;
        }

        if (isFlag(Token.BLOCK_IF)) {
            IfToken ifNode = (IfToken) node;

            if (node != null) {
                if (!cond) {
                    ifNode.setElseBlock(subArray(trimRight(blockStart + 1), trimLeft(blockEnd - 1)));
                    return node;
                }
                else {
                    IfToken tk = (IfToken) createBlockToken(startCond, endCond, trimRight(blockStart + 2),
                            trimLeft(blockEnd));

                    ifNode.setElseIf(tk);

                    return tk;
                }
            }
            else {
                return createBlockToken(startCond, endCond, trimRight(blockStart + 2),
                        trimLeft(blockEnd));
            }
        }
        else if (isFlag(Token.BLOCK_FOREACH)) {
            return createBlockToken(startCond, endCond, trimRight(blockStart + 2), trimLeft(blockEnd));
        }

        return null;
    }

    protected boolean blockContinues() {
        if ((cursor + 5) < length) {
            skipWhitespace();
            return expr[cursor] == 'e' && expr[cursor + 1] == 'l' && expr[cursor + 2] == 's' && expr[cursor + 3] == 'e'
                    && isWhitespace(expr[cursor + 4]);
        }
        return false;
    }

    protected void captureToEOS() {
        while (cursor < length && expr[cursor] != ';') {
            cursor++;
        }
    }

    protected void captureToEOLorOF() {
        while (cursor < length && (expr[cursor] != '\n' || expr[cursor] != '\r')) {
            cursor++;
        }
    }

    protected int trimLeft(int pos) {
        while (pos > 0 && isWhitespace(expr[pos - 1])) pos--;
        return pos;
    }

    protected int trimRight(int pos) {
        while (pos < length && isWhitespace(expr[pos])) pos++;
        return pos;
    }

    protected void skipWhitespace() {
        while (isWhitespace(expr[cursor])) cursor++;
    }

    protected void skipWhitlespaceSafe() {
        while (cursor < length && isWhitespace(expr[cursor])) cursor++;
    }

    protected void skipToWhitespace() {
        while (cursor < length && !isWhitespace(expr[cursor])) cursor++;
    }

    protected void trimWhitespace() {
        while (cursor > 0 && isWhitespace(expr[cursor - 1])) cursor--;
    }

    protected void setFieldFalse(int flag) {
        if (((fields & flag) != 0)) {
            fields = fields ^ flag;
        }
    }

    protected Token captureTokenToEOS() {
        int start = cursor;
        captureToEOS();
        return new Token(expr, start, cursor, 0);
    }

    protected void setExpression(String expression) {
        if (expression != null && !"".equals(expression)) {
            if (!EX_PRECACHE.containsKey(expression)) {
                length = (this.expr = expression.toCharArray()).length;

                // trim any whitespace.
                while (isWhitespace(this.expr[length - 1])) length--;

                char[] e = new char[length];
                System.arraycopy(this.expr, 0, e, 0, length);

                EX_PRECACHE.put(expression, e);
            }
            else {
                length = (expr = EX_PRECACHE.get(expression)).length;
            }
        }
    }

    protected void setExpression(char[] expression) {
        length = (this.expr = expression).length;
        while (isWhitespace(this.expr[length - 1])) length--;
    }

    private boolean isFlag(int bit) {
        return (fields & bit) != 0;
    }
}
