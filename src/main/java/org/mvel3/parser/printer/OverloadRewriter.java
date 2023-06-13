package org.mvel3.parser.printer;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType.Primitive;
import com.github.javaparser.resolution.Solver;
import com.github.javaparser.resolution.types.ResolvedType;
import org.mvel3.transpiler.context.TranspilerContext;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.mvel3.parser.printer.MVELToJavaRewriter.getArgumentsWithUnwrap;
import static org.mvel3.transpiler.MVELTranspiler.handleParserResult;

/**
 * the Expression being coerced must be cloned, rather than added. There are times that adding it doesn't always remove it from previous location.
 * So it sits in both locations and then removal from the oroginal location null's it's parent field.
 */
public class OverloadRewriter {
    TranspilerContext context;

    private Map<String, Function<Holder, Expression> > overloads = new HashMap<>();

    private ResolvedType bigDecimalType;
    private ResolvedType bigIntegerType;

    private Expression mathContext;

    public OverloadRewriter(TranspilerContext context) {
        this.context = context;

        Solver solver = context.getFacade().getSymbolSolver();

        bigDecimalType = solver.classToResolvedType(BigDecimal.class);
        bigIntegerType = solver.classToResolvedType(BigInteger.class);
        mathContext = handleParserResult(context.getParser().parseExpression("java.math.MathContext.DECIMAL128"));

        bigDecimalOverload();

//        bigDecimalCoercion();
//        bigIntegerCoercion();
//
//        integerToDateCoercion();
//        dateToLongCoercion();
    }
//
    private void bigDecimalOverload() {
        Function<Holder, Expression> overloadBigDecimal = e -> {
            Expression rewrite = rewriteBigNumberOperator(e.getLeft(), e.getRight(), e.getOperator(), true);
            return rewrite;
        };

        Function<Holder, Expression> overloadBigInteger = e -> {
            Expression rewrite = rewriteBigNumberOperator(e.getLeft(), e.getRight(), e.getOperator(), false);
            return rewrite;
        };

        String bigDecimal = BigDecimal.class.getCanonicalName();
        String bigInteger = BigInteger.class.getCanonicalName();

        overloads.put(bigDecimal, overloadBigDecimal);
        overloads.put(bigInteger, overloadBigInteger);

//        Arrays.stream(FLOAT_PRIMITIVES).forEach(p -> {
//            overloads.put(key(p.name().toUpperCase(), bigDecimal), toBigDecimal);
//            overloads.put(key("java.lang." + p.toBoxedType(), bigDecimal), toBigDecimal);
//        });
    }
//
//    public void bigDecimalCoercion() {
//        Function<Expression, Expression> toBigDecimal = e -> {
//            List<Expression> args = getArgumentsWithUnwrap(e.clone());
//
//            MethodCallExpr methodCallExpr = new MethodCallExpr(new NameExpr(BigDecimal.class.getSimpleName()), "valueOf", NodeList.nodeList(args));
//
//            return methodCallExpr;
//        };
//
//        String bigDecimal = BigDecimal.class.getCanonicalName();
//
//        Arrays.stream(FLOAT_PRIMITIVES).forEach(p -> {
//            overloads.put(key(p.name().toUpperCase(), bigDecimal), toBigDecimal);
//            overloads.put(key("java.lang." + p.toBoxedType(), bigDecimal), toBigDecimal);
//        });
//    }
//
//    public void bigIntegerCoercion() {
//        Function<Expression, Expression> toBigInteger = e -> {
//            List<Expression> args = getArgumentsWithUnwrap(e.clone());
//
//            MethodCallExpr methodCallExpr = new MethodCallExpr(new NameExpr(BigInteger.class.getSimpleName()), "valueOf", NodeList.nodeList(args));
//
//            return methodCallExpr;
//        };
//
//
//        String bigInteger = BigInteger.class.getCanonicalName();
//
//        Arrays.stream(INTEGER_PRIMITIVES).forEach(p -> {
//            overloads.put(key(p.name().toUpperCase(), bigInteger), toBigInteger);
//            overloads.put(key("java.lang." + p.toBoxedType(), bigInteger), toBigInteger);
//        });
//
//    }
//
//    public void integerToDateCoercion() {
//        ParseResult<ClassOrInterfaceType> result = context.getParser().parseClassOrInterfaceType("java.util.Date");
//        if (!result.isSuccessful()) {
//            throw new RuntimeException("Cannot resolve type:" + result.getProblems());
//        }
//
//        final ClassOrInterfaceType dateType = result.getResult().get();
//
//        Function<Expression, Expression> integerToDate = e -> {
//            ClassOrInterfaceType type = dateType.clone();
//
//            ObjectCreationExpr expr = new ObjectCreationExpr(null, type, NodeList.nodeList(e.clone()));
//
//            return expr;
//        };
//
//        String date = Date.class.getCanonicalName();
//
//        Arrays.stream(INTEGER_PRIMITIVES).forEach(p -> {
//            overloads.put(key(p.name().toUpperCase(), date), integerToDate);
//            overloads.put(key("java.lang." + p.toBoxedType(), date), integerToDate);
//        });
//    }
//
//
//    /**
//     * As MVEL3 does not narrow types, Date can only be coerced to long and Long.
//     */
//    public void dateToLongCoercion() {
//        String date = Date.class.getCanonicalName();
//
//        Function<Expression, Expression> toLong = e -> {
//            MethodCallExpr getTimeCall = new MethodCallExpr(e.clone(), "getTime");
//            return getTimeCall;
//        };
//
//        Arrays.stream(new Primitive[] {Primitive.LONG}).forEach(p -> {
//            overloads.put(key(date, p.name().toUpperCase()), toLong);
//            overloads.put(key(date, "java.lang." + p.toBoxedType()), toLong);
//        });
//
//    }
//
//    Function<Expression, Expression> toString = e -> {
//        MethodCallExpr methodCallExpr = new MethodCallExpr(e, "toString");
//        methodCallExpr.addArgument(e);
//        return methodCallExpr;
//    };

