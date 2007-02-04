package org.mvel;

import static org.mvel.util.ParseTools.debug;
import static org.mvel.util.ParseTools.handleEscapeSequence;
import static org.mvel.util.PropertyTools.isDigit;
import static org.mvel.util.PropertyTools.isIdentifierPart;

import static java.lang.Character.isWhitespace;

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
                assert debug("END_OF_IDENTIFIER");

                /**
                 * This hack is needed to handle inline collections within a projection.
                 */

                switch (expr[start]) {
                    case'i': //handle 'in'
                        if (cursor < (length - 2) && expr[start + 1] == 'n' && isWhitespace(expr[start + 2])) {
                            return createToken(expr, start, cursor, fields);
                        }
                        break;
                    case'n': //handle 'new'
                        if (cursor < (length - 3) && expr[start + 1] == 'e' && expr[start + 2] == 'w'
                                && isWhitespace(expr[start + 3])) {

                            fields |= Token.NEW;
                            start += 4;
                            capture = false;
                            continue;
                        }
                        break;

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
        return new Token(expr, start, end, fields);
    }

    private void captureToEOS() {
        while (cursor < length && expr[cursor] != ';') {
            cursor++;
        }
    }

    private void skipWhitespace() {
        while (isWhitespace(expr[cursor])) cursor++;
    }

    private void trimWhitespace() {
        while (cursor > 0 && isWhitespace(expr[cursor - 1])) cursor--;
    }

    protected void setFieldFalse(int flag) {
        if (((fields & flag) != 0)) {
            fields = fields ^ flag;
        }
    }
}
