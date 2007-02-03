package org.mvel;

import static org.mvel.DataConversion.canConvert;
import static org.mvel.Operator.*;
import static org.mvel.PropertyAccessor.get;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.util.ExecutionStack;
import static org.mvel.util.ParseTools.containsCheck;
import static org.mvel.util.PropertyTools.*;
import org.mvel.util.Stack;
import org.mvel.util.StringAppender;
import org.mvel.util.ParseTools;

import static java.lang.Class.forName;
import static java.lang.String.valueOf;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import static java.util.regex.Pattern.compile;

public class AcceleratedParser extends AbstractParser {
    private final TokenIterator tokens;
    private final Stack stk = new ExecutionStack();


    public AcceleratedParser(FastTokenIterator tokens) {
        this.tokens = new FastTokenIterator(tokens);
    }

    public Object execute(Object ctx, VariableResolverFactory variableFactory) {
        Token tk;
        Integer operator;

        while ((tk = tokens.nextToken()) != null) {
            //     assert debug("\nSTART_FRAME <<" + tk + ">> STK_SIZE=" + stk.size() + "; STK_PEEK=" + stk.peek() + "; TOKEN#=" + tokens.index());
            if (stk.isEmpty()) {
                stk.push(tk.getReducedValueAccelerated(ctx, ctx, variableFactory));
            }

            if (!tk.isOperator()) {
                continue;
            }

            switch (reduceBinary(operator = tk.getOperator())) {
                case-1:
                    // assert debug("FRAME_KILL_PROC");
                    return stk.pop();
                case 0:
                    // assert debug("FRAME_CONTINUE");
                    break;
                case 1:
                    // assert debug("FRAME_NEXT");
                    continue;
            }

            if (!tokens.hasMoreTokens()) return stk.pop();

            stk.push(tokens.nextToken().getReducedValueAccelerated(ctx, ctx, variableFactory), operator);

            reduceTrinary();
        }

        return stk.peek();
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
        // assert debug("BINARY_OP " + o + " PEEK=<<" + stk.peek() + ">>");
        switch (o) {
            case AND:
                if (stk.peek() instanceof Boolean && !((Boolean) stk.peek())) {
                    // assert debug("STMT_UNWIND");
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
                    // assert debug("STMT_UNWIND");
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
                if (!(Boolean) stk.pop()) {
                    skipToOperator(Operator.TERNARY_ELSE);
                }
                stk.clear();
                return 1;


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
                operator = (Integer) stk.pop();
                v1 = processToken(stk.pop());
                v2 = processToken(stk.pop());

                // assert debug("DO_TRINARY <<OPCODE_" + operator + ">> register1=" + v1 + "; register2=" + v2);
                switch (operator) {
                    case ADD:
//                        if (v1 instanceof BigDecimal && v2 instanceof BigDecimal) {
//                            stk.push(((BigDecimal) v1).add((BigDecimal) v2));
//                        }
//                        else {
//                            stk.push(valueOf(v2) + valueOf(v1));
//                        }
//                        break;

                    case SUB:
//                        stk.push(((BigDecimal) v2).subtract(((BigDecimal) v1)));
//                        break;

                    case DIV:
//                        stk.push(((BigDecimal) v2).divide(((BigDecimal) v1), 20, roundingMode));
//                        break;

                    case MULT:
//                        stk.push(((BigDecimal) v2).multiply((BigDecimal) v1));
//                        break;

                    case MOD:
//                        stk.push(((BigDecimal) v2).remainder((BigDecimal) v1));
//                        break;

                    case EQUAL:
//                        if (v1 instanceof BigDecimal && v2 instanceof BigDecimal) {
//                            stk.push(((BigDecimal) v2).compareTo((BigDecimal) v1) == 0);
//                        }
//                        else if (v1 != null)
//                            stk.push(v1.equals(v2));
//                        else if (v2 != null)
//                            stk.push(v2.equals(v1));
//                        else
//                            stk.push(v1 == v2);
//                        break;

                    case NEQUAL:
//                        if (v1 instanceof BigDecimal && v2 instanceof BigDecimal) {
//                            stk.push(((BigDecimal) v2).compareTo((BigDecimal) v1) != 0);
//                        }
//                        else if (v1 != null)
//                            stk.push(!v1.equals(v2));
//                        else if (v2 != null)
//                            stk.push(!v2.equals(v1));
//                        else
//                            stk.push(v1 != v2);
//                        break;
                    case GTHAN:
//                        stk.push(((BigDecimal) v2).compareTo((BigDecimal) v1) == 1);
//                        break;
                    case LTHAN:
//                        stk.push(((BigDecimal) v2).compareTo((BigDecimal) v1) == -1);
//                        break;
                    case GETHAN:
//                        stk.push(((BigDecimal) v2).compareTo((BigDecimal) v1) >= 0);
//                        break;
                    case LETHAN:
                        //                     stk.push(((BigDecimal) v2).compareTo((BigDecimal) v1) <= 0);

                        stk.push(ParseTools.doOperations(v2, operator, v1));
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
        return (Integer) o;
    }

    private Object processToken(Object operand) {
        if (operand instanceof BigDecimal) {
            return operand;
        }
//        else if (isNumber(operand)) {
//            return new BigDecimal(valueOf(operand));
//        }
        else {
            return operand;
        }
    }

    private boolean hasNoMore() {
        return !tokens.hasMoreTokens();
    }

    private boolean unwindStatement() {
        //noinspection StatementWithEmptyBody
        while (tokens.hasMoreTokens() && !tokens.nextToken().isOperator(Operator.END_OF_STMT)) ;
        return !tokens.hasMoreTokens();
    }

    @SuppressWarnings({"StatementWithEmptyBody"})
    private void skipToOperator(int operator) {
        while (tokens.hasMoreTokens() && !tokens.nextToken().isOperator(operator)) ;
    }

}
