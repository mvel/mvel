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

import static org.mvel.util.ParseTools.doOperations;
import static org.mvel.MVEL.compileExpression;
import static org.mvel.DataConversion.canConvert;
import static org.mvel.Operator.*;
import static org.mvel.PropertyAccessor.get;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.integration.impl.MapVariableResolverFactory;
import org.mvel.util.ExecutionStack;
import static org.mvel.util.ParseTools.*;
import static org.mvel.util.PropertyTools.*;
import org.mvel.util.Stack;
import org.mvel.util.StringAppender;
import org.mvel.util.ParseTools;

import static java.lang.Character.isWhitespace;
import static java.lang.Class.forName;
import static java.lang.String.valueOf;
import java.math.BigDecimal;
import java.util.*;
import static java.util.Collections.synchronizedMap;
import static java.util.regex.Pattern.compile;


public class ExpressionParser extends AbstractParser {
    private boolean returnBigDecimal = false;
    private int roundingMode = BigDecimal.ROUND_HALF_DOWN;

    private Object ctx;
    private VariableResolverFactory variableFactory;
    private final Stack stk = new ExecutionStack();

    private static Map<String, char[]> EX_PRECACHE;

    static {
        configureFactory();
    }

    static void configureFactory() {
        if (MVEL.THREAD_SAFE) {
            EX_PRECACHE = synchronizedMap(new WeakHashMap<String, char[]>(10));
        }
        else {
            EX_PRECACHE = new WeakHashMap<String, char[]>(10);
        }
    }

    Object parse() {
        stk.clear();

        cursor = 0;

        parseAndExecuteInterpreted();

        return handleParserEgress(stk.peek(), returnBigDecimal);
    }

    /**
     * Main interpreter loop.
     */
    private void parseAndExecuteInterpreted() {
        Token tk;
        Integer operator;

        while ((tk = nextToken()) != null) {
            if (stk.size() == 0) {
                stk.push(tk.getReducedValue(ctx, ctx, variableFactory));
            }

            if (!tk.isOperator()) {
                continue;
            }

            switch (reduceBinary(operator = tk.getOperator())) {
                case FRAME_END:
                    return;
                case FRAME_CONTINUE:
                    break;
                case FRAME_NEXT:
                    continue;
            }

            stk.push(nextToken().getReducedValue(ctx, ctx, variableFactory), operator);

            reduceTrinary();
        }
    }

    public TokenIterator compileTokens() {
        Token tk;
        Token tkOp;
        Token tkLA;
        Token tkLA2;
        TokenMap tokenMap = new TokenMap();

        boolean firstLA;

        while ((tk = nextToken()) != null) {
            if (tk.isSubeval()) {
                tk.setAccessor((ExecutableStatement) compileExpression(tk.getNameAsArray()));
            }

            /**
             * This kludge of code is to handle compile-time literal reduction.  We need to avoid
             * reducing for certain literals like, 'this', ternary and ternary else.
             */
            if (tk.isLiteral() && tk.getLiteralValue() != Token.LITERALS.get("this")) {
                if ((tkOp = nextToken()) != null && tkOp.isOperator()
                        && !tkOp.isOperator(Operator.TERNARY) && !tkOp.isOperator(Operator.TERNARY_ELSE)) {

                    /**
                     * If the next token is ALSO a literal, then we have a candidate for a compile-time
                     * reduction.
                     */
                    if ((tkLA = nextToken()) != null && tkLA.isLiteral()) {
                        stk.push(tk.getLiteralValue(), tkLA.getLiteralValue(), tkOp.getLiteralValue());

                        /**
                         * Reduce the token now.
                         */
                        reduceTrinary();

                        firstLA = true;

                        /**
                         * Now we need to check to see if this is actually a continuing reduction.
                         */
                        while ((tkOp = nextToken()) != null) {
                            if ((tkLA2 = nextToken()) != null && tkLA2.isLiteral()) {
                                stk.push(tkLA2.getLiteralValue(), tkOp.getLiteralValue());
                                reduceTrinary();
                                firstLA = false;
                            }
                            else {
                                if (firstLA) {
                                    /**
                                     * There are more tokens, but we can't reduce anymore.  So
                                     * we create a reduced token for what we've got.
                                     */
                                    tokenMap.addTokenNode(new Token(Token.LITERAL, stk.pop()));
                                }
                                else {
                                    /**
                                     * We have reduced additional tokens, but we can't reduce
                                     * anymore.
                                     */
                                    tokenMap.addTokenNode(new Token(Token.LITERAL, stk.pop()), tkOp);

                                    if (tkLA2 != null) tokenMap.addTokenNode(tkLA2);
                                }
                                break;
                            }
                        }

                        /**
                         * If there are no more tokens left to parse, we check to see if
                         * we've been doing any reducing, and if so we create the token
                         * now.
                         */
                        if (!stk.isEmpty())
                            tokenMap.addTokenNode(new Token(Token.LITERAL, stk.pop()));

                        continue;
                    }
                    else {
                        tokenMap.addTokenNode(tk, tkOp);
                        if (tkLA != null) tokenMap.addTokenNode(tkLA);
                        continue;
                    }
                }
                else {
                    tokenMap.addTokenNode(tk);
                    if (tkOp != null) tokenMap.addTokenNode(tkOp);
                    continue;
                }
            }

            tokenMap.addTokenNode(tk);
        }

        return new FastTokenIterator(tokenMap);
    }

