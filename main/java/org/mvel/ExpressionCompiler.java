package org.mvel;

import org.mvel.ast.Assignment;
import org.mvel.ast.LiteralNode;
import org.mvel.ast.Substatement;
import static org.mvel.util.CompilerTools.optimizeAST;
import org.mvel.util.ExecutionStack;
import static org.mvel.Operator.PTABLE;

public class ExpressionCompiler extends AbstractParser {
    private Class returnType;

    private boolean verifying = true;
    private boolean secondPassOptimization = false;

    private ParserContext pCtx;

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
        ASTLinkedList astLinkedList = new ASTLinkedList();

        int op, lastOp = -1;

        stk = new ExecutionStack();
        dStack = new ExecutionStack();

        boolean firstLA;

        debugSymbols = (pCtx = getParserContext()).isDebugSymbols();


        try {
            if (verifying) {
                pCtx.initializeTables();
            }

            fields |= ASTNode.COMPILE_IMMEDIATE;

            while ((tk = nextToken()) != null) {
                if (tk.fields == -1) {
                    astLinkedList.addTokenNode(tk);
                    continue;
                }

                returnType = tk.getEgressType();

                if (tk instanceof Substatement) {
                    ExpressionCompiler subCompiler = new ExpressionCompiler(tk.getNameAsArray(), pCtx);
                    tk.setAccessor(subCompiler._compile());

                    if (subCompiler.isLiteralOnly()) {
                        tk = new LiteralNode(tk.getReducedValueAccelerated(null, null, null));
                    }
                    returnType = subCompiler.getReturnType();
                }

                /**
                 * This kludge of code is to handle compile-time literal reduction.  We need to avoid
                 * reducing for certain literals like, 'this', ternary and ternary else.
                 */
                if (tk.isLiteral() && tk.getLiteralValue() != LITERALS.get("this")) {
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
                                    astLinkedList.addTokenNode(new LiteralNode(stk.pop()), verify(pCtx, tkOp2));
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
                                        astLinkedList.addTokenNode(new LiteralNode(stk.pop()));
                                    }
                                    else {
                                        /**
                                         * We have reduced additional tokens, but we can't reduce
                                         * anymore.
                                         */
                                        astLinkedList.addTokenNode(new LiteralNode(stk.pop()), tkOp);

                                        if (tkLA2 != null) astLinkedList.addTokenNode(tkLA2);
                                    }
                                    break;
                                }
                            }

                            /**
                             * If there are no more tokens left to parse, we check to see if
                             * we've been doing any reducing, and if so we create the token
                             * now.
                             */
                            if (!stk.isEmpty()) {
                                astLinkedList.addTokenNode(new LiteralNode(stk.pop()));
                            }

                            continue;
                        }
                        else {
                            literalOnly = false;
                            astLinkedList.addTokenNode(verify(pCtx, tk), verify(pCtx, tkOp));
                            if (tkLA != null) astLinkedList.addTokenNode(verify(pCtx, tkLA));
                            continue;
                        }
                    }
                    else {
                        literalOnly = false;
                        astLinkedList.addTokenNode(verify(pCtx, tk));
                        if (tkOp != null) astLinkedList.addTokenNode(verify(pCtx, tkOp));

                        continue;
                    }
                }
                else {
                    if (tk.isOperator()) {
                        lastOp = tk.getOperator();
                    }
                    
                    literalOnly = false;
                }

                astLinkedList.addTokenNode(verify(pCtx, tk));
            }

            if (verifying) {
                pCtx.processTables();
            }

            astLinkedList.finish();

            if (!stk.isEmpty()) throw new CompileException("COMPILE ERROR: non-empty stack after compile.");

            return new CompiledExpression(optimizeAST(astLinkedList, secondPassOptimization), getCurrentSourceFileName(), returnType, pCtx, literalOnly);
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
            if (tk.isAssignment()) {
                char[] assign = tk.getNameAsArray();
                int c = 0;
                while (c < assign.length && assign[c] != '=') c++;

                String varName = new String(assign, 0, c++).trim();

                if (isReservedWord(varName)) {
                    addFatalError("invalid assignment - variable name is a reserved keyword: " + varName);
                }

                new ExpressionCompiler(new String(assign, c, assign.length - c).trim())._compile();

                if (((Assignment) tk).isNewDeclaration() && pCtx.hasVarOrInput(varName)) {
                    throw new CompileException("statically-typed variable '" + varName + "' defined more than once in scope");
                }

                pCtx.addVariable(varName, returnType = tk.getEgressType());
            }
            else if (tk.isIdentifier()) {
                PropertyVerifier propVerifier = new PropertyVerifier(tk.getNameAsArray(), getParserContext());
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

    ExpressionCompiler(char[] expression, ParserContext ctx) {
        setExpression(expression);
        this.pCtx = ctx;
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
