/**
 * MVEL (The MVFLEX Expression Language)
 *
 * Copyright (C) 2007 Christopher Brock, MVFLEX/Valhalla Project and the Codehaus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.mvel.math;

import org.mvel.CompileException;
import org.mvel.ConversionException;
import static org.mvel.DataConversion.convert;
import org.mvel.DataTypes;
import static org.mvel.DataTypes.EMPTY;
import org.mvel.Operator;
import static org.mvel.util.ParseTools.resolveType;
import static org.mvel.util.PropertyTools.isNumber;

import static java.lang.String.valueOf;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * @author Christopher Brock
 */
public class JDK14CompatabilityMath implements MathProcessor {
    public static final int ROUND_MODE = BigDecimal.ROUND_CEILING;
    public static final int SCALE = 128;


    public Object doOperation(Object val1, int operation, Object val2) {
        int type1 = val1 == null ? DataTypes.NULL : resolveType(val1.getClass());
        int type2 = val2 == null ? DataTypes.NULL : resolveType(val2.getClass());

        if (type1 == DataTypes.BIG_DECIMAL) {
            if (type2 == DataTypes.BIG_DECIMAL) {
                return doBigDecimalArithmetic((BigDecimal) val1, operation, (BigDecimal) val2);
            }
            else if (type2 > 99) {
                return doBigDecimalArithmetic((BigDecimal) val1, operation, getBigDecimalFromType(val2, type2));
            }
            else {
                return _doOperations(type1, val1, operation, type2, val2);
            }
        }
        else if (type2 == DataTypes.BIG_DECIMAL && (type1 > 99 || (type1 == DataTypes.STRING && isNumber(val1)))) {
            return doBigDecimalArithmetic(getBigDecimalFromType(val1, type1), operation, (BigDecimal) val2);
        }
        else {
            return _doOperations(type1, val1, operation, type2, val2);
        }


    }

    private static Object doBigDecimalArithmetic(BigDecimal val1, int operation, BigDecimal val2) {
        switch (operation) {
            case Operator.ADD:
                return val1.add(val2);
            case Operator.DIV:
                return val1.divide(val2, SCALE, ROUND_MODE);
            case Operator.SUB:
                return val1.subtract(val2);
            case Operator.MULT:
                return val1.multiply(val2);
            case Operator.POWER:
                return Math.pow(val1.doubleValue(), val2.doubleValue());
            case Operator.MOD:
                return val1.doubleValue() % val2.doubleValue();
            case Operator.GTHAN:
                return val1.compareTo(val2) == 1 ? Boolean.TRUE : Boolean.FALSE;
            case Operator.GETHAN:
                return val1.compareTo(val2) >= 0 ? Boolean.TRUE : Boolean.FALSE;
            case Operator.LTHAN:
                return val1.compareTo(val2) == -1 ? Boolean.TRUE : Boolean.FALSE;
            case Operator.LETHAN:
                return val1.compareTo(val2) <= 0 ? Boolean.TRUE : Boolean.FALSE;
            case Operator.EQUAL:
                return val1.compareTo(val2) == 0 ? Boolean.TRUE : Boolean.FALSE;
            case Operator.NEQUAL:
                return val1.compareTo(val2) != 0 ? Boolean.TRUE : Boolean.FALSE;
        }
        return null;
    }

    private static Object _doOperations(int type1, Object val1, int operation, int type2, Object val2) {
        if (operation < 10 || operation == Operator.EQUAL || operation == Operator.NEQUAL) {
            if (type1 > 99 && type1 == type2) {
                return doOperationsSameType(type1, val1, operation, val2);
            }
            else if ((type1 > 99 && (type2 > 99)) || (isNumber(val1) && isNumber(val2))) {
                return doBigDecimalArithmetic(getBigDecimalFromType(val1, type1), operation, getBigDecimalFromType(val2, type2));
            }
            else if ((type1 == 15 || type2 == 15) && type1 != type2 && type1 != EMPTY && type2 != EMPTY) {
                return doOperationNonNumeric(convert(val1, Boolean.class), operation, convert(val2, Boolean.class));
            }
            // Fix for: MVEL-56
            else if ((type1 == 1 || type2 == 1) && (type1 == 8 || type1 == 16 || type2 == 8 || type2 == 16)) {
                if (type1 == 1) {
                    return doOperationNonNumeric(val1, operation, valueOf(val2));
                }
                else {
                    return doOperationNonNumeric(valueOf(val1), operation, val2);
                }
            }
        }
        return doOperationNonNumeric(val1, operation, val2);
    }

