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
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import org.mvel3.parser.ast.visitor.DrlGenericVisitorWithDefaults;
import org.mvel3.transpiler.ReProcessRHSPhase.Context;
import org.mvel3.transpiler.ast.TypedExpression;
import org.mvel3.transpiler.ast.BigDecimalConvertedExprT;
import org.mvel3.transpiler.ast.IntegerLiteralExpressionT;
import org.mvel3.transpiler.ast.LongLiteralExpressionT;
import org.mvel3.transpiler.ast.UnalteredTypedExpression;
import org.mvel3.transpiler.context.TranspilerContext;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Used when you need to reprocess the RHS after having processed the LHS
 */
public class ReProcessRHSPhase extends DrlGenericVisitorWithDefaults<Optional<TypedExpression>, Context> {

    private TypedExpression lhs;
    private TranspilerContext mvelTranspilerContext;

    static class Context {
        private UnaryExpr unaryExpr;

        Context withUnaryExpr(UnaryExpr unaryExpr) {
            this.unaryExpr = unaryExpr;
            return this;
        }

        Optional<UnaryExpr> getUnaryExpr() {
            return Optional.ofNullable(unaryExpr);
        }
    }

    ReProcessRHSPhase(TranspilerContext mvelTranspilerContext) {
        this.mvelTranspilerContext = mvelTranspilerContext;
    }

    public Optional<TypedExpression> invoke(TypedExpression rhs, TypedExpression lhs) {
        this.lhs = lhs;
        return Optional.ofNullable(rhs).flatMap(r -> r.toJavaExpression().accept(this, new Context()));
    }

    @Override
    public Optional<TypedExpression> defaultAction(Node n, Context context) {
        return Optional.empty();
    }

    @Override
    public Optional<TypedExpression> visit(UnaryExpr n, Context context) {
        return n.getExpression().accept(this, context.withUnaryExpr(n));
    }

    @Override
    public Optional<TypedExpression> visit(BinaryExpr n, Context context) {
        return convertWhenLHSISBigDecimal(() -> new UnalteredTypedExpression(n), context);
    }

    @Override
    public Optional<TypedExpression> visit(IntegerLiteralExpr n, Context context) {
        return convertWhenLHSISBigDecimal(() -> new IntegerLiteralExpressionT(n), context);
    }

    @Override
    public Optional<TypedExpression> visit(LongLiteralExpr n, Context context) {
        return convertWhenLHSISBigDecimal(() -> new LongLiteralExpressionT(n), context);
    }

    @Override
    public Optional<TypedExpression> visit(NameExpr n, Context context) {
//        if(mvelTranspilerContext
//                .findDeclarations(n.toString())
//                .filter(d -> d.getClazz() != BigDecimal.class)
//                .isPresent()) { // avoid wrapping BigDecimal declarations
//            return convertWhenLHSISBigDecimal(() -> new UnalteredTypedExpression(n), context);
//        } else {
//            return Optional.empty();
//        }
        return null;
    }

    private Optional<TypedExpression> convertWhenLHSISBigDecimal(Supplier<TypedExpression> conversionFunction, Context context) {
        return lhs.getType()
                .filter(BigDecimal.class::equals)
                .flatMap(t -> Optional.of(new BigDecimalConvertedExprT(conversionFunction.get(), context.getUnaryExpr())));
    }
}
