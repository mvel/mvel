package org.mvel3.transpiler;

import com.github.javaparser.ast.CompilationUnit;
import org.mvel3.parser.MvelParser;

public class DRLXTranspiler {
    public void transpileClass(String content, MvelParser parser) {
        CompilationUnit compilationUnit;
        System.out.println(content);
        // wrap as expression/block may or may not have {}, then unwrap latter.
        compilationUnit = MVELTranspiler.handleParserResult(parser.parse(content));
        System.out.println(compilationUnit);


//
//        VariableAnalyser analyser = new VariableAnalyser(context.getEvaluatorInfo().allVars().keySet());
//        blockStmt.accept(analyser, null);
//
//        if (!context.getEvaluatorInfo().rootDeclaration().type().isVoid() &&
//            !context.getEvaluatorInfo().rootDeclaration().equals(context.getEvaluatorInfo().variableInfo().declaration())) {
//            analyser.getUsed().add(context.getEvaluatorInfo().rootDeclaration().name());
//        }
//
//        analyser.getUsed().stream().forEach(v -> context.addInput(v));
//
//        preprocessPhase.removeEmptyStmt(blockStmt);
//
//        CompilationUnit unit = new CompilationUnit("org.mvel3");
//        context.setUnit(unit);
//
//        EvaluatorInfo<?, ?, ?> evalInfo = context.getEvaluatorInfo();
//
//        evalInfo.imports().stream().forEach(s -> unit.addImport(s));
//
//        evalInfo.staticImports().stream().forEach(s -> unit.addImport(s, true, false));
//
//        ContextInfo<?> ctxInf = evalInfo.variableInfo();
//
//        ClassOrInterfaceDeclaration classDeclaration = unit.addClass("GeneratorEvaluaor__");
//        context.setClassDeclaration(classDeclaration);
//
//
//        String implementedType = org.mvel3.Evaluator.class.getCanonicalName() + "<" +
//                                 ctxInf.declaration().type().getCanonicalGenericsName() + ", " +
//                                 evalInfo.rootDeclaration().type().getCanonicalGenericsName() + ", " +
//                                 evalInfo.outType().getCanonicalGenericsName() + "> ";
//        System.out.println(implementedType);
//        classDeclaration.addImplementedType(implementedType);
//
//        MethodDeclaration method = classDeclaration.addMethod("eval");
//        method.setPublic(true);
//
//        org.mvel3.Type outType = evalInfo.outType();
//        method.setType(handleParserResult(context.getParser().parseType(outType.getCanonicalGenericsName())));
//
//        method.addParameter(handleParserResult(context.getParser().parseType(ctxInf.declaration().type().getCanonicalGenericsName())), ctxInf.declaration().name());
//
//        NodeList<Statement> tempStmts = evalPre.evalPre(evalInfo, context, blockStmt.getStatements());
//        blockStmt.setStatements(tempStmts);
//
//        if (blockStmt.getStatements().size() == 1 && blockStmt.getStatement(0).isBlockStmt()) {
//            method.setBody(blockStmt.getStatement(0).asBlockStmt());
//        } else {
//            method.setBody(blockStmt);
//        }
//
//        context.getSymbolResolver().inject(unit);
//
//        MVELToJavaRewriter rewriter = new MVELToJavaRewriter(context);
//
//        rewriter.rewriteChildren(method.getBody().get());
//
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
//
//        System.out.println(PrintUtil.printNode(unit));

//        return new TranspiledBlockResult(unit, classDeclaration, method, context);

    }
}
