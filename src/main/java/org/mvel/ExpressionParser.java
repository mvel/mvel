package org.mvel;

import static org.mvel.DataConversion.canConvert;
import static org.mvel.DataConversion.convert;
import static org.mvel.Operator.*;
import static org.mvel.PropertyAccessor.get;
import org.mvel.compiled.GetterAccessor;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.integration.impl.LocalVariableResolverFactory;
import org.mvel.integration.impl.MapVariableResolverFactory;
import org.mvel.optimizers.ExecutableStatement;
import org.mvel.util.*;
import static org.mvel.util.ParseTools.*;
import static org.mvel.util.PropertyTools.*;
import org.mvel.util.Stack;

import java.io.Serializable;
import static java.lang.Character.isWhitespace;
import static java.lang.Class.forName;
import static java.lang.String.valueOf;
import static java.lang.System.arraycopy;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import static java.util.Collections.synchronizedMap;
import static java.util.regex.Pattern.compile;

public class ExpressionParser {
    private char[] expr;

    private boolean returnBigDecimal = false;

    private RoundingMode roundingMode = RoundingMode.HALF_DOWN;

    private boolean compileMode = false;
    private boolean fastExecuteMode = false;
    private boolean failNext = false;

    private int fields;
    private int cursor;
    private int length;

    private Object ctx;

    private TokenIterator tokens;

    private VariableResolverFactory variableFactory;

    private final Stack stk = new ExecutionStack();

    private PropertyAccessor propertyAccessor;
    private CompiledExpression compiledExpression;

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

        parser.parse();

        return new CompiledExpression(parser.getExpressionArray(), parser.tokens);
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

        parser.parse();

        return new CompiledExpression(parser.getExpressionArray(), parser.tokens);
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