    /**
     * This method is called to subEval a binary statement (or junction).  The difference between a binary and
     * trinary statement, as far as the parser is concerned is that a binary statement has an entrant state,
     * where-as a trinary statement does not.  Consider: (x && y): in this case, x will be reduced first, and
     * therefore will have a value on the stack, so the parser will then process the next statement as a binary,
     * which is (&& y).
     * <p/>
     * You can also think of a binary statement in terms of: ({stackvalue} op value)
     *
     * @param o - operator
     * @return int - behaviour code
     */
    private int reduceBinary(int o) {
     //   assert debug("BINARY_OP " + o + " PEEK=<<" + stk.peek() + ">>");
        switch (o) {
            case AND:
                if (stk.peek() instanceof Boolean && !((Boolean) stk.peek())) {
                    if (unwindStatement()) {
                        return FRAME_END;
                    }
                    else {
                        stk.clear();
                        return FRAME_NEXT;
                    }
                }
                else {
                    stk.discard();
                    return FRAME_NEXT;
                }
            case OR:
                if (stk.peek() instanceof Boolean && ((Boolean) stk.peek())) {
                   // assert debug("STMT_UNWIND");
                    if (unwindStatement()) {
                        return FRAME_END;
                    }
                    else {
                        stk.clear();
                        return FRAME_NEXT;
                    }
                }
                else {
                    stk.discard();
                    return FRAME_NEXT;
                }

            case TERNARY:
                Token tk;
                if ((Boolean) stk.pop()) {
                    return FRAME_NEXT;
                }
                else {
                    stk.clear();

                    while ((tk = nextToken()) != null && !tk.isOperator(Operator.TERNARY_ELSE)) {
                        //nothing
                    }

                    return FRAME_NEXT;
                }


            case TERNARY_ELSE:
                return FRAME_END;

            case END_OF_STMT:
                /**
                 * Assignments are a special scenario for dealing with the stack.  Assignments are basically like
                 * held-over failures that basically kickstart the parser when an assignment operator is is
                 * encountered.  The originating token is captured, and the the parser is told to march on.  The
                 * resultant value on the stack is then used to populate the target variable.
                 *
                 * The other scenario in which we don't want to wipe the stack, is when we hit the end of the
                 * statement, because that top stack value is the value we want back from the parser.
                 */

                if ((fields & Token.ASSIGN) != 0) {
                    return FRAME_END;
                }
                else if (!hasNoMore()) {
                    stk.clear();
                }

                return FRAME_NEXT;
        }
        return FRAME_CONTINUE;
    }

    private boolean hasNoMore() {
        return cursor >= length;
    }

