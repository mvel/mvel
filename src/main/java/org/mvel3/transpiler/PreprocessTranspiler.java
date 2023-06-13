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

import org.mvel3.EvaluatorBuilder;

import java.util.function.Consumer;

import static com.github.javaparser.ast.NodeList.nodeList;

// A special case of compiler in which
// * the modify statements are processed
// * multi line text blocks are converted to Strings
public class PreprocessTranspiler {

    private static final PreprocessPhase preprocessPhase = new PreprocessPhase();

    public <T, K,R> TranspiledBlockResult compile(String mvelBlock, Consumer<EvaluatorBuilder<T, K,R>> updateContextFunc) {
//        Set<String> imports = new HashSet<>();
//        imports.add("java.util.List");
//        imports.add("java.util.ArrayList");
//        imports.add("java.util.HashMap");
//        imports.add("java.util.Map");
//        imports.add("java.math.BigDecimal");
//        imports.add("org.mvel3.Address");
//
//        TypeResolver classTypeResolver = new ClassTypeResolver(imports, PreprocessTranspiler.class.getClassLoader());
//        MvelTranspilerContext context = new MvelTranspilerContext(classTypeResolver);
//        updateContextFunc.accept(context);
//
//        BlockStmt mvelExpression = StaticMvelParser.parseBlock(mvelBlock);
//
//        VariableAnalyser analyser = new VariableAnalyser(context.getDeclarations().keySet());
//        mvelExpression.accept(analyser, null);
//
//        preprocessPhase.removeEmptyStmt(mvelExpression);
//
//        mvelExpression.findAll(TextBlockLiteralExpr.class).forEach(e -> {
//            Optional<Node> parentNode = e.getParentNode();
//
//            StringLiteralExpr stringLiteralExpr = preprocessPhase.replaceTextBlockWithConcatenatedStrings(e);
//
//            parentNode.ifPresent(p -> {
//                if(p instanceof VariableDeclarator) {
//                    ((VariableDeclarator) p).setInitializer(stringLiteralExpr);
//                } else if(p instanceof MethodCallExpr) {
//                    // """exampleString""".formatted("arg0", 2);
//                    ((MethodCallExpr) p).setScope(stringLiteralExpr);
//                }
//            });
//        });
//
//        mvelExpression.findAll(ModifyStatement.class)
//                .forEach(s -> {
//                    Optional<Node> parentNode = s.getParentNode();
//                    PreprocessPhase.PreprocessPhaseResult invoke = preprocessPhase.invoke(s);
//                    parentNode.ifPresent(p -> {
//                        BlockStmt parentBlock = (BlockStmt) p;
//                        for (String modifiedFact : invoke.getUsedBindings()) {
//                            parentBlock.addStatement(new MethodCallExpr(null, "update", nodeList(new NameExpr(modifiedFact))));
//                        }
//                    });
//                    s.remove();
//                });

//        return new TranspiledBlockResult(mvelExpression.getStatements(), context.getDeclarations(),
//                                         analyser.getUsed(), context.getTypeResolver().getImports(),
//                                         StaticMvelParser.getStaticTypeSolver());

        return null;
    }
}
