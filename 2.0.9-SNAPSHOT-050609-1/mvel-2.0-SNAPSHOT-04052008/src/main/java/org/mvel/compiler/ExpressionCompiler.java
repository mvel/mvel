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
import org.mvel.Operator;
import org.mvel.ParserContext;
import static org.mvel.Soundex.soundex;
import org.mvel.ast.ASTNode;
import org.mvel.ast.LiteralNode;
import org.mvel.ast.Substatement;
import org.mvel.util.ASTLinkedList;
import static org.mvel.util.CompilerTools.optimizeAST;
import org.mvel.util.ExecutionStack;
import static org.mvel.util.ParseTools.doOperations;
import static org.mvel.util.PropertyTools.isEmpty;
import static org.mvel.util.PropertyTools.similarity;
import org.mvel.util.Stack;
import org.mvel.util.StringAppender;

import static java.lang.String.valueOf;
import java.util.regex.Pattern;

public class ExpressionCompiler extends AbstractParser {
    private final Stack stk = new ExecutionStack();

    private Class returnType;

    private boolean verifying = true;
    private boolean secondPassOptimization = false;

    //private ParserContext pCtx;

    public CompiledExpression compile() {
        return compile(new ParserContext());
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
                throw new CompileException("Failed to compile: " + pCtx.getErrorList().size() + " compilation error(s)", pCtx.getErrorList());
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


        ASTLinkedList astBuild = new ASTLinkedList();

        boolean firstLA;

        if (pCtx == null) pCtx = getParserContext();

        debugSymbols = pCtx.isDebugSymbols();


        try {
            if (verifying) {
                pCtx.initializeTables();
            }

            fields |= ASTNode.COMPILE_IMMEDIATE;

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
                        if ((tkLA = nextTokenSkipSymbols()) != null && tkLA.isLiteral()) {
                            stk.push(tk.getLiteralValue(), tkLA.getLiteralValue(), tkOp.getLiteralValue());

                            /**
                             * Reduce the token now.
                             */

                            reduce();

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

                                    stk.push(tkLA2.getLiteralValue(), tkOp2.getLiteralValue());

                                    reduce();
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
                                        astBuild.addTokenNode(new LiteralNode(stk.pop()), tkOp);

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
                    literalOnly = false;
                }

                astBuild.addTokenNode(verify(pCtx, tk));
            }


            astBuild.finish();

            if (verifying) {
                pCtx.processTables();
            }

            if (!stk.isEmpty()) throw new CompileException("COMPILE ERROR: non-empty stack after compile.");

            return new CompiledExpression(optimizeAST(astBuild, secondPassOptimization), getCurrentSourceFileName(), returnType, pCtx, literalOnly);
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
        if (tk.isOperator()) {
            if (tk.isOperator(Operator.AND) || tk.isOperator(Operator.OR)) {
                secondPassOptimization = true;
            }
        }

        if (tk.isDiscard() || tk.isOperator()) {
            return tk;
        }
        else if (tk.isLiteral()) {
            if ((fields & ASTNode.COMPILE_IMMEDIATE) != 0 && tk.getClass() == ASTNode.class) {
                return new LiteralNode(tk.getLiteralValue());
            }
            else {
                return tk;
            }
        }

        if (verifying) {
//            if (tk.isAssignment()) {
//                String varName = ((Assignment) tk).getAssignmentVar();
//
//                if (isReservedWord(varName)) {
//                    addFatalError("invalid assignment - variable name is a reserved keyword: " + varName);
//                }
//
//                new ExpressionCompiler(new String(((Assignment) tk).getExpression()).trim())._compile();
//
//                if (((Assignment) tk).isNewDeclaration() && pCtx.hasVarOrInput(varName)) {
//                    throw new CompileException("statically-typed variable '" + varName + "' defined more than once in scope: "
//                            + tk.getClass().getName());
//                }
//
//                pCtx.addVariable(varName, returnType = tk.getEgressType());
//            }
        //    else
            if (tk.isIdentifier()) {
                PropertyVerifier propVerifier = new PropertyVerifier(tk.getNameAsArray(), pCtx);
                returnType = propVerifier.analyze();

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


    /**
     * This method is called when we reach the point where we must subEval a trinary operation in the expression.
     * (ie. val1 op val2).  This is not the same as a binary operation, although binary operations would appear
     * to have 3 structures as well.  A binary structure (or also a junction in the expression) compares the
     * current state against 2 downrange structures (usually an op and a val).
     */
    private void reduce() {
        Object v1 = null, v2 = null;
        Integer operator;
        try {
            while (stk.size() > 1) {
                operator = (Integer) stk.pop();
                v1 = stk.pop();
                v2 = stk.pop();

                switch (operator) {
                    case Operator.ADD:
                    case Operator.SUB:
                    case Operator.DIV:
                    case Operator.MULT:
                    case Operator.MOD:
                    case Operator.EQUAL:
                    case Operator.NEQUAL:
                    case Operator.GTHAN:
                    case Operator.LTHAN:
                    case Operator.GETHAN:
                    case Operator.LETHAN:
                    case Operator.POWER:
                        stk.push(doOperations(v2, operator, v1));
                        break;

                    case Operator.AND:
                        stk.push(((Boolean) v2) && ((Boolean) v1));
                        break;

                    case Operator.OR:
                        stk.push(((Boolean) v2) || ((Boolean) v1));
                        break;

                    case Operator.CHOR:
                        if (!isEmpty(v2) || !isEmpty(v1)) {
                            stk.clear();
                            stk.push(!isEmpty(v2) ? v2 : v1);
                            return;
                        }
                        else stk.push(null);
                        break;

                    case Operator.REGEX:
                        stk.push(Pattern.compile(valueOf(v1)).matcher(valueOf(v2)).matches());
                        break;

                    case Operator.BW_AND:
                        stk.push(asInt(v2) & asInt(v1));
                        break;

                    case Operator.BW_OR:
                        stk.push(asInt(v2) | asInt(v1));
                        break;

                    case Operator.BW_XOR:
                        stk.push(asInt(v2) ^ asInt(v1));
                        break;

                    case Operator.BW_SHIFT_LEFT:
                        stk.push(asInt(v2) << asInt(v1));
                        break;

                    case Operator.BW_USHIFT_LEFT:
                        int iv2 = asInt(v2);
                        if (iv2 < 0) iv2 *= -1;
                        stk.push(iv2 << asInt(v1));
                        break;

                    case Operator.BW_SHIFT_RIGHT:
                        stk.push(asInt(v2) >> asInt(v1));
                        break;

                    case Operator.BW_USHIFT_RIGHT:
                        stk.push(asInt(v2) >>> asInt(v1));
                        break;

                    case Operator.STR_APPEND:
                        stk.push(new StringAppender(valueOf(v2)).append(valueOf(v1)).toString());
                        break;

                    case Operator.SOUNDEX:
                        stk.push(soundex(valueOf(v1)).equals(soundex(valueOf(v2))));
                        break;

                    case Operator.SIMILARITY:
                        stk.push(similarity(valueOf(v1), valueOf(v2)));
                        break;
                }
            }
        }
        catch (ClassCastException e) {
            throw new CompileException("syntax error or incomptable types (left=" +
                    (v1 != null ? v1.getClass().getName() : "null") + ", right=" +
                    (v2 != null ? v2.getClass().getName() : "null") + ")", expr, cursor, e);

        }
        catch (Exception e) {
            throw new CompileException("failed to subEval expression: <<" + new String(expr) + ">>", e);
        }

    }

    private static int asInt(final Object o) {
        return (Integer) o;
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
