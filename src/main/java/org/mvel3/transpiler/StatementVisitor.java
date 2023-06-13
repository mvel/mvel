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

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import org.mvel3.parser.ast.visitor.DrlGenericVisitorWithDefaults;
import org.mvel3.transpiler.ast.BlockStmtT;
import org.mvel3.transpiler.ast.DoStmtT;
import org.mvel3.transpiler.ast.ForEachDowncastStmtT;
import org.mvel3.transpiler.ast.ForStmtT;
import org.mvel3.transpiler.ast.IfStmtT;
import org.mvel3.transpiler.ast.SwitchEntryT;
import org.mvel3.transpiler.ast.SwitchStmtT;
import org.mvel3.transpiler.ast.TypedExpression;
import org.mvel3.transpiler.ast.WhileStmtT;
import org.mvel3.transpiler.context.Declaration;
import org.mvel3.transpiler.context.TranspilerContext;
import org.mvel3.parser.printer.PrintUtil;
import org.mvel3.transpiler.ast.UnalteredTypedExpression;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

public class StatementVisitor extends DrlGenericVisitorWithDefaults<TypedExpression, Void> {

    private TranspilerContext mvelTranspilerContext;

    public StatementVisitor(TranspilerContext mvelTranspilerContext) {
        this.mvelTranspilerContext = mvelTranspilerContext;
    }

    @Override
    public TypedExpression visit(ExpressionStmt n, Void arg) {
        return compileMVEL(n);
    }

    private TypedExpression compileMVEL(Node n) {
        TypedExpression rhs = new RHSPhase(mvelTranspilerContext).invoke(n);
        TypedExpression lhs = new LHSPhase(mvelTranspilerContext, ofNullable(rhs)).invoke(n);

        Optional<TypedExpression> postProcessedRHS = new ReProcessRHSPhase(mvelTranspilerContext).invoke(rhs, lhs);
        TypedExpression postProcessedLHS = postProcessedRHS.map(ppr -> new LHSPhase(mvelTranspilerContext, of(ppr)).invoke(n)).orElse(lhs);

        return postProcessedLHS;
    }

//    @Override
//    public TypedExpression visit(ForEachStmt n, Void arg) {
//        Expression iterable = n.getIterable();
//
//        Optional<TypedExpression> convertedToDowncastStmt =
//                iterable.toNameExpr()
//                        .map(PrintUtil::printNode)
//                        .flatMap(mvelTranspilerContext::findDeclarations)
//                        .filter(this::isDeclarationIterable)
//                        .map(d -> toForEachDowncastStmtT(n, arg));
//
//        if(convertedToDowncastStmt.isPresent()) {
//            return convertedToDowncastStmt.get();
//        }
//
//        TypedExpression variableDeclarationExpr = new LHSPhase(mvelTranspilerContext, Optional.empty()).invoke(n.getVariable());
//        TypedExpression typedIterable = new RHSPhase(mvelTranspilerContext).invoke(n.getIterable());
//        TypedExpression body = n.getBody().accept(this, arg);
//
//        return new ForEachStmtT(variableDeclarationExpr, typedIterable, body);
//    }

    private ForEachDowncastStmtT toForEachDowncastStmtT(ForEachStmt n, Void arg) {
        TypedExpression child = this.visit((BlockStmt) n.getBody(), arg);
        return new ForEachDowncastStmtT(n.getVariable(), PrintUtil.printNode(n.getIterable().asNameExpr()), child);
    }

    @Override
    public TypedExpression visit(BlockStmt n, Void arg) {
        List<TypedExpression> compiledStatements = n.getStatements()
                .stream()
                .map(s -> s.accept(this, arg))
                .collect(Collectors.toList());

        return new BlockStmtT(compiledStatements);
    }

    @Override
    public TypedExpression visit(IfStmt n, Void arg) {
        TypedExpression typedCondition = new RHSPhase(mvelTranspilerContext).invoke(n.getCondition());
        TypedExpression typedThen = n.getThenStmt().accept(this, arg);
        Optional<TypedExpression> typedElse = n.getElseStmt().map(e -> e.accept(this, arg));

        return new IfStmtT(typedCondition, typedThen, typedElse);
    }

    @Override
    public TypedExpression visit(WhileStmt n, Void arg) {
        TypedExpression typedCondition = new RHSPhase(mvelTranspilerContext).invoke(n.getCondition());
        TypedExpression typedThen = n.getBody().accept(this, arg);

        return new WhileStmtT(typedCondition, typedThen);
    }

    @Override
    public TypedExpression visit(DoStmt n, Void arg) {
        TypedExpression typedCondition = new RHSPhase(mvelTranspilerContext).invoke(n.getCondition());
        TypedExpression typedThen = n.getBody().accept(this, arg);

        return new DoStmtT(typedCondition, typedThen);
    }

    @Override
    public TypedExpression visit(ForStmt n, Void arg) {
        List<TypedExpression> typedInitialization = n.getInitialization().stream().map(this::compileMVEL).collect(Collectors.toList());
        Optional<TypedExpression> typedCompare = n.getCompare().map(c -> new RHSPhase(mvelTranspilerContext).invoke(c));
        List<TypedExpression> typedUpdate = n.getUpdate().stream().map(this::compileMVEL).collect(Collectors.toList());
        TypedExpression body = n.getBody().accept(this, arg);

        return new ForStmtT(typedInitialization, typedCompare, typedUpdate, body);
    }

    @Override
    public TypedExpression visit(SwitchStmt n, Void arg) {
        TypedExpression typedSelector = new RHSPhase(mvelTranspilerContext).invoke(n.getSelector());
        List<TypedExpression> typedEntries = n.getEntries().stream().map(e -> e.accept(this, arg)).collect(Collectors.toList());

        return new SwitchStmtT(typedSelector, typedEntries);
    }

    @Override
    public TypedExpression visit(SwitchEntry n, Void arg) {
        List<TypedExpression> typedStatements = n.getStatements().stream().map(this::compileMVEL).collect(Collectors.toList());

        return new SwitchEntryT(n.getLabels(), typedStatements);
    }

    private boolean isDeclarationIterable(Declaration declaration) {
        Class<?> declarationClazz = declaration.clazz();
        return Iterable.class.isAssignableFrom(declarationClazz);
    }

    @Override
    public TypedExpression defaultAction(Node n, Void context) {
        return new UnalteredTypedExpression(n);
    }
}
