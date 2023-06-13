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

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.AssignExpr.Operator;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import org.mvel3.EvaluatorBuilder.EvaluatorInfo;
import org.mvel3.parser.MvelParser;
import org.mvel3.parser.printer.PrintUtil;
import org.mvel3.transpiler.TranspiledResult;
import org.mvel3.transpiler.context.Declaration;
import org.mvel3.util.StringUtils;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CompilationUnitGenerator {

    private CompilationUnit template;
    private ClassOrInterfaceDeclaration evaluatorClass;
    private MethodDeclaration methodDeclaration;
    private BlockStmt methodBody;
    private BlockStmt bindingAssignmentBlock;
    private BlockStmt mvelExecutionBlock;
    private BlockStmt repopulateMapBlock;
    private Statement lastMVELStatement;
    private Statement lastBodyStatement;

    private static final String RESULT_VALUE_REFERENCE_NAME = "___resultValue";

    private static final String ROOT_PREFIX = "___this";



    private MvelParser parser;

    public CompilationUnitGenerator(MvelParser parser) {
        this.parser = parser;
    }

//    public CompilationUnit createCompilationUnit(String originalExpression, TranspiledResult input, Map<String, org.mvel3.Type> types, String... returnVars) {
//        loadTemplate("MapEvaluatorTemplate");
//        renameTemplateClass(originalExpression, "MapEvaluatorTemplate");
//        clearExamples();
//
//        createMapContextVariableAssignments(types.entrySet(), input.getInputs(), returnVars);
//
//        addImports(input);
//        rewriteBody(input, org.mvel3.Type.type(Object.class));
//
//        return template;
//    }

    public <T, K, R> CompilationUnit createMapEvaluatorUnit(TranspiledResult input, EvaluatorInfo<T, K, R> info) {
//        loadTemplate("MapEvaluatorTemplate");
//        renameTemplateClass(info.expression(), "MapEvaluatorTemplate");
//        clearExamples();
//
//
//        createMapContextVariableAssignments(info.allVars(), input.getInputs(), input.getInputs().toArray(new String[0]));
//
//        addImports(input);
//        rewriteBody(input, org.mvel3.Type.type(Object.class));

        return input.getUnit();//template;
    }

    public <T, K, R> CompilationUnit createArrayEvaluatorUnit(TranspiledResult input, EvaluatorInfo<T, K, R> info) {
//        loadTemplate("ArrayEvaluatorTemplate");
//        renameTemplateClass(info.expression(), "ArrayEvaluatorTemplate");
//
//        clearExamples();
//
//        createArrayContextVariableAssignments(info.allVars().values().toArray(new Declaration[0]),
//                                              info.allVars().keySet().toArray(new String[0]));
//
//        addImports(input);
//        rewriteBody(input, org.mvel3.Type.type(Object.class));
//
//        return template;
        return input.getUnit();//template;
    }

    private <R> void rewriteBody(TranspiledResult input, org.mvel3.Type<R> returnClass) {
        BlockStmt compiledMVELBlock = input.getBlock();
        mvelExecutionBlock.replace(compiledMVELBlock);

        lastMVELStatement = lastStatementOfABlock(compiledMVELBlock);
        lastBodyStatement = lastStatementOfABlock(methodBody);

        defineLastStatement(compiledMVELBlock, returnClass);

        logGenerateClass();
    }

    public <T, K, R> CompilationUnit createPojoEvaluatorUnit(TranspiledResult input, EvaluatorInfo<T, K, R> info) {
        loadTemplate("PojoEvaluatorTemplate");
        renameTemplateClass(info.expression(), "PojoEvaluatorTemplate");

        clearExamples();

        Type contextType = null;
        Type returnType = null;
        ParseResult<Type> parseTypeResults = parser.parseType(info.variableInfo().declaration().type().getCanonicalGenericsName());
        if (parseTypeResults.isSuccessful()){
            contextType = parseTypeResults.getResult().get();
        }

        parseTypeResults = parser.parseType(info.variableInfo().declaration().type().getCanonicalGenericsName());
        if (parseTypeResults.isSuccessful()){
            returnType = parseTypeResults.getResult().get();
        }

        returnType = new ClassOrInterfaceType(info.outType().getCanonicalGenericsName());
        evaluatorClass.getImplementedTypes(0).setTypeArguments(contextType, returnType);
        methodDeclaration.getParameter(0).setType(contextType);
        methodDeclaration.setType(info.outType().getCanonicalGenericsName());

        createPojoContextVariableAssignments(info);

        addImports(input);
        rewriteBody(input, info.outType());

        return template;
    }

    public <T, K, R> CompilationUnit createRootObjectEvaluatorUnit(TranspiledResult input, EvaluatorInfo<T, K, R> info) {
        loadTemplate("PojoEvaluatorTemplate");
        renameTemplateClass(info.expression(), "PojoEvaluatorTemplate");

        clearExamples();

        ParseResult<Type> rootTypeResult = parser.parseType(info.rootDeclaration().type().getCanonicalGenericsName());
        if (!rootTypeResult.isSuccessful()) {
            throwParserException(info.rootDeclaration().type(), rootTypeResult);
        }

        ParseResult<Type> outTypeResult = parser.parseType(info.rootDeclaration().type().getCanonicalGenericsName());
        if (!rootTypeResult.isSuccessful()) {
            throwParserException(info.rootDeclaration().type(), rootTypeResult);
        }

        Type rootType = rootTypeResult.getResult().get();
        Type outType = outTypeResult.getResult().get();

        evaluatorClass.getImplementedTypes(0).setTypeArguments(rootType, outType);

        methodDeclaration.getParameter(0).setType(rootType);
        methodDeclaration.setType(outType);

        ParseResult<Type> result = parser.parseType(info.rootDeclaration().type().getCanonicalGenericsName());
        if (result.isSuccessful()) {
            Type type = result.getResult().get();
            VariableDeclarationExpr variable = new VariableDeclarationExpr(type, ROOT_PREFIX);
            variable.getVariable(0).setInitializer(new NameExpr("pojo"));
//                Expression indexMethodExpression = new CastExpr(type, new MethodCallExpr(new NameExpr("pojo"), method.getName()));
//                methodBody.addStatement(0, variable);
//
//                final Expression expr = new AssignExpr(new NameExpr(binding), new NameExpr("pojo"), AssignExpr.Operator.ASSIGN);
            bindingAssignmentBlock.addStatement(variable);
        } else {
            throwParserException(info.rootDeclaration().type(), rootTypeResult);
        }

        addImports(input);
        rewriteBody(input, info.outType());

        return template;
    }

    private void addImports(TranspiledResult input) {
        NodeList<ImportDeclaration> imports = input.getImports();

        if (imports != null) {
            for (ImportDeclaration i : imports) {
                template.addImport(i.clone());
            }
        }
    }

    private Statement lastStatementOfABlock(BlockStmt mvelBlock) {
        NodeList<Statement> statements = mvelBlock.getStatements();
        return statements.get(statements.size() - 1);
    }

    private void clearExamples() {
        for (VariableDeclarationExpr variableDeclarationExpr : methodBody.findAll(VariableDeclarationExpr.class)) {
            variableDeclarationExpr.getParentNode().ifPresent(Node::remove);
        }
    }

    private void renameTemplateClass(String originalExpression, String templateName) {
        String newName = String.format("Evaluator%s", StringUtils.md5Hash(originalExpression));
        replaceSimpleNameWith(evaluatorClass, templateName, newName);
    }

    private void logGenerateClass() {
        System.out.println(PrintUtil.printNode(template));
        //LOG.debug(PrintUtil.printNode(template));
    }

    // Simulate "Last expression is a return statement"
    private <T> void defineLastStatement(BlockStmt mvelBlock, org.mvel3.Type<T> returnClass) {
        Expression expression;
        if (lastMVELStatement.isReturnStmt()) {
            expression = ((ReturnStmt)lastMVELStatement).getExpression().get();
        } else {
            expression = lastMVELStatement.asExpressionStmt().getExpression();
        }

        addResultReference(returnClass);

        if(expression.isMethodCallExpr() && expression.asMethodCallExpr().getNameAsString().startsWith("set")) {
            lastStatementIsGetter(mvelBlock, expression);
        } else {
            final Expression assignExpr = new AssignExpr(new NameExpr(RESULT_VALUE_REFERENCE_NAME), expression, AssignExpr.Operator.ASSIGN);
            mvelBlock.replace(lastMVELStatement, new ExpressionStmt(assignExpr));
        }

        returnResult();
    }

    private void lastStatementIsGetter(BlockStmt mvelBlock, Expression expression) {
        MethodCallExpr methodCallExprClone = expression.asMethodCallExpr().clone();
        String getterName = methodCallExprClone.getName().asString().replace("set", "get");
        MethodCallExpr getter = new MethodCallExpr(getterName);
        methodCallExprClone.getScope().ifPresent(getter::setScope);
        final Expression assignExpr = new AssignExpr(new NameExpr(RESULT_VALUE_REFERENCE_NAME), getter, AssignExpr.Operator.ASSIGN);
        mvelBlock.addStatement(assignExpr);
    }

    private void returnResult() {
        //ReturnStmt node = new ReturnStmt(new NameExpr(RESULT_VALUE_REFERENCE_NAME));
        ((CastExpr)((ReturnStmt)lastBodyStatement).getExpression().get()).setExpression(new NameExpr(RESULT_VALUE_REFERENCE_NAME));
        //lastBodyStatement.replace(node);
    }

    private <T> void addResultReference(org.mvel3.Type<T> type) {
        ParseResult<Type> result = parser.parseType(type.getCanonicalGenericsName());
        if (result.isSuccessful()) {

            VariableDeclarationExpr returnVariable = new VariableDeclarationExpr(result.getResult().get(),
                                                                                 RESULT_VALUE_REFERENCE_NAME);
            methodBody.addStatement(0, returnVariable);
        } else {
            throwParserException(type.getClazz(), type.getGenerics(), result);
        }
    }
    private static <T> void throwParserException(org.mvel3.Type<T> type, ParseResult<Type> result) {
        throwParserException(type.getCanonicalGenericsName(), result);
    }

    private static void throwParserException(Class outClass, String outGenerics, ParseResult<Type> result) {
        throwParserException(outClass.getCanonicalName() + outGenerics, result);
    }

    private static void throwParserException(String type, ParseResult<Type> result) {
        throw new RuntimeException("Unable to parser type: " + type + "\n" + result.getProblems().toString());
    }

    private void createMapContextVariableAssignments(Map<String, Declaration> entries, Set<String> inputs, String[] returnVars) {
        Set<String> returnSet = new HashSet<>(Arrays.asList(returnVars));


        for (Map.Entry<String, Declaration> entry : entries.entrySet()) {
            String variable = entry.getKey();
            if (!inputs.contains(variable)) {
                continue;
            }
            Declaration declr = entry.getValue();

            org.mvel3.Type contextVarType = declr.type();
            if (contextVarType.getClazz().getCanonicalName() != null) {
                ParseResult<Type> result = parser.parseType(contextVarType.getClazz().getCanonicalName() + contextVarType.getGenerics());
                if (result.isSuccessful()) {
                    Type type = result.getResult().get();
                    VariableDeclarationExpr varDecl = new VariableDeclarationExpr(type, variable);
                    Expression indexMethodExpression = new CastExpr(type, new MethodCallExpr(new NameExpr("map"), "get", NodeList.nodeList(new StringLiteralExpr(variable))));
                    methodBody.addStatement(0, varDecl);

                    final Expression expr = new AssignExpr(new NameExpr(variable), indexMethodExpression, AssignExpr.Operator.ASSIGN);
                    bindingAssignmentBlock.addStatement(expr);

                    if (returnSet.contains(variable)) {
                        MethodCallExpr putExpr = new MethodCallExpr(new NameExpr("map"), "put", NodeList.nodeList(new StringLiteralExpr(variable), new NameExpr(variable)));
                        repopulateMapBlock.addStatement(putExpr);
                    }
                } else {
                    throwParserException(contextVarType.getClazz(), contextVarType.getGenerics(), result);
                }
            }
        }
    }

    private void createArrayContextVariableAssignments(Declaration[] types, String... returnVars) {
        Set<String> returnSet = new HashSet<>(Arrays.asList(returnVars));

        for (int i = 0; i < types.length; i++) {
            Class<?> contextVarClass = types[i].type().getClazz();
            String binding = types[i].name();
            ParseResult<Type> result = parser.parseType(types[i].type().getClazz().getCanonicalName() + types[i].type().getGenerics());
            if (contextVarClass.getCanonicalName() != null) {
                if (result.isSuccessful()) {
                    Type type = result.getResult().get();
                    VariableDeclarationExpr variable = new VariableDeclarationExpr(type, binding);

                    Expression indexMethodExpression = new CastExpr(type, new ArrayAccessExpr(new NameExpr("array"), new IntegerLiteralExpr(i)));

                    methodBody.addStatement(0, variable);


                    final Expression expr = new AssignExpr(new NameExpr(binding), indexMethodExpression, AssignExpr.Operator.ASSIGN);
                    bindingAssignmentBlock.addStatement(expr);

                    if (returnSet.contains(binding)) {
                        AssignExpr putExpr = new AssignExpr(new ArrayAccessExpr(new NameExpr("array"), new IntegerLiteralExpr(i)),
                                                            new NameExpr(binding),
                                                            Operator.ASSIGN);

                        repopulateMapBlock.addStatement(putExpr);
                    }
                } else {
                    throwParserException(types[i].type().getClazz(), types[i].type().getGenerics(), result);
                }
            }
        }
    }

    private <T, K, R> void createPojoContextVariableAssignments(EvaluatorInfo<T, K, R> info) {
        for (Declaration declr : info.variableInfo().vars()) {
            Class<?> contextVarClass = declr.type().getClass();

            // get getter Name
            ParseResult<Type> result = parser.parseType(declr.type().getClazz().getCanonicalName() + declr.type().getGenerics());
            if (result.isSuccessful()) {
                Type type = result.getResult().get();
                Method method = findAccessor(declr.type().getClazz(), declr.name());
                VariableDeclarationExpr variable = new VariableDeclarationExpr(type, declr.name());
                Expression indexMethodExpression = new CastExpr(type, new MethodCallExpr(new NameExpr("pojo"), method.getName()));

                methodBody.addStatement(0, variable);

                final Expression expr = new AssignExpr(new NameExpr(declr.name()),
                                                       indexMethodExpression, AssignExpr.Operator.ASSIGN);
                bindingAssignmentBlock.addStatement(expr);
            } else {
                throwParserException(declr.type().getClazz().getName(), result);
            }
        }
    }

    public Method findAccessor(Class clazz, String property) {
        Method method = null;
        try {
            String name = "get" + property.substring(0, 1).toUpperCase() + property.substring(1);
            method = clazz.getDeclaredMethod(name);
        } catch (NoSuchMethodException e) {
            // swallow, we tried getPropertyName() this time, will try propertyName() next.
        }

        if (method == null) {
            try {
                method = clazz.getDeclaredMethod(property);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        return method;

    }

    private void loadTemplate(String templateName) {
        template = getMethodTemplate(templateName);
        evaluatorClass = template.getClassByName(templateName)
                .orElseThrow(() -> new RuntimeException("Cannot find class"));
        methodDeclaration = evaluatorClass.findFirst(MethodDeclaration.class)
                .orElseThrow(() -> new RuntimeException("cannot find Method"));
        methodBody = methodDeclaration.findFirst(BlockStmt.class)
                .orElseThrow(() -> new RuntimeException("cannot find method body"));

        bindingAssignmentBlock = findBlock(" binding assignment");
        mvelExecutionBlock = findBlock(" execute MVEL here");
        repopulateMapBlock = findBlock(" repopulate map");
    }

    private BlockStmt findBlock(String comment) {
        BlockStmt block = methodBody.findFirst(BlockStmt.class, b -> blockHasComment(b, comment))
                .orElseThrow(() -> new RuntimeException(comment + " not found"));
        block.getStatements().clear();
        return block;
    }

    private CompilationUnit getMethodTemplate(String templateName) {
        InputStream resourceAsStream = this.getClass()
                .getResourceAsStream("/org/mvel3/" + templateName + ".java");
        return new JavaParser().parse(resourceAsStream).getResult().get();
    }

    public static void replaceSimpleNameWith(Node source, String oldName, String newName) {
        source.findAll(SimpleName.class, ne -> ne.toString().equals(oldName))
                .forEach(r -> r.replace(new SimpleName(newName)));
    }

    private static boolean blockHasComment(BlockStmt block, String comment) {
        return block.getComment().filter(c -> comment.equals(c.getContent()))
                .isPresent();
    }

    public <R, K, T> CompilationUnit createEvaluatorUnit(TranspiledResult input, EvaluatorInfo<T,K,R> info) {
        return input.getUnit();
    }

}
