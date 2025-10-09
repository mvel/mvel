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

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.mvel3.ContentType;
import org.mvel3.CompilerParameters;
import org.mvel3.parser.MvelParser;
import org.mvel3.parser.printer.PrintUtil;
import org.mvel3.transpiler.context.TranspilerContext;

import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_15;

public class MVELTranspiler {
    private static final Logger logger = LoggerFactory.getLogger(MVELTranspiler.class);

    // development flag. Will be added to CompilerParameters later
    public static boolean ENABLE_REWRITE = true;

    private TranspilerContext context;

    public MVELTranspiler(TranspilerContext context) {
        this.context = context;
    }

    public static <T, K, R>  TranspiledResult transpile(CompilerParameters<T, K, R> evalInfo, EvalPre evalPre) {

        TypeSolver typeSolver = new ReflectionTypeSolver(false);
        JavaSymbolSolver solver = new JavaSymbolSolver(typeSolver);

        ParserConfiguration conf = new ParserConfiguration();
        conf.setLanguageLevel(JAVA_15);
        conf.setSymbolResolver(solver);

        MvelParser parser = MvelParser.Factory.get(conf);

        TranspilerContext context = new TranspilerContext(parser, typeSolver, evalInfo);

        MVELTranspiler mvelTranspiler = new MVELTranspiler(context);

        TranspiledResult transpiledResult =  mvelTranspiler.transpileContent(evalInfo, evalPre);

        return transpiledResult;
    }

    public static <T> T handleParserResult(ParseResult<T> result) {
        if (result.isSuccessful()) {
            return result.getResult().get();
        } else {
            throw new ParseProblemException(result.getProblems());
        }
    }

    public <T, K, R> TranspiledBlockResult transpileContent(CompilerParameters<T, K, R> evalInfo, EvalPre evalPre) {
        BlockStmt blockStmt;
        String content = evalInfo.expression();
        System.out.println(content);


        if (evalInfo.contentType() == ContentType.BLOCK) {
            blockStmt = handleParserResult(context.getParser().parseBlock("{" + content + "}\n"));
        } else {
            Expression expr = handleParserResult(context.getParser().parseExpression(content));
            if (context.getEvaluatorInfo().outType().isVoid()) {
                ExpressionStmt exprStmt = new ExpressionStmt(expr);
                blockStmt = new  BlockStmt(NodeList.nodeList(exprStmt));
            } else {
                ReturnStmt returnStmt = new ReturnStmt(expr);
                blockStmt = new  BlockStmt(NodeList.nodeList(returnStmt));
            }
        }

        VariableAnalyser analyser = new VariableAnalyser(context.getEvaluatorInfo().allVars().keySet());
        blockStmt.accept(analyser, null);

        if (!context.getEvaluatorInfo().withDeclaration().type().isVoid() &&
            !context.getEvaluatorInfo().withDeclaration().equals(context.getEvaluatorInfo().contextDeclaration())) {
            analyser.getUsed().add(context.getEvaluatorInfo().withDeclaration().name());
        }

        analyser.getUsed().stream().forEach(v -> context.addInput(v));

        CompilationUnit unit = new CompilationUnit(context.getGeneratedPackageName());
        context.setUnit(unit);

        evalInfo.imports().stream().forEach(s -> unit.addImport(s));

        evalInfo.staticImports().stream().forEach(s -> unit.addImport(s, true, false));

        ClassOrInterfaceDeclaration classDeclaration = unit.addClass(context.getEvaluatorInfo().generatedClassName());

        if (context.getEvaluatorInfo().generatedSuperName() != null) {
            classDeclaration.addExtendedType(context.getEvaluatorInfo().generatedSuperName());
        }

        context.setClassDeclaration(classDeclaration);

        String implementedType = org.mvel3.Evaluator.class.getCanonicalName() + "<" +
                                 evalInfo.contextDeclaration().type().getCanonicalGenericsName() + ", " +
                                 evalInfo.withDeclaration().type().getCanonicalGenericsName() + ", " +
                                 evalInfo.outType().getCanonicalGenericsName() + "> ";
        logger.trace("Implemented type: {}", implementedType);
        classDeclaration.addImplementedType(implementedType);

        MethodDeclaration method = classDeclaration.addMethod(context.getEvaluatorInfo().generatedMethodName());
        method.setPublic(true);

        org.mvel3.Type outType = evalInfo.outType();
        if (!outType.isVoid()) {
            method.setType(handleParserResult(context.getParser().parseType(outType.getCanonicalGenericsName())));
        }

        method.addParameter(handleParserResult(context.getParser().parseType(evalInfo.contextDeclaration().type().getCanonicalGenericsName())), evalInfo.contextDeclaration().name());

        NodeList<Statement> tempStmts = evalPre.evalPre(evalInfo, context, blockStmt.getStatements());
        blockStmt.setStatements(tempStmts);

        if (blockStmt.getStatements().size() == 1 && blockStmt.getStatement(0).isBlockStmt()) {
            method.setBody(blockStmt.getStatement(0).asBlockStmt());
        } else {
            method.setBody(blockStmt);
        }

        logger.debug("CompilationUnit before rewriting:\n{}", unit.toString());

        context.getSymbolResolver().inject(unit);

        if (ENABLE_REWRITE) {
            MVELToJavaRewriter rewriter = new MVELToJavaRewriter(context);
            rewriter.rewriteChildren(method.getBody().get());
        }

//        // Inject the "return" if one is needed and it's missing and it's a statement expression.
//        // This will not check branchs of an if statement or for loop, those need explicit returns
//        Statement stmt = method.getBody().get().getStatements().getLast().get();
//        if (evalInfo.outType().isVoid()) {
//            ReturnStmt returnStmt = new ReturnStmt(new NullLiteralExpr());
//            method.getBody().get().getStatements().add(returnStmt);
//        } else if (stmt.isExpressionStmt() && method.getType() != null) {
//            if (stmt.asExpressionStmt().getExpression().isVariableDeclarationExpr()) {
//                ReturnStmt returnStmt = new ReturnStmt( new NameExpr(stmt.asExpressionStmt().getExpression().asVariableDeclarationExpr().getVariables().get(0).getNameAsString()));
//                method.getBody().get().getStatements().add(returnStmt);
//            } else {
//                ReturnStmt returnStmt = new ReturnStmt(stmt.asExpressionStmt().getExpression());
//                stmt.replace(returnStmt);
//            }
//        }

        logger.debug("Generated compilation unit:\n{}", PrintUtil.printNode(unit));

        return new TranspiledBlockResult(unit, classDeclaration, method, context);
    }
}
