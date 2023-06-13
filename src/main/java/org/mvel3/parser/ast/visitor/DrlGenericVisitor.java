/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */
package org.mvel3.parser.ast.visitor;

import com.github.javaparser.ast.visitor.GenericVisitor;
import org.mvel3.parser.ast.expr.BigDecimalLiteralExpr;
import org.mvel3.parser.ast.expr.BigIntegerLiteralExpr;
import org.mvel3.parser.ast.expr.DrlNameExpr;
import org.mvel3.parser.ast.expr.DrlxExpression;
import org.mvel3.parser.ast.expr.FullyQualifiedInlineCastExpr;
import org.mvel3.parser.ast.expr.HalfBinaryExpr;
import org.mvel3.parser.ast.expr.HalfPointFreeExpr;
import org.mvel3.parser.ast.expr.InlineCastExpr;
import org.mvel3.parser.ast.expr.ListCreationLiteralExpression;
import org.mvel3.parser.ast.expr.ListCreationLiteralExpressionElement;
import org.mvel3.parser.ast.expr.MapCreationLiteralExpression;
import org.mvel3.parser.ast.expr.MapCreationLiteralExpressionKeyValuePair;
import org.mvel3.parser.ast.expr.ModifyStatement;
import org.mvel3.parser.ast.expr.NullSafeFieldAccessExpr;
import org.mvel3.parser.ast.expr.NullSafeMethodCallExpr;
import org.mvel3.parser.ast.expr.OOPathChunk;
import org.mvel3.parser.ast.expr.OOPathExpr;
import org.mvel3.parser.ast.expr.PointFreeExpr;
import org.mvel3.parser.ast.expr.RuleBody;
import org.mvel3.parser.ast.expr.RuleConsequence;
import org.mvel3.parser.ast.expr.RuleDeclaration;
import org.mvel3.parser.ast.expr.RuleJoinedPatterns;
import org.mvel3.parser.ast.expr.RulePattern;
import org.mvel3.parser.ast.expr.TemporalLiteralChunkExpr;
import org.mvel3.parser.ast.expr.TemporalLiteralExpr;
import org.mvel3.parser.ast.expr.TemporalLiteralInfiniteChunkExpr;
import org.mvel3.parser.ast.expr.WithStatement;

public interface DrlGenericVisitor<R, A> extends GenericVisitor<R,A> {

    R visit(RuleDeclaration ruleDeclaration, A arg);

    R visit(RuleBody n, A arg);

    R visit(RulePattern n, A arg);

    R visit(RuleJoinedPatterns n, A arg);

    R visit(DrlxExpression n, A arg);

    R visit(OOPathExpr n, A arg);

    R visit(OOPathChunk n, A arg);

    R visit(RuleConsequence n, A arg);

    R visit(InlineCastExpr n, A arg);

    R visit(FullyQualifiedInlineCastExpr n, A arg);

    R visit(NullSafeFieldAccessExpr n, A arg);

    R visit(NullSafeMethodCallExpr n, A arg);

    R visit(PointFreeExpr n, A arg);

    R visit(TemporalLiteralExpr n, A arg);

    R visit(TemporalLiteralChunkExpr n, A arg);

    R visit(HalfBinaryExpr n, A arg);

    R visit(HalfPointFreeExpr n, A arg);

    R visit(BigDecimalLiteralExpr n, A arg);

    R visit(BigIntegerLiteralExpr n, A arg);

    R visit(TemporalLiteralInfiniteChunkExpr n, A arg);

    R visit(DrlNameExpr n, A arg);

    R visit(ModifyStatement n, A arg);

    R visit(MapCreationLiteralExpression n, A arg);

    R visit(MapCreationLiteralExpressionKeyValuePair n, A arg);

    R visit(ListCreationLiteralExpression n, A arg);

    R visit(ListCreationLiteralExpressionElement n, A arg);

    R visit(WithStatement withStatement, A arg);
}
