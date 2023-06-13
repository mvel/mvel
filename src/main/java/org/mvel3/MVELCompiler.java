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

package org.mvel3;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import org.mvel3.EvaluatorBuilder.EvaluatorInfo;
import org.mvel3.javacompiler.KieMemoryCompiler;
import org.mvel3.parser.printer.MVELToJavaRewriter;
import org.mvel3.parser.printer.PrintUtil;
import org.mvel3.transpiler.EvalPre;
import org.mvel3.transpiler.MVELTranspiler;
import org.mvel3.transpiler.TranspiledResult;
import org.mvel3.transpiler.context.Declaration;
import org.mvel3.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

import static org.mvel3.transpiler.MVELTranspiler.handleParserResult;

public class MVELCompiler {

    public <T, K, R> Evaluator<T, K, R> compile(EvaluatorInfo<T, K, R> info) {
        CompilationUnit unit = compileNoLoad(info);
        Evaluator<T, K, R> evaluator = compileEvaluator(unit, info);

        return evaluator;
    }
    public <T, K, R> TranspiledResult transpile(EvaluatorInfo<T, K, R> info) {
        EvalPre  evalPre;
        switch(info.variableInfo().declaration().type().getClazz().getSimpleName()){
            case "Map":
                evalPre = (evalInfo, context, statements) -> {
                    NodeList tempStmts = new NodeList<Statement>();
                    context.getInputs().stream().forEach(var -> {
                        Declaration declr = evalInfo.allVars().get(var);

                        MethodCallExpr methodCallExpr = new MethodCallExpr(new NameExpr(evalInfo.variableInfo().declaration().name()),
                                                                           "get", NodeList.nodeList(new StringLiteralExpr(declr.name())));
                        Type castType = handleParserResult(context.getParser().parseType(declr.type().getCanonicalGenericsName()));
                        CastExpr castExpr = new CastExpr(castType.clone(),
                                                         methodCallExpr);

                        VariableDeclarator varDeclr = new VariableDeclarator(castType, declr.name());
                        varDeclr.setInitializer(castExpr);
                        VariableDeclarationExpr varDeclExpr = new VariableDeclarationExpr(varDeclr);

                        tempStmts.add(new ExpressionStmt(varDeclExpr));
                    });

                    tempStmts.addAll(statements);

                    return tempStmts;
                };
                break;
            case "List":
                evalPre = (evalInfo, context, statements) -> {
                    NodeList tempStmts = new NodeList<Statement>();

                    for ( int i  = 0; i < evalInfo.variableInfo().vars().length; i++) {
                        Declaration declr = evalInfo.variableInfo().vars()[i];
                        if (context.getInputs().contains(declr.name())) {
                            MethodCallExpr methodCallExpr = new MethodCallExpr(new NameExpr(evalInfo.variableInfo().declaration().name()), "get", NodeList.nodeList(new IntegerLiteralExpr(i)));
                            CastExpr castExpr = new CastExpr(handleParserResult(context.getParser().parseType(declr.type().getCanonicalGenericsName())),
                                                             methodCallExpr);

                            Type               castType = handleParserResult(context.getParser().parseType(declr.type().getCanonicalGenericsName()));
                            VariableDeclarator varDeclr = new VariableDeclarator(castType, declr.name());
                            varDeclr.setInitializer(castExpr);
                            VariableDeclarationExpr varDeclExpr = new VariableDeclarationExpr(varDeclr);

                            tempStmts.add(new ExpressionStmt(varDeclExpr));
                        }
                    }

                    tempStmts.addAll(statements);

                    return tempStmts;
                };
                break;
            default: // pojo
                evalPre = (evalInfo, context, statements) -> {
                    NodeList tempStmts = new NodeList<Statement>();
                    context.getInputs().stream().forEach(var -> {
                        Declaration declr = evalInfo.allVars().get(var);

                        ResolvedType                     resolvedType = context.getFacade().getSymbolSolver().classToResolvedType(info.variableInfo().declaration().type().getClazz());
                        ResolvedReferenceTypeDeclaration d            = resolvedType.asReferenceType().getTypeDeclaration().get();

                        MethodUsage method = MVELToJavaRewriter.findGetterSetter("get", declr.name(), 0, d);

                        MethodCallExpr methodCallExpr = new MethodCallExpr(new NameExpr(info.variableInfo().declaration().name()), method.getName());

                        Type targetType = handleParserResult(context.getParser().parseType(declr.type().getCanonicalGenericsName()));

                        VariableDeclarator varDeclr = new VariableDeclarator(targetType, declr.name());
                        varDeclr.setInitializer(methodCallExpr);
                        VariableDeclarationExpr varDeclExpr = new VariableDeclarationExpr(varDeclr);

                        tempStmts.add(new ExpressionStmt(varDeclExpr));
                    });

                    tempStmts.addAll(statements);

                    return tempStmts;
                };
        }

        TranspiledResult input = MVELTranspiler.transpile(info, evalPre);

        return input;
    }

