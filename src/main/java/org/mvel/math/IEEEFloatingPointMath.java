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
import static org.mvel.Operator.*;
import org.mvel.Unit;
import org.mvel.debug.DebugTools;
import static org.mvel.util.ParseTools.resolveType;
import static org.mvel.util.PropertyTools.isNumber;

import static java.lang.String.valueOf;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

/**
 * @author Christopher Brock
 */
public class IEEEFloatingPointMath implements MathProcessor {
    private static final MathContext MATH_CONTEXT = MathContext.DECIMAL128;

    public Object doOperation(final Object val1, final int operation, final Object val2) {
        final int type1 = val1 == null ? DataTypes.NULL : resolveType(val1.getClass());
        final int type2 = val2 == null ? DataTypes.NULL : resolveType(val2.getClass());

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

    private static Object doBigDecimalArithmetic(final BigDecimal val1, final int operation, final BigDecimal val2) {
        switch (operation) {
            case ADD:
                return val1.add(val2);
            case DIV:
                return val1.divide(val2, MATH_CONTEXT);
            case SUB:
                return val1.subtract(val2);
            case MULT:
                return val1.multiply(val2);
            case POWER:
                return Math.pow(val1.doubleValue(), val2.doubleValue());
            case MOD:
                return val1.remainder(val2);

            case GTHAN:
                return val1.compareTo(val2) == 1 ? Boolean.TRUE : Boolean.FALSE;
            case GETHAN:
                return val1.compareTo(val2) >= 0 ? Boolean.TRUE : Boolean.FALSE;
            case LTHAN:
                return val1.compareTo(val2) == -1 ? Boolean.TRUE : Boolean.FALSE;
            case LETHAN:
                return val1.compareTo(val2) <= 0 ? Boolean.TRUE : Boolean.FALSE;
            case EQUAL:
                return val1.compareTo(val2) == 0 ? Boolean.TRUE : Boolean.FALSE;
            case NEQUAL:
                return val1.compareTo(val2) != 0 ? Boolean.TRUE : Boolean.FALSE;
        }
        return null;
    }

    private static Object _doOperations(final int type1, final Object val1, final int operation, final int type2, final Object val2) {
        if (operation < 10 || operation == EQUAL || operation == NEQUAL) {
            if (type1 > 99 && type1 == type2) {
                return doOperationsSameType(type1, val1, operation, val2);
            }
            else if ((type1 > 99 && (type2 > 99)) || (isNumber(val1) && isNumber(val2))) {
                return doBigDecimalArithmetic(getBigDecimalFromType(val1, type1), operation, getBigDecimalFromType(val2, type2));
            }
            else
            if (operation != ADD && (type1 == 15 || type2 == 15) && type1 != type2 && type1 != EMPTY && type2 != EMPTY) {
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

    private static Object doOperationNonNumeric(final Object val1, final int operation, final Object val2) {
        switch (operation) {
            case ADD:
                return valueOf(val1) + valueOf(val2);

            case EQUAL:
                return safeEquals(val2, val1) ? Boolean.TRUE : Boolean.FALSE;

            case NEQUAL:
                return safeNotEquals(val2, val1) ? Boolean.TRUE : Boolean.FALSE;

            case SUB:
            case DIV:
            case MULT:
            case MOD:
            case GTHAN:
                if (val1 instanceof Comparable) {
                    //noinspection unchecked
                    return ((Comparable) val1).compareTo(val2) == 1 ? Boolean.TRUE : Boolean.FALSE;
                }
                break;

            case GETHAN:
                if (val1 instanceof Comparable) {
                    //noinspection unchecked
                    return ((Comparable) val1).compareTo(val2) >= 0 ? Boolean.TRUE : Boolean.FALSE;
                }
                break;

            case LTHAN:
                if (val1 instanceof Comparable) {
                    //noinspection unchecked
                    return ((Comparable) val1).compareTo(val2) == -1 ? Boolean.TRUE : Boolean.FALSE;
                }
                break;

            case LETHAN:
                if (val1 instanceof Comparable) {
                    //noinspection unchecked
                    return ((Comparable) val1).compareTo(val2) <= 0 ? Boolean.TRUE : Boolean.FALSE;
                }
                break;
        }

        throw new CompileException("could not perform numeric operation on non-numeric types: left-type="
                + (val1 != null ? val1.getClass().getName() : "null") + "; right-type="
                + (val2 != null ? val2.getClass().getName() : "null")
                + " [vals (" + valueOf(val1) + ", " + valueOf(val2) + ") operation=" + DebugTools.getOperatorName(operation) + " ]");

    }

    private static Boolean safeEquals(final Object val1, final Object val2) {
        if (val1 != null) {
            return val1.equals(val2) ? Boolean.TRUE : Boolean.FALSE;
        }
        else if (val2 != null) {
            return val2.equals(val1) ? Boolean.TRUE : Boolean.FALSE;
        }
        else {
            return val1 == val2;
        }
    }

    private static Boolean safeNotEquals(final Object val1, final Object val2) {
        if (val1 != null) {
            return !val1.equals(val2) ? Boolean.TRUE : Boolean.FALSE;
        }
        else return (val2 != null && !val2.equals(val1)) ? Boolean.TRUE : Boolean.FALSE;
    }

    private static Object doOperationsSameType(int type1, Object val1, int operation, Object val2) {
        switch (type1) {
            case DataTypes.INTEGER:
            case DataTypes.W_INTEGER:
                switch (operation) {
                    case ADD:
                        return ((Integer) val1) + ((Integer) val2);
                    case SUB:
                        return ((Integer) val1) - ((Integer) val2);
                    case DIV:
                        return new BigDecimal((Integer) val1).divide(new BigDecimal((Integer) val2), MATH_CONTEXT);
                    case MULT:
                      //  return ((Integer) val1) * ((Integer) val2);
                        return new BigDecimal((Integer) val1).multiply(new BigDecimal((Integer) val2), MATH_CONTEXT);
                    case POWER:
                        double d = Math.pow((Integer) val1, (Integer) val2);
                        if (d > Integer.MAX_VALUE) return d;
                        else return (int) d;
                    case MOD:
                        return ((Integer) val1) % ((Integer) val2);

                    case GTHAN:
                        return ((Integer) val1) > ((Integer) val2) ? Boolean.TRUE : Boolean.FALSE;
                    case GETHAN:
                        return ((Integer) val1) >= ((Integer) val2) ? Boolean.TRUE : Boolean.FALSE;
                    case LTHAN:
                        return ((Integer) val1) < ((Integer) val2) ? Boolean.TRUE : Boolean.FALSE;
                    case LETHAN:
                        return ((Integer) val1) <= ((Integer) val2) ? Boolean.TRUE : Boolean.FALSE;
                    case EQUAL:
                        return ((Integer) val1).intValue() == ((Integer) val2).intValue() ? Boolean.TRUE : Boolean.FALSE;
                    case NEQUAL:
                        return ((Integer) val1).intValue() != ((Integer) val2).intValue() ? Boolean.TRUE : Boolean.FALSE;

                }

            case DataTypes.SHORT:
            case DataTypes.W_SHORT:
                switch (operation) {
                    case ADD:
                        return ((Short) val1) + ((Short) val2);
                    case SUB:
                        return ((Short) val1) - ((Short) val2);
                    case DIV:
                        return new BigDecimal((Short) val1).divide(new BigDecimal((Short) val2), MATH_CONTEXT);
                    case MULT:
                        return ((Short) val1) * ((Short) val2);
                    case POWER:
                        double d = Math.pow((Short) val1, (Short) val2);
                        if (d > Short.MAX_VALUE) return d;
                        else return (short) d;
                    case MOD:
                        return ((Short) val1) % ((Short) val2);

                    case GTHAN:
                        return ((Short) val1) > ((Short) val2) ? Boolean.TRUE : Boolean.FALSE;
                    case GETHAN:
                        return ((Short) val1) >= ((Short) val2) ? Boolean.TRUE : Boolean.FALSE;
                    case LTHAN:
                        return ((Short) val1) < ((Short) val2) ? Boolean.TRUE : Boolean.FALSE;
                    case LETHAN:
                        return ((Short) val1) <= ((Short) val2) ? Boolean.TRUE : Boolean.FALSE;
                    case EQUAL:
                        return ((Short) val1).shortValue() == ((Short) val2).shortValue() ? Boolean.TRUE : Boolean.FALSE;
                    case NEQUAL:
                        return ((Short) val1).shortValue() != ((Short) val2).shortValue() ? Boolean.TRUE : Boolean.FALSE;
                }

            case DataTypes.LONG:
            case DataTypes.W_LONG:
                switch (operation) {
                    case ADD:
                        return ((Long) val1) + ((Long) val2);
                    case SUB:
                        return ((Long) val1) - ((Long) val2);
                    case DIV:
                        return new BigDecimal((Long) val1).divide(new BigDecimal((Long) val2), MATH_CONTEXT);
                    case MULT:
                        return ((Long) val1) * ((Long) val2);
                    case POWER:
                        double d = Math.pow((Long) val1, (Long) val2);
                        if (d > Long.MAX_VALUE) return d;
                        else return (long) d;
                    case MOD:
                        return ((Long) val1) % ((Long) val2);

                    case GTHAN:
                        return ((Long) val1) > ((Long) val2) ? Boolean.TRUE : Boolean.FALSE;
                    case GETHAN:
                        return ((Long) val1) >= ((Long) val2) ? Boolean.TRUE : Boolean.FALSE;
                    case LTHAN:
                        return ((Long) val1) < ((Long) val2) ? Boolean.TRUE : Boolean.FALSE;
                    case LETHAN:
                        return ((Long) val1) <= ((Long) val2) ? Boolean.TRUE : Boolean.FALSE;
                    case EQUAL:
                        return ((Long) val1).longValue() == ((Long) val2).longValue() ? Boolean.TRUE : Boolean.FALSE;
                    case NEQUAL:
                        return ((Long) val1).longValue() != ((Long) val2).longValue() ? Boolean.TRUE : Boolean.FALSE;
                }

            case DataTypes.UNIT:
                val2 = ((Unit) val1).convertFrom(val2);
                val1 = ((Unit) val1).getValue();

            case DataTypes.DOUBLE:
            case DataTypes.W_DOUBLE:
                switch (operation) {
                    case ADD:
                        return ((Double) val1) + ((Double) val2);
                    case SUB:
                        return ((Double) val1) - ((Double) val2);
                    case DIV:
                        return new BigDecimal((Double) val1).divide(new BigDecimal((Double) val2), MATH_CONTEXT);
                    case MULT:
                        return ((Double) val1) * ((Double) val2);
                    case POWER:
                        return Math.pow((Double) val1, (Double) val2);
                    case MOD:
                        return ((Double) val1) % ((Double) val2);

                    case GTHAN:
                        return ((Double) val1) > ((Double) val2) ? Boolean.TRUE : Boolean.FALSE;
                    case GETHAN:
                        return ((Double) val1) >= ((Double) val2) ? Boolean.TRUE : Boolean.FALSE;
                    case LTHAN:
                        return ((Double) val1) < ((Double) val2) ? Boolean.TRUE : Boolean.FALSE;
                    case LETHAN:
                        return ((Double) val1) <= ((Double) val2) ? Boolean.TRUE : Boolean.FALSE;
                    case EQUAL:
                        return ((Double) val1).doubleValue() == ((Double) val2).doubleValue() ? Boolean.TRUE : Boolean.FALSE;
                    case NEQUAL:
                        return ((Double) val1).doubleValue() != ((Double) val2).doubleValue() ? Boolean.TRUE : Boolean.FALSE;
                }

            case DataTypes.FLOAT:
            case DataTypes.W_FLOAT:
                switch (operation) {
                    case ADD:
                        return ((Float) val1) + ((Float) val2);
                    case SUB:
                        return ((Float) val1) - ((Float) val2);
                    case DIV:
                        return new BigDecimal((Float) val1).divide(new BigDecimal((Float) val2), MATH_CONTEXT);
                    case MULT:
                        return ((Float) val1) * ((Float) val2);
                    case POWER:
                        return new BigDecimal((Float) val1).pow(new BigDecimal((Float) val2).intValue());
                    case MOD:
                        return ((Float) val1) % ((Float) val2);

                    case GTHAN:
                        return ((Float) val1) > ((Float) val2) ? Boolean.TRUE : Boolean.FALSE;
                    case GETHAN:
                        return ((Float) val1) >= ((Float) val2) ? Boolean.TRUE : Boolean.FALSE;
                    case LTHAN:
                        return ((Float) val1) < ((Float) val2) ? Boolean.TRUE : Boolean.FALSE;
                    case LETHAN:
                        return ((Float) val1) <= ((Float) val2) ? Boolean.TRUE : Boolean.FALSE;
                    case EQUAL:
                        return ((Float) val1).floatValue() == ((Float) val2).floatValue() ? Boolean.TRUE : Boolean.FALSE;
                    case NEQUAL:
                        return ((Float) val1).floatValue() != ((Float) val2).floatValue() ? Boolean.TRUE : Boolean.FALSE;
                }

            case DataTypes.BIG_INTEGER:
                switch (operation) {
                    case ADD:
                        return ((BigInteger) val1).add(((BigInteger) val2));
                    case SUB:
                        return ((BigInteger) val1).subtract(((BigInteger) val2));
                    case DIV:
                        return ((BigInteger) val1).divide(((BigInteger) val2));
                    case MULT:
                        return ((BigInteger) val1).multiply(((BigInteger) val2));
                    case POWER:
                        return ((BigInteger) val1).pow(((BigInteger) val2).intValue());
                    case MOD:
                        return ((BigInteger) val1).remainder(((BigInteger) val2));

                    case GTHAN:
                        return ((BigInteger) val1).compareTo(((BigInteger) val2)) == 1 ? Boolean.TRUE : Boolean.FALSE;
                    case GETHAN:
                        return ((BigInteger) val1).compareTo(((BigInteger) val2)) >= 0 ? Boolean.TRUE : Boolean.FALSE;
                    case LTHAN:
                        return ((BigInteger) val1).compareTo(((BigInteger) val2)) == -1 ? Boolean.TRUE : Boolean.FALSE;
                    case LETHAN:
                        return ((BigInteger) val1).compareTo(((BigInteger) val2)) <= 0 ? Boolean.TRUE : Boolean.FALSE;
                    case EQUAL:
                        return ((BigInteger) val1).compareTo(((BigInteger) val2)) == 0 ? Boolean.TRUE : Boolean.FALSE;
                    case NEQUAL:
                        return ((BigInteger) val1).compareTo(((BigInteger) val2)) != 0 ? Boolean.TRUE : Boolean.FALSE;

                }


            default:
                switch (operation) {
                    case EQUAL:
                        return safeEquals(val2, val1);
                    case NEQUAL:
                        return safeNotEquals(val2, val1);
                    case ADD:
                        return valueOf(val1) + valueOf(val2);
                }
        }
        return null;
    }

    private static BigDecimal getBigDecimalFromType(Object in, int type) {
        if (in == null)
            return new BigDecimal(0);
        switch (type) {
            case DataTypes.BIG_DECIMAL:
                return (BigDecimal) in;
            case DataTypes.BIG_INTEGER:
                return new BigDecimal((BigInteger) in);
            case DataTypes.W_INTEGER:
                return new BigDecimal((Integer) in);
            case DataTypes.W_LONG:
                return new BigDecimal((Long) in);
            case DataTypes.STRING:
                return new BigDecimal((String) in);
            case DataTypes.W_FLOAT:
                return new BigDecimal((Float) in);
            case DataTypes.W_DOUBLE:
                return new BigDecimal((Double) in);
            case DataTypes.W_SHORT:
                return new BigDecimal((Short) in);
            case DataTypes.W_CHAR:
                return new BigDecimal((Character) in);
            case DataTypes.W_BOOLEAN:
                return BigDecimal.valueOf(((Boolean) in) ? 1 : 0);
            case DataTypes.UNIT:
                return new BigDecimal(((Unit) in).getValue());

        }

        throw new ConversionException("cannot convert <" + in + "> to a numeric type");
    }
}
