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
package org.mvel2.ast;

import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.util.CompilerTools;

public class And extends ASTNode {
    private ASTNode left;
    private ASTNode right;

    public And(ASTNode left, ASTNode right, boolean strongTyping) {
        CompilerTools.expectType(this.left = left, Boolean.class, strongTyping);
        CompilerTools.expectType(this.right = right, Boolean.class, strongTyping);
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return (((Boolean) left.getReducedValueAccelerated(ctx, thisValue, factory))
                && ((Boolean) right.getReducedValueAccelerated(ctx, thisValue, factory)));
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        throw new RuntimeException("improper use of AST element");
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

    public void setRight(ASTNode right) {
        this.right = right;
    }

    public String toString() {
        return "(" + left.toString() + " && " + right.toString() + ")";
    }

    public Class getEgressType() {
        return Boolean.class;
    }
}


