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

package org.mvel2.util;

import org.mvel2.CompileException;
import org.mvel2.Operator;
import static org.mvel2.Operator.PTABLE;
import org.mvel2.ParserContext;
import org.mvel2.ast.*;
import static org.mvel2.compiler.AbstractParser.getCurrentThreadParserContext;
import org.mvel2.compiler.Accessor;
import org.mvel2.compiler.CompiledExpression;
import org.mvel2.compiler.ExecutableAccessor;
import org.mvel2.compiler.ExecutableLiteral;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.ClassImportResolverFactory;
import static org.mvel2.util.ParseTools.__resolveType;
import static org.mvel2.util.ParseTools.boxPrimitive;

import java.util.LinkedHashMap;
import java.util.Map;

public class CompilerTools {
    /**
     * Optimize the AST, by reducing any stack-based-operations to dedicated nodes where possible.
     *
     * @param astLinkedList          - AST to be optimized.
     * @param secondPassOptimization - perform a second pass optimization to optimize boolean expressions.
     * @param ctx                    - The parser context
     * @return optimized AST
     */
    public static ASTLinkedList optimizeAST(ASTLinkedList astLinkedList, boolean secondPassOptimization, ParserContext ctx) {
        ASTLinkedList optimizedAst = new ASTLinkedList();
        ASTNode tk, tkOp, tkOp2;

        /**
         * Re-process the AST and optimize it.
         */
        while (astLinkedList.hasMoreNodes()) {
            if ((tk = astLinkedList.nextNode()).getFields() == -1) {
                optimizedAst.addTokenNode(tk);
            }
            else if (astLinkedList.hasMoreNodes()) {
                if ((tkOp = astLinkedList.nextNode()).getFields() == -1) {
                    optimizedAst.addTokenNode(tk, tkOp);
                }
                else if (tkOp.isOperator() && tkOp.getOperator() < 21) {
                    int op = tkOp.getOperator();
                    int op2;

                    if (op == -1) {
                        throw new CompileException("illegal use of operator: " + tkOp.getName());
                    }


                    ASTNode tk2 = astLinkedList.nextNode();
                    BinaryOperation bo;

                    if (tk.getEgressType() == Integer.class && tk2.getEgressType() == Integer.class) {
                        switch (op) {
                            case Operator.ADD:
                                bo = new IntAdd(tk, tk2);
                                break;

                            case Operator.SUB:
                                bo = new IntSub(tk, tk2);
                                break;

                            case Operator.MULT:
                                bo = new IntMult(tk, tk2);
                                break;

                            case Operator.DIV:
                                bo = new IntDiv(tk, tk2);
                                break;

                            default:
                                bo = new BinaryOperation(op, tk, tk2, ctx);
                                break;

                        }
                    }
                    else {
                        bo = new BinaryOperation(op, tk, tk2, ctx);
                    }

                    tkOp2 = null;

                    /**
                     * If we have a chain of math/comparitive operators then we fill them into the tree
                     * right here.
                     */
                    while (astLinkedList.hasMoreNodes() && (tkOp2 = astLinkedList.nextNode()).isOperator()
                            && tkOp2.getFields() != -1 && (op2 = tkOp2.getOperator()) != -1 && op2 < 21) {

                        if (PTABLE[op2] > PTABLE[op]) {
                            bo.setRightMost(new BinaryOperation(op2, bo.getRightMost(), astLinkedList.nextNode(), ctx));
                        }
                        else if (bo.getOperation() != op2 && PTABLE[op] == PTABLE[op2]) {
                            if (PTABLE[bo.getOperation()] == PTABLE[op2]) {
                                bo = new BinaryOperation(op2, bo, astLinkedList.nextNode(), ctx);
                            }
                            else {
                                tk2 = astLinkedList.nextNode();

                                if (isIntOptimizationviolation(bo, tk2)) {
                                    bo = new BinaryOperation(bo.getOperation(), bo.getLeft(), bo.getRight(), ctx);
                                }

                                bo.setRight(new BinaryOperation(op2, bo.getRight(), tk2, ctx));
                            }
                        }
                        else if (PTABLE[bo.getOperation()] >= PTABLE[op2]) {
                            bo = new BinaryOperation(op2, bo, astLinkedList.nextNode(), ctx);
                        }
                        else {
                            tk2 = astLinkedList.nextNode();

                            if (isIntOptimizationviolation(bo, tk2)) {
                                bo = new BinaryOperation(bo.getOperation(), bo.getLeft(), bo.getRight(), ctx);
                            }
                            
                            bo.setRight(new BinaryOperation(op2, bo.getRight(), tk2, ctx));
                        }

                        op = op2;
                        tkOp = tkOp2;
                    }


                    if (tkOp2 != null && tkOp2 != tkOp) {
                        optimizeOperator(tkOp2.getOperator(), bo, tkOp2, astLinkedList, optimizedAst);
                    }
                    else {
                        optimizedAst.addTokenNode(bo);
                    }
                }
                else if (tkOp.isOperator()) {
                    optimizeOperator(tkOp.getOperator(), tk, tkOp, astLinkedList, optimizedAst);
                }
                else if (!tkOp.isAssignment() && !tkOp.isOperator() && tk.getLiteralValue() instanceof Class) {
                    optimizedAst.addTokenNode(new DeclTypedVarNode(tkOp.getName(), (Class) tk.getLiteralValue(), 0, ctx));
                }
                else if (tkOp.isAssignment() && tk.getLiteralValue() instanceof Class) {
                    tk.discard();
                    optimizedAst.addTokenNode(tkOp);
                }
                else if (astLinkedList.hasMoreNodes() && tkOp.getLiteralValue() instanceof Class
                        && astLinkedList.peekNode().isAssignment()) {
                    tkOp.discard();
                    optimizedAst.addTokenNode(tk, astLinkedList.nextNode());
                }
                else {
                    optimizedAst.addTokenNode(tk, tkOp);
                }
            }
            else {
                optimizedAst.addTokenNode(tk);
            }
        }

        if (secondPassOptimization) {
            /**
             * Perform a second pass optimization for boolean conditions.
             */
            (astLinkedList = optimizedAst).reset();
            optimizedAst = new ASTLinkedList();

            while (astLinkedList.hasMoreNodes()) {
                if ((tk = astLinkedList.nextNode()).getFields() == -1) {
                    optimizedAst.addTokenNode(tk);
                }
                else if (astLinkedList.hasMoreNodes()) {
                    if ((tkOp = astLinkedList.nextNode()).getFields() == -1) {
                        optimizedAst.addTokenNode(tk, tkOp);
                    }
                    else if (tkOp.isOperator()
                            && (tkOp.getOperator() == Operator.AND || tkOp.getOperator() == Operator.OR)) {

                        tkOp2 = null;
                        BooleanNode bool;

                        if (tkOp.getOperator() == Operator.AND) {
                            bool = new And(tk, astLinkedList.nextNode(), ctx.isStrongTyping());
                        }
                        else {
                            bool = new Or(tk, astLinkedList.nextNode(), ctx.isStrongTyping());
                        }

                        while (astLinkedList.hasMoreNodes() && (tkOp2 = astLinkedList.nextNode()).isOperator()
                                && (tkOp2.isOperator(Operator.AND) || tkOp2.isOperator(Operator.OR))) {

                            if ((tkOp = tkOp2).getOperator() == Operator.AND) {
                                bool.setRightMost(new And(bool.getRightMost(), astLinkedList.nextNode(), ctx.isStrongTyping()));
                            }
                            else {
                                bool = new Or(bool, astLinkedList.nextNode(), ctx.isStrongTyping());
                            }

                        }

                        optimizedAst.addTokenNode(bool);

                        if (tkOp2 != null && tkOp2 != tkOp) {
                            optimizedAst.addTokenNode(tkOp2);
                        }
                    }
                    else {
                        optimizedAst.addTokenNode(tk, tkOp);
                    }
                }
                else {
                    optimizedAst.addTokenNode(tk);
                }
            }
        }

        return optimizedAst;
    }

