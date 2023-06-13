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

package org.mvel3.transpiler.context;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import org.mvel3.EvaluatorBuilder.EvaluatorInfo;
import org.mvel3.parser.MvelParser;
import org.mvel3.parser.printer.CoerceRewriter;
import org.mvel3.parser.printer.OverloadRewriter;

import java.util.HashSet;
import java.util.Set;

public class TranspilerContext<T, K, R> {

    private Set<String> inputs;

    private EvaluatorInfo<T, K, R> evaluatorInfo;

    private final MvelParser parser;

    private TypeSolver typeSolver;

    private JavaSymbolSolver symbolResolver;

    private ParserConfiguration parserConfiguration;

    private JavaParserFacade facade;

    private CompilationUnit unit;

    private ClassOrInterfaceDeclaration classDeclaration;

    private CoerceRewriter coercer;

    private OverloadRewriter overloader;

    public TranspilerContext(MvelParser parser, TypeSolver typeSolver, EvaluatorInfo<T, K, R> evaluatorInfo) {
        this.parser = parser;
        this.typeSolver = typeSolver;
        this.parserConfiguration = parser.getParserConfiguration();
        this.symbolResolver = (JavaSymbolSolver) parserConfiguration.getSymbolResolver().get();
        this.facade = JavaParserFacade.get(typeSolver);
        this.coercer = new CoerceRewriter(this);
        this.overloader = new OverloadRewriter(this);
        this.evaluatorInfo = evaluatorInfo;
        this.inputs = new HashSet<>();
    }

    public EvaluatorInfo<T, K, R> getEvaluatorInfo() {
        return evaluatorInfo;
    }

    public MvelParser getParser() {
        return parser;
    }

    public TypeSolver getTypeSolver() {
        return typeSolver;
    }

    public JavaParserFacade getFacade() {
        return facade;
    }

    public ParserConfiguration getParserConfiguration() {
        return parserConfiguration;
    }

    public JavaSymbolSolver getSymbolResolver() {
        return symbolResolver;
    }
    public Declaration findDeclarations(String name) {
        return evaluatorInfo.allVars().get(name);
    }

    public TranspilerContext addInput(String name) {
        this.inputs.add(name);
        return this;
    }

    public Set<String> getInputs() {
        return inputs;
    }

    public CompilationUnit getUnit() {
        return unit;
    }

    public void setUnit(CompilationUnit unit) {
        this.unit = unit;
    }

    public CoerceRewriter getCoercer() {
        return coercer;
    }

    public OverloadRewriter getOverloader() {
        return overloader;
    }

    public ClassOrInterfaceDeclaration getClassDeclaration() {
        return classDeclaration;
    }

    public void setClassDeclaration(ClassOrInterfaceDeclaration classDeclaration) {
        this.classDeclaration = classDeclaration;
    }
}