    private static Object doOperationNonNumeric(Object val1, int operation, Object val2) {
        switch (operation) {
            case Operator.ADD:
                return valueOf(val1) + valueOf(val2);

            case Operator.EQUAL:
                return safeEquals(val2, val1);

            case Operator.NEQUAL:
                return safeNotEquals(val2, val1);

            case Operator.SUB:
            case Operator.DIV:
            case Operator.MULT:
            case Operator.MOD:
            case Operator.GTHAN:
            case Operator.GETHAN:
            case Operator.LTHAN:
            case Operator.LETHAN:
                throw new CompileException("could not perform numeric operation on non-numeric types: left-type="
                        + (val1 != null ? val1.getClass().getName() : "null") + "; right-type=" + (val2 != null ? val2.getClass().getName() : "null"));
        }

        throw new CompileException("unable to perform operation");
    }

    private static Boolean safeEquals(Object val1, Object val2) {
        if (val1 != null) {
            return val1.equals(val2);
        }
        else if (val2 != null) {
            return val2.equals(val1);
        }
        else {
            return val1 == val2;
        }
    }

    private static Boolean safeNotEquals(Object val1, Object val2) {
        if (val1 != null) {
            return !val1.equals(val2);
        }
        else return val2 != null && !val2.equals(val1);
    }

