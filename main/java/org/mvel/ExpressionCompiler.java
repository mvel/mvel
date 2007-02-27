package org.mvel;

import org.mvel.util.*;
import static org.mvel.util.ParseTools.doOperations;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class ExpressionCompiler extends AbstractParser {
    private final Stack stk = new ExecutionStack();
    private List<String> inputs;

    public CompiledExpression compile(boolean verifying) {
        Token tk;
        Token tkOp;
        Token tkLA;
        Token tkLA2;
        TokenSet tokenSet = new TokenSet();

        boolean firstLA;

        if (verifying) inputs = new LinkedList<String>();

        while ((tk = nextToken()) != null) {
            if (tk.isSubeval()) {
                ExpressionCompiler subCompiler = new ExpressionCompiler(tk.getNameAsArray());
                tk.setAccessor(subCompiler.compile(verifying));

                if (verifying)
                    inputs.addAll(subCompiler.getInputs());
            }

            /**
             * This kludge of code is to handle compile-time literal reduction.  We need to avoid
             * reducing for certain literals like, 'this', ternary and ternary else.
             */
            if (tk.isLiteral() && tk.getLiteralValue() != LITERALS.get("this")) {
                if ((tkOp = nextToken()) != null && tkOp.isOperator()
                        && !tkOp.isOperator(Operator.TERNARY) && !tkOp.isOperator(Operator.TERNARY_ELSE)) {

                    /**
                     * If the next token is ALSO a literal, then we have a candidate for a compile-time
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
                        while ((tkOp = nextToken()) != null) {
                            if ((tkLA2 = nextToken()) != null && tkLA2.isLiteral()) {
                                stk.push(tkLA2.getLiteralValue(), tkOp.getLiteralValue());
                                reduceTrinary();
                                firstLA = false;
                            }
                            else {
                                if (firstLA) {
                                    /**
                                     * There are more tokens, but we can't reduce anymore.  So
                                     * we create a reduced token for what we've got.
                                     */
                                    tokenSet.addTokenNode(new Token(Token.LITERAL, stk.pop()));
                                }
                                else {
                                    /**
                                     * We have reduced additional tokens, but we can't reduce
                                     * anymore.
                                     */
                                    tokenSet.addTokenNode(new Token(Token.LITERAL, stk.pop()), tkOp);

                                    if (tkLA2 != null) tokenSet.addTokenNode(tkLA2);
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
                            tokenSet.addTokenNode(new Token(Token.LITERAL, stk.pop()));

                        continue;
                    }
                    else {
                        tokenSet.addTokenNode(tk, tkOp);
                        if (tkLA != null) tokenSet.addTokenNode(tkLA);
                        continue;
                    }
                }
                else {


                    tokenSet.addTokenNode(tk);
                    if (tkOp != null) tokenSet.addTokenNode(tkOp);
                    continue;
                }
            }

            if (verifying && tk.isIdentifier()) {
                inputs.add(tk.getAbsoluteName());

                PropertyVerifier propVerifier = new PropertyVerifier(tk.getNameAsArray());
                propVerifier.analyze();

                inputs.addAll(propVerifier.getInputs());
            }

            tokenSet.addTokenNode(tk);
        }

        return new CompiledExpression(new FastTokenIterator(tokenSet));
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
                            stk.push(Class.forName(String.valueOf(v1)).isInstance(v2));

                        break;

                    case Operator.CONVERTABLE_TO:
                        if (v1 instanceof Class)
                            stk.push(DataConversion.canConvert(v2.getClass(), (Class) v1));
                        else
                            stk.push(DataConversion.canConvert(v2.getClass(), Class.forName(String.valueOf(v1))));
                        break;

                    case Operator.CONTAINS:
                        stk.push(ParseTools.containsCheck(v2, v1));
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
            throw new CompileException("syntax error or incomptable types (left=" +
                    (v1 != null ? v1.getClass().getName() : "null") + ", right=" +
                    (v2 != null ? v2.getClass().getName() : "null") + ")", expr, cursor, e);

        }
        catch (Exception e) {
            throw new CompileException("failed to subEval expression", e);
        }

    }

    private static int asInt(final Object o) {
        return (Integer) o;
    }


    public List<String> getInputs() {
        return inputs;
    }

    public void setInputs(List<String> inputs) {
        this.inputs = inputs;
    }

    public ExpressionCompiler(String expression) {
        setExpression(expression);
    }

    public ExpressionCompiler(char[] expression) {
        setExpression(expression);
    }
}
