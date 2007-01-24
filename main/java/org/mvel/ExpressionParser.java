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

import static org.mvel.DataConversion.canConvert;
import static org.mvel.DataConversion.convert;
import static org.mvel.Operator.*;
import static org.mvel.PropertyAccessor.get;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.integration.impl.MapVariableResolverFactory;
import org.mvel.optimizers.impl.refl.GetterAccessor;
import org.mvel.util.ExecutionStack;
import static org.mvel.util.ParseTools.containsCheck;
import static org.mvel.util.ParseTools.debug;
import static org.mvel.util.PropertyTools.*;
import org.mvel.util.Stack;
import org.mvel.util.StringAppender;

import java.io.Serializable;
import static java.lang.Character.isWhitespace;
import static java.lang.Class.forName;
import static java.lang.String.valueOf;
import static java.lang.System.arraycopy;
import java.math.BigDecimal;
import java.util.*;
import static java.util.Collections.synchronizedMap;
import static java.util.regex.Pattern.compile;


public class ExpressionParser extends AbstractParser {
    private boolean returnBigDecimal = false;
    private int roundingMode = BigDecimal.ROUND_HALF_DOWN;

    private boolean compileMode = false;
    private boolean fastExecuteMode = false;
    private boolean reduce = true;

    private Object ctx;
    private TokenIterator tokens;
    private VariableResolverFactory variableFactory;
    private final Stack stk = new ExecutionStack();
    private ExecutableStatement compiledExpression;

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

    public static Object eval(String expression, Object ctx) {
        return new ExpressionParser(expression, ctx).parse();
    }

    public static Object eval(String expression, VariableResolverFactory resolverFactory) {
        return new ExpressionParser(expression, resolverFactory).parse();
    }

    public static Object eval(char[] expression, Object ctx, VariableResolverFactory resolverFactory) {
        return new ExpressionParser(expression, ctx, resolverFactory).parse();
    }

    public static Object eval(String expression, Object ctx, VariableResolverFactory resolverFactory) {
        return new ExpressionParser(expression, ctx, resolverFactory).parse();
    }

    @SuppressWarnings({"unchecked"})
    public static Object eval(String expression, Map tokens) {
        return new ExpressionParser(expression, null, tokens).parse();
    }

    @SuppressWarnings({"unchecked"})
    public static Object eval(String expression, Object ctx, Map tokens) {
        return new ExpressionParser(expression, ctx, tokens).parse();
    }

    /**
     * Compiles an expression and returns a Serializable object containing the compiled
     * expression.
     *
     * @param expression - the expression to be compiled
     * @return -
     */
    public static Serializable compileExpression(String expression) {
        ExpressionParser parser = new ExpressionParser(expression)
                .setCompileMode(true);

        TokenIterator tokens = (TokenIterator) parser.parse();

        if (parser.tokens.size() == 1 && tokens.firstToken().isIdentifier()) {
            return new ExecutableAccessor(tokens.firstToken(), parser.isBooleanModeOnly(), parser.isReturnBigDecimal());
        }


        return new CompiledExpression(parser.getExpressionArray(), tokens);
    }

    /**
     * Compiles an expression and returns a Serializable object containing the compiled
     * expression.
     *
     * @param expression - the expression to be compiled
     * @return -
     */
    public static Serializable compileExpression(char[] expression) {
        ExpressionParser parser = new ExpressionParser(expression)
                .setCompileMode(true);

        TokenIterator tokens = (TokenIterator) parser.parse();

        /**
         * If there is only one token, and it's an identifier, we can optimize this as an accessor expression.
         */
        if (tokens.size() == 1 && tokens.firstToken().isIdentifier()) {
            return new ExecutableAccessor(tokens.firstToken(), parser.isBooleanModeOnly(), parser.isReturnBigDecimal());
        }


        return new CompiledExpression(parser.getExpressionArray(), tokens);
    }

    public static Object executeExpression(Object compiledExpression) {
        return ((ExecutableStatement) compiledExpression).getValue(null, null);
        //   return new ExpressionParser(compiledExpression).parse();
    }