        //    return new ExpressionParser(compiledExpression, ctx, vars).parse();
    }

    public static Object executeExpression(final Object compiledExpression, final Object ctx, final VariableResolverFactory resolverFactory) {
        return ((ExecutableStatement) compiledExpression).getValue(ctx, resolverFactory);

        //   return new ExpressionParser(compiledExpression, ctx, resolverFactory).parse();
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

        //    return new ExpressionParser(compiledExpression, factory).parse();
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

        //     return new ExpressionParser(compiledExpression, ctx).parse();
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

        //     return new ExpressionParser(compiledExpression, null, vars).parse();
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

    public static <T> T eval(char[] expression, Object ctx, Class<T> toType) {
        return convert(new ExpressionParser(expression, ctx).parse(), toType);
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

        if (fastExecuteMode)
            parseAndExecuteAccelerated();
        else
            parseAndExecuteInterpreted();

        Object result = stk.peek();

        if (isBooleanModeOnly()) {
            if (result instanceof Boolean) return result;
            else if (result instanceof Token) {
                if (((Token) result).getValue() instanceof Boolean) {
                    return ((Token) result).getValue();
                }
                return !BlankLiteral.INSTANCE.equals(((Token) result).getValue());
            }
            else if (result instanceof BigDecimal) {
                return !BlankLiteral.INSTANCE.equals(((BigDecimal) result).floatValue());
            }
            throw new CompileException("unknown exception in expression: encountered unknown stack element: " + result);
        }
        else if (result instanceof Token) {
            result = ((Token) result).getValue();
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

    /**
     * This method is called to reduce a binary statement (or junction).  The difference between a binary and
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
        switch (o) {
            case AND:
                if (stk.peek() instanceof Boolean && !((Boolean) valueOnly(stk.peek()))) {
                    fields |= Token.DO_NOT_REDUCE;
                    return unwindStatement() ? -1 : 0;
                }
                break;
            case OR:
                if (stk.peek() instanceof Boolean && ((Boolean) valueOnly(stk.peek()))) {
                    return unwindStatement() ? -1 : 0;
                }
                break;

            case TERNARY:
                Token tk;
                if (!compileMode && (Boolean) valueOnly(stk.peek())) {
                    stk.discard();
                    return 1;
                }
                else {
                    fields |= Token.CAPTURE_ONLY;
                    stk.clear();

                    while ((tk = nextToken()) != null && !tk.isOperator(Operator.TERNARY_ELSE)) {
                        //nothing
                    }

                    setFieldFalse(Token.CAPTURE_ONLY);

                    return 1;
                }


            case TERNARY_ELSE:
                return -1;

            case END_OF_STMT:
                setFieldFalse(Token.LISTCREATE);
                if (fastExecuteMode) {
                    if ((fields & Token.ASSIGN) != 0 || !tokens.hasMoreTokens()) {
                        return -1;
                    }
                    else {
                        stk.clear();
                        return 1;
                    }
                }

                if ((fields & Token.ASSIGN) != 0 || cursor == length) {
                    return -1;
                }
                else {
                    stk.clear();
                    return 1;
                }

            case ASSIGN:
                if (!(tk = (Token) stk.pop()).isValidNameIdentifier())
                    throw new CompileException("invalid identifier: " + tk.getName());

                fields |= Token.ASSIGN;
                parseAndExecuteInterpreted();
                fields ^= Token.ASSIGN;

                //noinspection unchecked
                finalLocalVariableFactory().createVariable(tk.getName(), stk.pushAndPeek(valueOnly(stk.pop())));

                if (fastExecuteMode) {
                    if (tokens.hasMoreTokens()) {
                        stk.clear();
                    }
                }
                else if (cursor != length) {
                    stk.clear();
                }

                return 1;

            case NEW:
                stk.discard();

                try {
                    fields |= Token.CAPTURE_ONLY;

                    String[] name = captureContructorAndResidual(fastExecuteMode ? nextCompiledToken().getName() : nextToken().getName());

                    stk.push(constructObject(name[0], ctx, variableFactory));
                    setFieldFalse(Token.CAPTURE_ONLY);

                    if (name.length == 2) {
                        stk.push(get(name[1], stk.pop()));
                    }
                }
                catch (InstantiationException e) {
                    throw new CompileException("unable to isntantiate class", e);
                }
                catch (IllegalAccessException e) {
                    throw new CompileException("unable to instantiate class", e);
                }
                catch (InvocationTargetException e) {
                    throw new CompileException("unable to instantiate class", e);

                }
                catch (ClassNotFoundException e) {
                    throw new CompileException("class not found: " + e.getMessage(), e);
                }

                return 1;
        }
        return 0;
    }

    /**
     * This method is called when we reach the point where we must reduce a trinary operation in the expression.
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
                else if ((fields & Token.EVAL_RIGHT) != 0) {
                    operator = (Integer) v1;
                    v2 = processToken(stk.pop());
                    v1 = processToken(stk.pop());
                }
                else {
                    operator = (Integer) v1;
                    v1 = processToken(stk.pop());
                    v2 = processToken(stk.pop());
                }

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
                 * This will allow for some developers who like messy expressions to compileGetChain
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
            throw new CompileException("failed to reduce expression", e);
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
                return ((Token) operand).getValue();
            }
            else {
                if (((Token) operand).isEvalRight()) fields |= Token.EVAL_RIGHT;
                return ((Token) operand).getValue();
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


    private void parseAndExecuteInterpreted() {
        Token tk;
        Integer operator;

        while ((tk = nextToken()) != null) {
            if (stk.size() == 0) {
                if (tk.isSubeval()) {
                    stk.push(reduce(tk));
                }
                else {
                    stk.push(tk);
                }

                if (!tk.isOperator() && (tk = nextToken()) == null) {
                    return;
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

            if (((tk = nextToken()).isSubeval())) {
                stk.push(reduce(tk), operator);
            }
            else {
                stk.push(tk, operator);
            }

            if (!compileMode) reduceTrinary();
        }
    }


    private void parseAndExecuteAccelerated() {
        Token tk;
        Integer operator;

        while ((tk = nextCompiledToken()) != null) {
            if (stk.size() == 0) {
                if (tk.isSubeval()) {
                    stk.push(reduce(tk));
                }
                else {
                    stk.push(tk);
                }

                if (!tk.isOperator() && (tk = nextCompiledToken()) == null) {
                    return;
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

            if ((tk = nextCompiledToken()).isSubeval()) {
                stk.push(reduce(tk), operator);
            }
            else {
                stk.push(tk, operator);
            }

            if (!compileMode) reduceTrinary();
        }
    }


    private static Object valueOnly(Object o) {
        return (o instanceof Token) ? ((Token) o).getValue() : o;
    }

    private void reduceFast(Token tk) {
        if (tk.isSubeval()) {
            /**
             * This token represents a subexpression, and we must recurse into that expression.
             */

            setFieldFalse(Token.SUBEVAL);

            if (fastExecuteMode) {
                /**
                 * We are executing it fast mode, so we simply execute the compiled subexpression.
                 */
                tk.setFinalValue(tk.getCompiledExpression().getValue(ctx, variableFactory)).getValue();

                //      tk.setFinalValue(executeExpression(tk.getCompiledExpression(), ctx, variableFactory)).getValue();
            }
            else if (compileMode) {
                /**
                 * If we are compiling, then we compile the subexpression.
                 */
                tk.setCompiledExpression((CompiledExpression) compileExpression(tk.getValueAsString()));
            }
        }
        else if (!tk.isDoNotReduce()) {
            tk.setFinalValue(reduce(reduceToken(tk))).getValue();
        }
    }

    private static Object reduceParse(String ex, Object ctx, VariableResolverFactory variableFactory) {
        return new ExpressionParser(ex, ctx, variableFactory).parse();
    }


    private Object reduce(Token tok) {
        if (compileMode) return tok.getName();
        else if (tok.isNegation()) {
            return !((Boolean) reduceParse(tok.getValueAsString(), ctx, variableFactory));
        }
        else if (tok.isInvert()) {
            Object o = reduceParse(tok.getValueAsString(), ctx, variableFactory);

            if (o instanceof Integer)
                return ~((Integer) o);
            else
                return ~((BigDecimal) o).intValue();
        }
        else if (tok.isSubeval()) {
            setFieldFalse(Token.SUBEVAL);
            return reduceParse(tok.getValueAsString(), ctx, variableFactory);
        }
        else return tok.getValue();
    }


    /**
     * Retrieve the next token in the expression.
     *
     * @return -
     */
    private Token nextToken() {
        if (fastExecuteMode) return nextCompiledToken();

        Token tk;

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

        fields = fields & (Token.ASSIGN | Token.BOOLEAN_MODE | Token.LISTCREATE | Token.CAPTURE_ONLY | Token.NOCOMPILE
                | Token.MAPCREATE | Token.ARRAYCREATE | Token.PUSH | Token.NEST | Token.ENDNEST);

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
                /**
                 * If we *were* capturing a token, and we just hit a non-identifier
                 * character, we stop and figure out what to do.
                 */

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
                     * compile error now.
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
                            if ((fields & (Token.LISTCREATE | Token.MAPCREATE)) != 0) break;
                        case'[':
                        case'\'':
                        case'"':
                        case'.':
                            cursor++;
                            continue;
                    }

                }

                /**
                 * Produce the token.
                 */
                return createToken(expr, start, cursor, fields);
            }
            else
                switch (expr[cursor]) {
                    case'=': {
                        if (expr[++cursor] != '=') {
                            return createToken(expr, start, cursor++, fields);
                        }
                        else {
                            return createToken(expr, start, ++cursor, fields);
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

                    case';':
                    case'#':
                    case'?':
                    case':':
                    case'^':
                    case'/':
                    case'+':
                    case'*':
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
                            }
                        }
                        if (brace > 0)
                            throw new CompileException("unbalanced braces in expression: (" + brace + "):" + new String(expr));

                        tk = createToken(expr, start + 1, cursor - 1, fields |= Token.SUBEVAL);

                        if (cursor < length && (expr[cursor] == '.')) {
                            stk.push(reduce(tk));
                            continue;
                        }

                        return tk;
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
                        return createToken(expr, start + 1, cursor++, fields |= Token.STR_LITERAL | Token.LITERAL);


                    case'"':
                        while (++cursor < length && expr[cursor] != '"') {
                            if (expr[cursor] == '\\') handleEscapeSequence(expr[++cursor]);
                        }
                        if (cursor == length || expr[cursor] != '"') {
                            throw new CompileException("unterminated literal: " + new String(expr));
                        }
                        return createToken(expr, start + 1, cursor++, fields |= Token.STR_LITERAL | Token.LITERAL);


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

                    /**
                     * The inline map, array, and list creation code starts here.  Be very careful about
                     * attempting any optimizations that might look obvious or useful.  This is already
                     * quite optimized.  Keep in mind the parser uses a sliding state system when doing
                     * anything.  Often the appearance of redundancy is not such.
                     */
                    case'[':
                        if (capture) {
                            cursor++;
                            continue;
                        }

                        cursor++;

                        fields |= Token.LISTCREATE | Token.NOCOMPILE;

                        Token tk1 = nextToken();

                        fields |= Token.NOCOMPILE;
                        Token tk2 = nextToken();

                        Token starting = null;

                        if (tk2 != null && tk2.isOperator(Operator.TERNARY_ELSE)) {
                            setFieldFalse(Token.LISTCREATE);

                            if (compileMode) {
                                setFieldFalse(Token.NOCOMPILE);
                                tk1.setFlag(false, Token.LISTCREATE);
                                tk1.setFlag(true, Token.MAPCREATE);

                                tk2.setFlag(false, Token.LISTCREATE);
                                tk2.setFlag(true, Token.MAPCREATE);

                                ((TokenMap) tokens).addTokenNode(starting = new Token('[', Token.MAPCREATE | Token.NEST));
                                ((TokenMap) tokens).addTokenNode(tk1);
                            }

                            tk2 = nextToken();

                            fields |= Token.MAPCREATE;

                            Map<Object, Object> map = new HashMap<Object, Object>();
                            map.put(reduce(tk1), reduce(tk2));
                            skipWhitespace();

                            try {
                                while (expr[cursor++] != ']') {
                                    tk1 = nextToken();
                                    fields |= Token.NOCOMPILE;

                                    if ((tk2 = nextToken()) == null || !tk2.isOperator(Operator.TERNARY_ELSE))
                                        throw new CompileException("unexpected token or end of expression, in map creation construct: " + tk2.getName());

                                    map.put(reduce(tk1), reduce(nextToken()));

                                    skipWhitespace();
                                }
                            }
                            catch (ArrayIndexOutOfBoundsException e) {
                                throw new CompileException("unterminated list projection");
                            }

                            if (compileMode) {
                                ((TokenMap) tokens).addTokenNode(new Token(']', Token.ENDNEST));
                                starting.setKnownSize(map.size());
                            }

                            setFieldFalse(Token.MAPCREATE);

                            if (cursor < length && (expr[cursor] == '.')) {
                                capture = false;
                                fields |= Token.PUSH;
                                stk.push(map);
                                continue;
                            }

                            return (createToken(expr, start + 1, cursor - 1, fields |= Token.DO_NOT_REDUCE | Token.NOCOMPILE))
                                    .setValue(map);
                        }
                        else {
                            tk1.setFlag(false, Token.MAPCREATE);

                            ArrayList<Object> projectionList = new ArrayList<Object>();
                            projectionList.add(reduce(tk1));

                            if (compileMode) {
                                ((TokenMap) tokens).addTokenNode(starting = new Token('[', Token.LISTCREATE | Token.NEST));
                                ((TokenMap) tokens).addTokenNode(tk1);
                            }

                            try {
                                while (expr[cursor++] != ']') {
                                    projectionList.add(reduce(nextToken()));

                                    skipWhitespace();
                                }

                                if (compileMode) {
                                    addTokenToMap(new Token(']', fields | Token.ENDNEST));
                                    starting.setKnownSize(projectionList.size());
                                }
                            }
                            catch (ArrayIndexOutOfBoundsException e) {
                                throw new CompileException("unterminated list projection");
                            }

                            setFieldFalse(Token.LISTCREATE);

                            if (cursor < length && (expr[cursor] == '.')) {
                                capture = false;

                                fields |= Token.PUSH;

                                stk.push(projectionList);
                                continue;
                            }

                            return (createToken(expr, start + 1, cursor - 1, fields |= Token.DO_NOT_REDUCE | Token.NOCOMPILE))
                                    .setValue(projectionList);
                        }


                    case'{':
                        fields |= Token.ARRAYCREATE;

                        starting = null;

                        if (compileMode) {
                            addTokenToMap(starting = new Token('{', fields | Token.NEST));
                        }

                        ArrayList<Object> projectionList = new ArrayList<Object>();

                        try {
                            while (expr[cursor++] != '}') {
                                projectionList.add(reduce(nextToken()));
                            }

                            if (compileMode) {
                                addTokenToMap(new Token('}', fields | Token.ENDNEST));
                                starting.setKnownSize(projectionList.size());
                            }

                        }
                        catch (ArrayIndexOutOfBoundsException e) {
                            throw new CompileException("unterminated list projection");
                        }

                        setFieldFalse(Token.ARRAYCREATE);

                        if (cursor < length && (expr[cursor] == '.')) {
                            capture = false;

                            fields |= Token.PUSH;

                            stk.push(projectionList.toArray());
                            continue;
                        }


                        return (createToken(expr, start + 1, cursor - 1, fields |= Token.DO_NOT_REDUCE | Token.NOCOMPILE))
                                .setValue(projectionList.toArray());

                    case']':
                    case'}':
                    case',':
                        if ((fields & (Token.LISTCREATE | Token.ARRAYCREATE | Token.MAPCREATE)) != 0) {
                            return createToken(expr, start, cursor, fields |= Token.DO_NOT_REDUCE | Token.NOCOMPILE);
                        }
                        else if (!capture) {
                            throw new CompileException("unexpected: " + expr[cursor]);

                        }
                        else {
                            ++cursor;
                            continue;
                        }

                    case'.':
                        start++;
                        if (!capture) {
                            cursor++;
                            fields |= Token.CAPTURE_ONLY | Token.PUSH;
                            tk = nextToken();
                            setFieldFalse(Token.CAPTURE_ONLY);
                            setFieldFalse(Token.PUSH);

                            if (!compileMode)
                                return tk.setValue(get((tk).getName(), stk.pop()));
                            else
                                return tk;
                        }

                    default:
                        cursor++;
                }
        }

        return createToken(expr, start, cursor, fields);
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
    private Token createToken(final char[] expr, final int start, final int end, final int fields) {
        Token tk = new Token(expr, start, end, fields);
        if (compileMode) {
            if (!tk.isNoCompile()) {
                ((TokenMap) tokens).addTokenNode(tk);

                if (tk.isSubeval()) reduceFast(tk);
            }
            setFieldFalse(Token.NOCOMPILE);
        }
        else if (tk.isIdentifier() && (fields & Token.DO_NOT_REDUCE) == 0) {
            return reduceToken(tk);
        }

        if (tk.isThisRef()) return tk.setFinalValue(ctx);

        return tk;
    }

    /**
     * Reduce a token.  When a token is reduced, it's targets are evaluated it's value is populated.
     * <p/>
     * NOTE: It's currently an architectural must that both interpreted and compiled mode share this method, unlike
     * some of the other seperate methods due to the way things work.   So don't try and create a "reduceTokenFast"
     * method at this point, as you'll experience unexpected results.
     *
     * @param token -
     * @return -
     */
    private Token reduceToken(Token token) {
        String s;
        if (token.isLiteral() || ((fields & Token.CAPTURE_ONLY) != 0)) {
            return token;
        }
        else if (fastExecuteMode) {
            try {
                return token.getOptimizedValue(token.isPush() ? valueOnly(stk.pop()) : ctx, ctx, variableFactory);
            }
            catch (OptimizationFailure e) {
                /**
                 * If the token failed to optimize, we perform a lookahead to see if this is forgivable.
                 */
                if (lookAhead()) {
                    /**
                     * The token failed to reduce for forgivable reasons (ie. an assignment) so we record this
                     * as a deferral, so we don't try to reduce this anymore at runtime.
                     */
                    token.createDeferralOptimization();
                    return token;
                }
                else {
                    /**
                     * This is a genuine error.  We bail.
                     */
                    throw e;
                }
            }
            catch (PropertyAccessException e) {
                throw e;
            }
            catch (Exception e) {
                /**
                 * For the purpose of dynamic re-optimization we allow a "retry" of the reduction after we have
                 * cleared the old optimized tree.  However, if the failNext bit remains high, we bail as we
                 * clearly can't re-optimize.
                 */
                if (failNext) {
                    throw new CompileException("optimization failure for: " + token, e);
                }
                else {
                    failNext = false;
                }

                try {
                    synchronized (token) {
                        token.deOptimize();
                        failNext = true;
                        return reduceToken(token);
                    }
                }
                catch (Exception e2) {
                    throw new CompileException("optimization failure for: " + token, e);
                }
            }
        }

        int tkflags = token.getFlags();

        /**
         * To save the GC a little bit of work, we don't initialize a property accessor unless we need it, and we
         * reuse the same instance if we need it more than once.
         */
        if (propertyAccessor == null)
            propertyAccessor = new PropertyAccessor(variableFactory, ctx);

        if (((tkflags | fields) & Token.PUSH) != 0) {
            /**
             * This token is attached to a stack value, and we use the top stack value as the context for the
             * property accessor.
             */
            return token.setValue(propertyAccessor.setParameters(expr, token.getStart(), token.getEnd(), valueOnly(stk.pop()), ctx).get());
        }
        else if ((tkflags & Token.DEEP_PROPERTY) != 0) {
            /**
             * The token is a DEEP PROPERTY (meaning it contains unions) in which case we need to traverse an object
             * graph.
             */
            if (Token.LITERALS.containsKey(s = token.getAbsoluteRootElement())) {
                /**
                 * The root of the DEEP PROPERTY is a literal.
                 */
                Object literal = Token.LITERALS.get(s);
                if (literal == ThisLiteral.class) literal = ctx;

                return token.setValue(propertyAccessor.setParameters(expr, token.getStart() + token.getFirstUnion(), token.getEnd(), literal, ctx).get());
            }
            else if (variableFactory != null && variableFactory.isResolveable(s)) {
                /**
                 * The root of the DEEP PROPERTY is a local or global var.
                 */
                return token.setValue(propertyAccessor.setParameters(expr, token.getStart() +
                        token.getAbsoluteFirstPart(),
                        token.getEnd(), variableFactory.getVariableResolver(s).getValue(), ctx).get());

            }
            else if (ctx != null) {
                /**
                 * We didn't resolve the root, yet, so we assume that if we have a VROOT then the property must be
                 * accessible as a field of the VROOT.
                 */

                try {
                    return token.setValue(propertyAccessor.setParameters(expr, token.getStart(), token.getEnd(), ctx, ctx).get());
                }
                catch (PropertyAccessException e) {
                    /**
                     * No luck. Make a last-ditch effort to resolve this as a static-class reference.
                     */
                    Token tk = tryStaticAccess(token);
                    if (tk == null) throw e;
                    return tk;
                }
            }
            else {
                Token tk = tryStaticAccess(token);
                if (tk == null) throw new CompileException("unable to resolve token: " + s);
                return tk;
            }
        }
        else {
            if (Token.LITERALS.containsKey(s = token.getAbsoluteName())) {
                /**
                 * The token is actually a literal.
                 */
                return token.setValue(Token.LITERALS.get(s));
            }
            else if (variableFactory != null && variableFactory.isResolveable(s)) {
                /**
                 * The token is a local or global var.
                 */

                if (token.isCollection()) {
                    return token.setValue(propertyAccessor.setParameters(expr, token.getStart()
                            + token.getEndOfName(), token.getEnd(), variableFactory.getVariableResolver(s).getValue(), ctx).get());
                }
                return token.setValue(variableFactory.getVariableResolver(s).getValue());
            }
            else if (ctx != null) {
                /**
                 * Check to see if the var exists in the VROOT.
                 */
                try {
                    return token.setValue(propertyAccessor.setParameters(expr, token.getStart(),
                            token.getEnd(), ctx, ctx).get());
                }
                catch (RuntimeException e) {
                    /**
                     * Nope.  Let's see if this a a LA-issue.
                     */
                    if (!lookAhead()) throw e;
                }
            }
            else {
                if (!lookAhead())
                    throw new CompileException("unable to resolve token: " + s);
            }
        }
        return token;
    }

    private Token tryStaticAccess(Token token) {
        try {
            /**
             * Try to resolve this *smartly* as a static class reference.
             *
             * This starts at the end of the token and starts to step backwards to figure out whether
             * or not this may be a static class reference.  We search for method calls simply by
             * inspecting for ()'s.  The first union area we come to where no brackets are present is our
             * test-point for a class reference.  If we find a class, we pass the reference to the
             * property accessor along  with trailing methods (if any).
             *
             */
            boolean meth = false;
            int depth = 0;
            int last = token.getEnd();
            for (int i = last - 1; i > token.getStart(); i--) {
                switch (expr[i]) {
                    case'.':
                        if (!meth) {
                            return token.setValue(
                                    propertyAccessor.setParameters(
                                            expr, last, token.getEnd(),
                                            forName(new String(expr, token.getStart(), last - token.getStart())), ctx
                                    ).get());
                        }
                        meth = false;
                        last = i;
                        break;
                    case')':
                        if (depth++ == 0)
                            meth = true;
                        break;
                    case'(':
                        depth--;
                        break;
                }
            }
        }
        catch (Exception cnfe) {
            // do nothing.
        }

        return null;
    }

    /**
     * This method is called by the parser when it can't resolve a token.  There are two cases where this may happen
     * under non-fatal circumstances: ASSIGNMENT or PROJECTION.  If one of these situations is indeed the case,
     * the execution continues after a quick adjustment to allow the parser to continue as if we're just at a
     * junction.  Otherwise we explode.
     *
     * @return -
     */
    private boolean lookAhead() {
        Token tk;

        int cursorCurrent = cursor;
        if (!compileMode && (tk = nextToken()) != null) {
            if (!tk.isOperator()) {
                throw new CompileException("expected operator but encountered token: " + tk.getName());
            }
            else if (tk.getOperator() == Operator.ASSIGN || tk.getOperator() == Operator.PROJECTION) {
                cursor = cursorCurrent;
                if (fastExecuteMode) {
                    tokens.back();
                }
            }
            else
                return false;
        }
        else {
            return false;
        }
        return true;
    }

    public String getExpression() {
        return new String(expr);
    }

    private void skipWhitespace() {
        while (isWhitespace(expr[cursor])) cursor++;
    }

    /**
     * This method is called to unwind the current statement without any reduction or further parsing.
     *
     * @return -
     */
    private boolean unwindStatement() {
        Token tk;
        fields |= Token.CAPTURE_ONLY;
        while ((tk = nextToken()) != null && !tk.isOperator(Operator.END_OF_STMT)) {
            //nothing
        }
        setFieldFalse(Token.CAPTURE_ONLY);
        return tk == null;
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

    public RoundingMode getRoundingMode() {
        return roundingMode;
    }

    public void setRoundingMode(RoundingMode roundingMode) {
        this.roundingMode = roundingMode;
    }

    public boolean isReturnBigDecimal() {
        return returnBigDecimal;
    }

    public void setReturnBigDecimal(boolean returnBigDecimal) {
        this.returnBigDecimal = returnBigDecimal;
    }

    private void setFieldFalse(int flag) {
        if (((fields & flag) != 0)) {
            fields = fields ^ flag;
        }
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
     * 3. Is the value on the stack empty (0, zero-length, or an empty collection? If so, return false.<br/>
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

    private Token nextCompiledToken() {
        if (!tokens.hasMoreTokens()) return null;
        Token tk;
        /**
         * If we're running in fast-execute mode (aka. running a compiled expression)
         * we retrieve the next token from the compiled stack
         *
         * TODO: Move this to another method ASAP.  This is ridiculous.  (Note from Mike to Mike)
         */

        if ((tk = tokens.nextToken()).isCollectionCreation()) {
            /**
             * We must handle collection creation differently for compiled
             * execution.  This is not code duplication.  Don't report this.
             */
            switch (tk.getCollectionCreationType()) {
                case Token.LISTCREATE: {
                  //  List<Object> newList = new ArrayList<Object>(tk.getKnownSize());
                   Object[] newList = new Object[tk.getKnownSize()];
                    int i = 0;

                    newList[i++] = handleSubNesting(tokens.nextToken());

                    //newList.add(handleSubNesting(tokens.nextToken()));

                    while (tokens.hasMoreTokens() &&
                            (!tokens.peekToken().isEndNest())) {

                       // newList.add(handleSubNesting(tokens.nextToken()));
                        newList[i++] = handleSubNesting(tokens.nextToken());
                    }

                    tokens.skipToken();

                    tk.setFlag(true, Token.DO_NOT_REDUCE);

                    return tk.setFinalValue(new FastList(newList));
                }

                case Token.MAPCREATE: {
                    Map<Object, Object> newMap = new FastMap(tk.getKnownSize());

                    newMap.put(handleSubNesting(tk = tokens.nextToken()), handleSubNesting(tokens.nextToken()));

                    while (tokens.hasMoreTokens() && !tokens.peekToken().isEndNest()) {
                        newMap.put(handleSubNesting(tokens.nextToken()), handleSubNesting(tokens.nextToken()));
                    }

                    tokens.skipToken();

                    tk.setFlag(true, Token.DO_NOT_REDUCE);
                    tk.setFinalValue(newMap);
                }
                break;

                case Token.ARRAYCREATE: {
                    Object[] newArray = new Object[tk.getKnownSize()];
                    int i = 0;

                    newArray[i++] = handleSubNesting(tokens.nextToken());

                    while (tokens.hasMoreTokens() &&
                            !tokens.peekToken().isEndNest()) {
                        newArray[i++] = handleSubNesting(tokens.nextToken());
                    }

                    tokens.skipToken();

                    tk.setFlag(true, Token.DO_NOT_REDUCE);

                    return tk.setFinalValue(newArray);
                }
            }

            if (tokens.hasMoreTokens() && tokens.peekToken().isPush()) {
                stk.push(tk.getValue());
                return (tk = tokens.nextToken()).setFinalValue(get(tk.getName(), stk.pop()));
            }
        }
        else if (tk.isIdentifier()) {
            reduceToken(tk);
        }
        else if (tk.isOperator(Operator.ASSIGN)) {
            return tk;
        }
        else if (tk.isThisRef()) {
            tk.setFinalValue(ctx);
        }

        if (tk.isPush()) {
            stk.push(tk.getValue());
        }


        return tk;
    }

    private Object handleSubNesting(Token token) {
        if (token.isNestBegin()) {
            tokens.back();
            return nextCompiledToken().getValue();
        }
        else {
            return reduceToken(token).getValue();
        }
    }

    private VariableResolverFactory finalLocalVariableFactory() {
        VariableResolverFactory v = variableFactory;
        while (v != null) {
            if (v instanceof LocalVariableResolverFactory) return v;
            v = v.getNextFactory();
        }
        if (variableFactory == null)
            return variableFactory = new LocalVariableResolverFactory(new HashMap<String, Object>());
        else
            return new LocalVariableResolverFactory(new HashMap<String, Object>()).setNextFactory(variableFactory);
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
        this.expr = expression;
    }


    /**
     * Lots of messy constructors beyond here.  Most exist for performance considerations (code inlinability, etc.)
     */
    public ExpressionParser() {
    }

    ExpressionParser(Object precompiedExpr) {
        (this.tokens = (this.compiledExpression = (CompiledExpression) precompiedExpr).getTokenMap()).reset();
        this.fastExecuteMode = true;
    }

    ExpressionParser(Object precompiedExpr, Object ctx) {
        (this.tokens = (this.compiledExpression = (CompiledExpression) precompiedExpr).getTokenMap()).reset();
        this.ctx = ctx;

        this.fastExecuteMode = true;
    }

    ExpressionParser(Object precompiedExpr, VariableResolverFactory factory) {
        (this.tokens = (this.compiledExpression = (CompiledExpression) precompiedExpr).getTokenMap()).reset();
        this.variableFactory = factory;
        this.fastExecuteMode = true;
    }


    ExpressionParser(Object precompiedExpr, Object ctx, Map<String, Object> variables) {
        (this.tokens = (this.compiledExpression = (CompiledExpression) precompiedExpr).getTokenMap()).reset();
        setExpressionArray(compiledExpression.getExpression());

        this.ctx = ctx;

        this.variableFactory = variables != null ? new MapVariableResolverFactory(variables) : null;
        this.fastExecuteMode = true;
    }

    ExpressionParser(Object precompiedExpr, Object ctx, VariableResolverFactory resolverFactory) {
        (this.tokens = (this.compiledExpression = (CompiledExpression) precompiedExpr).getTokenMap()).reset();
        setExpressionArray(compiledExpression.getExpression());

        this.ctx = ctx;

        this.variableFactory = resolverFactory;
        this.fastExecuteMode = true;
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
        (this.tokens = (this.compiledExpression = (CompiledExpression) compiled).getTokenMap()).reset();
    }

    public void setTokens(TokenIterator tokenIterator) {
        fastExecuteMode = true;
        (this.tokens = tokenIterator).reset();
    }

    public void setVariableResolverFactory(VariableResolverFactory factory) {
        this.variableFactory = factory;
    }

    public Object executeFast() {
        tokens.reset();
        return parse();
    }

    public ExpressionParser resetParser() {
        tokens.reset();
        return this;
    }
}

