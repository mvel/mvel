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
import com.github.javaparser.ast.stmt.BlockStmt;
import org.mvel3.transpiler.context.TranspilerContext;

import java.util.Set;

public interface TranspiledResult {

    BlockStmt getBlock();

    default CompilationUnit getUnit() {
        throw new UnsupportedOperationException();
    }

    NodeList<ImportDeclaration> getImports();

    // this overlaps with getUsedBindings, I've left above for now and will unify on this later.
    Set<String> getInputs();

    public TranspilerContext getTranspilerContext();


    Object methodBodyAsString();
}