    /**
     * Executes a compiled expression.
     *
     * @param compiledExpression -
     * @param ctx                -
     * @param vars               -
     * @return -
     * @see #compileExpression(String)
     */
    @SuppressWarnings({"unchecked"})
    public static Object executeExpression(final Object compiledExpression, final Object ctx, final Map vars) {
        return ((ExecutableStatement) compiledExpression).getValue(ctx, new MapVariableResolverFactory(vars));
    }

    public static Object executeExpression(final Object compiledExpression, final Object ctx, final VariableResolverFactory resolverFactory) {
        return ((ExecutableStatement) compiledExpression).getValue(ctx, resolverFactory);
    }

    /**
     * Executes a compiled expression.
     *
     * @param compiledExpression -
     * @param factory            -
     * @return -
     * @see #compileExpression(String)
     */
    public static Object executeExpression(final Object compiledExpression, final VariableResolverFactory factory) {
        return ((ExecutableStatement) compiledExpression).getValue(null, factory);
    }

    /**
     * Executes a compiled expression.
     *
     * @param compiledExpression -
     * @param ctx                -
     * @return -
     * @see #compileExpression(String)
     */
    public static Object executeExpression(final Object compiledExpression, final Object ctx) {
        return ((ExecutableStatement) compiledExpression).getValue(ctx, null);
    }


    /**
     * Executes a compiled expression.
     *
     * @param compiledExpression -
     * @param vars               -
     * @return -
     * @see #compileExpression(String)
     */
    @SuppressWarnings({"unchecked"})
    public static Object executeExpression(final Object compiledExpression, final Map vars) {
        return ((ExecutableStatement) compiledExpression).getValue(null, new MapVariableResolverFactory(vars));
    }


    /**
     * Execute a compiled expression and convert the result to a type
     *
     * @param compiledExpression -
     * @param ctx                -
     * @param vars               -
     * @param toType             -
     * @return -
     */
    @SuppressWarnings({"unchecked"})
    public static <T> T executeExpression(final Object compiledExpression, final Object ctx, final Map vars, Class<T> toType) {
        return convert(executeExpression(compiledExpression, ctx, vars), toType);
    }

    /**
     * Execute a compiled expression and convert the result to a type
     *
     * @param compiledExpression -
     * @param vars               -
     * @param toType             -
     * @return -
     */
    @SuppressWarnings({"unchecked"})
    public static <T> T executeExpression(final Object compiledExpression, Map vars, Class<T> toType) {
        return convert(executeExpression(compiledExpression, vars), toType);
    }


    /**
     * Execute a compiled expression and convert the result to a type.
     *
     * @param compiledExpression -
     * @param ctx                -
     * @param toType             -
     * @return -
     */
    public static <T> T executeExpression(final Object compiledExpression, final Object ctx, Class<T> toType) {
        return convert(executeExpression(compiledExpression, ctx), toType);
    }


    public static Object[] executeAllExpression(Serializable[] compiledExpressions, Object ctx, VariableResolverFactory vars) {
        if (compiledExpressions == null) return GetterAccessor.EMPTY;

        Object[] o = new Object[compiledExpressions.length];
        for (int i = 0; i < compiledExpressions.length; i++) {
            o[i] = executeExpression(compiledExpressions[i], ctx, vars);
        }
        return o;
    }

    @SuppressWarnings({"unchecked"})
    public static <T> T eval(char[] expression, Object ctx, Map vars, Class<T> toType) {
        return convert(new ExpressionParser(expression, ctx, vars).parse(), toType);
    }

    @SuppressWarnings({"unchecked"})
    public static <T> T eval(char[] expression, Map vars, Class<T> toType) {
        return convert(new ExpressionParser(expression, null, vars).parse(), toType);
    }

    @SuppressWarnings({"unchecked"})
    public static Object eval(char[] expression, Object ctx, Map vars) {
        return new ExpressionParser(expression, ctx, vars).parse();
    }

    public static String evalToString(String expression, Object ctx) {
        return valueOf(eval(expression, ctx));
    }

    @SuppressWarnings({"unchecked"})
    public static String evalToString(String expression, Map vars) {
        return valueOf(eval(expression, vars));
    }

    @SuppressWarnings({"unchecked"})
    public static String evalToString(String expression, Object ctx, Map vars) {
        return valueOf(eval(expression, ctx, vars));
    }

