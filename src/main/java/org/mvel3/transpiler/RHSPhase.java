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
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.PatternExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TextBlockLiteralExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.YieldStmt;
import org.mvel3.parser.ast.visitor.DrlGenericVisitorWithDefaults;
import org.mvel3.transpiler.RHSPhase.Context;
import org.mvel3.transpiler.ast.ObjectCreationExpressionT;
import org.mvel3.parser.ast.expr.BigDecimalLiteralExpr;
import org.mvel3.parser.ast.expr.BigIntegerLiteralExpr;
import org.mvel3.parser.ast.expr.DrlNameExpr;
import org.mvel3.transpiler.ast.BigDecimalArithmeticExprT;
import org.mvel3.transpiler.ast.BigDecimalConvertedExprT;
import org.mvel3.transpiler.ast.BigDecimalRelationalExprT;
import org.mvel3.transpiler.ast.BigIntegerConvertedExprT;
import org.mvel3.transpiler.ast.BinaryExprT;
import org.mvel3.transpiler.ast.BooleanLiteralExpressionT;
import org.mvel3.transpiler.ast.CastExprT;
import org.mvel3.transpiler.ast.CharacterLiteralExpressionT;
import org.mvel3.transpiler.ast.FieldAccessTExpr;
import org.mvel3.transpiler.ast.FieldToAccessorTExpr;
import org.mvel3.transpiler.ast.IntegerLiteralExpressionT;
import org.mvel3.transpiler.ast.ListAccessExprT;
import org.mvel3.transpiler.ast.LongLiteralExpressionT;
import org.mvel3.transpiler.ast.SimpleNameTExpr;
import org.mvel3.transpiler.ast.StringLiteralExpressionT;
import org.mvel3.transpiler.ast.TypedExpression;
import org.mvel3.transpiler.ast.UnalteredTypedExpression;
import org.mvel3.transpiler.context.Declaration;
import org.mvel3.transpiler.context.TranspilerContext;
import org.mvel3.util.ClassUtils;
import org.mvel3.util.MethodUtils.NullType;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Optional.ofNullable;
import static org.mvel3.transpiler.ast.BigDecimalArithmeticExprT.toBigDecimalMethod;
import static org.mvel3.transpiler.util.OptionalUtils.map2;
import static org.mvel3.util.ClassUtils.classFromType;
import static org.mvel3.util.ClassUtils.getAccessor;

/**
 * This phase processes the right hand side of a Java Expression and creates a new AST
 * with the transformation rules applied i.e.
 *
 * person.name;
 *
 * becomes
 *
 * person.getName();
 *
 * It also returns the type of the expression, useful in the subsequent phase in which we
 * might need to create new variables accordingly.
 *
 */
public class RHSPhase extends DrlGenericVisitorWithDefaults<TypedExpression, Context> {

    private static final Set<BinaryExpr.Operator> arithmeticOperators = Set.of(
            BinaryExpr.Operator.PLUS,
            BinaryExpr.Operator.MINUS,
            BinaryExpr.Operator.MULTIPLY,
            BinaryExpr.Operator.DIVIDE,
            BinaryExpr.Operator.REMAINDER
    );

    private static final Set<BinaryExpr.Operator> relationalOperators = Set.of(
            BinaryExpr.Operator.EQUALS,
            BinaryExpr.Operator.NOT_EQUALS,
            BinaryExpr.Operator.LESS,
            BinaryExpr.Operator.GREATER,
            BinaryExpr.Operator.LESS_EQUALS,
            BinaryExpr.Operator.GREATER_EQUALS
    );

    private final MethodCallExprVisitor methodCallExprVisitor;

    static class Context {
        final Optional<TypedExpression> scope;

        Context(TypedExpression scope) {
            this.scope = ofNullable(scope);
        }

        Optional<Type> getScopeType() {
            return scope.flatMap(TypedExpression::getType);
        }
    }

    private final TranspilerContext mvelTranspilerContext;

    RHSPhase(TranspilerContext mvelTranspilerContext) {
        this.mvelTranspilerContext = mvelTranspilerContext;
        methodCallExprVisitor = new MethodCallExprVisitor(this, this.mvelTranspilerContext);
    }

