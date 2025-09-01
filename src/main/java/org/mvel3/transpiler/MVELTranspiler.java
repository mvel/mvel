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
import org.mvel3.EvaluatorBuilder.ContextInfo;
import org.mvel3.EvaluatorBuilder.EvaluatorInfo;
import org.mvel3.parser.MvelParser;
import org.mvel3.parser.printer.MVELToJavaRewriter;
import org.mvel3.parser.printer.PrintUtil;
import org.mvel3.transpiler.context.TranspilerContext;

import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_15;

public class MVELTranspiler {
    private static final Logger logger = LoggerFactory.getLogger(MVELTranspiler.class);

    private final PreprocessPhase preprocessPhase = new PreprocessPhase();

    private TranspilerContext context;

    public MVELTranspiler(TranspilerContext context) {
        this.context = context;
    }

    public static <T, K, R>  TranspiledResult transpile(EvaluatorInfo<T, K, R> evalInfo, EvalPre evalPre) {

        TypeSolver typeSolver = new ReflectionTypeSolver(false);
        JavaSymbolSolver solver = new JavaSymbolSolver(typeSolver);

        ParserConfiguration conf = new ParserConfiguration();
        conf.setLanguageLevel(JAVA_15);
        conf.setSymbolResolver(solver);

        MvelParser parser = MvelParser.Factory.get(conf);

        TranspilerContext context = new TranspilerContext(parser, typeSolver, evalInfo);

        MVELTranspiler mvelTranspiler = new MVELTranspiler(context);

        TranspiledResult transpiledResult =  mvelTranspiler.transpileBlock(evalInfo.expression(), evalPre);

        return transpiledResult;
    }

    public static <T> T handleParserResult(ParseResult<T> result) {
        if (result.isSuccessful()) {
            return result.getResult().get();
        } else {
            throw new ParseProblemException(result.getProblems());
        }
    }