    /**
     * Evaluate an expression in Boolean-only mode.
     *
     * @param expression -
     * @param ctx        -
     * @param vars       -
     * @return -
     */
    @SuppressWarnings({"unchecked"})
    public static Boolean evalToBoolean(String expression, Object ctx, Map vars) {
        return (Boolean) new ExpressionParser(expression, ctx, vars, true).parse();
    }


    /**
     * Evaluate an expression in Boolean-only mode.
     *
     * @param expression -
     * @param ctx        -
     * @return -
     */
    public static Boolean evalToBoolean(String expression, Object ctx) {
        return (Boolean) new ExpressionParser(expression, ctx, true).parse();
    }

    /**
     * Evaluate an expression in Boolean-only mode.
     *
     * @param expression -
     * @param ctx        -
     * @param factory    -
     * @return -
     */
    public static Boolean evalToBoolean(String expression, Object ctx, VariableResolverFactory factory) {
        return (Boolean) new ExpressionParser(expression, ctx, factory, true).parse();
    }

    /**
     * Evaluate an expression in Boolean-only mode.
     *
     * @param expression -
     * @param factory    -
     * @return -
     */
    public static Boolean evalToBoolean(String expression, VariableResolverFactory factory) {
        return (Boolean) new ExpressionParser(expression, null, factory, true).parse();
    }


    /**
     * Evaluate an expression in Boolean-only mode.
     *
     * @param expression -
     * @param vars       -
     * @return -
     */
    public static Boolean evalToBoolean(String expression, Map vars) {
        return evalToBoolean(expression, null, vars);
    }

    Object parse() {
        stk.clear();

        fields = (Token.BOOLEAN_MODE & fields);

        cursor = 0;

        assert debug("\n**************\nPARSER_START_" + (fastExecuteMode ? "ACCEL" : (compileMode ? "COMPILE" : "INTERP")));
        assert debug("EXPR: " + (expr != null ? new String(expr) : "<COMPILED>"));

        if (fastExecuteMode) {
            assert debug(tokens.showTokenChain());

            parseAndExecuteAccelerated();
        }
        else if (compileMode) {
            return parseCompile();
        }
        else {
            parseAndExecuteInterpreted();
        }

        Object result = stk.peek();

        if (isBooleanModeOnly()) {
            if (result instanceof Boolean) return result;
            else if (result instanceof BigDecimal) {
                return !BlankLiteral.INSTANCE.equals(((BigDecimal) result).floatValue());
            }
            else {
                if (result instanceof Boolean) {
                    return ((Token) result).getLiteralValue();
                }
                return !BlankLiteral.INSTANCE.equals(result);
            }
        }

        if (result instanceof BigDecimal) {
            if (returnBigDecimal) return result;
            else if (((BigDecimal) result).scale() > 14) {
                return ((BigDecimal) result).floatValue();
            }
            else if (((BigDecimal) result).scale() > 0) {
                return ((BigDecimal) result).doubleValue();
            }
            else if (((BigDecimal) result).longValue() > Integer.MAX_VALUE) {
                return ((BigDecimal) result).longValue();
            }
            else {
                return ((BigDecimal) result).intValue();
            }
        }
        else
            return result;

    }

    boolean lookAhead = false;

    private void parseAndExecuteInterpreted() {
        Token tk;
        Integer operator;

        while ((tk = nextToken()) != null) {
            if (stk.size() == 0) {
                if (!compileMode) {
                    stk.push(tk.getReducedValue(ctx, ctx, variableFactory));
                }
            }

            if (!tk.isOperator()) {
                continue;
            }

            switch (reduceBinary(operator = tk.getOperator())) {
                case-1:
                    return;
                case 0:
                    break;
                case 1:
                    continue;
            }

            if ((tk = nextToken()) == null) continue;

            stk.push(compileMode ? "" : tk.getReducedValue(tk.isPush() ? stk.pop() : ctx, ctx, variableFactory), operator);

            if (!compileMode) reduceTrinary();
        }
    }

