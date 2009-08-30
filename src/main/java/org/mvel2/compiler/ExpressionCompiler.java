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

package org.mvel2.compiler;

import org.mvel2.CompileException;
import static org.mvel2.DataConversion.canConvert;
import static org.mvel2.DataConversion.convert;
import org.mvel2.ErrorDetail;
import org.mvel2.Operator;
import static org.mvel2.Operator.PTABLE;
import org.mvel2.ParserContext;
import org.mvel2.ast.*;
import static org.mvel2.ast.ASTNode.COMPILE_IMMEDIATE;
import org.mvel2.util.ASTLinkedList;
import static org.mvel2.util.CompilerTools.optimizeAST;
import org.mvel2.util.ExecutionStack;
import static org.mvel2.util.ParseTools.subCompileExpression;
import static org.mvel2.util.ParseTools.unboxPrimitive;
import org.mvel2.util.StringAppender;

/**
 * This is the main MVEL compiler.
 */
public class ExpressionCompiler extends AbstractParser {
    private Class returnType;

    private boolean verifyOnly = false;
    private boolean verifying = true;
    private boolean secondPassOptimization = false;

    public CompiledExpression compile() {
        return compile(contextControl(GET_OR_CREATE, null, this));
    }

    @Deprecated
    /**
     * @deprecated use {@link org.mvel2.MVEL#compileExpression(String, org.mvel2.ParserContext)} instead.
     * @param ctx
     * @return compile payload.
     */
    public CompiledExpression compile(ParserContext ctx) {
        try {
            this.debugSymbols = (this.pCtx = ctx).isDebugSymbols();
            newContext(ctx);
            return _compile();
        }
        finally {
            //noinspection ThrowFromFinallyBlock
            removeContext();

            if (pCtx.isFatalError()) {
                contextControl(REMOVE, null, null);

                StringAppender err = new StringAppender();

                for (ErrorDetail e : pCtx.getErrorList()) {
                    err.append("\n - ").append("(").append(e.getRow()).append(",").append(e.getCol()).append(")")
                            .append(" ").append(e.getMessage());
                }

                //noinspection ThrowFromFinallyBlock
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
        compileMode = true;

        boolean firstLA;

        if (pCtx == null) pCtx = getParserContext();

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

                /**
                 * Record the type of the current node..
                 */
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
                if (!verifyOnly && tk.isLiteral()) {
                    if (literalOnly == -1) literalOnly = 1;

                    if ((tkOp = nextTokenSkipSymbols()) != null && tkOp.isOperator()
                            && !tkOp.isOperator(Operator.TERNARY) && !tkOp.isOperator(Operator.TERNARY_ELSE)) {

                        /**
                         * If the next token is ALSO a literal, then we have a candidate for a compile-time literal
                         * reduction.
                         */
                        if ((tkLA = nextTokenSkipSymbols()) != null && tkLA.isLiteral()
                                && tkOp.getOperator() < 34 && ((lastOp == -1
                                || (lastOp < PTABLE.length && PTABLE[lastOp] <= PTABLE[tkOp.getOperator()])))) {

                            stk.push(tk.getLiteralValue(), tkLA.getLiteralValue(), op = tkOp.getOperator());

                            /**
                             * Reduce the token now.
                             */
                            if (isArithmeticOperator(op)) {
                                if (!compileReduce(op, astBuild)) continue;
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
                                else if ((tkLA2 = nextTokenSkipSymbols()) != null) {

                                    if (tkLA2.isLiteral()) {
                                        stk.push(tkLA2.getLiteralValue(), op = tkOp2.getOperator());

                                        if (isArithmeticOperator(op)) {
                                            compileReduce(op, astBuild);
                                        }
                                        else {
                                            reduce();
                                        }
                                    }
                                    else {
                                        /**
                                         * A reducable line of literals has ended.  We must now terminate here and
                                         * leave the rest to be determined at runtime.
                                         */
                                        if (!stk.isEmpty()) astBuild.addTokenNode(new LiteralNode(stk.pop()));

                                        astBuild.addTokenNode(new OperatorNode(tkOp2.getOperator()), verify(pCtx, tkLA2));
                                        break;
                                    }

                                    firstLA = false;
                                    literalOnly = 0;
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

                                        if (tkLA2 != null) astBuild.addTokenNode(verify(pCtx, tkLA2));
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
                    else if (tkOp != null && !tkOp.isOperator() && !(tk.getLiteralValue() instanceof Class)) {
                        throw new CompileException("unexpected token: " + tkOp.getName());
                    }
                    else {
                        literalOnly = 0;
                        astBuild.addTokenNode(verify(pCtx, tk));
                        if (tkOp != null) astBuild.addTokenNode(verify(pCtx, tkOp));
                        continue;
                    }
                }
                else {
                    if (tk.isOperator()) {
                        lastOp = tk.getOperator();
                    }

                    literalOnly = 0;
                }

                astBuild.addTokenNode(verify(pCtx, tk));
            }

            astBuild.finish();

            if (verifying && !verifyOnly) {
                pCtx.processTables();
            }

            if (!stk.isEmpty()) {
                throw new CompileException("COMPILE ERROR: non-empty stack after compile.");
            }


            if (!verifyOnly) {
                return new CompiledExpression(optimizeAST(astBuild, secondPassOptimization, pCtx), pCtx.getSourceFile(), returnType, pCtx, literalOnly == 1);
            }
            else {
                return null;
            }

        }
        catch (NullPointerException e) {
            throw new CompileException("not a statement, or badly formed structure", e);
        }
        catch (CompileException e) {
            e.setExpr(expr);
            e.setLineNumber(pCtx.getLineCount());
            e.setCursor(cursor);
            e.setColumn(cursor - pCtx.getLineOffset());
            throw e;
        }
        catch (Throwable e) {
            parserContext.set(null);
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            else {
                throw new CompileException(e.getMessage(), e);
            }
        }
    }

    private boolean compileReduce(int opCode, ASTLinkedList astBuild) {
        switch (arithmeticFunctionReduction(opCode)) {
            case -1:
                /**
                 * The reduction failed because we encountered a non-literal,
                 * so we must now back out and cleanup.
                 */

                stk.xswap_op();

                astBuild.addTokenNode(new LiteralNode(stk.pop()));
                astBuild.addTokenNode(
                        (OperatorNode) splitAccumulator.pop(),
                        verify(pCtx, (ASTNode) splitAccumulator.pop())
                );
                return false;
            case -2:
                /**
                 * Back out completely, pull everything back off the stack and add the instructions
                 * to the output payload as they are.
                 */

                LiteralNode rightValue = new LiteralNode(stk.pop());
                OperatorNode operator = new OperatorNode((Integer) stk.pop());

                astBuild.addTokenNode(new LiteralNode(stk.pop()), operator);
                astBuild.addTokenNode(rightValue, (OperatorNode) splitAccumulator.pop());
                astBuild.addTokenNode(verify(pCtx, (ASTNode) splitAccumulator.pop()));
        }
        return true;
    }

    private static boolean isBooleanOperator(int operator) {
        return operator == Operator.AND || operator == Operator.OR || operator == Operator.TERNARY || operator == Operator.TERNARY_ELSE;
    }

    protected ASTNode verify(ParserContext pCtx, ASTNode tk) {
        if (tk.isOperator() && (tk.getOperator().equals(Operator.AND) || tk.getOperator().equals(Operator.OR))) {
            secondPassOptimization = true;
        }
        if (tk.isDiscard() || tk.isOperator()) {
            return tk;
        }
        else if (tk.isLiteral()) {
            /**
             * Convert literal values from the default ASTNode to the more-efficient LiteralNode.
             */
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

                if (tk instanceof Union) {
                    propVerifier.setCtx(((Union) tk).getLeftEgressType());
                    tk.setEgressType(returnType = propVerifier.analyze());
                }
                else {
                    tk.setEgressType(returnType = propVerifier.analyze());

                    if (propVerifier.isClassLiteral()) {
                        return new LiteralNode(returnType);
                    }
                    if (propVerifier.isResolvedExternally()) {
                        pCtx.addInput(tk.getAbsoluteName(), propVerifier.isDeepProperty() ? Object.class : returnType);
                    }
                }
            }
            else if (tk.isAssignment()) {
                Assignment a = (Assignment) tk;

                if (a.getAssignmentVar() != null) {
                    PropertyVerifier propVerifier = new PropertyVerifier(a.getAssignmentVar(), pCtx);
                    tk.setEgressType(returnType = propVerifier.analyze());

                    if (!a.isNewDeclaration() && propVerifier.isResolvedExternally()) {
                        pCtx.addInput(tk.getAbsoluteName(), returnType);
                    }

                    ExecutableStatement c = (ExecutableStatement) subCompileExpression(a.getExpression());

                    if (pCtx.isStrictTypeEnforcement()) {
                        /**
                         * If we're using strict type enforcement, we need to see if this coercion can be done now,
                         * or fail epicly.
                         */
                        if (!returnType.isAssignableFrom(c.getKnownEgressType()) && c.isLiteralOnly()) {
                            if (canConvert(c.getKnownEgressType(), returnType)) {
                                /**
                                 * We convert the literal to the proper type.
                                 */
                                try {
                                    a.setValueStatement(new ExecutableLiteral(convert(c.getValue(null, null), returnType)));
                                    return tk;
                                }
                                catch (Exception e) {
                                    // fall through.
                                }
                            }
                            else if (returnType.isPrimitive()
                                    && unboxPrimitive(c.getKnownEgressType()).equals(returnType)) {
                                /**
                                 * We ignore boxed primitive cases, since MVEL does not recognize primitives.
                                 */
                                return tk;
                            }

                            throw new CompileException(
                                    "cannot assign type " + c.getKnownEgressType().getName()
                                            + " to " + returnType.getName());
                        }
                    }
                }
            }
            returnType = tk.getEgressType();
        }

        if (!tk.isLiteral() && tk.getClass() == ASTNode.class && (tk.getFields() & ASTNode.ARRAY_TYPE_LITERAL) == 0) {
            if (pCtx.isStrongTyping()) tk.strongTyping();
            tk.storePctx();
            tk.storeInLiteralRegister(pCtx);
        }

        return tk;
    }

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

    public boolean isVerifyOnly() {
        return verifyOnly;
    }

    public void setVerifyOnly(boolean verifyOnly) {
        this.verifyOnly = verifyOnly;
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
        return literalOnly == 1;
    }
}