    private <T, K, R> CompilationUnit compileNoLoad(EvaluatorInfo<T, K, R> info) {
        TranspiledResult input = transpile(info);

        return new CompilationUnitGenerator(input.getTranspilerContext().getParser()).createEvaluatorUnit(input, info);
    }

    private <T, K, R> Evaluator<T, K, R> compileEvaluator(CompilationUnit unit, EvaluatorInfo<T, K, R> info) {
        String javaFQN = evaluatorFullQualifiedName(unit);
        ClassManager clsManager = info.classManager();
        if (clsManager == null) {
            clsManager = new ClassManager();
        }

        compileEvaluatorClass(clsManager, info.classLoader(), unit, javaFQN);

        Class<Evaluator<T, K, R>> evaluatorDefinition = clsManager.getClass(javaFQN);

        Evaluator<T, K, R> evaluator = createEvaluatorInstance(evaluatorDefinition);

        return evaluator;
    }


    private <T> T compileEvaluator(ClassManager classManager, ClassLoader classLoader, CompilationUnit unit) {
        String javaFQN = evaluatorFullQualifiedName(unit);

        compileEvaluatorClass(classManager, classLoader, unit, javaFQN);

        Class<T> evaluatorDefinition = classManager.getClass(javaFQN);
        T evaluator = createEvaluatorInstance(evaluatorDefinition);

        return evaluator;
    }

    public Method getMethod(Class contextClass, String var)  {
        Method method = null;
        try {
            String getterName = "get" + StringUtils.ucFirst(var);
            method = contextClass.getMethod(getterName);
        } catch (NoSuchMethodException e) {
            // swallow
        }

        try {
            method = contextClass.getMethod(var);
        } catch (NoSuchMethodException e) {
            // swallow
        }

        return method;
    }


    private String evaluatorFullQualifiedName(CompilationUnit evaluatorCompilationUnit) {
        ClassOrInterfaceDeclaration evaluatorClass = evaluatorCompilationUnit
                .findFirst(ClassOrInterfaceDeclaration.class)
                .orElseThrow(() -> new RuntimeException("class expected"));

        String evaluatorClassName = evaluatorClass.getNameAsString();
        Name packageName = evaluatorCompilationUnit.getPackageDeclaration().map(PackageDeclaration::getName)
                .orElseThrow(() -> new RuntimeException("No package in template"));
        return String.format("%s.%s", packageName, evaluatorClassName);
    }

    private <T> T createEvaluatorInstance(Class<T> evaluatorDefinition) {
        T evaluator;
        try {
            evaluator = (T) evaluatorDefinition.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return evaluator;
    }

    private void compileEvaluatorClass(ClassManager classManager, ClassLoader classLoader, CompilationUnit compilationUnit, String javaFQN) {
        Map<String, String> sources = Collections.singletonMap(
                javaFQN,
                PrintUtil.printNode(compilationUnit)
        );
        KieMemoryCompiler.compile(classManager, sources, classLoader);
    }
}