    private static void optimizeOperator(int operator, ASTNode tk, ASTNode tkOp,
                                         ASTLinkedList astLinkedList,
                                         ASTLinkedList optimizedAst) {

        switch (operator) {
            case Operator.REGEX:
                optimizedAst.addTokenNode(new RegExMatchNode(tk, astLinkedList.nextNode()));
                break;
            case Operator.CONTAINS:
                optimizedAst.addTokenNode(new Contains(tk, astLinkedList.nextNode()));
                break;
            case Operator.INSTANCEOF:
                optimizedAst.addTokenNode(new Instance(tk, astLinkedList.nextNode()));
                break;
            case Operator.CONVERTABLE_TO:
                optimizedAst.addTokenNode((new Convertable(tk, astLinkedList.nextNode())));
                break;
            case Operator.SIMILARITY:
                optimizedAst.addTokenNode(new Strsim(tk, astLinkedList.nextNode()));
                break;
            case Operator.SOUNDEX:
                optimizedAst.addTokenNode(new Soundslike(tk, astLinkedList.nextNode()));
                break;

            default:
                optimizedAst.addTokenNode(tk, tkOp);
        }
    }

    private static boolean isIntOptimizationviolation(BooleanNode bn, ASTNode bn2) {
        return (bn instanceof IntOptimized && bn2.getEgressType() != Integer.class);
    }