    public TypedExpression invoke(Node statement) {
        Context ctx = new Context(null);

        return statement.accept(this, ctx);
    }

    @Override
    public TypedExpression visit(DrlNameExpr n, Context arg) {
        return n.getName().accept(this, arg);
    }

    @Override
    public TypedExpression visit(SimpleName n, Context arg) {
        if (arg.scope.isEmpty()) { // first node
            return simpleNameAsFirstNode(n);
        } else {
            return simpleNameAsField(n, arg);
        }
    }

    @Override
    public TypedExpression visit(YieldStmt n, Context arg) {
        return null;
    }

    @Override
    public TypedExpression visit(TextBlockLiteralExpr n, Context arg) {
        return new UnalteredTypedExpression(n, String.class);
    }

    @Override
    public TypedExpression visit(PatternExpr n, Context arg) {
        return null;
    }

    private TypedExpression simpleNameAsFirstNode(SimpleName n) {
        return asDeclaration(n)
                .map(Optional::of)
                .orElseGet(() -> asPropertyAccessorOfRootPattern(n))
                .map(Optional::of)
                .orElseGet(() -> asEnum(n))
                .orElseGet(() -> new UnalteredTypedExpression(n));
    }

    private TypedExpression simpleNameAsField(SimpleName n, Context arg) {
        return asPropertyAccessor(n, arg)
                .map(Optional::of)
                .orElseGet(() -> asFieldAccessTExpr(n, arg))
                .orElseGet(() -> new UnalteredTypedExpression(n));
    }

    private Optional<TypedExpression> asFieldAccessTExpr(SimpleName n, Context arg) {
        Optional<TypedExpression> lastTypedExpression = arg.scope;
        Optional<Type> scopeType = arg.getScopeType();

        Optional<Field> fieldType = scopeType.flatMap(te -> {
            Class parentClass = classFromType(te);
            Field field = ClassUtils.getField(parentClass, n.asString());
            return ofNullable(field);
        });

        return map2(lastTypedExpression, fieldType, FieldAccessTExpr::new);
    }

    private Optional<TypedExpression> asDeclaration(SimpleName n) {
//        Optional<Declaration> typeFromDeclarations = mvelTranspilerContext.findDeclarations(n.asString());
//        return typeFromDeclarations.map(d -> {
//            Class<?> clazz = d.getClazz();
//            return new SimpleNameTExpr(n.asString(), clazz);
//        });
        return null;
    }

    private Optional<TypedExpression> asEnum(SimpleName n) {
//        Optional<Class<?>> enumType = mvelTranspilerContext.findEnum(n.asString());
//        return enumType.map(clazz -> new SimpleNameTExpr(n.asString(), clazz));
        return null; // (mdp)
    }

    private Optional<TypedExpression> asPropertyAccessor(SimpleName n, Context arg) {
        Optional<TypedExpression> lastTypedExpression = arg.scope;

        Optional<Type> scopeType = lastTypedExpression.filter(ListAccessExprT.class::isInstance)
                                                      .map(ListAccessExprT.class::cast)
                                                      .map(expr -> expr.getElementType())
                                                      .orElse(arg.getScopeType());

        Optional<Method> optAccessor = scopeType.flatMap(t -> ofNullable(getAccessor(classFromType(t), n.asString())));

        return map2(lastTypedExpression, optAccessor, FieldToAccessorTExpr::new);
    }

    private Optional<TypedExpression> asPropertyAccessorOfRootPattern(SimpleName n) {
//        Optional<Class<?>> scopeType = mvelTranspilerContext.getRootObject();
//        Optional<Method> optAccessor = scopeType.flatMap(t -> ofNullable(getAccessor(classFromType(t), n.asString())));
//
//        return map2(mvelTranspilerContext.createRootTypePrefix(), optAccessor, FieldToAccessorTExpr::new);
        return null;
    }

    @Override
    public TypedExpression visit(FieldAccessExpr n, Context arg) {
        TypedExpression scope = n.getScope().accept(this, arg);
        return n.getName().accept(this, new Context(scope));
    }

