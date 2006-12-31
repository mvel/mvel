package org.mvel;

import static org.mvel.util.ParseTools.debug;
import org.mvel.util.ExecutionStack;
import org.mvel.util.ParseTools;
import org.mvel.util.PropertyTools;
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

    private int fields;

    private int cursor;
    private int length;

    private Object ctx;
    private Map tokens;

    private TokenIterator tokenMap;

    private final Stack stk = new ExecutionStack();

    private PropertyAccessor propertyAccessor;

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
        return new ExpressionParser(expression, ctx, null).parse();
    }

    public static Object eval(String expression, Map tokens) {
        return new ExpressionParser(expression, null, tokens).parse();
    }

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

        return new CompiledExpression(parser.getExpressionArray(), parser.tokenMap);
    }

    /**
     * Compiles an expression and returns a Serializable object containing the compiled
     * expression.
     *
     * @param expression -
     * @param ctx        -
     * @param tokens     -
     * @return -
     */
    public static Serializable compileExpression(char[] expression, Object ctx, Map tokens) {
        ExpressionParser parser = new ExpressionParser(expression, ctx, tokens)
                .setCompileMode(true);

        parser.parse();

        return new CompiledExpression(parser.getExpressionArray(), parser.tokenMap);
    }

    public static Object executeExpression(final Object compiledExpression) {
        return new ExpressionParser(compiledExpression, null, null).parse();
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
    public static Object executeExpression(final Object compiledExpression, final Object ctx, final Map vars) {
        return new ExpressionParser(compiledExpression, ctx, vars).parse();
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
        return new ExpressionParser(compiledExpression, ctx, null).parse();
    }


    /**
     * Executes a compiled expression.
     *
     * @param compiledExpression -
     * @param vars               -
     * @return -
     * @see #compileExpression(String)
     */
    public static Object executeExpression(final Object compiledExpression, final Map vars) {
        return new ExpressionParser(compiledExpression, null, vars).parse();
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
    public static <T> T executeExpression(final Object compiledExpression, final Object ctx, final Map vars, Class<T> toType) {
        return DataConversion.convert(new ExpressionParser(compiledExpression, ctx, vars).parse(), toType);
    }

    /**
     * Execute a compiled expression and convert the result to a type
     *
     * @param compiledExpression -
     * @param vars               -
     * @param toType             -
     * @return -
     */
    public static <T> T executeExpression(final Object compiledExpression, final Map vars, Class<T> toType) {
        return DataConversion.convert(new ExpressionParser(compiledExpression, null, vars).parse(), toType);
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
        return DataConversion.convert(new ExpressionParser(compiledExpression, ctx, null).parse(), toType);
    }

    public static <T> T eval(char[] expression, Object ctx, Map tokens, Class<T> toType) {
        return DataConversion.convert(new ExpressionParser(expression, ctx, tokens).parse(), toType);
    }

    public static <T> T eval(char[] expression, Object ctx, Class<T> toType) {
        return DataConversion.convert(new ExpressionParser(expression, ctx, null).parse(), toType);
    }

    public static <T> T eval(char[] expression, Map tokens, Class<T> toType) {
        return DataConversion.convert(new ExpressionParser(expression, null, tokens).parse(), toType);
    }

    public static Object eval(char[] expression, Object ctx, Map tokens) {
        return new ExpressionParser(expression, ctx, tokens).parse();
    }

    public static String evalToString(String expression, Object ctx) {
        return valueOf(eval(expression, ctx));
    }

    public static String evalToString(String expression, Map tokens) {
        return valueOf(eval(expression, tokens));
    }

    public static String evalToString(String expression, Object ctx, Map tokens) {
        return valueOf(eval(expression, ctx, tokens));
    }

    /**
     * Evaluate an expression in Boolean-only mode.
     *
     * @param expression -
     * @param ctx        -
     * @param tokens     -
     * @return -
     */
    public static Boolean evalToBoolean(String expression, Object ctx, Map tokens) {
        return (Boolean) new ExpressionParser(expression, ctx, tokens, true).parse();
    }

    /**
     * Evaluate an expression in Boolean-only mode.
     *
     * @param expression -
     * @param ctx        -
     * @return -
     */
    public static Boolean evalToBoolean(String expression, Object ctx) {
        return evalToBoolean(expression, ctx, null);
    }

    /**
     * Evaluate an expression in Boolean-only mode.
     *
     * @param expression -
     * @param tokens     -
     * @return -
     */
    public static Boolean evalToBoolean(String expression, Map tokens) {
        return evalToBoolean(expression, null, tokens);
    }

    public ExpressionParser(char[] expression, Object ctx, Map tokens) {
        this.expr = expression;
        this.length = expr.length;
        this.ctx = ctx;
        this.tokens = tokens;
    }

    public ExpressionParser(String expression, Object ctx, Map tokens) {
        setExpression(expression);
        this.ctx = ctx;
        this.tokens = tokens;
    }

    public ExpressionParser(String expression) {
        setExpression(expression);
    }

    public ExpressionParser(Object precompiedExpr, Object ctx, Map tokens) {
        this.tokenMap = ((CompiledExpression) precompiedExpr).getTokenMap();
        this.tokenMap.reset();

        this.expr = ((CompiledExpression) precompiedExpr).getExpression();


        assert debug("\n<<Executing in Compiled Mode>>");
        assert debug("Expression::<<" + new String(expr) + ">>");
        assert debug(this.tokenMap.showTokenChain());


        this.ctx = ctx;
        this.tokens = tokens;
        this.fastExecuteMode = true;

    }

    public ExpressionParser(String expression, Object ctx, Map tokens, boolean booleanMode) {
        setExpression(expression);
        this.ctx = ctx;
        this.tokens = tokens;
        this.fields = booleanMode ? fields | Token.BOOLEAN_MODE : fields;
    }

    public ExpressionParser(Object ctx, Map tokens) {
        this.ctx = ctx;
        this.tokens = tokens;
    }

    public Object parse(Object ctx, Map tokens) {
        this.ctx = ctx;
        this.tokens = tokens;
        return parse();
    }

    public Object parse() {
        stk.clear();

        fields = (Token.BOOLEAN_MODE & fields);

        cursor = 0;

        parseAndExecute();

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

    private int reduceBinary(Operator o) {
        switch (o) {
            case AND:
                if (stk.peek() instanceof Boolean && !((Boolean) stk.peek())) {
                    nextToken();
                    return -1;
                }
                break;
            case OR:
                if (stk.peek() instanceof Boolean && ((Boolean) stk.peek())) {
                    nextToken();
                    return -1;
                }
                break;

            case TERNARY:
                Token tk;
                if (!compileMode && (Boolean) stk.peek()) {
                    stk.discard();
                    return 1;
                }
                else {
                    fields |= Token.CAPTURE_ONLY;
                    stk.clear();

                    while ((tk = nextToken()) != null && !(tk.isOperator() && tk.getOperator() == Operator.TERNARY_ELSE)) {
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
                    if ((fields & Token.ASSIGN) != 0 || !tokenMap.hasMoreTokens()) {
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

                assert debug("ASSIGNMENT");


                fields |= Token.ASSIGN;
                parseAndExecute();
                fields ^= Token.ASSIGN;


                if (tokens == null) {
                    assert debug("<<CREATING local token table>>");
                    tokens = new HashMap();
                }
                assert debug("<<variable table size=" + tokens.size() + ">>");                

                //noinspection unchecked
                tokens.put(tk.getName(), stk.pushAndPeek(valueOnly(stk.pop())));

                assert debug("ASSIGNMENT to <<" + tk.getName() + ">> value=" + valueOnly(stk.peek()));

                if (fastExecuteMode) {
                    if (tokenMap.hasMoreTokens()) {
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

                    String[] name = ParseTools.captureContructorAndResidual(nextToken().getName());

                    stk.push(ParseTools.constructObject(name[0], ctx, tokens));
                    setFieldFalse(Token.CAPTURE_ONLY);

                    if (name.length == 2) {
                        stk.push(PropertyAccessor.get(name[1], stk.pop()));
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

    private void reduceTrinary() {
        Object v1 = null, v2;
        Operator operator;
        try {
            while (stk.size() > 1) {
                if ((v1 = stk.pop()) instanceof Boolean) {
                    /**
                     * There is a boolean value at the top of the stk, so we
                     * are at a boolean junction.
                     */
                    operator = (Operator) stk.pop();
                    v2 = processToken(stk.pop());
                }
                else if ((fields & Token.EVAL_RIGHT) != 0) {
                    operator = (Operator) v1;
                    v2 = processToken(stk.pop());
                    v1 = processToken(stk.pop());
                }
                else {
                    operator = (Operator) v1;
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
                            stk.push(DataConversion.canConvert(v2.getClass(), (Class) v1));
                        else
                            stk.push(DataConversion.canConvert(v2.getClass(), forName(valueOf(v1))));
                        break;

                    case CONTAINS:
                        stk.push(ParseTools.containsCheck(v2, v1));
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
                        stk.push(new StringBuilder(valueOf(v2)).append(valueOf(v1)).toString());
                        break;

                    case PROJECTION:
                        try {
                            List<Object> list = new ArrayList<Object>(((Collection) v1).size());
                            for (Object o : (Collection) v1) {
                                list.add(PropertyAccessor.get(valueOf(v2), o));
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
                 * This will allow for some developers who like messy expressions to get
                 * away with some messy constructs like: a + b < c && e + f > g + q instead
                 * of using brackets like (a + b < c) && (e + f > g + q)
                 */

                fields |= Token.LOOKAHEAD;

                Token tk = nextToken();
                stk.push(v1, nextToken(), tk.getOperator());

                reduceTrinary();
            }
            else {
                throw new CompileException("syntax error or incomptable types", expr, cursor, e);
            }
        }
        catch (Exception e) {
            throw new CompileException("failed to reduce expression: " + e);
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
        else if (isNumber(valueOf(operand))) {
            return new BigDecimal(valueOf(operand));
        }
        else {
            return operand;
        }
    }


    private void parseAndExecute() {
        assert debug("<<Executing has Begun>>");

        Token tk;
        Operator operator;

        while ((tk = nextToken()) != null) {

            if (stk.size() == 0) {
                if ((fields & Token.SUBEVAL) != 0) {
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

            assert tk.isOperator(); // The parser/or input script is borked if this isn't true.

            switch (reduceBinary(operator = tk.getOperator())) {
                case-1:
                    return;
                case 0:
                    break;
                case 1:
                    continue;
            }

            tk = nextToken();

            if ((fields & Token.SUBEVAL) != 0) {
                stk.push(reduce(tk));
            }
            else {
                stk.push(tk);
            }

            stk.push(operator);

            if (!compileMode) reduceTrinary();
        }
    }

    private static Object valueOnly(Object o) {
        return (o instanceof Token) ? ((Token) o).getValue() : o;
    }

    private Object reduceFast(Token tk) {
        assert debug("<<REDUCEFAST: " + tk + ">>");

        if ((tk.getFlags() & Token.SUBEVAL) != 0) {
            setFieldFalse(Token.SUBEVAL);

            if (compileMode) {
                tk.setCompiledExpression((CompiledExpression) compileExpression(tk.getValueAsCharArray(), ctx, tokens));
            }
            else if (fastExecuteMode) {
                return tk.setFinalValue(executeExpression(tk.getCompiledExpression(), ctx, tokens)).getValue();
            }
        }
        else if ((tk.getFlags() & Token.DO_NOT_REDUCE) == 0) {
            assert debug("<<FULL-REDUCING: " + tk + ">>");

            return tk.setFinalValue(reduce(reduceToken(tk))).getValue();
        }
        return tk;
    }

    private static Object reduceParse(char[] ex, Object ctx, Map tokens) {
        return new ExpressionParser(ex, ctx, tokens).parse();
    }


    private Object reduce(Token tok) {
        if ((tok.getFlags() & Token.NEGATION) != 0) {
            return !((Boolean) reduceParse(tok.getValueAsCharArray(), ctx, tokens));
        }
        else if ((tok.getFlags() & Token.INVERT) != 0) {
            Object o = reduceParse(tok.getValueAsCharArray(), ctx, tokens);

            if (o instanceof Integer)
                return ~((Integer) o);
            else
                return ~((BigDecimal) o).intValue();
        }
        else if (((tok.getFlags() | fields) & Token.SUBEVAL) != 0) {
            setFieldFalse(Token.SUBEVAL);
            return reduceParse(tok.getValueAsCharArray(), ctx, tokens);
        }
        else return tok.getValue();
    }


    /**
     * Retrieve the next token in the expression.
     *
     * @return -
     */
    private Token nextToken() {
        Token tk;

        if (fastExecuteMode) return nextCompiledToken();

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
        fields = ((fields & Token.ASSIGN) | (fields & Token.BOOLEAN_MODE) | (fields & Token.LISTCREATE)
                | (fields & Token.CAPTURE_ONLY) | (fields & Token.NOCOMPILE) | (fields & Token.MAPCREATE)
                | (fields & Token.ARRAYCREATE) | (fields & Token.PUSH) | (fields & Token.NEST) | (fields & Token.ENDNEST));


        boolean capture = false;

        /**
         * Skip any whitespace currently under the starting point.
         */
        while (start < length && isWhitespace(expr[start])) start++;

        for (cursor = start; cursor < length;) {
            if (isIdentifierPart(expr[cursor])) {
                /**
                 * If the current character under the cursor is a valid
                 * part of an identifier, we keep capturing.
                 */

                capture = true;
                cursor++;
                // continue;
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
                            if (((fields & Token.LISTCREATE) | (fields & Token.MAPCREATE)) != 0) break;
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
                        if (!PropertyTools.isDigit(expr[cursor + 1])) {
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
                            if (expr[cursor] == '\\') ParseTools.handleEscapeSequence(expr[++cursor]);
                        }

                        if (cursor == length || expr[cursor] != '\'') {
                            throw new CompileException("unterminated literal: " + new String(expr));
                        }
                        return createToken(expr, start + 1, cursor++, fields |= Token.LITERAL);


                    case'"':
                        while (++cursor < length && expr[cursor] != '"') {
                            if (expr[cursor] == '\\') ParseTools.handleEscapeSequence(expr[++cursor]);
                        }
                        if (cursor == length || expr[cursor] != '"') {
                            throw new CompileException("unterminated literal: " + new String(expr));
                        }
                        return createToken(expr, start + 1, cursor++, fields |= Token.LITERAL);


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
                            cursor++;
                            continue;
                        }

                        cursor++;

                        fields |= Token.LISTCREATE | Token.NOCOMPILE;

                        Token tk1 = nextToken();

                        fields |= Token.NOCOMPILE;
                        Token tk2 = nextToken();

                        if (tk2 != null && tk2.getName().equals(":")) {
                            setFieldFalse(Token.LISTCREATE);

                            if (compileMode) {
                                setFieldFalse(Token.NOCOMPILE);
                                tk1.setFlag(false, Token.LISTCREATE);
                                tk1.setFlag(true, Token.MAPCREATE);

                                tk2.setFlag(false, Token.LISTCREATE);
                                tk2.setFlag(true, Token.MAPCREATE);

                                ((TokenMap) tokenMap).addTokenNode(new Token('[', Token.MAPCREATE | Token.NEST));
                                ((TokenMap) tokenMap).addTokenNode(tk1);
                            }

                            tk2 = nextToken();

                            fields |= Token.MAPCREATE;

                            Map<Object, Object> map = new HashMap<Object, Object>();
                            map.put(reduce(tk1), reduce(tk2));

                            try {
                                while (expr[cursor++] != ']') {
                                    tk1 = nextToken();
                                    fields |= Token.NOCOMPILE;
                                    if ((tk2 = nextToken()) == null || !tk2.getName().equals(":"))
                                        throw new CompileException("unexpected token or end of expression, in map creation construct: " + tk2.getName());

                                    map.put(reduce(tk1), reduce(nextToken()));
                                }
                            }
                            catch (ArrayIndexOutOfBoundsException e) {
                                throw new CompileException("unterminated list projection");
                            }

                            if (compileMode) ((TokenMap) tokenMap).addTokenNode(new Token(']', Token.ENDNEST));

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
                                ((TokenMap) tokenMap).addTokenNode(new Token('[', Token.LISTCREATE | Token.NEST));
                                ((TokenMap) tokenMap).addTokenNode(tk1);
                            }

                            try {
                                while (expr[cursor++] != ']') {
                                    projectionList.add(reduce(nextToken()));
                                }

                                if (compileMode) {
                                    addTokenToMap(new Token(']', fields | Token.ENDNEST));
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

                        if (compileMode) {
                            addTokenToMap(new Token('{', fields | Token.NEST));
                        }


                        ArrayList<Object> projectionList = new ArrayList<Object>();

                        try {
                            while (expr[cursor++] != '}') {
                                projectionList.add(reduce(nextToken()));
                            }

                            if (compileMode) {
                                addTokenToMap(new Token('}', fields | Token.ENDNEST));
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
//                        if (compileMode) {
//                            ((TokenMap) tokenMap).addTokenNode(new Token(expr[cursor], fields | Token.ENDNEST));
//                        }
                    case',':
                        if (((fields & Token.LISTCREATE | fields & Token.ARRAYCREATE | fields & Token.MAPCREATE)) != 0) {
                            fields |= Token.DO_NOT_REDUCE;
                            return createToken(expr, start, cursor, fields);
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

                            return tk.setValue(PropertyAccessor.get((tk).getName(), stk.pop()));
                        }

                    default:
                        cursor++;

                }

        }

        return createToken(expr, start, cursor, fields);
    }

    private Token createToken(char[] expr, int start, int end, int fields) {
        Token tk = new Token(expr, start, end, fields);

        if (compileMode) {
            if ((tk.getFlags() & Token.NOCOMPILE) == 0) {
                if ((tk.getFlags() & Token.SUBEVAL) != 0) reduceFast(tk);
                ((TokenMap) tokenMap).addTokenNode(tk.clone());
            }
            setFieldFalse(Token.NOCOMPILE);
        }
        else if ((tk.getFlags() & Token.IDENTIFIER) != 0 && (fields & Token.DO_NOT_REDUCE) == 0) {
            return reduceToken(tk);
        }

        if ((tk.getFlags() & Token.THISREF) != 0) tk.setFinalValue(ctx);

        return tk;
    }

    private Token reduceToken(Token token) {
        String s;

        if (((fields & Token.CAPTURE_ONLY) | (token.getFlags() & Token.LITERAL)) != 0) {
            assert debug("<<NOT reducing literal: " + token + ">>");
            return token;
        }

        if (propertyAccessor == null) propertyAccessor = new PropertyAccessor(tokens);

        if (((token.getFlags() | fields) & Token.PUSH) != 0) {
            assert debug("<<PUSH REDUCE>>");
            return token.setValue(propertyAccessor.setParameters(expr, token.getStart(), token.getEnd(), valueOnly(stk.pop())).get());
        }
        else if ((token.getFlags() & Token.DEEP_PROPERTY) != 0) {
            if (Token.LITERALS.containsKey(s = token.getAbsoluteRootElement())) {
                Object literal = Token.LITERALS.get(s);
                if (literal == ThisLiteral.class) literal = ctx;

                return token.setValue(propertyAccessor.setParameters(expr, token.getStart() + token.getFirstUnion(), token.getEnd(), literal).get());
            }
            else if (tokens != null && tokens.containsKey(s)) {
                return token.setValue(propertyAccessor.setParameters(expr, token.getStart() +
                        token.getAbsoluteFirstPart(),
                        token.getEnd(), tokens.get(s)).get());

            }
            else if (ctx != null) {
                try {
                    return token.setValue(propertyAccessor.setParameters(expr, token.getStart(), token.getEnd(), ctx).get());
                }
                catch (PropertyAccessException e) {

                    /**
                     * Make a last-ditch effort to resolve this as a static-class reference.
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
                return token.setValue(Token.LITERALS.get(s));
            }
            else if (tokens != null && tokens.containsKey(s)) {
                if ((token.getFlags() & Token.COLLECTION) != 0) {
                    return token.setValue(propertyAccessor.setParameters(expr, token.getStart()
                            + token.getEndOfName(), token.getEnd(), tokens.get(s)).get());
                }
                return token.setValue(tokens.get(s));
            }

            else if (ctx != null) {
                try {
                    return token.setValue(propertyAccessor.setParameters(expr, token.getStart(),
                            token.getEnd(), ctx).get());
                }
                catch (RuntimeException e) {
                    if (!lookAhead()) throw e;
                }
            }
            else {
                if (!lookAhead())
                    new CompileException("unable to resolve token: " + s);
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
                                            forName(new String(expr, token.getStart(), last - token.getStart()))
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

    private boolean lookAhead() {
        Token tk;

        int cursorCurrent = cursor;
        if (!compileMode && (tk = nextToken()) != null) {
            if (!tk.isOperator()) {
                throw new CompileException("expected operator but encountered token: " + tk.getName());
            }
            else if (tk.getOperator() == Operator.ASSIGN || tk.getOperator() == Operator.PROJECTION) {
                cursor = cursorCurrent;
                if (fastExecuteMode) tokenMap.back();
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

    public ExpressionParser setExpression(String expression) {
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
        return this;
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
        if (this.compileMode = compileMode) tokenMap = new TokenMap(null);
        return this;
    }

    public ExpressionParser setPrecompiledExpression(Object expression) {
        this.tokenMap = ((CompiledExpression) expression).getTokenMap();
        this.tokenMap.reset();
        this.fastExecuteMode = true;
        return this;
    }

    private void addTokenToMap(Token tk) {
        ((TokenMap) tokenMap).addTokenNode(tk);
    }

    public Token nextCompiledToken() {
        Token tk;
        /**
         * If we're running in fast-execute mode (aka. running a compiled expression)
         * we retrieve the next token from the compiled stack
         *
         * TODO: Move this to another method ASAP.  This is ridiculous.  (Note from Mike to Mike)
         */

        if ((tk = tokenMap.nextToken()) != null) {
     //       assert debug("nextToken <<" + tk + ">>");

            if (tk.isOperator() && tk.getOperator() == Operator.ASSIGN) {
                //     stk.push(tokenMap.tokensBack(2));
                return tk;
            }
            else if (tk.isCollectionCreation()) {
                /**
                 * We must handle collection creation differently for compiled
                 * execution.  This is not code duplication.  Don't report this.
                 */

                switch (tk.getCollectionCreationType()) {
                    case Token.LISTCREATE: {
       //                 assert debug("<<Creating Inline-List : " + tk + " >>");


                        List<Object> newList = new ArrayList<Object>();
                        newList.add(handleSubNesting(tk.isNestBegin() ? tokenMap.nextToken() : tk));

                        while (tokenMap.hasMoreTokens() &&
                                (tokenMap.peekToken().getFlags() & Token.ENDNEST) == 0) {

                            newList.add(handleSubNesting(tokenMap.nextToken()));
                        }

                        tokenMap.nextToken();
                        
   //                     assert debug("<<Inline List Created: " + newList + ">>");


                        tk.setFlag(true, Token.DO_NOT_REDUCE);
                        return tk.setFinalValue(newList);
                    }

                    case Token.MAPCREATE: {
                        tk = tokenMap.nextToken();

    //                    assert debug("<<Creating Inline-Map: " + tk + ">>");

                        Map<Object, Object> newMap = new HashMap<Object, Object>();

                        newMap.put(handleSubNesting(tk), handleSubNesting(tokenMap.nextToken()));

                        while (tokenMap.hasMoreTokens() && (tokenMap.peekToken().getFlags() & Token.ENDNEST) == 0) {
                            newMap.put(handleSubNesting(tokenMap.nextToken()), handleSubNesting(tokenMap.nextToken()));
                        }

                        tokenMap.nextToken();

                        tk.setFlag(true, Token.DO_NOT_REDUCE);
                        tk.setFinalValue(newMap);
                    }
                    break;

                    case Token.ARRAYCREATE: {
                        List<Object> newList = new ArrayList<Object>();

                        while (tokenMap.hasMoreTokens() &&
                                (tokenMap.peekToken().getFlags() & Token.ENDNEST) == 0) {
                             newList.add(handleSubNesting(tokenMap.nextToken()));
                        }

                        tokenMap.nextToken();

                        tk.setFlag(true, Token.DO_NOT_REDUCE);
                        return tk.setFinalValue(newList.toArray());
                    }
                }

                if (tokenMap.hasMoreTokens() && (tokenMap.peekToken().getFlags() & Token.PUSH) != 0) {
                    stk.push(tk.getValue());
                    return (tk = tokenMap.nextToken()).setFinalValue(PropertyAccessor.get(tk.getName(), stk.pop()));
                }
            }
            else if ((tk.getFlags() & Token.IDENTIFIER) != 0) {
  //              assert debug("<<Reducing Identifier: " + tk + ">>");
                reduceToken(tk);
  //              assert debug("<<Reduction Result: " + tk + ">>");
            }
            else if ((tk.getFlags() & Token.THISREF) != 0) {
                tk.setFinalValue(ctx);
            }

            fields |= (tk.getFlags() & Token.SUBEVAL);

            if ((tk.getFlags() & Token.PUSH) != 0) {
  //              assert debug("<<PUSH: " + tk + ">>");
                stk.push(tk.getValue());
            }
        }

 //       assert debug("<<Returning Token From CompiledTokens: " + tk + ">>");
        return tk;
    }

    private Object handleSubNesting(Token token) {
        if ((token.getFlags() & Token.NEST) != 0) {
            return nextToken().getValue();
        }
        else {
            return reduceToken(token).getValue();
        }
    }
}

