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
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import org.mvel3.parser.ast.visitor.DrlGenericVisitorWithDefaults;
import org.mvel3.transpiler.RHSPhase.Context;
import org.mvel3.transpiler.ast.MethodCallExprT;
import org.mvel3.transpiler.ast.TypedExpression;
import org.mvel3.transpiler.context.TranspilerContext;
import org.mvel3.util.ClassUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MethodCallExprVisitor extends DrlGenericVisitorWithDefaults<TypedExpression, Context> {

    final RHSPhase parentVisitor;
    final TranspilerContext mvelTranspilerContext;

    public MethodCallExprVisitor(RHSPhase parentVisitor, TranspilerContext mvelTranspilerContext) {
        this.parentVisitor = parentVisitor;
        this.mvelTranspilerContext = mvelTranspilerContext;
    }

    @Override
    public TypedExpression defaultAction(Node n, RHSPhase.Context context) {
        return n.accept(parentVisitor, context);
    }

    @Override
    public TypedExpression visit(MethodCallExpr n, RHSPhase.Context arg) {
        Optional<TypedExpression> scope = n.getScope().map(s -> s.accept(this, arg));
        TypedExpression name = n.getName().accept(this, new RHSPhase.Context(scope.orElse(null)));
        final List<TypedExpression> arguments = new ArrayList<>(n.getArguments().size());
        for(Expression child : n.getArguments()) {
            TypedExpression a = child.accept(this, arg);
            arguments.add(a);
        }

        Class<?>[] argumentsTypes = parametersType(arguments);

        return parseMethodFromDeclaredFunction(n, arguments)
                .orElseGet(() -> parseMethod(n, scope, name, arguments, argumentsTypes));
    }

    private Optional<TypedExpression> parseMethodFromDeclaredFunction(MethodCallExpr n, List<TypedExpression> arguments) {
//
//        Optional<DeclaredFunction> optDeclaredFunction = mvelTranspilerContext.findDeclaredFunction(n.getNameAsString());
//
//        if(optDeclaredFunction.isEmpty()) return Optional.empty();
//
//        DeclaredFunction declaredFunction = optDeclaredFunction.get();
//
//        Optional<Class<?>> methodReturnType = declaredFunction.findReturnType();
//        List<Class<?>> actualArgumentTypes = declaredFunction.findArgumentsType();
//
//        return methodReturnType.map(t -> new MethodCallExprT(n.getName().asString(), Optional.empty(), arguments,
//                                                             actualArgumentTypes, Optional.of(t)));
        return null; // (mdp)
    }

    private MethodCallExprT parseMethod(MethodCallExpr n,
                                        Optional<TypedExpression> scope,
                                        TypedExpression name,
                                        List<TypedExpression> arguments,
                                        Class<?>[] argumentsType) {
//        Optional<Method> method = scope.flatMap(TypedExpression::getType)
//                .<Class<?>>map(ClassUtils::classFromType)
//                .map(scopeClazz -> MethodUtils.findMethod(scopeClazz, n.getNameAsString(), argumentsType));
//
//        if(method.isEmpty()) {
//            method = mvelTranspilerContext.getRootPattern()
//                                          .map(scopeClazz -> MethodUtils.findMethod(scopeClazz, n.getNameAsString(), argumentsType));
//            if(method.isPresent()) {
//                scope = mvelTranspilerContext.createRootTypePrefix();
//            }
//        }
//
//        if(method.isEmpty()) {
//            method = mvelTranspilerContext.findStaticMethod(n.getNameAsString());
//        }
//
//        Optional<Method> finalMethod = method;
//        Optional<Type> methodReturnType =
//                name.getType()
//                        .map(Optional::of)
//                        .orElseGet(() -> finalMethod.map(Method::getReturnType));
//
//        List<Class<?>> actualArgumentType = optionalToStream(method)
//                .flatMap((Method m) -> Arrays.stream(m.getParameterTypes()))
//                .collect(Collectors.toList());
//
//        return new MethodCallExprT(n.getName().asString(), scope, arguments,
//                                   actualArgumentType, methodReturnType);
        return null; // (mdp)
    }

    private Class<?>[] parametersType(List<TypedExpression> arguments) {
        return arguments.stream()
                .map(TypedExpression::getType)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(ClassUtils::classFromType)
                .toArray(Class[]::new);
    }
}