    /**
     * Returns an ordered Map of all functions declared within an compiled script.
     *
     * @param compile
     * @return - ordered Map
     */
    public static Map<String, Function> extractAllDeclaredFunctions(CompiledExpression compile) {
        Map<String, Function> allFunctions = new LinkedHashMap<String, Function>();
        ASTIterator instructions = new ASTLinkedList(compile.getFirstNode());

        ASTNode n;
        while (instructions.hasMoreNodes()) {
            if ((n = instructions.nextNode()) instanceof Function) {
                allFunctions.put(n.getName(), (Function) n);
            }
        }

        return allFunctions;
    }

    public static void expectType(Accessor expression, Class type, boolean compileMode) {
        Class retType = expression.getKnownEgressType();
        if (compileMode) {
            if ((retType == null || !boxPrimitive(type).isAssignableFrom(boxPrimitive(retType))) && (!Object.class.equals(retType)
                    || getCurrentThreadParserContext().isStrictTypeEnforcement())) {
                throw new CompileException("was expecting type: " + type.getName() + "; but found type: "
                        + (retType != null ? retType.getName() : "null"));
            }
        }
        else if (retType == null || !Object.class.equals(retType) && !boxPrimitive(type).isAssignableFrom(boxPrimitive(retType))) {
            throw new CompileException("was expecting type: " + type.getName() + "; but found type: "
                    + (retType != null ? retType.getName() : "null"));
        }
    }

    public static void expectType(ASTNode node, Class type, boolean compileMode) {
        Class retType = boxPrimitive(node.getEgressType());
        if (compileMode) {
            if ((retType == null || !boxPrimitive(type).isAssignableFrom(retType)) && (!Object.class.equals(retType) &&
                    (getCurrentThreadParserContext().isStrictTypeEnforcement()
                            || getCurrentThreadParserContext().isStrictTypeEnforcement()))) {
                throw new CompileException("was expecting type: " + type.getName() + "; but found type: "
                        + (retType != null ? retType.getName() : "null"));
            }
        }
        else if (retType == null || !Object.class.equals(retType) && !boxPrimitive(type).isAssignableFrom(retType)) {
            throw new CompileException("was expecting type: " + type.getName() + "; but found type: "
                    + (retType != null ? retType.getName() : "null"));
        }
    }

    public static Class getReturnTypeFromOp(int operation, Class left, Class right) {
        switch (operation) {
            case Operator.LETHAN:
            case Operator.LTHAN:
            case Operator.GETHAN:
            case Operator.GTHAN:
            case Operator.EQUAL:
            case Operator.NEQUAL:
            case Operator.AND:
            case Operator.OR:
            case Operator.CONTAINS:
            case Operator.CONVERTABLE_TO:
                return Boolean.class;

            case Operator.ADD:
                if (left == String.class) return String.class;
            case Operator.SUB:
            case Operator.MULT:
            case Operator.POWER:
            case Operator.MOD:
            case Operator.DIV:
                if (left == Object.class || right == Object.class)
                    return Object.class;
                else
                    return __resolveType(boxPrimitive(left)) < __resolveType(boxPrimitive(right)) ? right : left;

            case Operator.BW_AND:
            case Operator.BW_OR:
            case Operator.BW_XOR:
            case Operator.BW_SHIFT_RIGHT:
            case Operator.BW_SHIFT_LEFT:
            case Operator.BW_USHIFT_LEFT:
            case Operator.BW_USHIFT_RIGHT:
            case Operator.BW_NOT:
                return Integer.class;

            case Operator.STR_APPEND:
                return String.class;
        }
        return null;
    }

    public static Accessor extractAccessor(ASTNode n) {
        if (n instanceof LiteralNode) return new ExecutableLiteral(n.getLiteralValue());
        else return new ExecutableAccessor(n, n.getEgressType());
    }


    public static Map<String, Object> getInjectedImports(VariableResolverFactory factory) {
        if (factory == null) return null;
        do {
            if (factory instanceof ClassImportResolverFactory) {
                return ((ClassImportResolverFactory) factory).getImportedClasses();
            }
        }
        while ((factory = factory.getNextFactory()) != null);

        return null;
    }

}
