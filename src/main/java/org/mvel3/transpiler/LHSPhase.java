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
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import org.mvel3.parser.ast.expr.DrlNameExpr;
import org.mvel3.parser.ast.visitor.DrlGenericVisitorWithDefaults;
import org.mvel3.transpiler.ast.AssignExprT;
import org.mvel3.transpiler.ast.BigDecimalArithmeticExprT;
import org.mvel3.transpiler.ast.BigDecimalConvertedExprT;
import org.mvel3.transpiler.ast.BinaryExprT;
import org.mvel3.transpiler.ast.ExpressionStmtT;
import org.mvel3.transpiler.ast.FieldToAccessorTExpr;
import org.mvel3.transpiler.ast.ListAccessExprT;
import org.mvel3.transpiler.ast.MapPutExprT;
import org.mvel3.transpiler.ast.SimpleNameTExpr;
import org.mvel3.transpiler.ast.TypedExpression;
import org.mvel3.transpiler.ast.UnalteredTypedExpression;
import org.mvel3.transpiler.ast.VariableDeclaratorTExpr;
import org.mvel3.transpiler.context.Declaration;
import org.mvel3.transpiler.context.TranspilerContext;
import org.mvel3.util.ClassUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.mvel3.parser.printer.PrintUtil.printNode;
import static org.mvel3.transpiler.util.TypeUtils.toJPType;
import static org.mvel3.util.ClassUtils.getAccessor;
import static org.mvel3.util.ClassUtils.getSetter;

/**
 * This phase processes the left hand side of a MVEL target expression, if present, such as
 *
 * int a = 0
 *
 * b = 2
 *
 * It also creates a new AST with the transformation rules applied i.e.
 *
 * person.name = "Name";
 *
 * becomes
 *
 * person.setName("Name");
 */
public class LHSPhase extends DrlGenericVisitorWithDefaults<TypedExpression, Void> {

    //private Logger logger = LoggerFactory.getLogger(LHSPhase.class);

    private final TranspilerContext mvelTranspilerContext;
    private final Optional<TypedExpression> rhs;

    public LHSPhase(TranspilerContext mvelTranspilerContext, Optional<TypedExpression> rhs) {
        this.mvelTranspilerContext = mvelTranspilerContext;
        this.rhs = rhs;
    }

    public TypedExpression invoke(Node n) {
        logPhase("LHS phase on: {}", n);

        TypedExpression typedExpression = n.accept(this, null);
        if (typedExpression == null) {
            throw new MVELTranspilerException("Type check of " + printNode(n) + " failed.");
        }
        //logger.debug("LHS phase completed");
        return typedExpression;
    }

    @Override
    public TypedExpression visit(DrlNameExpr n, Void arg) {
//        logPhase("DrlNameExpr {}", n);
//
//        String variableName = printNode(n);
//        Optional<Declaration> declaration = mvelTranspilerContext.findDeclarations(variableName);
//
//        return declaration.<TypedExpression>map(d -> new SimpleNameTExpr(n.getNameAsString(), d.getClazz()))
//                .orElseGet(() -> {
//                    mvelTranspilerContext.addDeclaration(variableName, getRHSType(rhs));
//                    return new VariableDeclaratorTExpr(n, variableName, getRHSType(rhs), rhs);
//                });
        return null;
    }

    @Override
    public TypedExpression visit(FieldAccessExpr n, Void arg) {
        logPhase("FieldAccessExpr {}", n);

        if (parentIsExpressionStmt(n)) {
            return rhsOrError();
        }

        TypedExpression fieldAccessScope = n.getScope().accept(this, arg);
        n.getName().accept(this, arg);

        if (n.isInternal()) {
            // a part of a larger FieldAccessExpr. e.g. [$p.address] of [$p.address.city]
            return tryParseItAsGetter(n, fieldAccessScope)
                    .orElse(new UnalteredTypedExpression(n));
        } else if(parentIsArrayAccessExpr(n)) {
            return tryParseItAsMap(n, fieldAccessScope)
                    .map(Optional::of)
                    .orElseGet(() -> tryParseItAsSetter(n, fieldAccessScope, getRHSType(rhs)))
                    .orElse(new UnalteredTypedExpression(n));
        } else {
            return tryParseAsArithmeticExpression(n, fieldAccessScope)
                    .map(Optional::of)
                    .orElseGet(() -> tryParseItAsSetter(n, fieldAccessScope, getRHSType(rhs)))
                    .orElse(new UnalteredTypedExpression(n));
        }
    }

