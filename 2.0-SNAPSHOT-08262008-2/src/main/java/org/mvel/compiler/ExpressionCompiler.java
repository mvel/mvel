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
package org.mvel.compiler;

import org.mvel.CompileException;
import org.mvel.ErrorDetail;
import org.mvel.Operator;
import static org.mvel.Operator.PTABLE;
import org.mvel.ParserContext;
import org.mvel.debug.DebugTools;
import org.mvel.ast.ASTNode;
import static org.mvel.ast.ASTNode.COMPILE_IMMEDIATE;
import org.mvel.ast.LiteralNode;
import org.mvel.ast.Substatement;
import org.mvel.util.ASTLinkedList;
import static org.mvel.util.CompilerTools.optimizeAST;
import org.mvel.util.ExecutionStack;
import org.mvel.util.StringAppender;

/**
 * This is the main MVEL compiler. 
 */
public class ExpressionCompiler extends AbstractParser {
    private Class returnType;

    private boolean verifying = true;
    private boolean secondPassOptimization = false;

    public CompiledExpression compile() {
        return compile(contextControl(GET_OR_CREATE, null, null));
    }

    public CompiledExpression compile(ParserContext ctx) {
        if (debugSymbols) {
            ctx.setDebugSymbols(debugSymbols);
        }
        else if (ctx.isDebugSymbols()) {
            debugSymbols = true;
        }

        try {
            newContext(ctx);
            return _compile();
        }
        finally {
            removeContext();
            if (pCtx.isFatalError()) {
                contextControl(REMOVE, null, null);
                //noinspection ThrowFromFinallyBlock

                StringAppender err = new StringAppender();

                for (ErrorDetail e : pCtx.getErrorList()) {
                    err.append("\n - ").append("(").append(e.getRow()).append(",").append(e.getCol()).append(")")
                            .append(" ").append(e.getMessage());
                }

                throw new CompileException("Failed to compile: " + pCtx.getErrorList().size() + " compilation error(s): " + err.toString(), pCtx.getErrorList());
            }
        }
    }