    private static Object doOperationsSameType(int type1, Object val1, int operation, Object val2) {
        switch (type1) {
            case DataTypes.INTEGER:
            case DataTypes.W_INTEGER:
                switch (operation) {
                    case Operator.ADD:
                        return ((Integer) val1) + ((Integer) val2);
                    case Operator.SUB:
                        return ((Integer) val1) - ((Integer) val2);
                    case Operator.DIV:
                        return new BigDecimal((Integer) val1).divide(new BigDecimal((Integer) val2), SCALE, ROUND_MODE);
                    case Operator.MULT:
                        return ((Integer) val1) * ((Integer) val2);
                    case Operator.POWER:
                        double d = Math.pow((Integer) val1, (Integer) val2);
                        if (d > Integer.MAX_VALUE) return d;
                        else return (int) d;
                    case Operator.MOD:
                        return ((Integer) val1) % ((Integer) val2);

                    case Operator.GTHAN:
                        return ((Integer) val1) > ((Integer) val2) ? Boolean.TRUE : Boolean.FALSE;
                    case Operator.GETHAN:
                        return ((Integer) val1) >= ((Integer) val2) ? Boolean.TRUE : Boolean.FALSE;
                    case Operator.LTHAN:
                        return ((Integer) val1) < ((Integer) val2) ? Boolean.TRUE : Boolean.FALSE;
                    case Operator.LETHAN:
                        return ((Integer) val1) <= ((Integer) val2) ? Boolean.TRUE : Boolean.FALSE;
                    case Operator.EQUAL:
                        return ((Integer) val1).intValue() == ((Integer) val2).intValue() ? Boolean.TRUE : Boolean.FALSE;
                    case Operator.NEQUAL:
                        return ((Integer) val1).intValue() != ((Integer) val2).intValue() ? Boolean.TRUE : Boolean.FALSE;

                }

            case DataTypes.SHORT:
            case DataTypes.W_SHORT:
                switch (operation) {
                    case Operator.ADD:
                        return ((Short) val1) + ((Short) val2);
                    case Operator.SUB:
                        return ((Short) val1) - ((Short) val2);
                    case Operator.DIV:
                        return new BigDecimal((Short) val1).divide(new BigDecimal((Short) val2), SCALE, ROUND_MODE);
                    case Operator.MULT:
                        return ((Short) val1) * ((Short) val2);
                    case Operator.POWER:
                        double d = Math.pow((Short) val1, (Short) val2);
                        if (d > Short.MAX_VALUE) return d;
                        else return (short) d;
                    case Operator.MOD:
                        return ((Short) val1) % ((Short) val2);

                    case Operator.GTHAN:
                        return ((Short) val1) > ((Short) val2) ? Boolean.TRUE : Boolean.FALSE;
                    case Operator.GETHAN:
                        return ((Short) val1) >= ((Short) val2) ? Boolean.TRUE : Boolean.FALSE;
                    case Operator.LTHAN:
                        return ((Short) val1) < ((Short) val2) ? Boolean.TRUE : Boolean.FALSE;
                    case Operator.LETHAN:
                        return ((Short) val1) <= ((Short) val2) ? Boolean.TRUE : Boolean.FALSE;
                    case Operator.EQUAL:
                        return ((Short) val1).shortValue() == ((Short) val2).shortValue() ? Boolean.TRUE : Boolean.FALSE;
                    case Operator.NEQUAL:
                        return ((Short) val1).shortValue() != ((Short) val2).shortValue() ? Boolean.TRUE : Boolean.FALSE;
                }

            case DataTypes.LONG:
            case DataTypes.W_LONG:
                switch (operation) {
                    case Operator.ADD:
                        return ((Long) val1) + ((Long) val2);
                    case Operator.SUB:
                        return ((Long) val1) - ((Long) val2);
                    case Operator.DIV:
                        return new BigDecimal((Long) val1).divide(new BigDecimal((Long) val2), SCALE, ROUND_MODE);
                    case Operator.MULT:
                        return ((Long) val1) * ((Long) val2);
                    case Operator.POWER:
                        double d = Math.pow((Long) val1, (Long) val2);
                        if (d > Long.MAX_VALUE) return d;
                        else return (long) d;
                    case Operator.MOD:
                        return ((Long) val1) % ((Long) val2);

                    case Operator.GTHAN:
                        return ((Long) val1) > ((Long) val2) ? Boolean.TRUE : Boolean.FALSE;
                    case Operator.GETHAN:
                        return ((Long) val1) >= ((Long) val2) ? Boolean.TRUE : Boolean.FALSE;
                    case Operator.LTHAN:
                        return ((Long) val1) < ((Long) val2) ? Boolean.TRUE : Boolean.FALSE;
                    case Operator.LETHAN:
                        return ((Long) val1) <= ((Long) val2) ? Boolean.TRUE : Boolean.FALSE;
                    case Operator.EQUAL:
                        return ((Long) val1).longValue() == ((Long) val2).longValue() ? Boolean.TRUE : Boolean.FALSE;
                    case Operator.NEQUAL:
                        return ((Long) val1).longValue() != ((Long) val2).longValue() ? Boolean.TRUE : Boolean.FALSE;
                }

            case DataTypes.DOUBLE:
            case DataTypes.W_DOUBLE:
                switch (operation) {
                    case Operator.ADD:
                        return ((Double) val1) + ((Double) val2);
                    case Operator.SUB:
                        return ((Double) val1) - ((Double) val2);
                    case Operator.DIV:
                        return new BigDecimal((Double) val1).divide(new BigDecimal((Double) val2), SCALE, ROUND_MODE);
                    case Operator.MULT:
                        return ((Double) val1) * ((Double) val2);
                    case Operator.POWER:
                        return Math.pow((Double) val1, (Double) val2);
                    case Operator.MOD:
                        return ((Double) val1) % ((Double) val2);

                    case Operator.GTHAN:
                        return ((Double) val1) > ((Double) val2) ? Boolean.TRUE : Boolean.FALSE;
                    case Operator.GETHAN:
                        return ((Double) val1) >= ((Double) val2) ? Boolean.TRUE : Boolean.FALSE;
                    case Operator.LTHAN:
                        return ((Double) val1) < ((Double) val2) ? Boolean.TRUE : Boolean.FALSE;
                    case Operator.LETHAN:
                        return ((Double) val1) <= ((Double) val2) ? Boolean.TRUE : Boolean.FALSE;
                    case Operator.EQUAL:
                        return ((Double) val1).doubleValue() == ((Double) val2).doubleValue() ? Boolean.TRUE : Boolean.FALSE;
                    case Operator.NEQUAL:
                        return ((Double) val1).doubleValue() != ((Double) val2).doubleValue() ? Boolean.TRUE : Boolean.FALSE;
                }

            case DataTypes.FLOAT:
            case DataTypes.W_FLOAT:
                switch (operation) {
                    case Operator.ADD:
                        return ((Float) val1) + ((Float) val2);
                    case Operator.SUB:
                        return ((Float) val1) - ((Float) val2);
                    case Operator.DIV:
                        return new BigDecimal((Float) val1).divide(new BigDecimal((Float) val2), SCALE, ROUND_MODE);
                    case Operator.MULT:
                        return ((Float) val1) * ((Float) val2);
                    case Operator.POWER:
                        Math.pow((Float) val1, (Float) val2);
                    case Operator.MOD:
                        return ((Float) val1) % ((Float) val2);

                    case Operator.GTHAN:
                        return ((Float) val1) > ((Float) val2) ? Boolean.TRUE : Boolean.FALSE;
                    case Operator.GETHAN:
                        return ((Float) val1) >= ((Float) val2) ? Boolean.TRUE : Boolean.FALSE;
                    case Operator.LTHAN:
                        return ((Float) val1) < ((Float) val2) ? Boolean.TRUE : Boolean.FALSE;
                    case Operator.LETHAN:
                        return ((Float) val1) <= ((Float) val2) ? Boolean.TRUE : Boolean.FALSE;
                    case Operator.EQUAL:
                        return ((Float) val1).floatValue() == ((Float) val2).floatValue() ? Boolean.TRUE : Boolean.FALSE;
                    case Operator.NEQUAL:
                        return ((Float) val1).floatValue() != ((Float) val2).floatValue() ? Boolean.TRUE : Boolean.FALSE;
                }


            case DataTypes.BIG_INTEGER:
                switch (operation) {
                    case Operator.ADD:
                        return ((BigInteger) val1).add(((BigInteger) val2));
                    case Operator.SUB:
                        return ((BigInteger) val1).subtract(((BigInteger) val2));
                    case Operator.DIV:
                        return ((BigInteger) val1).divide(((BigInteger) val2));
                    case Operator.MULT:
                        return ((BigInteger) val1).multiply(((BigInteger) val2));
                    case Operator.POWER:
                        return ((BigInteger) val1).pow(((BigInteger) val2).intValue());
                    case Operator.MOD:
                        return ((BigInteger) val1).remainder(((BigInteger) val2));

                    case Operator.GTHAN:
                        return ((BigInteger) val1).compareTo(((BigInteger) val2)) == 1 ? Boolean.TRUE : Boolean.FALSE;
                    case Operator.GETHAN:
                        return ((BigInteger) val1).compareTo(((BigInteger) val2)) >= 0 ? Boolean.TRUE : Boolean.FALSE;
                    case Operator.LTHAN:
                        return ((BigInteger) val1).compareTo(((BigInteger) val2)) == -1 ? Boolean.TRUE : Boolean.FALSE;
                    case Operator.LETHAN:
                        return ((BigInteger) val1).compareTo(((BigInteger) val2)) <= 0 ? Boolean.TRUE : Boolean.FALSE;
                    case Operator.EQUAL:
                        return ((BigInteger) val1).compareTo(((BigInteger) val2)) == 0 ? Boolean.TRUE : Boolean.FALSE;
                    case Operator.NEQUAL:
                        return ((BigInteger) val1).compareTo(((BigInteger) val2)) != 0 ? Boolean.TRUE : Boolean.FALSE;

                }

            default:
                switch (operation) {
                    case Operator.EQUAL:
                        return safeEquals(val2, val1) ? Boolean.TRUE : Boolean.FALSE;
                    case Operator.NEQUAL:
                        return safeNotEquals(val2, val1) ? Boolean.TRUE : Boolean.FALSE;
                    case Operator.ADD:
                        return valueOf(val1) + valueOf(val2);
                }
        }
        return null;
    }

    public static BigDecimal getBigDecimalFromType(Object in, int type) {
        if (in == null)
            return new BigDecimal(0);
        switch (type) {
            case DataTypes.BIG_DECIMAL:
                return (BigDecimal) in;
            case DataTypes.BIG_INTEGER:
                return new BigDecimal((BigInteger) in);
            case DataTypes.W_INTEGER:
                return BigDecimal.valueOf((Integer) in);
            case DataTypes.W_LONG:
                return BigDecimal.valueOf((Long) in);
            case DataTypes.STRING:
                return new BigDecimal((String) in);
            case DataTypes.W_FLOAT:
                return new BigDecimal((Float) in);
            case DataTypes.W_DOUBLE:
                return new BigDecimal((Double) in);
            case DataTypes.W_SHORT:
                return BigDecimal.valueOf((Short) in);
            case DataTypes.W_BOOLEAN:
                return BigDecimal.valueOf(((Boolean) in) ? 1 : 0);

        }

        throw new ConversionException("cannot convert <" + in + "> to a numeric type");
    }
}
