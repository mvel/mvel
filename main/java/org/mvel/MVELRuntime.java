package org.mvel;

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
    private static ThreadLocal<Set<Integer>> breakpoints;

    /**
     * Main interpreter loop.
     *
     * @param ctx             -
     * @param variableFactory -
     * @return -
     */
    public static Object execute(boolean debugger, FastASTIterator tokens, Object ctx, VariableResolverFactory variableFactory) {
        Stack stk = new ExecutionStack();
        Object v1, v2;

        ASTNode tk = null;
        Integer operator;

        try {
            while ((tk = tokens.nextToken()) != null) {
                if (tk.fields == -1) {
                    if (breakpoints != null && breakpoints.get() != null) {
                        debugger = true;
                    }

                    if (debugger && breakpoints != null
                            && breakpoints.get().contains(((LineLabel) tk).getLineNumber())) {
                        System.out.println("[Encountered Breakpoint!]: " + ((LineLabel) tk).getLineNumber());

                        try {
                            Thread.sleep(10);
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

                switch (operator = tk.getOperator()) {
                    case AND:
                        if (stk.peek() instanceof Boolean && !((Boolean) stk.peek())) {
                            while (tokens.hasMoreTokens() && !tokens.nextToken().isOperator(Operator.END_OF_STMT)) ;
                            if (!tokens.hasMoreTokens()) {
                                return stk.pop();
                            }
                            else {
                                stk.clear();
                                continue;
                            }
                        }
                        else {
                            stk.discard();
                            continue;
                        }
                    case OR:
                        if (stk.peek() instanceof Boolean && ((Boolean) stk.peek())) {
                            while (tokens.hasMoreTokens() && !tokens.nextToken().isOperator(Operator.END_OF_STMT)) ;
                            if (!tokens.hasMoreTokens()) {
                                return stk.pop();
                            }
                            else {
                                stk.clear();
                                continue;
                            }
                        }
                        else {
                            stk.discard();
                            continue;
                        }
                    case TERNARY:
                        if (!(Boolean) stk.pop()) {
                            while (tokens.hasMoreTokens() && !tokens.nextToken().isOperator(Operator.TERNARY_ELSE)) ;
                        }
                        stk.clear();
                        continue;

                    case TERNARY_ELSE:
                        return stk.pop();

                    case END_OF_STMT:
                        /**
                         * If the program doesn't end here then we wipe anything off the stack that remains.
                         * Althought it may seem like intuitive stack optimizations could be leveraged by
                         * leaving hanging values on the stack,  trust me it's not a good idea.
                         */
                        if (tokens.hasMoreTokens()) {
                            stk.clear();
                        }

                        continue;
                }

                stk.push(tokens.nextToken().getReducedValueAccelerated(ctx, ctx, variableFactory), operator);

                try {
                    while (stk.size() > 1) {
                        operator = (Integer) stk.pop();
                        v1 = stk.pop();
                        v2 = stk.pop();

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

    private static int asInt(final Object o) {
        return (Integer) o;
    }

    public static void registerBreakpoint(int line) {
        if (breakpoints == null) {
            breakpoints = new ThreadLocal<Set<Integer>>();
            breakpoints.set(new HashSet<Integer>());
        }
        breakpoints.get().add(line);
    }

    public static void removeBreakpoint(int line) {
        if (breakpoints != null && breakpoints.get() != null) {
            breakpoints.get().remove(line);
        }
    }

    public static void clearAllBreakpoints() {
        if (breakpoints != null && breakpoints.get() != null) {
            breakpoints.get().clear();
        }
    }
}