    @Override
    public TypedExpression visit(MethodCallExpr n, Context arg) {
        return n.accept(methodCallExprVisitor, arg);
    }

    @Override
    public TypedExpression visit(BinaryExpr n, Context arg) {
        TypedExpression left = n.getLeft().accept(this, arg);
        TypedExpression right = n.getRight().accept(this, arg);
        return withPossiblyBigDecimalConversion(left, right, n.getOperator());
    }

    private TypedExpression withPossiblyBigDecimalConversion(TypedExpression left, TypedExpression right, BinaryExpr.Operator operator) {
        Optional<Type> optTypeLeft = left.getType();
        Optional<Type> optTypeRight = right.getType();

        if (optTypeLeft.isEmpty() || optTypeRight.isEmpty()) { // coerce only when types are known
            return new BinaryExprT(left, right, operator);
        }

        Type typeLeft = optTypeLeft.get();
        Type typeRight = optTypeRight.get();

        boolean isStringConcatenation = operator == BinaryExpr.Operator.PLUS &&
                (typeLeft == String.class || typeRight == String.class);

        if (arithmeticOperators.contains(operator) && !isStringConcatenation) {
            return convertToBigDecimalArithmeticExprTIfNeeded(left, right, operator, typeLeft, typeRight);
        } else if (relationalOperators.contains(operator)) {
            return convertToBigDecimalRelationalExprTIfNeeded(left, right, operator, typeLeft, typeRight);
        }

        return new BinaryExprT(left, right, operator);
    }

    private TypedExpression convertToBigDecimalArithmeticExprTIfNeeded(TypedExpression left, TypedExpression right, BinaryExpr.Operator operator, Type typeLeft, Type typeRight) {
        if (typeLeft == BigDecimal.class && typeRight == BigDecimal.class) { // do not convert
            return new BigDecimalArithmeticExprT(toBigDecimalMethod(operator), left, right);
        } else if (typeLeft != BigDecimal.class && typeRight == BigDecimal.class) { // convert left
            return new BigDecimalArithmeticExprT(toBigDecimalMethod(operator), new BigDecimalConvertedExprT(left), right);
        } else if (typeLeft == BigDecimal.class && typeRight != BigDecimal.class) { // convert right
            return new BigDecimalArithmeticExprT(toBigDecimalMethod(operator), left, new BigDecimalConvertedExprT(right));
        } else {
            return new BinaryExprT(left, right, operator);
        }
    }

    private TypedExpression convertToBigDecimalRelationalExprTIfNeeded(TypedExpression left, TypedExpression right, BinaryExpr.Operator operator, Type typeLeft, Type typeRight) {
        if (typeLeft == BigDecimal.class && typeRight == BigDecimal.class) { // do not convert
            return new BigDecimalRelationalExprT(operator, left, right);
        } else if (typeLeft != BigDecimal.class && typeRight == BigDecimal.class) { // convert left
            return new BigDecimalRelationalExprT(operator, new BigDecimalConvertedExprT(left), right);
        } else if (typeLeft == BigDecimal.class && typeRight != BigDecimal.class) { // convert right
            return new BigDecimalRelationalExprT(operator, left, new BigDecimalConvertedExprT(right));
        } else {
            return new BinaryExprT(left, right, operator);
        }
    }

    @Override
    public TypedExpression visit(ExpressionStmt n, Context arg) {
        return n.getExpression().accept(this, arg);
    }

    @Override
    public TypedExpression visit(VariableDeclarationExpr n, Context arg) {
        return n.getVariables().iterator().next().accept(this, arg);
    }

    @Override
    public TypedExpression visit(VariableDeclarator n, Context arg) {
        Optional<TypedExpression> initExpression = n.getInitializer().map(i -> i.accept(this, arg));
        return initExpression.orElse(null);
    }

    @Override
    public TypedExpression visit(AssignExpr n, Context arg) {
        return n.getValue().accept(this, arg);
    }

    @Override
    public TypedExpression visit(StringLiteralExpr n, Context arg) {
        return new StringLiteralExpressionT(n);
    }

    @Override
    public TypedExpression visit(IntegerLiteralExpr n, Context arg) {
        return new IntegerLiteralExpressionT(n);
    }

