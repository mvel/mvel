package org.mvel;

import static org.mvel.DataConversion.canConvert;
import static org.mvel.Operator.*;
import static org.mvel.Soundex.soundex;
import org.mvel.ast.ASTNode;
import org.mvel.ast.LineLabel;
import org.mvel.compiler.CompiledExpression;
import org.mvel.debug.Debugger;
import org.mvel.debug.DebuggerContext;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.integration.impl.ClassImportResolverFactory;
import org.mvel.util.ASTLinkedList;
import org.mvel.util.ExecutionStack;
import static org.mvel.util.ParseTools.containsCheck;
import static org.mvel.util.PropertyTools.isEmpty;
import static org.mvel.util.PropertyTools.similarity;
import org.mvel.util.Stack;

import static java.lang.String.valueOf;
import static java.lang.Thread.currentThread;

/**
 * This class contains the runtime for running compiled MVEL expressions.
 */
@SuppressWarnings({"CaughtExceptionImmediatelyRethrown"})
public class MVELRuntime {
//    private static ThreadLocal<Map<String, Set<Integer>>> threadBreakpoints;
//    private static ThreadLocal<Debugger> threadDebugger;

    private static ThreadLocal<DebuggerContext> debuggerContext;

    /**
     * Main interpreter.
     *
     * @param debugger        -
     * @param expression      -
     * @param ctx             -
     * @param variableFactory -
     * @return -
     * @see org.mvel.MVEL
     */
    public static Object execute(boolean debugger, final CompiledExpression expression, final Object ctx, VariableResolverFactory variableFactory) {
        final ASTLinkedList node = new ASTLinkedList(expression.getInstructions().firstNode());

        if (expression.isImportInjectionRequired()) {
            variableFactory = new ClassImportResolverFactory(expression.getParserContext().getParserConfiguration(), variableFactory);
        }

        Stack stk = new ExecutionStack();
        Object v1, v2;

        ASTNode tk = null;
        Integer operator;

        try {
            while ((tk = node.nextNode()) != null) {
                if (tk.fields == -1) {
                    /**
                     * This may seem silly and redundant, however, when an MVEL script recurses into a block
                     * or substatement, a new runtime loop is entered.   Since the debugger state is not
                     * passed through the AST, it is not possible to forward the state directly.  So when we
                     * encounter a debugging symbol, we check the thread local to see if there is are registered
                     * breakpoints.  If we find them, we assume that we are debugging.
                     *
                     * The consequence of this of course, is that it's not ideal to compile expressions with
                     * debugging symbols which you plan to use in a production enviroment.
                     */
                    if (!debugger && hasDebuggerContext()) {
                        debugger = true;
                    }

                    /**
                     * If we're not debugging, we'll just skip over this.
                     */
                    if (debugger) {
                        LineLabel label = (LineLabel) tk;
                        DebuggerContext context = debuggerContext.get();

                        try {
                            context.checkBreak(label, variableFactory, expression);
                        }
                        catch (NullPointerException e) {
                            // do nothing for now.  this isn't as calus as it seems.   
                        }
                    }
                    continue;
                }

                if (stk.isEmpty()) {
                    stk.push(tk.getReducedValueAccelerated(ctx, ctx, variableFactory));
                }

//                if (!tk.isOperator()) {
//                    continue;
//                }

                switch (operator = tk.getOperator()) {
                    case NOOP:
                        continue;

                    case TERNARY:
                        if (!(Boolean) stk.pop()) {
                            //noinspection StatementWithEmptyBody
                            while (node.hasMoreNodes() && !node.nextNode().isOperator(Operator.TERNARY_ELSE)) ;
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
                        if (node.hasMoreNodes()) {
                            stk.clear();
                        }

                        continue;

                }

                stk.push(node.nextNode().getReducedValueAccelerated(ctx, ctx, variableFactory), operator);

                try {
                    while (stk.size() > 1) {
                        operator = (Integer) stk.pop();
                        v1 = stk.pop();
                        v2 = stk.pop();

                        switch (operator) {
                            case CHOR:
                                if (!isEmpty(v2) || !isEmpty(v1)) {
                                    stk.clear();
                                    stk.push(!isEmpty(v2) ? v2 : v1);
                                }
                                else stk.push(null);
                                break;

                            case INSTANCEOF:
                                if (v1 instanceof Class)
                                    stk.push(((Class) v1).isInstance(v2));
                                else
                                    stk.push(currentThread().getContextClassLoader().loadClass(valueOf(v1)).isInstance(v2));

                                break;

                            case CONVERTABLE_TO:
                                if (v1 instanceof Class)
                                    stk.push(canConvert(v2.getClass(), (Class) v1));
                                else
                                    stk.push(canConvert(v2.getClass(), currentThread().getContextClassLoader().loadClass(valueOf(v1))));
                                break;

                            case CONTAINS:
                                stk.push(containsCheck(v2, v1));
                                break;

                            case BW_AND:
                                stk.push((Integer) v2 & (Integer) v1);
                                break;

                            case BW_OR:
                                stk.push((Integer) v2 | (Integer) v1);
                                break;

                            case BW_XOR:
                                stk.push((Integer) v2 ^ (Integer) v1);
                                break;

                            case BW_SHIFT_LEFT:
                                stk.push((Integer) v2 << (Integer) v1);
                                break;

                            case BW_USHIFT_LEFT:
                                int iv2 = (Integer) v2;
                                if (iv2 < 0) iv2 *= -1;
                                stk.push(iv2 << (Integer) v1);
                                break;

                            case BW_SHIFT_RIGHT:
                                stk.push((Integer) v2 >> (Integer) v1);
                                break;

                            case BW_USHIFT_RIGHT:
                                stk.push((Integer) v2 >>> (Integer) v1);
                                break;

                            case SOUNDEX:
                                stk.push(soundex(valueOf(v1)).equals(soundex(valueOf(v2))));
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

            return stk.pop();
        }
        catch (NullPointerException e) {
            if (tk != null && tk.isOperator() && !node.hasMoreNodes()) {
                throw new CompileException("incomplete statement: "
                        + tk.getName() + " (possible use of reserved keyword as identifier: " + tk.getName() + ")");
            }
            else {
                throw e;
            }
        }
    }

    /**
     * Register a debugger breakpoint.
     *
     * @param source - the source file the breakpoint is registered in
     * @param line   - the line number of the breakpoint
     */
    public static void registerBreakpoint(String source, int line) {
        ensureDebuggerContext();
        debuggerContext.get().registerBreakpoint(source, line);
    }

    /**
     * Remove a specific breakpoint.
     *
     * @param source - the source file the breakpoint is registered in
     * @param line   - the line number of the breakpoint to be removed
     */
    public static void removeBreakpoint(String source, int line) {
        if (hasDebuggerContext()) {
            debuggerContext.get().removeBreakpoint(source, line);
        }
    }

    private static boolean hasDebuggerContext() {
        return debuggerContext != null && debuggerContext.get() != null;
    }

    private static void ensureDebuggerContext() {
        if (debuggerContext == null) debuggerContext = new ThreadLocal<DebuggerContext>();
        if (debuggerContext.get() == null) debuggerContext.set(new DebuggerContext());
    }

    /**
     * Reset all the currently registered breakpoints.
     */
    public static void clearAllBreakpoints() {
        if (hasDebuggerContext()) {
            debuggerContext.get().clearAllBreakpoints();
        }
    }

    public static boolean hasBreakpoints() {
        return hasDebuggerContext() && debuggerContext.get().hasBreakpoints();
    }

    /**
     * Sets the Debugger instance to handle breakpoints.   A debugger may only be registered once per thread.
     * Calling this method more than once will result in the second and subsequent calls to simply fail silently.
     * To re-register the Debugger, you must call {@link #resetDebugger}
     *
     * @param debugger - debugger instance
     */
    public static void setThreadDebugger(Debugger debugger) {
        ensureDebuggerContext();
        debuggerContext.get().setDebugger(debugger);
    }

    /**
     * Reset all information registered in the debugger, including the actual attached Debugger and registered
     * breakpoints.
     */
    public static void resetDebugger() {
        debuggerContext = null;
    }
}
