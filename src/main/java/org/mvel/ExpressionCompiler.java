package org.mvel;

import static org.mvel.DataConversion.canConvert;
import org.mvel.ast.AssignmentNode;
import org.mvel.ast.LiteralNode;
import org.mvel.ast.Substatement;
import org.mvel.util.ExecutionStack;
import static org.mvel.util.ParseTools.containsCheck;
import static org.mvel.util.ParseTools.doOperations;
import org.mvel.util.PropertyTools;
import org.mvel.util.Stack;
import org.mvel.util.StringAppender;

import static java.lang.Class.forName;
import java.util.regex.Pattern;

public class ExpressionCompiler extends AbstractParser {
    private final Stack stk = new ExecutionStack();

 //   private Set<String> inputs;
 //   private Set<String> locals;

    private Class returnType;

    private boolean verifying = true;

    private ParserContext pCtx;

    public CompiledExpression compile() {
        return compile(new ParserContext());
    }

    public CompiledExpression compile(ParserContext ctx) {
        if (parserContext == null) {
            parserContext = new ThreadLocal<ParserContext>();
        }
        parserContext.set(ctx);
        
        return _compile();
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

        boolean firstLA;

        pCtx = getParserContext();

        try {
            if (verifying) {
//                inputs = new LinkedHashSet<String>();
//                locals = new LinkedHashSet<String>();

                getParserContext().initializeTables();
            }

            fields |= ASTNode.COMPILE_IMMEDIATE;

            while ((tk = nextToken()) != null) {
                if (tk.fields == -1) {
                    astLinkedList.addTokenNode(tk);
                    continue;
                }

                returnType = tk.getEgressType();

                if (pCtx.isStrictTypeEnforcement() && tk instanceof AssignmentNode
                        && (pCtx.getInputs() == null
                        || !pCtx.getInputs().containsKey(tk.getName()))) {

                    addFatalError("untyped var not permitted in strict-mode: " + tk.getName());
                }

                if (tk instanceof Substatement) {
                    ExpressionCompiler subCompiler = new ExpressionCompiler(tk.getNameAsArray());
                    tk.setAccessor(subCompiler._compile());
//
//                    if (verifying) {
//                        inputs.addAll(subCompiler.getInputs());
//                    }
                }

                /**
                 * This kludge of code is to handle _compile-time literal reduction.  We need to avoid
                 * reducing for certain literals like, 'this', ternary and ternary else.
                 */
                if (tk.isLiteral() && tk.getLiteralValue() != LITERALS.get("this")) {
                    if ((tkOp = nextToken()) != null && tkOp.isOperator()
                            && !tkOp.isOperator(Operator.TERNARY) && !tkOp.isOperator(Operator.TERNARY_ELSE)) {

                        /**
                         * If the next token is ALSO a literal, then we have a candidate for a _compile-time
                         * reduction.
                         */
                        if ((tkLA = nextToken()) != null && tkLA.isLiteral()) {
                            stk.push(tk.getLiteralValue(), tkLA.getLiteralValue(), tkOp.getLiteralValue());

                            /**
                             * Reduce the token now.
                             */
                            reduceTrinary();

                            firstLA = true;

                            /**
                             * Now we need to check to see if this is actually a continuing reduction.
                             */
                            while ((tkOp2 = nextToken()) != null) {
                                if (!tkOp2.isOperator(tkOp.getOperator())) {
                                    /**
                                     * We can't continue any further because we are dealing with
                                     * different operators.
                                     */
                                    astLinkedList.addTokenNode(new LiteralNode(stk.pop()));
                                    astLinkedList.addTokenNode(tkOp2);
                                    break;
                                }
                                else if ((tkLA2 = nextToken()) != null
                                        && tkLA2.isLiteral()) {

                                    stk.push(tkLA2.getLiteralValue(), tkOp2.getLiteralValue());
                                    reduceTrinary();
                                    firstLA = false;
                                }
                                else {
                                    if (firstLA) {
                                        /**
                                         * There are more tokens, but we can't reduce anymore.  So
                                         * we create a reduced token for what we've got.
                                         */
                                        astLinkedList.addTokenNode(new ASTNode(ASTNode.LITERAL, stk.pop()));
                                    }
                                    else {
                                        /**
                                         * We have reduced additional tokens, but we can't reduce
                                         * anymore.
                                         */
                                        astLinkedList.addTokenNode(new ASTNode(ASTNode.LITERAL, stk.pop()), tkOp);

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
                            if (!stk.isEmpty())
                                astLinkedList.addTokenNode(new ASTNode(ASTNode.LITERAL, stk.pop()));

                            continue;
                        }
                        else {
                            astLinkedList.addTokenNode(verify(pCtx, tk), verify(pCtx, tkOp));
                            if (tkLA != null) astLinkedList.addTokenNode(verify(pCtx, tkLA));
                            continue;
                        }
                    }
                    else {
                        astLinkedList.addTokenNode(verify(pCtx, tk));
                        if (tkOp != null) astLinkedList.addTokenNode(verify(pCtx, tkOp));

                        continue;
                    }
                }
                astLinkedList.addTokenNode(verify(pCtx, tk));
            }

            if (verifying) {
//                for (String s : locals) {
//                    inputs.remove(s);
//                }
                pCtx.processTables();
            }

            if (pCtx.isFatalError()) {
                parserContext.remove();
                throw new CompileException("Failed to _compile: " + pCtx.getErrorList().size() + " compilation error(s)", pCtx.getErrorList());
            }
            else if (pCtx.isFatalError()) {
                parserContext.remove();
                throw new CompileException("Failed to _compile: " + pCtx.getErrorList().size() + " compilation error(s)", pCtx.getErrorList());
            }

            return new CompiledExpression(new ASTArrayList(astLinkedList), getCurrentSourceFileName());
        }
        catch (Throwable e) {
            parserContext.remove();
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            else {
                throw new CompileException(e.getMessage(), e);
            }
        }

    }

    protected ASTNode verify(ParserContext pCtx, ASTNode tk) {
        if (tk.isDiscard() || (tk.fields & (ASTNode.OPERATOR | ASTNode.LITERAL)) != 0) return tk;

        if (verifying) {
            if (tk.isAssignment()) {
                char[] assign = tk.getNameAsArray();
                int c = 0;
                while (c < assign.length && assign[c] != '=') c++;

                String varName = new String(assign, 0, c++).trim();

                if (isReservedWord(varName)) {
                    addFatalError("invalid assignment - variable name is a reserved keyword: " + varName);
                }

                ExpressionCompiler subCompiler =
                        new ExpressionCompiler(new String(assign, c, assign.length - c).trim());

                subCompiler._compile();

                pCtx.addVariable(varName, returnType = tk.getEgressType());
            }
            else if (tk.isIdentifier()) {
                PropertyVerifier propVerifier = new PropertyVerifier(tk.getNameAsArray(), getParserContext());
                pCtx.addInput(tk.getAbsoluteName(), returnType = propVerifier.analyze());

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
    private void reduceTrinary() {
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
                        if (!PropertyTools.isEmpty(v2) || !PropertyTools.isEmpty(v1)) {
                            stk.clear();
                            stk.push(!PropertyTools.isEmpty(v2) ? v2 : v1);
                            return;
                        }
                        else stk.push(null);
                        break;

                    case Operator.REGEX:
                        stk.push(Pattern.compile(String.valueOf(v1)).matcher(String.valueOf(v2)).matches());
                        break;

                    case Operator.INSTANCEOF:
                        if (v1 instanceof Class)
                            stk.push(((Class) v1).isInstance(v2));
                        else
                            stk.push(forName(String.valueOf(v1)).isInstance(v2));

                        break;

                    case Operator.CONVERTABLE_TO:
                        if (v1 instanceof Class)
                            stk.push(canConvert(v2.getClass(), (Class) v1));
                        else
                            stk.push(canConvert(v2.getClass(), forName(String.valueOf(v1))));
                        break;

                    case Operator.CONTAINS:
                        stk.push(containsCheck(v2, v1));
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
                        stk.push(new StringAppender(String.valueOf(v2)).append(String.valueOf(v1)).toString());
                        break;

                    case Operator.SOUNDEX:
                        stk.push(Soundex.soundex(String.valueOf(v1)).equals(Soundex.soundex(String.valueOf(v2))));
                        break;

                    case Operator.SIMILARITY:
                        stk.push(PropertyTools.similarity(String.valueOf(v1), String.valueOf(v2)));
                        break;
                }
            }
        }
        catch (ClassCastException e) {
            if ((fields & ASTNode.LOOKAHEAD) == 0) {
                /**
                 * This will allow for some developers who like messy expressions to compileAccessor
                 * away with some messy constructs like: a + b < c && e + f > g + q instead
                 * of using brackets like (a + b < c) && (e + f > g + q)
                 */
                fields |= ASTNode.LOOKAHEAD;

                ASTNode tk = nextToken();
                if (tk != null) {
                    stk.push(v1, nextToken(), tk.getOperator());

                    reduceTrinary();
                    return;
                }
            }
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


//    public Set<String> getInputs() {
//        return inputs;
//    }
//
//    public Set<String> getLocals() {
//        return locals;
//    }

    public ExpressionCompiler(String expression) {
        setExpression(expression);
    }

    public ExpressionCompiler(char[] expression) {
        setExpression(expression);
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
}