    @Override
    public TypedExpression visit(CharLiteralExpr n, Context arg) {
        return new CharacterLiteralExpressionT(n);
    }

    @Override
    public TypedExpression visit(LongLiteralExpr n, Context arg) {
        return new LongLiteralExpressionT(n);
    }

    @Override
    public TypedExpression visit(BooleanLiteralExpr n, Context arg) {
        return new BooleanLiteralExpressionT(n);
    }

    @Override
    public TypedExpression defaultAction(Node n, Context context) {
        return new UnalteredTypedExpression(n);
    }

    @Override
    public TypedExpression visit(ObjectCreationExpr n, Context arg) {
        List<TypedExpression> constructorArguments = new ArrayList<>();
        for(Expression e : n.getArguments()) {
            TypedExpression compiledArgument = e.accept(this, arg);
            constructorArguments.add(compiledArgument);
        }
        return new ObjectCreationExpressionT(constructorArguments, n.getType(), resolveType(n.getType()));
    }

    @Override
    public TypedExpression visit(NullLiteralExpr n, Context arg) {
        return new UnalteredTypedExpression(n, NullType.class);
    }

    @Override
    public TypedExpression visit(ArrayAccessExpr n, Context arg) {
        TypedExpression name = n.getName().accept(this, arg);

        Optional<Type> type = name.getType();
        if(type.filter(ClassUtils::isCollection).isPresent()) {
            return new ListAccessExprT(name, n.getIndex(), type.get());
        }
        return new UnalteredTypedExpression(n, type.orElse(null));
    }

    @Override
    public TypedExpression visit(EnclosedExpr n, Context arg) {
        return n.getInner().accept(this, arg);
    }

    @Override
    public TypedExpression visit(CastExpr n, Context arg) {
        TypedExpression innerExpr = n.getExpression().accept(this, arg);
        return new CastExprT(innerExpr, n.getType(), resolveType(n.getType()));
    }

    @Override
    public TypedExpression visit(BigDecimalLiteralExpr n, Context arg) {
        return new BigDecimalConvertedExprT(new StringLiteralExpressionT(new StringLiteralExpr(n.getValue())));
    }

    @Override
    public TypedExpression visit(BigIntegerLiteralExpr n, Context arg) {
        return new BigIntegerConvertedExprT(new StringLiteralExpressionT(new StringLiteralExpr(n.getValue())));
    }

    @Override
    public TypedExpression visit(UnaryExpr n, Context arg) {
        Expression innerExpr = n.getExpression();
        UnaryExpr.Operator operator = n.getOperator();
        if (innerExpr instanceof BigDecimalLiteralExpr && operator == UnaryExpr.Operator.MINUS) {
            return new BigDecimalConvertedExprT(new StringLiteralExpressionT(new StringLiteralExpr(operator.asString() + ((BigDecimalLiteralExpr) innerExpr).getValue())));
        } else if (innerExpr instanceof BigIntegerLiteralExpr && operator == UnaryExpr.Operator.MINUS) {
            return new BigIntegerConvertedExprT(new StringLiteralExpressionT(new StringLiteralExpr(operator.asString() + ((BigIntegerLiteralExpr) innerExpr).getValue())));
        } else {
            return defaultAction(n, arg);
        }
    }

    private Class<?> resolveType(com.github.javaparser.ast.type.Type type) {
//        if (type instanceof ClassOrInterfaceType) {
//            return mvelTranspilerContext.resolveType(((NodeWithSimpleName)type).getNameAsString());
//        } else if (type instanceof TypeParameter) {
//            //return mvelTranspilerContext.resolveType(((NodeWithSimpleName)type).getNameAsString());
//        } else if (type instanceof ArrayType) {
//            //throw new RuntimeException("Unsupported Type");
//        } else if (type instanceof PrimitiveType) {
//            //throw new RuntimeException("Unsupported Type");
//        } else if (type instanceof VarType) {
//            //throw new RuntimeException("Unsupported Type");
//        } else if (type instanceof WildcardType) {
//            //throw new RuntimeException("Unsupported Type");
//        }
//
//        return mvelTranspilerContext.resolveType(type.toString());
        return null; //(mdp)
    }
}