    private Optional<TypedExpression> tryParseAsArithmeticExpression(FieldAccessExpr n, TypedExpression scope) {
        Optional<Node> optParentAssignExpr = n.getParentNode().filter(p -> p instanceof AssignExpr);
        String setterName = printNode(n.getName());

        return optParentAssignExpr.flatMap(parentAssignExpr -> findAccessorsAndConvert(scope, setterName, (AssignExpr) parentAssignExpr));
    }


    // Conversion of AssignExpr to BigDecimal Arithmetic operation when LHS is is a BigDecimal variable
    public static Optional<TypedExpression> withBigDecimalConversion(AssignExpr assignExpr,
                                                              TypedExpression target,
                                                              TypedExpression value) {

        Optional<Type> optRHSType = value.getType();
        if(!optRHSType.isPresent()) {
            return Optional.empty();
        }

        AssignExpr.Operator operator = assignExpr.getOperator();
        if(operator == AssignExpr.Operator.ASSIGN) {
            return Optional.empty();
        }

        boolean assigningToFieldAccess = target instanceof FieldToAccessorTExpr; // handled previously in FieldAccessExpr visitor
        if (!assigningToFieldAccess && target.getType().filter(t -> t == BigDecimal.class).isPresent()) {
            String bigDecimalMethod = BigDecimalArithmeticExprT.toBigDecimalMethod(operator);
            BigDecimalArithmeticExprT convertedBigDecimalExpr = new BigDecimalArithmeticExprT(bigDecimalMethod, target, value);
            return Optional.of(new AssignExprT(AssignExpr.Operator.ASSIGN, target, convertedBigDecimalExpr));
        }
        return Optional.empty();
    }

    private Optional<TypedExpression> findAccessorsAndConvert(TypedExpression fieldAccessScope,
                                                              String accessorName,
                                                              AssignExpr parentAssignExpr) {

        Class<?> scopeType = (Class<?>) fieldAccessScope.getType().orElseThrow(() -> new MVELTranspilerException("Scope without a type"));

        Optional<Method> optSetter = ofNullable(getSetter( scopeType, accessorName, BigDecimal.class));
        AssignExpr.Operator parentOperator = parentAssignExpr.getOperator();

        return optSetter.map(setter -> {
            if(parentOperator.equals(AssignExpr.Operator.ASSIGN)) {
                return new FieldToAccessorTExpr(fieldAccessScope, setter, singletonList(rhsOrError()));
            } else if(setter.getParameterTypes()[0] == BigDecimal.class) {
                return bigDecimalCompoundOperator(fieldAccessScope, accessorName, scopeType, parentOperator, setter);
            } else {
                return compoundOperator(fieldAccessScope, accessorName, scopeType, parentOperator, setter);
            }
        });
    }

    /**
        Conversion of the compound operator applied to BigDecimal
        $p.salary += 50000B;
        $p.setSalary($p.getSalary().add(new BigDecimal(\"50000\")));
     */
    private FieldToAccessorTExpr bigDecimalCompoundOperator(TypedExpression fieldAccessScope,
                                                            String accessorName,
                                                            Class<?> scopeType,
                                                            AssignExpr.Operator parentOperator,
                                                            Method setter) {
        String bigDecimalArithmeticMethod = BigDecimalArithmeticExprT.toBigDecimalMethod(parentOperator);

        Method optGetter = ofNullable(getAccessor(scopeType, accessorName))
                .orElseThrow(() -> new MVELTranspilerException("No getter found but setter is present for accessor: " + accessorName));

        FieldToAccessorTExpr getterExpression = new FieldToAccessorTExpr(fieldAccessScope, optGetter, emptyList());
        TypedExpression argument = rhsOrError();
        if(argument.getType().filter(t -> t != BigDecimal.class).isPresent()) {
            argument = new BigDecimalConvertedExprT(argument);
        }
        BigDecimalArithmeticExprT bigDecimalArithmeticExprT = new BigDecimalArithmeticExprT(bigDecimalArithmeticMethod, getterExpression, argument);
        return new FieldToAccessorTExpr(fieldAccessScope, setter, singletonList(bigDecimalArithmeticExprT));
    }

