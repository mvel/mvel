package org.mvel2;

import org.junit.Test;
import org.mvel2.util.CompatibilityStrategy;
import org.mvel2.util.NullType;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

import static org.junit.Assert.assertNotNull;

public class AnalyserTest {

    public class MVELDateCoercion implements ConversionHandler {

        public boolean canConvertFrom(Class cls) {
            return cls == String.class || cls.isAssignableFrom( Date.class );
        }

        public Object convertFrom(Object o) {
            if (o instanceof String) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");
                try {
                    return sdf.parse( (String) o );
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            } else {
                return o;
            }
        }

    }

    @Test
    public void testCoercionDuringAnalyze() {
        CompatibilityStrategy.setCompatibilityEvaluator(StringCoercionCompatibilityEvaluator.INSTANCE);

        DataConversion.addConversionHandler(Date.class,
                                            new MVELDateCoercion());

        String expr = "f.departureTime >= \"01-Jan-1970\" && f.departureTime <= \"01-Jan-2018\"";

        ParserContext ctx = new ParserContext();
        ctx.setStrongTyping(true);
        ctx.setStrictTypeEnforcement(true);
        ctx.addInput("f", Flight.class);

        Object o = MVEL.compileExpression( expr, ctx );

        Class cls = MVEL.analyze(expr, ctx );

        assertNotNull(cls);
    }

    public static class Flight {
        private Date departureTime;

        public Flight(Date departureTime) {
            this.departureTime = departureTime;
        }

        public Date getDepartureTime() {
            return departureTime;
        }

        public void setDepartureTime(Date departureTime) {
            this.departureTime = departureTime;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Flight flight = (Flight) o;

            return Objects.equals(departureTime, flight.departureTime);
        }

        @Override
        public int hashCode() {
            return departureTime != null ? departureTime.hashCode() : 0;
        }

        @Override
        public String toString() {
            return "Flight{" +
                   "departureTime=" + departureTime +
                   '}';
        }
    }

    public static class StringCoercionCompatibilityEvaluator extends CompatibilityStrategy.DefaultCompatibilityEvaluator {

        public static final CompatibilityStrategy.CompatibilityEvaluator INSTANCE = new StringCoercionCompatibilityEvaluator();

        private StringCoercionCompatibilityEvaluator() { }

        @Override
        public boolean areEqualityCompatible(Class<?> c1, Class<?> c2) {
            if (c1 == NullType.class || c2 == NullType.class) {
                return true;
            }
            if (c1 == String.class || c2 == String.class) {
                return true;
            }
            Class<?> boxed1 = convertFromPrimitiveType(c1);
            Class<?> boxed2 = convertFromPrimitiveType(c2);

            if (boxed1.isAssignableFrom(boxed2) || boxed2.isAssignableFrom(boxed1)) {
                return true;
            }

            if (Number.class.isAssignableFrom(boxed1) && Number.class.isAssignableFrom(boxed2)) {
                return true;
            }
            if (areEqualityCompatibleEnums(boxed1, boxed2)) {
                return true;
            }

            if ( DataConversion.canConvert(boxed1, boxed2) && DataConversion.canConvert(boxed2, boxed1) ) {
                return true;
            }
            return !Modifier.isFinal(c1.getModifiers()) && !Modifier.isFinal(c2.getModifiers());
        }

        protected boolean areEqualityCompatibleEnums(final Class<?> boxed1,
                                                     final Class<?> boxed2) {
            return boxed1.isEnum() && boxed2.isEnum() && boxed1.getName().equals(boxed2.getName())
                   && equalEnumConstants(boxed1.getEnumConstants(), boxed2.getEnumConstants());
        }

        private boolean equalEnumConstants(final Object[] aa,
                                           final Object[] bb) {
            if (aa.length != bb.length) {
                return false;
            }
            for (int i = 0; i < aa.length; i++) {
                if (!Objects.equals(aa[i].getClass().getName(), bb[i].getClass().getName())) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean areComparisonCompatible(Class<?> c1, Class<?> c2) {
            return areEqualityCompatible(c1, c2);
        }
    }

    public static Class<?> convertFromPrimitiveType(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        if (type == short.class) return Short.class;
        if (type == byte.class) return Byte.class;
        if (type == char.class) return Character.class;
        if (type == boolean.class) return Boolean.class;
        throw new RuntimeException("Class not convertible from primitive: " + type.getName());
    }

    public static Class<?> convertToPrimitiveType(Class<?> type) {
        if (type.isPrimitive()) return type;
        if (type == Integer.class) return int.class;
        if (type == Long.class) return long.class;
        if (type == Float.class) return float.class;
        if (type == Double.class) return double.class;
        if (type == Short.class) return short.class;
        if (type == Byte.class) return byte.class;
        if (type == Character.class) return char.class;
        if (type == Boolean.class) return boolean.class;
        if (type == BigInteger.class) return long.class;
        if (type == BigDecimal.class) return double.class;
        if (type == Number.class) return double.class;
        throw new RuntimeException("Class not convertible to primitive: " + type.getName());
    }
}