    /**
     * This method is called when we reach the point where we must subEval a trinary operation in the expression.
     * (ie. val1 op val2).  This is not the same as a binary operation, although binary operations would appear
     * to have 3 structures as well.  A binary structure (or also a junction in the expression) compares the
     * current state against 2 downrange structures (usually an op and a val).
     */
    private void reduceTrinary() {
        Object v1 = null, v2 = null;
        Integer operator;
        try {
            while (stk.size() > 1) {
                operator = (Integer) stk.pop();
                v1 = processToken(stk.pop());
                v2 = processToken(stk.pop());

//                assert debug("DO_TRINARY <<OPCODE_" + operator + ">> register1=" + v1 + "; register2=" + v2);

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
                        stk.push(doOperations(v2, operator, v1));
                        break;

                    case AND:
                        if (v2 instanceof Boolean && v1 instanceof Boolean) {
                            stk.push(((Boolean) v2) && ((Boolean) v1));
                            break;
                        }
                        else if (((Boolean) v2)) {
                            stk.push(v2, Operator.AND, v1);
                        }
                        return;

                    case OR:
                        if (v2 instanceof Boolean && v1 instanceof Boolean) {
                            stk.push(((Boolean) v2) || ((Boolean) v1));
                            break;
                        }
                        else {
                            stk.push(v2, Operator.OR, v1);
                            return;
                        }

                    case CHOR:
                        if (!isEmpty(v2) || !isEmpty(v1)) {
                            stk.clear();
                            stk.push(!isEmpty(v2) ? v2 : v1);
                            return;
                        }
                        else stk.push(null);
                        break;

                    case REGEX:
                        stk.push(compile(valueOf(v1)).matcher(valueOf(v2)).matches());
                        break;

                    case INSTANCEOF:
                        if (v1 instanceof Class)
                            stk.push(((Class) v1).isInstance(v2));
                        else
                            stk.push(forName(valueOf(v1)).isInstance(v2));

                        break;

                    case CONVERTABLE_TO:
                        if (v1 instanceof Class)
                            stk.push(canConvert(v2.getClass(), (Class) v1));
                        else
                            stk.push(canConvert(v2.getClass(), forName(valueOf(v1))));
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
                        stk.push(new StringAppender(valueOf(v2)).append(valueOf(v1)).toString());
                        break;

                    case PROJECTION:
                        try {
                            List<Object> list = new ArrayList<Object>(((Collection) v1).size());
                            for (Object o : (Collection) v1) {
                                list.add(get(valueOf(v2), o));
                            }
                            stk.push(list);
                        }
                        catch (ClassCastException e) {
                            throw new ParseException("projections can only be peformed on collections");
                        }
                        break;

                    case SOUNDEX:
                        stk.push(Soundex.soundex(valueOf(v1)).equals(Soundex.soundex(valueOf(v2))));
                        break;

                    case SIMILARITY:
                        stk.push(similarity(valueOf(v1), valueOf(v2)));
                        break;

                }
            }
        }
        catch (ClassCastException e) {
            if ((fields & Token.LOOKAHEAD) == 0) {
                /**
                 * This will allow for some developers who like messy expressions to compileAccessor
                 * away with some messy constructs like: a + b < c && e + f > g + q instead
                 * of using brackets like (a + b < c) && (e + f > g + q)
                 */

                fields |= Token.LOOKAHEAD;

                Token tk = nextToken();
                if (tk != null) {
                    stk.push(v1, nextToken(), tk.getOperator());

                    reduceTrinary();
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
        return ((BigDecimal) o).intValue();
    }

    private Object processToken(Object operand) {
        //   setFieldFalse(Token.EVAL_RIGHT);

        if (operand instanceof BigDecimal) {
            return operand;
        }
        else if (isNumber(operand)) {
            return new BigDecimal(valueOf(operand));
        }
        else {
            return operand;
        }
    }

    /**
     * This method is called to unwind the current statement without any reduction or further parsing.
     *
     * @return -
     */
    private boolean unwindStatement() {
        Token tk;
        while ((tk = nextToken()) != null && !tk.isOperator(Operator.END_OF_STMT)) {
            //nothing
        }
        return tk == null;
    }

    private void setExpression(String expression) {
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

    public ExpressionParser setExpressionArray(char[] expressionArray) {
        this.length = (this.expr = expressionArray).length;
        return this;
    }

    public int getRoundingMode() {
        return roundingMode;
    }

    public void setRoundingMode(int roundingMode) {
        this.roundingMode = roundingMode;
    }

    public boolean isReturnBigDecimal() {
        return returnBigDecimal;
    }

    public void setReturnBigDecimal(boolean returnBigDecimal) {
        this.returnBigDecimal = returnBigDecimal;
    }

    ExpressionParser(char[] expression, Object ctx, Map<String, Object> variables) {
        this.expr = expression;
        this.length = expr.length;
        this.ctx = ctx;
        this.variableFactory = new MapVariableResolverFactory(variables);
    }

    ExpressionParser(char[] expression, Object ctx) {
        this.expr = expression;
        this.length = expr.length;
        this.ctx = ctx;
    }


    ExpressionParser(String expression, Object ctx, Map<String, Object> variables) {
        setExpression(expression);
        this.ctx = ctx;
        this.variableFactory = new MapVariableResolverFactory(variables);
    }

    ExpressionParser(String expression) {
        setExpression(expression);
    }

    ExpressionParser(char[] expression) {
        this.length = (this.expr = expression).length;
    }

    /**
     * Lots of messy constructors beyond here.  Most exist for performance considerations (code inlinability, etc.)
     */
    public ExpressionParser() {
    }

    ExpressionParser(char[] expr, Object ctx, VariableResolverFactory resolverFactory) {
        this.length = (this.expr = expr).length;
        this.ctx = ctx;
        this.variableFactory = resolverFactory;
    }

    ExpressionParser(Object ctx, Map<String, Object> variables) {
        this.ctx = ctx;
        this.variableFactory = new MapVariableResolverFactory(variables);
    }

    ExpressionParser(String expression, Object ctx, VariableResolverFactory resolverFactory) {
        setExpression(expression);
        this.ctx = ctx;
        this.variableFactory = resolverFactory;
    }

    ExpressionParser(String expression, VariableResolverFactory resolverFactory) {
        setExpression(expression);
        this.variableFactory = resolverFactory;
    }

    ExpressionParser(String expression, Object ctx) {
        setExpression(expression);
        this.ctx = ctx;
    }
}

