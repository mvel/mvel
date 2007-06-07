package org.mvel;

import static org.mvel.AbstractParser.*;
import static org.mvel.DataConversion.canConvert;
import static org.mvel.Operator.*;
import org.mvel.ast.LineLabel;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.util.ExecutionStack;
import static org.mvel.util.ParseTools.containsCheck;
import static org.mvel.util.ParseTools.doOperations;
import static org.mvel.util.PropertyTools.isEmpty;
import static org.mvel.util.PropertyTools.similarity;
import org.mvel.util.Stack;
import org.mvel.util.StringAppender;

import static java.lang.Class.forName;
import static java.lang.String.valueOf;
import java.util.HashSet;
import java.util.Set;
import static java.util.regex.Pattern.compile;

public class MVELRuntime {
    private final ASTIterator tokens;
    private final Stack stk = new ExecutionStack();
    private Set<Integer> breakpoints;
    private boolean debugger = false;

    public MVELRuntime(CompiledExpression expression) {
        this.tokens = new FastASTIterator(expression.getTokens());
    }

    public MVELRuntime(FastASTIterator tokens) {
        this.tokens = new FastASTIterator(tokens);
    }

    /**
     * Main interpreter loop.
     *
     * @param ctx             -
     * @param variableFactory -
     * @return -
     */
    public Object execute(Object ctx, VariableResolverFactory variableFactory) {
        ASTNode tk = null;
        Integer operator;

        try {
            while ((tk = tokens.nextToken()) != null) {
                if (tk.fields == -1) {
                    if (debugger && breakpoints != null
                            && breakpoints.contains(((LineLabel) tk).getLineNumber())) {
                        System.out.println("[Encountered Breakpoint!]");

                        try {
                            Thread.sleep(5000);
                        }
                        catch (InterruptedException e) {
                        }
                    }

                    continue;
                }

                if (stk.isEmpty()) {
                    stk.push(tk.getReducedValueAccelerated(ctx, ctx, variableFactory));
                }

                if (!tk.isOperator()) {
                    continue;
                }

                switch (reduceBinary(operator = tk.getOperator())) {
                    case FRAME_END:
                        return stk.pop();
                    case FRAME_CONTINUE:
                        break;
                    case FRAME_NEXT:
                        continue;
                }

                stk.push(tokens.nextToken().getReducedValueAccelerated(ctx, ctx, variableFactory), operator);

                reduceTrinary();
            }

            return stk.peek();

        }
        catch (NullPointerException e) {
            if (tk != null && tk.isOperator() && !tokens.hasMoreTokens()) {
                throw new CompileException("incomplete statement: "
                        + tk.getName() + " (possible use of reserved keyword as identifier: " + tk.getName() + ")");
            }
            else {
                throw e;
            }
        }
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
                        return FRAME_END;
                    }
                    else {
                        stk.clear();
                        return FRAME_NEXT;
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
                if (!(Boolean) stk.pop()) {
                    skipToOperator(Operator.TERNARY_ELSE);
                }
                stk.clear();
                return FRAME_NEXT;


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

                if (!hasNoMore()) {
                    stk.clear();
                }

                return FRAME_NEXT;

        }
        return FRAME_CONTINUE;
    }

    /**
     * This method is called when we reach the point where we must subEval a trinary operation in the expression.
     * (ie. val1 op val2).  This is not the same as a binary operation, although binary operations would appear
     * to have 3 structures as well.  A binary structure (or also a junction in the expression) compares the
     * current state against 2 downrange structures (usually an op and a val).
     */
    private void reduceTrinary() {
        Object v1, v2;
        Integer operator;
        try {
            while (stk.size() > 1) {
                operator = (Integer) stk.pop();
                v1 = stk.pop();
                v2 = stk.pop();

                // assert debug("DO_TRINARY <<OPCODE_" + operator + ">> register1=" + v1 + "; register2=" + v2);
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
            throw new CompileException("syntax error or incomptable types", e);

        }
        catch (Exception e) {
            throw new CompileException("failed to subEval expression", e);
        }
    }

    private static int asInt(final Object o) {
        return (Integer) o;
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

    public void registerBreakpoint(int line) {
        if (breakpoints == null) breakpoints = new HashSet<Integer>();
        breakpoints.add(line);
    }

    public void removeBreakpoint(int line) {
        if (breakpoints != null) {
            breakpoints.remove(line);
        }
    }

    public void clearAllBreakpoints() {
        if (breakpoints != null) {
            breakpoints.clear();
        }
    }

    public boolean isDebugger() {
        return debugger;
    }

    public void setDebugger(boolean debugger) {
        this.debugger = debugger;
    }
}
