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

package org.mvel3.transpiler.ast;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import org.mvel3.parser.ast.expr.DrlNameExpr;

import java.lang.reflect.Type;
import java.util.Optional;

import static org.mvel3.transpiler.util.TypeUtils.toJPType;

public class VariableDeclaratorTExpr implements TypedExpression {

    private final Node originalNode;
    private final String name;
    private final Type type;
    private final Optional<TypedExpression> initExpression;

    public VariableDeclaratorTExpr(Node originalNode, String name,
                                   Type type, Optional<TypedExpression> initExpression) {
        this.originalNode = originalNode;
        this.name = name;
        this.type = type;
        this.initExpression = initExpression;
    }

    @Override
    public Optional<Type> getType() {
        return Optional.of(type);
    }

    @Override
    public Node toJavaExpression() {
        //com.github.javaparser.ast.type.Type declaredJpType = null;
        String declaredType = null;

        if (originalNode instanceof ClassOrInterfaceType) {
            ClassOrInterfaceType originalType= (ClassOrInterfaceType) originalNode;
            if (originalType.getScope().isPresent()) {
                declaredType = originalType.getNameWithScope();
            } else {
                declaredType = originalType.getNameAsString();
            }
        } else if (originalNode instanceof DrlNameExpr ){
            declaredType = ((DrlNameExpr)originalNode).getNameAsString();
        }



        Optional<Type> optInitType = initExpression.flatMap(TypedExpression::getType);
        com.github.javaparser.ast.type.Type jpType = toJPType(this.type);

        return initExpression.map(ie -> {

            Expression initializer = (Expression) ie.toJavaExpression();
            // Used to downcast map.get see testAddCastToMapGetOfDeclaration
            if(optInitType.isEmpty() || optInitType.get().equals(Object.class)) {
                initializer = new CastExpr(jpType, new EnclosedExpr(initializer));
            }
            return (Node) new VariableDeclarationExpr(new VariableDeclarator(jpType, name, initializer));
        }).orElse(new VariableDeclarationExpr(jpType, name));
    }

    @Override
    public String toString() {
        return "VariableDeclaratorTExpr{" +
                "originalNode=" + originalNode +
                ", name=" + name +
                ", initExpression=" + initExpression +
                '}';
    }
}
