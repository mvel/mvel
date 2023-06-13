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

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import org.mvel3.transpiler.context.TranspilerContext;

import java.util.Set;

import static org.mvel3.parser.printer.PrintUtil.printNode;

public class TranspiledBlockResult implements TranspiledResult {

    private CompilationUnit unit;

    private ClassOrInterfaceDeclaration clazz;
    private MethodDeclaration method;

    private TranspilerContext<?, ?, ?> context;

    public TranspiledBlockResult(CompilationUnit unit, ClassOrInterfaceDeclaration clazz, MethodDeclaration method, TranspilerContext context) {
        this.unit = unit;
        this.clazz = clazz;
        this.method = method;
        this.context = context;
    }

    public String methodBodyAsString() {
        StringBuilder sbuilder = new StringBuilder();
        NodeList<Statement> stmts = method.getBody().get().getStatements();
        int subListStart = context.getInputs().size();
        if ( !context.getEvaluatorInfo().rootDeclaration().type().isVoid()) {
            // root vars are not assigned at start, so remove that from the count
            subListStart--;
        }
        for (Statement stmt : stmts.subList(subListStart, stmts.size())) {
            sbuilder.append(printNode(stmt, context.getTypeSolver()));
        }

        return sbuilder.toString();
    }

    @Override
    public BlockStmt getBlock() {
        return method.getBody().get();
    }

    public CompilationUnit getUnit() {
        return context.getUnit();
    }

    public ClassOrInterfaceDeclaration getClassDeclaration() {
        return clazz;
    }

    public NodeList<ImportDeclaration> getImports() {
        return unit.getImports();
    }

    @Override
    public Set<String> getInputs() {
        return context.getInputs();
    }

    public TranspilerContext getTranspilerContext() {
        return context;
    }

    @Override
    public String toString() {
        return "ParsingResult{" +
               "statements='" + methodBodyAsString() + '\'' +
               '}';
    }
}