    public TranspiledBlockResult transpileBlock(String content, EvalPre evalPre) {
        BlockStmt blockStmt;
        logger.debug("Transpiling block: {}", content);

        // This is a terrible hack, we either need a better way to snoop, or force the user to specify expression or block

// Can we remove the try/catch? Statement can be expression in JavaParser.g4
        try {
            // wrap as expression/block may or may not have {}, then unwrap latter.xs
            blockStmt = handleParserResult(context.getParser().parseBlock("{" + content + "}\n"));
        } catch (RuntimeException eParseBlock) {
            logger.debug("Failed parseBlock, trying parseExpression. Error: {}", eParseBlock.getMessage());
            // Block failed, try parsing an expression
            Expression expr;
            try {
                expr = handleParserResult(context.getParser().parseExpression(content));
            } catch (RuntimeException eParseExpression) {
                logger.error("Stack trace at parseBlock : ", eParseBlock);
                throw eParseExpression;
            }
            if (context.getEvaluatorInfo().outType().isVoid()) {
                ExpressionStmt exprStmt = new ExpressionStmt(expr);
                blockStmt = new  BlockStmt(NodeList.nodeList(exprStmt));
            } else {
                ReturnStmt returnStmt = new ReturnStmt(expr);
                blockStmt = new  BlockStmt(NodeList.nodeList(returnStmt));
            }
        }

        // === Use this for parseExpression only ===
//        Expression expr;
//        try {
//            expr = handleParserResult(context.getParser().parseExpression(content));
//        } catch (RuntimeException eParseExpression) {
//            throw eParseExpression;
//        }
//        if (context.getEvaluatorInfo().outType().isVoid()) {
//            ExpressionStmt exprStmt = new ExpressionStmt(expr);
//            blockStmt = new  BlockStmt(NodeList.nodeList(exprStmt));
//        } else {
//            ReturnStmt returnStmt = new ReturnStmt(expr);
//            blockStmt = new  BlockStmt(NodeList.nodeList(returnStmt));
//        }

        logger.debug("Initial parsed block:\n{}", blockStmt.toString());

        VariableAnalyser analyser = new VariableAnalyser(context.getEvaluatorInfo().allVars().keySet());
        blockStmt.accept(analyser, null);

        if (!context.getEvaluatorInfo().rootDeclaration().type().isVoid() &&
            !context.getEvaluatorInfo().rootDeclaration().equals(context.getEvaluatorInfo().variableInfo().declaration())) {
            analyser.getUsed().add(context.getEvaluatorInfo().rootDeclaration().name());
        }

        analyser.getUsed().stream().forEach(v -> context.addInput(v));

        preprocessPhase.removeEmptyStmt(blockStmt);

        CompilationUnit unit = new CompilationUnit(context.getGeneratedPackageName());
        context.setUnit(unit);

        EvaluatorInfo<?, ?, ?> evalInfo = context.getEvaluatorInfo();

        evalInfo.imports().stream().forEach(s -> unit.addImport(s));

        evalInfo.staticImports().stream().forEach(s -> unit.addImport(s, true, false));

        ContextInfo<?> ctxInf = evalInfo.variableInfo();

        ClassOrInterfaceDeclaration classDeclaration = unit.addClass(context.getEvaluatorInfo().generatedClassName());

        if (context.getEvaluatorInfo().generatedSuperName() != null) {
            classDeclaration.addExtendedType(context.getEvaluatorInfo().generatedSuperName());
        }

        context.setClassDeclaration(classDeclaration);

        String implementedType = org.mvel3.Evaluator.class.getCanonicalName() + "<" +
                                 ctxInf.declaration().type().getCanonicalGenericsName() + ", " +
                                 evalInfo.rootDeclaration().type().getCanonicalGenericsName() + ", " +
                                 evalInfo.outType().getCanonicalGenericsName() + "> ";
        logger.trace("Implemented type: {}", implementedType);
        classDeclaration.addImplementedType(implementedType);

        MethodDeclaration method = classDeclaration.addMethod(context.getEvaluatorInfo().generatedMethodName());
        method.setPublic(true);

        org.mvel3.Type outType = evalInfo.outType();
        method.setType(handleParserResult(context.getParser().parseType(outType.getCanonicalGenericsName())));

        method.addParameter(handleParserResult(context.getParser().parseType(ctxInf.declaration().type().getCanonicalGenericsName())), ctxInf.declaration().name());

        NodeList<Statement> tempStmts = evalPre.evalPre(evalInfo, context, blockStmt.getStatements());
        blockStmt.setStatements(tempStmts);

        if (blockStmt.getStatements().size() == 1 && blockStmt.getStatement(0).isBlockStmt()) {
            method.setBody(blockStmt.getStatement(0).asBlockStmt());
        } else {
            method.setBody(blockStmt);
        }

        logger.debug("CompilationUnit before rewriting:\n{}", unit.toString());

        context.getSymbolResolver().inject(unit);

        MVELToJavaRewriter rewriter = new MVELToJavaRewriter(context);

        rewriter.rewriteChildren(method.getBody().get());

        // Inject the "return" if one is needed and it's missing and it's a statement expression.
        // This will not check branchs of an if statement or for loop, those need explicit returns
        Statement stmt = method.getBody().get().getStatements().getLast().get();
        if (evalInfo.outType().isVoid()) {
            ReturnStmt returnStmt = new ReturnStmt(new NullLiteralExpr());
            method.getBody().get().getStatements().add(returnStmt);
        } else if (stmt.isExpressionStmt() && method.getType() != null) {
            if (stmt.asExpressionStmt().getExpression().isVariableDeclarationExpr()) {
                ReturnStmt returnStmt = new ReturnStmt( new NameExpr(stmt.asExpressionStmt().getExpression().asVariableDeclarationExpr().getVariables().get(0).getNameAsString()));
                method.getBody().get().getStatements().add(returnStmt);
            } else {
                ReturnStmt returnStmt = new ReturnStmt(stmt.asExpressionStmt().getExpression());
                stmt.replace(returnStmt);
            }
        }

        logger.debug("Generated compilation unit:\n{}", PrintUtil.printNode(unit));

        return new TranspiledBlockResult(unit, classDeclaration, method, context);
    }
}
