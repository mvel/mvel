package org.mvel3.parser.printer;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType.Primitive;
import com.github.javaparser.resolution.types.ResolvedType;
import org.mvel3.transpiler.MVELTranspiler;
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

/**
 * the Expression being coerced must be cloned, rather than added. There are times that adding it doesn't always remove it from previous location.
 * So it sits in both locations and then removal from the oroginal location null's it's parent field.
 */
public class CoerceRewriter {

    public static final Primitive[] INTEGER_PRIMITIVES = new Primitive[] {Primitive.CHAR,
                                                                          Primitive.SHORT,
                                                                          Primitive.INT,
                                                                          Primitive.LONG};

    public static final Primitive[] FLOAT_PRIMITIVES = new Primitive[] {Primitive.CHAR,
                                                                        Primitive.SHORT,
                                                                        Primitive.INT,
                                                                        Primitive.LONG,
                                                                        Primitive.FLOAT,
                                                                        Primitive.DOUBLE};

    TranspilerContext context;


    private Map<Key, Function<Expression, Expression>> coercions = new HashMap<>();

    public CoerceRewriter(TranspilerContext context) {
        this.context = context;

        bigDecimalCoercion();
        bigIntegerCoercion();

        integerToDateCoercion();
        dateToLongCoercion();

        numberToStringCoercion();
        stringToNumberCoercion();

    }

    private void bigDecimalCoercion() {
        ClassOrInterfaceType bigDecimalClass = MVELTranspiler.handleParserResult(context.getParser().parseClassOrInterfaceType(BigDecimal.class.getSimpleName()));

        Function<Expression, Expression> toBigDecimal = e -> {
            List<Expression> args = getArgumentsWithUnwrap(e.clone());

            if (e.isLiteralExpr()) {
                String literal;
                if (e.isStringLiteralExpr()) {
                    literal = e.asStringLiteralExpr().asString();
                } else if (e.isIntegerLiteralExpr()) {
                    literal = e.asIntegerLiteralExpr().getValue();
                } else {
                    literal = e.toString();
                }

                literal = trim(literal);
                return new ObjectCreationExpr(null, bigDecimalClass.clone(),  NodeList.nodeList(new StringLiteralExpr(literal)));
            } else {
                return new MethodCallExpr(new NameExpr(BigDecimal.class.getSimpleName()), "valueOf", NodeList.nodeList(args));
            }

        };

        String bigDecimal = BigDecimal.class.getCanonicalName();

        Arrays.stream(FLOAT_PRIMITIVES).forEach(p -> {
            coercions.put(key(p.name().toUpperCase(), bigDecimal), toBigDecimal);
            coercions.put(key("java.lang." + p.toBoxedType(), bigDecimal), toBigDecimal);
        });
    }

    private void bigIntegerCoercion() {
        ClassOrInterfaceType bigIntegerClass = MVELTranspiler.handleParserResult(context.getParser().parseClassOrInterfaceType(BigInteger.class.getSimpleName()));

        Function<Expression, Expression> toBigInteger = e -> {
            List<Expression> args = getArgumentsWithUnwrap(e.clone());

            if (e.isLiteralExpr()) {
                String literal;
                if (e.isStringLiteralExpr()) {
                    literal = e.asStringLiteralExpr().asString();
                } else if (e.isIntegerLiteralExpr()) {
                    literal = e.asIntegerLiteralExpr().getValue();
                } else {
                    literal = e.toString();
                }

                literal = trim(literal);
                return new ObjectCreationExpr(null, bigIntegerClass.clone(),  NodeList.nodeList(new StringLiteralExpr(literal)));
            } else {
                return new MethodCallExpr(new NameExpr(BigInteger.class.getSimpleName()), "valueOf", NodeList.nodeList(args));
            }
        };


        String bigInteger = BigInteger.class.getCanonicalName();

        Arrays.stream(INTEGER_PRIMITIVES).forEach(p -> {
            coercions.put(key(p.name().toUpperCase(), bigInteger), toBigInteger);
            coercions.put(key("java.lang." + p.toBoxedType(), bigInteger), toBigInteger);
        });

    }

    private static String trim(String literal) {
        if (!Character.isDigit(literal.charAt(literal.length() - 1))) {
            literal = literal.substring(0, literal.length() - 1); // strip any suffixes, not necessary
        }
        return literal;
    }

    private void integerToDateCoercion() {
        ParseResult<ClassOrInterfaceType> result = context.getParser().parseClassOrInterfaceType("java.util.Date");
        if (!result.isSuccessful()) {
            throw new RuntimeException("Cannot resolve type:" + result.getProblems());
        }

        final ClassOrInterfaceType dateType = result.getResult().get();

        Function<Expression, Expression> integerToDate = e -> {
            ClassOrInterfaceType type = dateType.clone();

            ObjectCreationExpr expr = new ObjectCreationExpr(null, type, NodeList.nodeList(e.clone()));

            return expr;
        };

        String date = Date.class.getCanonicalName();

        Arrays.stream(INTEGER_PRIMITIVES).forEach(p -> {
            coercions.put(key(p.name().toUpperCase(), date), integerToDate);
            coercions.put(key("java.lang." + p.toBoxedType(), date), integerToDate);
        });
    }


    /**
     * As MVEL3 does not narrow types, Date can only be coerced to long and Long.
     */
    private void dateToLongCoercion() {
        String date = Date.class.getCanonicalName();

        Function<Expression, Expression> toLong = e -> {
            MethodCallExpr getTimeCall = new MethodCallExpr(e.clone(), "getTime");
            return getTimeCall;
        };

        Arrays.stream(new Primitive[] {Primitive.LONG}).forEach(p -> {
            coercions.put(key(date, p.name().toUpperCase()), toLong);
            coercions.put(key(date, "java.lang." + p.toBoxedType()), toLong);
        });

    }