    private void parseAndExecuteAccelerated() {
        Token tk;
        Integer operator;

        while ((tk = tokens.nextToken()) != null) {
            assert debug("\nSTART_FRAME <<" + tk + ">> STK_SIZE=" + stk.size() + "; STK_PEEK=" + stk.peek() + "; TOKEN#=" + tokens.index());
            if (stk.size() == 0) {
                stk.push(tk.getReducedValueAccelerated(ctx, ctx, variableFactory));
                // tk = tokens.nextToken();
            }

            if (!tk.isOperator()) {
                continue;
            }

            switch (reduceBinary(operator = tk.getOperator())) {
                case-1:
                    assert debug("FRAME_KILL_PROC");
                    return;
                case 0:
                    assert debug("FRAME_CONTINUE");
                    break;
                case 1:
                    assert debug("FRAME_NEXT");
                    continue;
            }

            if (!tokens.hasMoreTokens()) return;

            stk.push(reduce ? (tokens.nextToken()).getReducedValueAccelerated(ctx, ctx, variableFactory) : tk, operator);


            if (!compileMode) reduceTrinary();
        }
        assert debug("NO_MORE_TOKENS");
    }

    private TokenIterator parseCompile() {
        assert debug("BEGIN_COMPILE length=" + length + ", cursor=" + cursor);
        Token tk;
        TokenMap tokenMap = null;
        
        while ((tk = nextToken()) != null) {
            assert debug ("COMPILING_TOKEN <<" + tk + ">>::ASSIGNMENT=" + (tk.getFlags() & Token.ASSIGN));
            if (tk.isSubeval()) {
                assert debug ("BEGIN_SUBCOMPILE");
                tk.setCompiledExpression((ExecutableStatement) compileExpression(tk.getNameAsArray()));
                assert debug ("FINISH_SUBCOMPILE");
            }

            if (tokenMap == null) {
                tokenMap = new TokenMap(tk);
            }
            else {
                tokenMap.addTokenNode(tk);
            }
        }

        if (tokenMap == null)
            throw new CompileException("Nothing to do.");

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
        assert debug("BINARY_OP " + o + " PEEK=<<" + stk.peek() + ">>");
        switch (o) {
            case AND:
                if (stk.peek() instanceof Boolean && !((Boolean) stk.peek())) {
                    assert debug("STMT_UNWIND");
                    if (unwindStatement()) {
                        return -1;
                    }
                    else {
                        stk.clear();
                        return 1;
                    }
                }
                else {
                    stk.discard();
                    return 1;
                }
            case OR:
                if (stk.peek() instanceof Boolean && ((Boolean) stk.peek())) {
                    assert debug("STMT_UNWIND");
                    if (unwindStatement()) {
                        return -1;
                    }
                    else {
                        stk.clear();
                        return 1;
                    }
                }
                else {
                    stk.discard();
                    return 1;
                }

            case TERNARY:
                Token tk;
                if (!compileMode && (Boolean) stk.pop()) {
                    //   stk.discard();
                    return 1;
                }
                else {
                    reduce = false;
                    stk.clear();

                    while ((tk = nextToken()) != null && !tk.isOperator(Operator.TERNARY_ELSE)) {
                        //nothing
                    }

                    reduce = true;

                    return 1;
                }


            case TERNARY_ELSE:
                return -1;

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
                    return -1;
                }
                else if (!hasNoMore()) {
                    stk.clear();
                }

                return 1;
        }
        return 0;
    }

    private boolean hasNoMore() {
        if (fastExecuteMode) return !tokens.hasMoreTokens();
        else return cursor >=
                length;
    }

    /**
     * This method is called when we reach the point where we must subEval a trinary operation in the expression.
     * (ie. val1 op val2).  This is not the same as a binary operation, although binary operations would appear
     * to have 3 structures as well.  A binary structure (or also a junction in the expression) compares the
     * current state against 2 downrange structures (usually an op and a val).
     */
    private void reduceTrinary() {
        Object v1 = null, v2;
        Integer operator;
        try {
            while (stk.size() > 1) {
                if ((v1 = stk.pop()) instanceof Boolean) {
                    /**
                     * There is a boolean value at the top of the stk, so we
                     * are at a boolean junction.
                     */
                    operator = (Integer) stk.pop();
                    v2 = processToken(stk.pop());
                }
                else {
                    operator = (Integer) v1;
                    v1 = processToken(stk.pop());
                    v2 = processToken(stk.pop());
                }

                assert debug("DO_TRINARY <<OPCODE_" + operator + ">> register1=" + v1 + "; register2=" + v2);

                switch (operator) {
                    case ADD:
                        if (v1 instanceof BigDecimal && v2 instanceof BigDecimal) {
                            stk.push(((BigDecimal) v1).add((BigDecimal) v2));
                        }
                        else {
                            stk.push(valueOf(v2) + valueOf(v1));
                        }
                        break;

                    case SUB:
                        stk.push(((BigDecimal) v2).subtract(((BigDecimal) v1)));
                        break;

                    case DIV:
                        stk.push(((BigDecimal) v2).divide(((BigDecimal) v1), 20, roundingMode));
                        break;

                    case MULT:
                        stk.push(((BigDecimal) v2).multiply((BigDecimal) v1));
                        break;

                    case MOD:
                        stk.push(((BigDecimal) v2).remainder((BigDecimal) v1));
                        break;

                    case EQUAL:

                        if (v1 instanceof BigDecimal && v2 instanceof BigDecimal) {
                            stk.push(((BigDecimal) v2).compareTo((BigDecimal) v1) == 0);
                        }
                        else if (v1 != null)
                            stk.push(v1.equals(v2));
                        else if (v2 != null)
                            stk.push(v2.equals(v1));
                        else
                            stk.push(v1 == v2);
                        break;

                    case NEQUAL:

                        if (v1 instanceof BigDecimal && v2 instanceof BigDecimal) {
                            stk.push(((BigDecimal) v2).compareTo((BigDecimal) v1) != 0);
                        }
                        else if (v1 != null)
                            stk.push(!v1.equals(v2));
                        else if (v2 != null)
                            stk.push(!v2.equals(v1));
                        else
                            stk.push(v1 != v2);
                        break;
                    case GTHAN:
                        stk.push(((BigDecimal) v2).compareTo((BigDecimal) v1) == 1);
                        break;
                    case LTHAN:
                        stk.push(((BigDecimal) v2).compareTo((BigDecimal) v1) == -1);
                        break;
                    case GETHAN:
                        stk.push(((BigDecimal) v2).compareTo((BigDecimal) v1) >= 0);
                        break;
                    case LETHAN:
                        stk.push(((BigDecimal) v2).compareTo((BigDecimal) v1) <= 0);
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
            throw new CompileException("syntax error or incomptable types", expr, cursor, e);

        }
        catch (Exception e) {
            throw new CompileException("failed to subEval expression", e);
        }

    }

    private static int asInt(final Object o) {
        return ((BigDecimal) o).intValue();
    }

    private Object processToken(Object operand) {
        setFieldFalse(Token.EVAL_RIGHT);

        if (operand instanceof Token) {
            if (((Token) operand).isNumeric()) {
                return ((Token) operand).getNumericValue();
            }
            else if (!((Token) operand).isLiteral()) {
                return ((Token) operand).getLiteralValue();
            }
            else {
                if (((Token) operand).isEvalRight()) fields |= Token.EVAL_RIGHT;
                return ((Token) operand).getLiteralValue();
            }
        }
        else if (operand instanceof BigDecimal) {
            return operand;
        }
        else if (isNumber(operand)) {
            return new BigDecimal(valueOf(operand));
        }
        else {
            return operand;
        }
    }


    private Object reduceFast(Token tk) {
        if (tk.isSubeval()) {
            /**
             * This token represents a subexpression, and we must recurse into that expression.
             */

            if (fastExecuteMode) {
                /**
                 * We are executing it fast mode, so we simply execute the compiled subexpression.
                 */
                return (tk.getCompiledExpression().getValue(ctx, variableFactory));

            }
            else if (compileMode) {
                /**
                 * If we are compiling, then we optimize the subexpression.
                 */
                tk.setCompiledExpression((ExecutableStatement) compileExpression(tk.getValueAsString()));
            }
        }
        else if (!tk.isDoNotReduce()) {
            //   tk.setFinalValue(subEval(reduceToken(tk))).getLiteralValue();
        }
        return tk;
    }


    public String getExpression() {
        return new String(expr);
    }


    /**
     * This method is called to unwind the current statement without any reduction or further parsing.
     *
     * @return -
     */
    private boolean unwindStatement() {
        if (fastExecuteMode) return unwindStatementAccelerated();

        Token tk;
        reduce = false;
        while ((tk = nextToken()) != null && !tk.isOperator(Operator.END_OF_STMT)) {
            //nothing
        }
        reduce = true;
        return tk == null;
    }

    private boolean unwindStatementAccelerated() {
        reduce = false;
        //noinspection StatementWithEmptyBody
        while (tokens.hasMoreTokens() && !tokens.nextToken().isOperator(Operator.END_OF_STMT)) ;
        reduce = true;
        return !tokens.hasMoreTokens();
    }

    public void setExpression(String expression) {
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

    public void setExpressionArray(char[] expressionArray, int start, int offset) {
        arraycopy(expressionArray, start, this.expr = new char[this.length = offset - start], 0, offset);
    }

    public char[] getExpressionArray() {
        return expr;
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


    public boolean isBooleanModeOnly() {
        return (fields & Token.BOOLEAN_MODE) != 0;
    }

    /**
     * <p>Sets the compiler into boolean mode.  When operating in boolean-mode, the
     * parser ALWAYS returns a Boolean value based on the Boolean-only rules.</p>
     * <p/>
     * The returned boolean value will be returned based on the following rules, in this order:
     * <p/>
     * 1. Is the terminal value on the stack a Boolean? If so, return it directly.<br/>
     * 2. Is the value on the stack null? If so, return false.<br/>
     * 3. Is the value on the stack empty (0, zero-length, or an empty collections? If so, return false.<br/>
     * 4. Otherwise return true.<br/>
     *
     * @param booleanModeOnly - boolean denoting mode.
     */
    public void setBooleanModeOnly(boolean booleanModeOnly) {
        if (booleanModeOnly)
            fields |= Token.BOOLEAN_MODE;
        else
            setFieldFalse(Token.BOOLEAN_MODE);
    }

    private ExpressionParser setCompileMode(boolean compileMode) {
        if (this.compileMode = compileMode) tokens = new TokenMap(null);
        return this;
    }

    public ExpressionParser setPrecompiledExpression(Object expression) {
        (this.tokens = ((CompiledExpression) expression).getTokenMap()).reset();

        this.fastExecuteMode = true;
        return this;
    }

    private void addTokenToMap(Token tk) {
        ((TokenMap) tokens).addTokenNode(tk);
    }


    ExpressionParser(char[] expression, Object ctx, Map<String, Object> variables) {
        this.expr = expression;
        this.length = expr.length;
        this.ctx = ctx;
        this.variableFactory = new MapVariableResolverFactory(variables);
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


    ExpressionParser(String expression, Object ctx, Map<String, Object> variables, boolean booleanMode) {
        setExpression(expression);
        this.ctx = ctx;
        this.variableFactory = new MapVariableResolverFactory(variables);
        this.fields = booleanMode ? fields | Token.BOOLEAN_MODE : fields;
    }

    ExpressionParser(String expression, Object ctx, boolean booleanMode) {
        setExpression(expression);
        this.ctx = ctx;
        this.fields = booleanMode ? fields | Token.BOOLEAN_MODE : fields;
    }


    ExpressionParser(String expression, Object ctx, VariableResolverFactory resolverFactory, boolean booleanMode) {
        setExpression(expression);
        this.ctx = ctx;
        this.variableFactory = resolverFactory;
        this.fields = booleanMode ? fields | Token.BOOLEAN_MODE : fields;
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

    ExpressionParser(VariableResolverFactory resolverFactory, Object ctx, TokenIterator tokens) {
        this.ctx = ctx;
        this.variableFactory = resolverFactory;
        (this.tokens = tokens).reset();
        this.fastExecuteMode = true;
    }

    ExpressionParser(String expression, Object ctx) {
        setExpression(expression);
        this.ctx = ctx;
    }

    public void setCompiledStatement(Serializable compiled) {
        fastExecuteMode = true;
        this.compiledExpression = (ExecutableStatement) compiled;
    }

    public void setTokens(TokenIterator tokenIterator) {
        fastExecuteMode = true;
        (this.tokens = tokenIterator).reset();
    }

    public void setVariableResolverFactory(VariableResolverFactory factory) {
        this.variableFactory = factory;
    }

    public Object executeFast() {
//        tokens.reset();
        return compiledExpression.getValue(ctx, variableFactory);
    }

    public ExpressionParser resetParser() {
        tokens.reset();
        return this;
    }
}

