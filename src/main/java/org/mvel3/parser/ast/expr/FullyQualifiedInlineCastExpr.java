/*
 * Copyright (c) 2021. Red Hat, Inc. and/or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mvel3.parser.ast.expr;

import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.observer.ObservableProperty;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import org.mvel3.parser.ast.visitor.DrlGenericVisitor;
import org.mvel3.parser.ast.visitor.DrlVoidVisitor;

public class FullyQualifiedInlineCastExpr extends Expression {

    // @TODO make this work with Generics

    private Expression expression;

    private Type type;

    public FullyQualifiedInlineCastExpr(Expression expression, ClassOrInterfaceType type) {
        this(null, expression, type);
    }
    public FullyQualifiedInlineCastExpr(TokenRange tokenRange, Expression expression, Type type) {
        super( tokenRange );
        setExpression(expression);
        setType(type);
    }

    public Type getType() {
        return type;
    }

    public FullyQualifiedInlineCastExpr setType(Type type) {
        if (type == this.type) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.EXPRESSION, this.type, type);
        if (this.type != null) {
            this.type.setParentNode(null);
        }

        this.type = type;

        setAsParentNodeOf(type);

        return this;
    }

    public Expression getExpression() {
        return expression;
    }

    public FullyQualifiedInlineCastExpr setExpression(Expression scope) {
        if (scope == this.expression) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.SCOPE, this.expression, scope);
        if (this.expression != null) {
            this.expression.setParentNode(null);
        }

        this.expression = scope;

        setAsParentNodeOf(scope);

        return this;
    }

    @Override
    public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
        return ((DrlGenericVisitor<R, A>)v).visit(this, arg);
    }

    @Override
    public <A> void accept(VoidVisitor<A> v, A arg) {
        ((DrlVoidVisitor<A>)v).visit(this, arg);
    }

}
