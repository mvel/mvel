/**
 * MVEL 2.0
 * Copyright (C) 2007 The Codehaus
 * Mike Brock, Dhanji Prasanna, John Graham, Mark Proctor
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
 */

package org.mvel2;

import static org.mvel2.Operator.*;
import org.mvel2.ast.ASTNode;
import org.mvel2.ast.Substatement;
import org.mvel2.compiler.AbstractParser;
import org.mvel2.compiler.EndWithValue;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;
import org.mvel2.util.ExecutionStack;
import static org.mvel2.util.ParseTools.findClassImportResolverFactory;

import java.util.Map;


/**
 * The MVEL interpreted runtime, used for fast parse and execution of scripts.
 */
@SuppressWarnings({"CaughtExceptionImmediatelyRethrown"})
public class MVELInterpretedRuntime extends AbstractParser {
    public Object parse() {
        try {
            stk = new ExecutionStack();
            dStack = new ExecutionStack();
            cursor = 0;
            return parseAndExecuteInterpreted();
        }
        catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
            throw new ParseException("unexpected end of statement", expr, length);
        }
        catch (NullPointerException e) {
            e.printStackTrace();

            if (cursor >= length) {
                throw new ParseException("unexpected end of statement", expr, length);
            }
            else {
                throw e;
            }
        }
        catch (EndWithValue end) {
            return end.getValue();
        }
        finally {
            if (parserContext != null) contextControl(REMOVE, null, null);
        }
    }

    private Object holdOverRegister;

    /**
     * Main interpreter loop.
     *
     * @return value
     */
    private Object parseAndExecuteInterpreted() {
        ASTNode tk = null;
        int operator;
        lastWasIdentifier = false;

        try {
            while ((tk = nextToken()) != null) {
                holdOverRegister = null;

                if (lastWasIdentifier && lastNode.isDiscard()) {
                    stk.discard();
                }

                /**
                 * If we are at the beginning of a statement, then we immediately push the first token
                 * onto the stack.
                 */
                if (stk.isEmpty()) {
                    stk.push(tk.getReducedValue(ctx, ctx, variableFactory));

                    /**
                     * If this is a substatement, we need to move the result into the d-stack to preserve
                     * proper execution order.
                     */
                    if (tk instanceof Substatement && (tk = nextToken()) != null) {
                        if (isArithmeticOperator(operator = tk.getOperator())) {
                            stk.push(nextToken().getReducedValue(ctx, ctx, variableFactory), operator);

                            if (procBooleanOperator(arithmeticFunctionReduction(operator)) == -1)
                                return stk.peek();
                            else
                                continue;
                        }
                    }
                    else {
                        continue;
                    }
                }

                switch (procBooleanOperator(operator = tk.getOperator())) {
                    case OP_TERMINATE:
                        return stk.peek();
                    case OP_RESET_FRAME:
                        continue;
                    case OP_OVERFLOW:
                        if (!tk.isOperator()) {
                            if (!(stk.peek() instanceof Class)) {
                                throw new CompileException("unexpected token: " + tk.getName());
                            }
                            variableFactory.createVariable(tk.getName(), null, (Class) stk.peek());
                        }
                        continue;
                }

                stk.push(nextToken().getReducedValue(ctx, ctx, variableFactory), operator);

                switch ((operator = arithmeticFunctionReduction(operator))) {
                    case OP_TERMINATE:
                        return stk.peek();
                    case OP_RESET_FRAME:
                        continue;
                }

                if (procBooleanOperator(operator) == -1) return stk.peek();
            }

            if (holdOverRegister != null) {
                return holdOverRegister;
            }
        }
        catch (CompileException e) {
            e.setExpr(expr);
            e.setLineNumber(line + e.getLineNumber());
            e.setCursor(cursor);
            throw e;
        }
        catch (NullPointerException e) {
            if (tk != null && tk.isOperator()) {
                CompileException ce = new CompileException("incomplete statement: "
                        + tk.getName() + " (possible use of reserved keyword as identifier: " + tk.getName() + ")", e);

                ce.setExpr(expr);
                ce.setLineNumber(line);
                ce.setCursor(cursor);
                throw ce;
            }
            else {
                throw e;
            }
        }
        return stk.peek();
    }

    private int procBooleanOperator(int operator) {
        switch (operator) {
            case NOOP:
                return -2;

            case AND:
                reduceRight();

                if (!stk.peekBoolean()) {
                    if (unwindStatement(operator)) {
                        return -1;
                    }
                    else {
                        stk.clear();
                        return 0;
                    }
                }
                else {
                    stk.discard();
                    return 0;
                }

            case OR:
                reduceRight();

                if (stk.peekBoolean()) {
                    if (unwindStatement(operator)) {
                        return -1;
                    }
                    else {
                        stk.clear();
                        return 0;
                    }
                }
                else {
                    stk.discard();
                    return 0;
                }

            case TERNARY:
                if (!stk.popBoolean()) {
                    stk.clear();

                    ASTNode tk;
                    while ((tk = nextToken()) != null && !tk.isOperator(Operator.TERNARY_ELSE)) {
                        //nothing
                    }
                }


                return 0;

            case TERNARY_ELSE:
                captureToEOS();
                return 0;

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

                if (hasMore()) {
                    holdOverRegister = stk.pop();
                    stk.clear();
                }

                return 0;
        }

        return 1;
    }

    /**
     * This method peforms the equivilent of an XSWAP operation to flip the operator
     * over to the top of the stack, and loads the stored values on the d-stack onto
     * the main program stack.
     */
    private void reduceRight() {
        if (dStack.isEmpty()) return;

        Object o = stk.pop();
        stk.push(dStack.pop());
        stk.push(o);
        stk.push(dStack.pop());

        reduce();
    }

    private boolean hasMore() {
        return cursor <= length;
    }

    /**
     * This method is called to unwind the current statement without any reduction or further parsing.
     *
     * @param operator -
     * @return -
     */
    private boolean unwindStatement(int operator) {
        ASTNode tk;

        switch (operator) {
            case AND:
                while ((tk = nextToken()) != null && !tk.isOperator(Operator.END_OF_STMT) && !tk.isOperator(Operator.OR)) {
                    //nothing
                }
                break;
            default:
                while ((tk = nextToken()) != null && !tk.isOperator(Operator.END_OF_STMT)) {
                    //nothing
                }
        }
        return tk == null;
    }

    public MVELInterpretedRuntime setExpressionArray(char[] expressionArray) {
        this.length = (this.expr = expressionArray).length;
        return this;
    }

    MVELInterpretedRuntime(char[] expression, Object ctx, Map<String, Object> variables) {
        this.expr = expression;
        this.length = expr.length;
        this.ctx = ctx;
        this.variableFactory = new MapVariableResolverFactory(variables);
    }

    MVELInterpretedRuntime(char[] expression, Object ctx) {
        this.expr = expression;
        this.length = expr.length;
        this.ctx = ctx;
        this.variableFactory = MVELRuntime.IMMUTABLE_DEFAULT_FACTORY;
    }


    MVELInterpretedRuntime(String expression) {
        setExpression(expression);
        this.variableFactory = MVELRuntime.IMMUTABLE_DEFAULT_FACTORY;
    }

    MVELInterpretedRuntime(char[] expression) {
        this.length = (this.expr = expression).length;
    }

    public MVELInterpretedRuntime(char[] expr, Object ctx, VariableResolverFactory resolverFactory) {
        this.length = (this.expr = expr).length;
        this.ctx = ctx;
        this.variableFactory = resolverFactory;
    }

    MVELInterpretedRuntime(char[] expr, Object ctx, VariableResolverFactory resolverFactory, boolean returnBigDecimal) {
        this.length = (this.expr = expr).length;
        this.ctx = ctx;
        this.variableFactory = resolverFactory;
    }

    MVELInterpretedRuntime(Object ctx, Map<String, Object> variables) {
        this.ctx = ctx;
        this.variableFactory = new MapVariableResolverFactory(variables);
    }

    public MVELInterpretedRuntime(String expression, Object ctx, VariableResolverFactory resolverFactory) {
        setExpression(expression);
        this.ctx = ctx;
        this.variableFactory = resolverFactory;
    }

    MVELInterpretedRuntime(String expression, Object ctx, VariableResolverFactory resolverFactory, boolean returnBigDecimal) {
        setExpression(expression);
        this.ctx = ctx;
        this.variableFactory = resolverFactory;
    }

    MVELInterpretedRuntime(String expression, VariableResolverFactory resolverFactory) {
        setExpression(expression);
        this.variableFactory = resolverFactory;
    }

    MVELInterpretedRuntime(String expression, Object ctx) {
        setExpression(expression);
        this.ctx = ctx;
    }

    protected boolean hasImport(String name) {
        if (pCtx == null) pCtx = getParserContext();

        if (pCtx.hasImport(name)) {
            return true;
        }
        else {
            VariableResolverFactory vrf = findClassImportResolverFactory(variableFactory);
            return vrf != null && vrf.isResolveable(name);
        }
    }

    protected Class getImport(String name) {
        if (pCtx == null) pCtx = getParserContext();
        if (pCtx.hasImport(name)) return pCtx.getImport(name);

        return (Class) findClassImportResolverFactory(variableFactory).getVariableResolver(name).getValue();
    }
}

