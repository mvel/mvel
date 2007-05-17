package org.mvel;

import static org.mvel.Operator.*;
import org.mvel.ast.*;
import org.mvel.util.ParseTools;
import static org.mvel.util.ParseTools.handleEscapeSequence;
import static org.mvel.util.PropertyTools.isDigit;
import static org.mvel.util.PropertyTools.isIdentifierPart;
import org.mvel.util.ThisLiteral;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Character.isWhitespace;
import static java.lang.System.arraycopy;
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
    protected boolean lastWasIdentifier = false;
    protected ASTNode lastNode;

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
        OPERATORS.put("for", FOR);
        OPERATORS.put("switch", SWITCH);
        OPERATORS.put("do", DO);
        OPERATORS.put("with", WITH);

        OPERATORS.put("assert", ASSERT);

        OPERATORS.put("++", INC);
        OPERATORS.put("--", DEC);
        OPERATORS.put("+=", ASSIGN_ADD);
        OPERATORS.put("-=", ASSIGN_SUB);

        OPERATORS.put("var", TYPED_VAR);

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
    protected ASTNode nextToken() {
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

        fields = fields & (ASTNode.CAPTURE_ONLY | ASTNode.NOCOMPILE | ASTNode.INLINE_COLLECTION | ASTNode.PUSH);

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
                            start = cursor + 1;
                            captureToEOT();
                            return new NewObjectASTNode(subArray(start, cursor), fields);

                        case ASSERT:
                            start = cursor + 1;
                            captureToEOS();
                            return new AssertASTNode(subArray(start, cursor), fields);

                        case RETURN:
                            fields |= ASTNode.RETURN;
                            start = cursor + 1;
                            capture = false;
                            continue;

                        case IF:
                            fields |= ASTNode.BLOCK_IF;
                            return captureCodeBlock(expr);

                        case FOREACH:
                            fields |= ASTNode.BLOCK_FOREACH;
                            return captureCodeBlock(expr);

                        case WITH:
                            fields |= ASTNode.BLOCK_WITH;
                            return captureCodeBlock(expr);

                        case TYPED_VAR:
                            fields |= ASTNode.TYPED;
                            skipWhitespace();
                            start = cursor;
                            continue;
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
                        throw new CompileException("unbalanced braces in expression: (" + brace + "):", expr, cursor);
                }

                /**
                 * If we encounter any of the following cases, we are still dealing with
                 * a contiguous token.
                 */
                if (cursor < length) {
                    switch (expr[cursor]) {
                        case']':
                            if ((fields & (ASTNode.INLINE_COLLECTION)) != 0) {
                                break;
                            }
                        case'[':
                            balancedCapture('[');
                            cursor++;
                            continue;
                        case'.':
                            cursor++;
                            continue;
                        case'=':
                            if (greedy && expr[cursor + 1] != '=') {
                                cursor++;

                                fields |= ASTNode.ASSIGN;

                                skipWhitespace();

                                captureToEOS();

                                break;
                            }
                        case'i': // handle "in" fold operator
                            if (greedy && (cursor + 2) < length && expr[cursor + 1] == 'n' && isWhitespace(expr[cursor + 2])) {
                                cursor += 2;

                                fields |= ASTNode.FOLD;

                                capture = false;

                                continue;
                            }
                    }

                }

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
                            return createToken(expr, start, ++cursor, fields |= ASTNode.ASSIGN);
                        }
                        else {
                            return createToken(expr, start, (cursor += 2), fields);
                        }
                    }

                    case'-':
                        if ((cursor > 0 && !isWhitespace(expr[cursor - 1])) || !isDigit(expr[cursor + 1])) {
                            return createToken(expr, start, cursor++ + 1, fields);
                        }
                        else if ((cursor - 1) < 0 || (!isDigit(expr[cursor - 1])) && isDigit(expr[cursor + 1])) {
                            cursor++;
                            break;
                        }


                    case'+':
                        if (cursor + 1 < length && expr[cursor + 1] == '+') {
                            cursor++;
                        }
                        return createToken(expr, start, cursor++ + 1, fields);

                    case'*':
                        if (cursor < length && expr[cursor + 1] == '*') {
                            cursor++;
                        }
                        return createToken(expr, start, cursor++ + 1, fields);

                    case';':
                    case'#':
                    case'?':
                    case':':
                    case'^':
                    case'/':
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
                                        fields |= ASTNode.FOLD;
                                    }
                                    break;
                            }
                        }
                        if (brace > 0)
                            throw new CompileException("unbalanced braces in expression: (" + brace + "):", expr, cursor);

                        if ((fields & ASTNode.FOLD) != 0) {
                            if (cursor < length && expr[cursor] == '.') {
                                cursor++;
                                continue;
                            }

                            return createToken(expr, start, cursor, ASTNode.FOLD);
                        }
                        else if ((fields & ASTNode.ASSIGN) != 0) {
                            return createToken(expr, start, cursor, fields | ASTNode.SUBEVAL);
                        }
                        else if (cursor < length && (expr[cursor] == '.')) {

                            cursor++;
                            continue;
                        }

                        return createToken(expr, start + 1, cursor - 1, fields |= ASTNode.SUBEVAL);
                    }

                    case'}':
                    case']':
                    case')': {
                        throw new ParseException("unbalanced braces", expr, cursor);
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
                            throw new CompileException("unterminated literal", expr, cursor);
                        }

                        if ((fields & ASTNode.ASSIGN) != 0) {
                            return createToken(expr, start, ++cursor, ASTNode.ASSIGN);
                        }
                        else {
                            return createToken(expr, start + 1, ++cursor - 1, ASTNode.STR_LITERAL | ASTNode.LITERAL);
                        }


                    case'"':
                        while (++cursor < length && expr[cursor] != '"') {
                            if (expr[cursor] == '\\') handleEscapeSequence(expr[++cursor]);
                        }
                        if (cursor == length || expr[cursor] != '"') {
                            throw new CompileException("unterminated literal", expr, cursor);
                        }

                        if ((fields & ASTNode.ASSIGN) != 0) {
                            return createToken(expr, start, ++cursor, ASTNode.ASSIGN);
                        }
                        else {
                            return createToken(expr, start + 1, ++cursor - 1, ASTNode.STR_LITERAL | ASTNode.LITERAL);
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
                            return createToken(expr, start, ++cursor, fields);
                        }

                    case'!': {
                        if (isIdentifierPart(expr[++cursor]) || expr[cursor] == '(') {
                            start = cursor;
                            fields |= ASTNode.NEGATION;
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
                                throw new CompileException("unbalanced brace", expr, cursor);
                            }
                            cursor++;
                            continue;
                        }

                    case'{':
                        if (balancedCapture(expr[cursor]) == -1) {
                            if (cursor >= length) cursor--;
                            throw new CompileException("unbalanced brace: in inline map/list/array creation", expr, cursor);
                        }

                        if (cursor < (length - 1) && expr[cursor + 1] == '.') {
                            fields |= ASTNode.INLINE_COLLECTION;
                            cursor++;
                            continue;
                        }

                        return createToken(expr, start, ++cursor, fields | ASTNode.INLINE_COLLECTION);

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
    private ASTNode createToken(final char[] expr, final int start, final int end, int fields) {
        ASTNode tk = new ASTNode(expr, start, end, fields);

        if (tk.isIdentifier()) {

            if (lastWasIdentifier) {
                /**
                 * Check for typing information.
                 */
                if (lastNode.isLiteral() && lastNode.getLiteralValue() instanceof Class) {
                    lastNode.setDiscard(true);

                    captureToEOS();
                    return new TypedVarASTNode(subArray(start, cursor), fields, (Class)
                            lastNode.getLiteralValue());
                }

                throw new ParseException("not a statement", expr, cursor);
            }

            lastWasIdentifier = true;
        }
        else {
            lastWasIdentifier = false;
        }

        return lastNode = tk;
    }

    private char[] subArray(final int start, final int end) {
        char[] newA = new char[end - start];
        arraycopy(expr, start, newA, 0, newA.length);
        return newA;
    }

    private ASTNode createBlockToken(final int condStart,
                                     final int condEnd, final int blockStart, final int blockEnd) {

        lastWasIdentifier = false;

        cursor++;

        if (isFlag(ASTNode.BLOCK_IF)) {
            return new IfASTNode(subArray(condStart, condEnd), subArray(blockStart, blockEnd), fields);
        }
        else if (isFlag(ASTNode.BLOCK_FOREACH)) {
            return new ForEachASTNode(subArray(condStart, condEnd), subArray(blockStart, blockEnd), fields);
        }
        else if (isFlag(ASTNode.BLOCK_WITH)) {
            return new WithASTNode(subArray(condStart, condEnd), subArray(blockStart, blockEnd), fields);
        }
        else {
            return null;
        }
    }

    private ASTNode captureCodeBlock(final char[] expr) {
        boolean cond = true;

        ASTNode first = null;
        ASTNode tk = null;

        if (isFlag(ASTNode.BLOCK_IF)) {
            do {
                if (tk != null) {
                    skipToNextTokenJunction();
                    skipWhitespace();

                    cond = expr[cursor] != '{' && expr[cursor] == 'i' && expr[++cursor] == 'f'
                            && (isWhitespace(expr[++cursor]) || expr[cursor] == '(');
                }

                if (((IfASTNode) (tk = _captureBlock(tk, expr, cond))).getElseBlock() != null) {
                    cursor++;
                    return first;
                }

                if (first == null) first = tk;

                cursor++;
            }
            while (blockContinues());
        }
        else if (isFlag(ASTNode.BLOCK_FOREACH) || isFlag(ASTNode.BLOCK_WITH)) {
            skipToNextTokenJunction();
            skipWhitespace();
            return _captureBlock(null, expr, true);
        }


        return first;
    }

    private ASTNode _captureBlock(ASTNode node, final char[] expr, boolean cond) {
        skipWhitespace();
        int startCond = 0;
        int endCond = 0;

        if (cond) {
            startCond = ++cursor;
            endCond = balancedCapture('(');
            cursor++;
        }

        int blockStart;
        int blockEnd;

        skipWhitespace();

        if (expr[cursor] == '{') {
            blockStart = cursor;
            if ((blockEnd = balancedCapture('{')) == -1) {
                throw new CompileException("unbalanced braces { }", expr, cursor);
            }
        }
        else {
            blockStart = cursor - 1;
            captureToEOLorOF();
            blockEnd = cursor + 1;
        }

        if (isFlag(ASTNode.BLOCK_IF)) {
            IfASTNode ifNode = (IfASTNode) node;

            if (node != null) {
                if (!cond) {
                    ifNode.setElseBlock(subArray(trimRight(blockStart + 1), trimLeft(blockEnd - 1)));
                    return node;
                }
                else {
                    IfASTNode tk = (IfASTNode) createBlockToken(startCond, endCond, trimRight(blockStart + 1),
                            trimLeft(blockEnd));

                    ifNode.setElseIf(tk);

                    return tk;
                }
            }
            else {
                return createBlockToken(startCond, endCond, trimRight(blockStart + 1),
                        trimLeft(blockEnd));
            }
        }
        else if (isFlag(ASTNode.BLOCK_FOREACH) || isFlag(ASTNode.BLOCK_WITH)) {
            return createBlockToken(startCond, endCond, trimRight(blockStart + 1), trimLeft(blockEnd));
        }


        return null;
    }

    protected boolean blockContinues() {
        if ((cursor + 4) < length) {
            skipWhitespace();
            return expr[cursor] == 'e' && expr[cursor + 1] == 'l' && expr[cursor + 2] == 's' && expr[cursor + 3] == 'e'
                    && (isWhitespace(expr[cursor + 4]) || expr[cursor] == '{');
        }
        return false;
    }

    protected void captureToEOS() {
        while (cursor < length && expr[cursor] != ';') {
            cursor++;
        }
    }

    protected void captureToEOLorOF() {
        while (cursor < length && (expr[cursor] != '\n' && expr[cursor] != '\r' && expr[cursor] != ';')) {
            cursor++;
        }
    }

    protected void captureToEOT() {
        while (++cursor < length) {
            switch (expr[cursor]) {
                case'(':
                case'[':
                case'{':
                    if ((cursor = ParseTools.balancedCapture(expr, cursor, expr[cursor])) == -1) {
                        throw new CompileException("unbalanced braces", expr, cursor);
                    }
                    break;

                case';':
                    return;

                default:
                    if (isWhitespace(expr[cursor])) {
                        skipWhitespace();

                        if (expr[cursor] == '.') break;
                        else {
                            trimWhitespace();
                            return;
                        }
                    }
            }

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

    protected void skipToNextTokenJunction() {
        while (cursor < length) {
            switch (expr[cursor]) {
                case'{':
                    return;
                case'(':
                    return;
                default:
                    if (isWhitespace(expr[cursor])) return;
                    cursor++;
            }
        }
    }

    protected void trimWhitespace() {
        while (cursor > 0 && isWhitespace(expr[cursor - 1])) cursor--;
    }

    protected void setFieldFalse(int flag) {
        if (((fields & flag) != 0)) {
            fields = fields ^ flag;
        }
    }

    protected ASTNode captureTokenToEOS() {
        int start = cursor;
        captureToEOS();
        return new ASTNode(expr, start, cursor, 0);
    }

    protected void setExpression(String expression) {
        if (expression != null && !"".equals(expression)) {
            if (!EX_PRECACHE.containsKey(expression)) {
                length = (this.expr = expression.toCharArray()).length;

                // trim any whitespace.
                while (isWhitespace(this.expr[length - 1])) length--;

                char[] e = new char[length];
                arraycopy(this.expr, 0, e, 0, length);

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

    public static boolean isReservedWord(String name) {
        return LITERALS.containsKey(name) || OPERATORS.containsKey(name);
    }
}
