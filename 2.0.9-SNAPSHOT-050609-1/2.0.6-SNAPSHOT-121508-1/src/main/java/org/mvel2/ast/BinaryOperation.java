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

package org.mvel2.ast;

import org.mvel2.CompileException;
import org.mvel2.DataConversion;
import org.mvel2.Operator;
import static org.mvel2.Operator.PTABLE;
import org.mvel2.ParserContext;
import org.mvel2.math.MathProcessor;
import static org.mvel2.debug.DebugTools.getOperatorSymbol;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.util.CompilerTools;
import static org.mvel2.util.CompilerTools.getReturnTypeFromOp;

public class BinaryOperation extends ASTNode {
    private final int operation;
    private ASTNode left;
    private ASTNode right;

    public BinaryOperation(int operation, ASTNode left, ASTNode right) {
        this.operation = operation;
        if ((this.left = left) == null) {
            throw new CompileException("not a statement");
        }
        if ((this.right = right) == null) {
            throw new CompileException("not a statement");
        }

        egressType = getReturnTypeFromOp(operation, left.egressType, right.egressType);
    }

    public BinaryOperation(int operation, ASTNode left, ASTNode right, ParserContext ctx) {
        this.operation = operation;
        if ((this.left = left) == null) {
            throw new CompileException("not a statement");
        }
        if ((this.right = right) == null) {
            throw new CompileException("not a statement");
        }

        if (ctx.isStrongTyping()) {
            switch (operation) {
                case Operator.ADD:
                    if (left.getEgressType() == String.class || right.getEgressType() == String.class) {
                        break;
                    }

                default:
                    if (!left.getEgressType().isAssignableFrom(right.getEgressType())) {
                        if (right.isLiteral() && DataConversion.canConvert(right.getEgressType(), left.getEgressType())) {
                            this.right = new LiteralNode(DataConversion.convert(right.getReducedValueAccelerated(null, null, null), left.getEgressType()));
                        }
                        else {
                            throw new CompileException("incompatible types in statement: " + right.getEgressType() + " (compared from: " + left.getEgressType() + ")");
                        }
                    }
            }
        }

        egressType = getReturnTypeFromOp(operation, left.egressType, right.egressType);
    }


    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return MathProcessor.doOperations(left.getReducedValueAccelerated(ctx, thisValue, factory), operation, right.getReducedValueAccelerated(ctx, thisValue, factory));
    }


    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        throw new RuntimeException("unsupported AST operation");
    }

    public int getOperation() {
        return operation;
    }

    public ASTNode getLeft() {
        return left;
    }

    public void setLeft(ASTNode left) {
        this.left = left;
    }

    public ASTNode getRight() {
        return right;
    }

    public ASTNode getRightMost() {
        BinaryOperation n = this;
        while (n.right != null && n.right instanceof BinaryOperation) {
            n = (BinaryOperation) n.right;
        }
        return n.right;
    }

    public BinaryOperation getRightBinary() {
        return right != null && right instanceof BinaryOperation ? (BinaryOperation) right : null;
    }

    public void setRight(ASTNode right) {
        this.right = right;
    }

    public void setRightMost(ASTNode right) {
        BinaryOperation n = this;
        while (n.right != null && n.right instanceof BinaryOperation) {
            n = (BinaryOperation) n.right;
        }
        n.right = right;
    }

    public int getPrecedence() {
        return PTABLE[operation];
    }

    public boolean isGreaterPrecedence(BinaryOperation o) {
        return o.getPrecedence() > PTABLE[operation];
    }

    public String toString() {
        return "(" + left + " " + getOperatorSymbol(operation) + " " + right + ")";
    }
}