    /**
        Conversion of the compound operator applied to number literals
        $p.age += 50;
        $p.setAge($p.getAge() + 50));
     */
    private FieldToAccessorTExpr compoundOperator(TypedExpression fieldAccessScope,
                                                            String accessorName,
                                                            Class<?> scopeType,
                                                            AssignExpr.Operator parentOperator,
                                                            Method setter) {
        BinaryExpr.Operator operator = BinaryExprT.compoundToArithmeticOperation(parentOperator);

        Method optGetter = ofNullable(getAccessor(scopeType, accessorName))
                .orElseThrow(() -> new MVELTranspilerException("No getter found but setter is present for accessor: " + accessorName));

        FieldToAccessorTExpr getterExpression = new FieldToAccessorTExpr(fieldAccessScope, optGetter, emptyList());
        TypedExpression argument = rhsOrError();

        BinaryExprT arithmeticExprT = new BinaryExprT(getterExpression, argument, operator);
        return new FieldToAccessorTExpr(fieldAccessScope, setter, singletonList(arithmeticExprT));
    }

    private Optional<TypedExpression> tryParseItAsMap(FieldAccessExpr n, TypedExpression scope) {
        return scope.getType().flatMap(scopeType -> {
            String getterName = printNode(n.getName());

            return ofNullable(getAccessor((Class<?>) scopeType, getterName))
                    .filter(t -> Map.class.isAssignableFrom(t.getReturnType()))
                    .map(accessor -> new FieldToAccessorTExpr(scope, accessor, emptyList()));
        });
    }

    private Optional<TypedExpression> tryParseItAsSetter(FieldAccessExpr n, TypedExpression scope, Class<?> setterArgumentType) {
        return scope.getType().flatMap(scopeType -> {
            String setterName = printNode(n.getName());
            Optional<Method> optAccessor =
                    ofNullable(getSetter((Class<?>) scopeType, setterName, setterArgumentType))
                    .map(Optional::of)
                    .orElse(ofNullable(getSetter((Class<?>) scopeType, setterName, String.class)));

            List<TypedExpression> arguments = rhs.map(Collections::singletonList)
                    .orElse(emptyList());

            return optAccessor.map(accessor -> new FieldToAccessorTExpr(scope, accessor, arguments));
        });
    }

    private Optional<TypedExpression> tryParseItAsGetter(FieldAccessExpr n, TypedExpression scope) {
        return scope.getType().flatMap(scopeType -> {
            String propertyName = printNode(n.getName());
            Optional<Method> optAccessor =
                    ofNullable(getAccessor((Class<?>) scopeType, propertyName));

            return optAccessor.map(accessor -> new FieldToAccessorTExpr(scope, accessor, emptyList()));
        });
    }

    @Override
    public TypedExpression visit(MethodCallExpr n, Void arg) {
        logPhase("MethodCallExpr {}", n);

        return rhsOrError();
    }

    @Override
    public TypedExpression visit(VariableDeclarationExpr n, Void arg) {
        logPhase("VariableDeclarationExpr {}", n);

        // assuming there's always one declaration for variable
        return n.getVariables().iterator().next().accept(this, arg);
    }

