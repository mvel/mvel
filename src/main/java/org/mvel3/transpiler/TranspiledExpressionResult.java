/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mvel3.transpiler;

import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import org.mvel3.transpiler.context.TranspilerContext;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.mvel3.parser.printer.PrintUtil.printNode;

public class TranspiledExpressionResult implements TranspiledResult {

    private Expression expression;
    private Optional<Type> type;

    private Set<String> inputs;

    public TranspiledExpressionResult(Expression expression, Optional<Type> type, Set<String> inputs) {
        this.expression = expression;
        this.type = type;
        this.inputs = inputs;
    }

    public Expression getExpression() {
        return expression;
    }

    public Optional<Type> getType() {
        return type;
    }

    public NodeList<ImportDeclaration> getImports() {
        throw new UnsupportedOperationException();
    }

    public String methodBodyAsString() {
        return printNode(expression);
    }

    @Override
    public BlockStmt getBlock() {
        return new BlockStmt(NodeList.nodeList(new ExpressionStmt(expression)));
    }

    @Override
    public TranspilerContext getTranspilerContext() {
        return null;
    }

    @Override
    public Set<String> getInputs() {
        return inputs;
    }

}