    private void numberToStringCoercion() {
        String string = String.class.getCanonicalName();

        Function<Expression, Expression> numberToString = e -> {
            MethodCallExpr methodCallExpr = new MethodCallExpr(new NameExpr("String"), "valueOf");
            methodCallExpr.addArgument(e.clone());
            return methodCallExpr;
        };

        Function<Expression, Expression> objectToString = e -> {
            MethodCallExpr methodCallExpr = new MethodCallExpr(new FieldAccessExpr(new FieldAccessExpr(new NameExpr("java"), "util"), "Objects"), "toString");
            methodCallExpr.addArgument(e.clone());
            methodCallExpr.addArgument(new NullLiteralExpr());
            return methodCallExpr;
        };



        Arrays.stream(FLOAT_PRIMITIVES).forEach(p -> {
            coercions.put(key(p.name().toUpperCase(), string), numberToString);
            coercions.put(key("java.lang." + p.toBoxedType(), string), objectToString);
        });

        Arrays.stream(new String[] {BigDecimal.class.getCanonicalName(), BigInteger.class.getCanonicalName()}).forEach(p -> {
            coercions.put(key(p, string), objectToString);
        });

    }

    private void stringToNumberCoercion() {
        String string = String.class.getCanonicalName();

        // This class will preserve the coercion for target's type of primitive or Number Object wrapper.

        // This needs a function for each number type.
        Arrays.stream(FLOAT_PRIMITIVES).forEach(p -> {
            Function<Expression, Expression> toNumber = e -> {
                MethodCallExpr methodCallExpr = new MethodCallExpr(new NameExpr(p.toBoxedType().getNameAsString()), "valueOf");
                methodCallExpr.addArgument(e.clone());
                return methodCallExpr;
            };

            coercions.put(key(string, "java.lang." + p.toBoxedType()), toNumber);
        });

        // This needs a function for each number type.
        // ignore Char
        Arrays.stream(FLOAT_PRIMITIVES).forEach(p -> {
            if ( p != Primitive.CHAR) {
                Function<Expression, Expression> toNumber = e -> {
                    String         primName       = p.name().toLowerCase();
                    String         parseName      = "parse" + primName.substring(0, 1).toUpperCase() + primName.substring(1);
                    MethodCallExpr methodCallExpr = new MethodCallExpr(new NameExpr(p.toBoxedType().getNameAsString()), parseName);
                    methodCallExpr.addArgument(e.clone());
                    return methodCallExpr;
                };

                coercions.put(key(string, p.name().toUpperCase()), toNumber);
            }
        });

        // Noq add Char
        // This does not work, so always coerce to char
        // Character x = Character.valueOf("a".charAt(0)); x += Character.valueOf("a".charAt(0));
        Function<Expression, Expression> toNumber = e -> {
            MethodCallExpr methodCallExpr = new MethodCallExpr(e.clone(), "charAt");
            methodCallExpr.addArgument(new IntegerLiteralExpr(0));
            return methodCallExpr;
        };

        coercions.put(key(string, Primitive.CHAR.name().toUpperCase()), toNumber);
        coercions.put(key(string, "java.lang." + Primitive.CHAR.toBoxedType()), toNumber);

        ClassOrInterfaceType bigIntegerClass = MVELTranspiler.handleParserResult(context.getParser().parseClassOrInterfaceType(BigInteger.class.getSimpleName()));
        ClassOrInterfaceType bigDecimalClass = MVELTranspiler.handleParserResult(context.getParser().parseClassOrInterfaceType(BigDecimal.class.getSimpleName()));

        Arrays.stream(new ClassOrInterfaceType[] {bigIntegerClass, bigDecimalClass}).forEach(p -> {
            Function<Expression, Expression> toBigNumber = e -> new ObjectCreationExpr(null, p, NodeList.nodeList(e.clone()));

            coercions.put(key(string, "java.math." + p.getNameAsString()), toBigNumber);
        });
    }

    Key key(String sourceType, String targetType) {
        return Key.create(sourceType, targetType);
    }

    private static class Key {
        private String sourceType;
        private String targetType;

        public static Key create(String sourceType, String targetType) {
            return new Key(sourceType, targetType);
        }

        public Key(String sourceType, String targetType) {
            this.sourceType = sourceType;
            this.targetType = targetType;
        }

        public String getSourceType() {
            return sourceType;
        }

        public String getTargetType() {
            return targetType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Key that = (Key) o;

            if (!sourceType.equals(that.sourceType)) {
                return false;
            }
            return targetType.equals(that.targetType);
        }

        @Override
        public int hashCode() {
            int result = sourceType.hashCode();
            result = 31 * result + targetType.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "Key{" +
                   "sourceType='" + sourceType + '\'' +
                   ", targetType='" + targetType + '\'' +
                   '}';
        }
    }

    public Expression coerce(ResolvedType sourceType, Expression sourceExpr, ResolvedType targetType) {
        Key key = key(describe(sourceType),
                      describe(targetType));
        if (sourceType.isNull()) {
            return null;
        }

        Function<Expression, Expression> coerce = coercions.get(key);

        if (coerce != null) {
            return coerce.apply(sourceExpr);
        }

        return null;
    }

    private String describe(ResolvedType type) {
        return type.isPrimitive() ? type.describe().toUpperCase() : type.describe();
    }
}