    @Override
    public TypedExpression visit(VariableDeclarator n, Void arg) {
        logPhase("VariableDeclarator {}", n);

        String variableName = n.getName().asString();
        Class<?> type = getRHSorLHSType(n);

//        mvelTranspilerContext.addDeclaration(variableName, type);

//        String declaredType = null;
//
//        if (originalNode instanceof ClassOrInterfaceType) {
//            ClassOrInterfaceType originalType= (ClassOrInterfaceType) originalNode;
//            if (originalType.getScope().isPresent()) {
//                declaredType = originalType.getNameWithScope();
//            } else {
//                declaredType = originalType.getNameAsString();
//            }
//        } else if (originalNode instanceof DrlNameExpr ){
//            declaredType = ((DrlNameExpr)originalNode).getNameAsString();
//        }
//
//
//
//        Optional<Type> optInitType = initExpression.flatMap(TypedExpression::getType);
//        com.github.javaparser.ast.type.Type jpType = toJPType(this.type);

        return new VariableDeclaratorTExpr(n, variableName, type, rhs);
    }

    @Override
    public TypedExpression visit(ExpressionStmt n, Void arg) {
        logPhase("ExpressionStmt {}", n);

        Optional<TypedExpression> expression = ofNullable(n.getExpression().accept(this, arg));
        return new ExpressionStmtT(expression.orElseGet(this::rhsOrError));
    }

    @Override
    public TypedExpression visit(EnclosedExpr n, Void arg) {
        // We don't want our LHS to be ever wrapped
        return n.getInner().accept(this, arg);
    }

    @Override
    public TypedExpression visit(AssignExpr n, Void arg) {
        logPhase("AssignExpr {}", n);

        TypedExpression target = n.getTarget().accept(this, arg);

        Optional<TypedExpression> bigDecimalConversion =
                withBigDecimalConversion(n, target, rhsOrError());

        if(bigDecimalConversion.isPresent()) {
            return bigDecimalConversion.get();
        }

        if (target instanceof FieldToAccessorTExpr || target instanceof VariableDeclaratorTExpr || target instanceof MapPutExprT) {
            return target;
        }
        return new AssignExprT(n.getOperator(), target, rhsOrNull());
    }

    @Override
    public TypedExpression visit(ArrayAccessExpr n, Void arg) {
        if (parentIsExpressionStmt(n)) {
            return rhsOrError();
        }

        TypedExpression name = n.getName().accept(this, arg);

        Optional<Type> type = name.getType();
        if(type.filter(ClassUtils::isCollection).isPresent()) {
            Expression index = n.getIndex();
            if(index.isStringLiteralExpr() || index.isNameExpr()) {
                return new MapPutExprT(name, index, rhsOrNull(), name.getType());
            } else {
                return new ListAccessExprT(name, index, type.get());
            }
        }
        return new UnalteredTypedExpression(n, type.orElse(null));
    }


    private TypedExpression rhsOrNull() {
        return rhs.orElse(null);
    }

    private TypedExpression rhsOrError() {
        return rhs.orElseThrow(() -> new MVELTranspilerException("RHS not found, need a valid expression"));
    }

    /*
        This means there's no LHS
     */
    private boolean parentIsExpressionStmt(Node n) {
        return n.getParentNode().filter(p -> p instanceof ExpressionStmt).isPresent();
    }

    private boolean parentIsArrayAccessExpr(Node n) {
        return n.getParentNode().filter(p -> p instanceof ArrayAccessExpr).isPresent();
    }

    public static Class<?> getRHSType(Optional<TypedExpression> rhs) {
        return rhs
                .flatMap(TypedExpression::getType)
                .map(ClassUtils::classFromType)
                .orElseThrow(() -> new MVELTranspilerException("RHS doesn't have a type"));
    }

    private Class<?> getRHSorLHSType(VariableDeclarator n) {
//        return mvelTranspilerContext.resolveType(n.getType() instanceof ClassOrInterfaceType ?
//                n.getType().asClassOrInterfaceType().getNameAsString() :
//                n.getType().asString());
        return null; // (mdp)
    }

    private void logPhase(String phase, Node statement) {
        System.out.println(printNode(statement));
//        if(logger.isDebugEnabled()) {
//            logger.debug(phase, printNode(statement));
//        }
    }

    @Override
    public TypedExpression defaultAction(Node n, Void unused) {
        return new UnalteredTypedExpression(n);
    }
}