    public Expression overload(ResolvedType subject, Expression left, Expression right, BinaryExpr.Operator op) {
        Function<Holder, Expression>  overload = overloads.get(describe(subject));

        if (overload != null) {
            return overload.apply(new Holder(left, right, op));
        }

        return null;
    }

    static class Holder {
        Expression left;
        Expression right;
        BinaryExpr.Operator op;

        public Holder(Expression left, Expression right, Operator op) {
            this.left  = left;
            this.right = right;
            this.op    = op;
        }

        public Expression getLeft() {
            return left;
        }

        public Expression getRight() {
            return right;
        }

        public Operator getOperator() {
            return op;
        }
    }

    public String describe(ResolvedType type) {
        return type.isPrimitive() ? type.describe().toUpperCase() : type.describe();
    }

    private Expression rewriteBigNumberOperator(Expression e1,  Expression e2, BinaryExpr.Operator opEnum, boolean addMathContext) {
        String op;
        Expression result;

        switch (opEnum) {
            case MULTIPLY : op = "multiply"; break;
            case DIVIDE : op = "divide"; break;
            case PLUS : op = "add"; break;
            case MINUS : op = "subtract"; break;
            case REMAINDER : op = "remainder"; break;
            case EQUALS : op = "compareTo"; break;
            case NOT_EQUALS : op = "compareTo"; break;
            case GREATER : op = "compareTo"; break;
            case GREATER_EQUALS : op = "compareTo"; break;
            case LESS : op = "compareTo"; break;
            case LESS_EQUALS : op = "compareTo"; break;
            default:
                throw new RuntimeException("Overloading not supported for the operator: " + opEnum);
        }

        MethodCallExpr methodCallExpr;

        List<Expression> children = getArgumentsWithUnwrap(e2.clone());
        if (!op.equals("compareTo") && addMathContext) {
            // an array is used, because BigDecimal takes a second argument for the mathContext
            children.add(mathContext.clone());
        }

        methodCallExpr = new MethodCallExpr(op, children.toArray( new Expression[0]));
        methodCallExpr.setScope(e1.clone());
        result = methodCallExpr;

        if (op.equals("compareTo")) {
            result = new BinaryExpr(methodCallExpr, new IntegerLiteralExpr("0"), opEnum);
        }

        return result;
    }


//    private Expression rewriteBigNumberOperator(Expression e1, ResolvedType e1Type, Expression e2, ResolvedType e2Type, BinaryExpr.Operator opEnum) {
//        String op = null;
//        Expression result = null;
//
//        switch (opEnum) {
//            case MULTIPLY : op = "multiply"; break;
//            case DIVIDE : op = "divide"; break;
//            case PLUS : op = "add"; break;
//            case MINUS : op = "subtract"; break;
//            case REMAINDER : op = "remainder"; break;
//            case EQUALS : op = "compareTo"; break;
//            case NOT_EQUALS : op = "compareTo"; break;
//            case GREATER : op = "compareTo"; break;
//            case GREATER_EQUALS : op = "compareTo"; break;
//            case LESS : op = "compareTo"; break;
//            case LESS_EQUALS : op = "compareTo"; break;
//        }
//
//        MethodCallExpr methodCallExpr;
//
//        List<Expression> children = getArgumentsWithUnwrap(e2);
//        if (!op.equals("compareTo") && e1Type.equals(bigDecimalType)) {
//            // an array is used, because BigDecimal takes a second argument for the mathContext
//            children.add(mathContext.clone());
//        }
//
//        methodCallExpr = new MethodCallExpr(op, children.toArray( new Expression[0]));
//        methodCallExpr.setScope(e1);
//        result = methodCallExpr;
//
//        if (op.equals("compareTo")) {
//            result = new BinaryExpr(methodCallExpr, new IntegerLiteralExpr("0"), opEnum);
//        }
//
//        return result;
//    }
}