    /**
     * Initiate an in-context compile.  This method should really only be called by the internal API.
     *
     * @return compiled expression object
     */
    public CompiledExpression _compile() {
        ASTNode tk;
        ASTNode tkOp;
        ASTNode tkOp2;
        ASTNode tkLA;
        ASTNode tkLA2;

        int op, lastOp = -1;

        ASTLinkedList astBuild = new ASTLinkedList();
        stk = new ExecutionStack();
        dStack = new ExecutionStack();

        boolean firstLA;

        if (pCtx == null) pCtx = getParserContext();

        debugSymbols = pCtx.isDebugSymbols();

        try {
            if (verifying) {
                pCtx.initializeTables();
            }

            fields |= COMPILE_IMMEDIATE;

            while ((tk = nextToken()) != null) {
                /**
                 * If this is a debug symbol, just add it and continue.
                 */
                if (tk.fields == -1) {
                    astBuild.addTokenNode(tk);
                    continue;
                }

                returnType = tk.getEgressType();

                if (tk instanceof Substatement) {
                    ExpressionCompiler subCompiler = new ExpressionCompiler(tk.getNameAsArray(), pCtx);
                    tk.setAccessor(subCompiler._compile());

                    returnType = subCompiler.getReturnType();
                }

                /**
                 * This kludge of code is to handle compile-time literal reduction.  We need to avoid
                 * reducing for certain literals like, 'this', ternary and ternary else.
                 */
                if (tk.isLiteral()) {
                    literalOnly = true;

                    if ((tkOp = nextTokenSkipSymbols()) != null && tkOp.isOperator()
                            && !tkOp.isOperator(Operator.TERNARY) && !tkOp.isOperator(Operator.TERNARY_ELSE)) {

                        /**
                         * If the next token is ALSO a literal, then we have a candidate for a compile-time literal
                         * reduction.
                         */
                        if ((tkLA = nextTokenSkipSymbols()) != null && tkLA.isLiteral()
                                && ((lastOp == -1 || PTABLE[lastOp] < PTABLE[tkOp.getOperator()]))) {
                            
                            stk.push(tk.getLiteralValue(), tkLA.getLiteralValue(), op = tkOp.getOperator());

                            /**
                             * Reduce the token now.
                             */
                            if (isArithmeticOperator(op)) {
                                arithmeticFunctionReduction(op);
                            }
                            else {
                                reduce();
                            }

                            firstLA = true;

                            /**
                             * Now we need to check to see if this is a continuing reduction.
                             */
                            while ((tkOp2 = nextTokenSkipSymbols()) != null) {
                                if (isBooleanOperator(tkOp2.getOperator())) {
                                    astBuild.addTokenNode(new LiteralNode(stk.pop()), verify(pCtx, tkOp2));
                                    break;
                                }
                                else if ((tkLA2 = nextTokenSkipSymbols()) != null && tkLA2.isLiteral()) {
                                    stk.push(tkLA2.getLiteralValue(), op = tkOp2.getOperator());

                                    if (isArithmeticOperator(op)) {
                                        arithmeticFunctionReduction(op);
                                    }
                                    else {
                                        reduce();
                                    }

                                    firstLA = false;
                                    literalOnly = false;
                                }
                                else {
                                    if (firstLA) {
                                        /**
                                         * There are more tokens, but we can't reduce anymore.  So
                                         * we create a reduced token for what we've got.
                                         */
                                        astBuild.addTokenNode(new LiteralNode(stk.pop()));
                                    }
                                    else {
                                        /**
                                         * We have reduced additional tokens, but we can't reduce
                                         * anymore.
                                         */
                                        astBuild.addTokenNode(new LiteralNode(stk.pop()), tkOp2);

                                        if (tkLA2 != null) astBuild.addTokenNode(tkLA2);
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
                                astBuild.addTokenNode(new LiteralNode(stk.pop()));

                            continue;
                        }
                        else {
                            astBuild.addTokenNode(verify(pCtx, tk), verify(pCtx, tkOp));
                            if (tkLA != null) astBuild.addTokenNode(verify(pCtx, tkLA));
                            continue;
                        }
                    }
                    else {
                        literalOnly = false;
                        astBuild.addTokenNode(verify(pCtx, tk));
                        if (tkOp != null) astBuild.addTokenNode(verify(pCtx, tkOp));

                        continue;
                    }
                }
                else {
                    if (tk.isOperator()) {
                        lastOp = tk.getOperator();
                    }

                    literalOnly = false;
                }

                astBuild.addTokenNode(verify(pCtx, tk));
            }

            astBuild.finish();

            if (verifying) {
                pCtx.processTables();
            }

            if (!stk.isEmpty()) throw new CompileException("COMPILE ERROR: non-empty stack after compile.");


            return new CompiledExpression(optimizeAST(astBuild, secondPassOptimization, pCtx), getCurrentSourceFileName(), returnType, pCtx, literalOnly);

        }
        catch (Throwable e) {
            parserContext.set(null);
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            else {
                throw new CompileException(e.getMessage(), e);
            }
        }

    }

    private static boolean isBooleanOperator(int operator) {
        return operator == Operator.AND || operator == Operator.OR;
    }

    protected ASTNode verify(ParserContext pCtx, ASTNode tk) {
        if (tk.isOperator() && (tk.getOperator().equals(Operator.AND) || tk.getOperator().equals(Operator.OR))) {
            secondPassOptimization = true;
        }

        if (tk.isDiscard() || tk.isOperator()) {
            return tk;
        }
        else if (tk.isLiteral()) {
            if ((fields & COMPILE_IMMEDIATE) != 0 && tk.getClass() == ASTNode.class) {
                return new LiteralNode(tk.getLiteralValue());
            }
            else {
                return tk;
            }
        }

        if (verifying) {
            if (tk.isIdentifier()) {
                PropertyVerifier propVerifier = new PropertyVerifier(tk.getNameAsArray(), pCtx);
                tk.setEgressType(returnType = propVerifier.analyze());

                if (propVerifier.isResolvedExternally()) {
                    pCtx.addInput(tk.getAbsoluteName(), returnType);
                }
            }
            else {
                returnType = tk.getEgressType();
            }
        }
        return tk;
    }

//    private static int asInt(final Object o) {
//        return (Integer) o;
//    }

    public ExpressionCompiler(String expression) {
        setExpression(expression);
    }

    public ExpressionCompiler(String expression, boolean verifying) {
        setExpression(expression);
        this.verifying = verifying;
    }

    public ExpressionCompiler(char[] expression) {
        setExpression(expression);
    }

    public ExpressionCompiler(String expression, ParserContext ctx) {
        setExpression(expression);
        contextControl(SET, ctx, this);
    }

    public ExpressionCompiler(char[] expression, ParserContext ctx) {
        setExpression(expression);
        contextControl(SET, ctx, this);
    }

    public boolean isVerifying() {
        return verifying;
    }

    public void setVerifying(boolean verifying) {
        this.verifying = verifying;
    }

    public Class getReturnType() {
        return returnType;
    }

    public void setReturnType(Class returnType) {
        this.returnType = returnType;
    }

    public String getExpression() {
        return new String(expr);
    }

    public ParserContext getParserContextState() {
        return pCtx;
    }

    public void removeParserContext() {
        removeContext();
    }

    public boolean isLiteralOnly() {
        return literalOnly;
    }
}
