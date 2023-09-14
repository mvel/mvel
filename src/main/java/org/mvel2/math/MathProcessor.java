/**
 * MVEL 2.0
 * Copyright (C) 2007 The Codehaus
 * Mike Brock, Dhanji Prasanna, John Graham, Mark Proctor
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
 */
package org.mvel2.math;

import org.mvel2.DataTypes;
import org.mvel2.Unit;
import org.mvel2.compiler.BlankLiteral;
import org.mvel2.debug.DebugTools;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.lang.String.valueOf;
import static org.mvel2.DataConversion.convert;
import static org.mvel2.DataTypes.BIG_DECIMAL;
import static org.mvel2.DataTypes.EMPTY;
import static org.mvel2.Operator.*;
import static org.mvel2.util.ParseTools.*;
import static org.mvel2.util.Soundex.soundex;

/**
 * @author Christopher Brock
 */
public strictfp class MathProcessor {
  private static final MathContext MATH_CONTEXT = MathContext.DECIMAL128;

  public static Object doOperations(Object val1, int operation, Object val2) {
    return doOperations(val1 == null ? DataTypes.OBJECT : __resolveType(val1.getClass()),
        val1, operation,
        val2 == null ? DataTypes.NULL : __resolveType(val2.getClass()), val2);
  }

  public static Object doOperations(Object val1, int operation, int type2, Object val2) {
    return doOperations(val1 == null ? DataTypes.OBJECT : __resolveType(val1.getClass()), val1, operation, type2, val2);
  }

  public static Object doOperations(int type1, Object val1, int operation, int type2, Object val2) {
    if (type1 < 1)
      type1 = val1 == null ? DataTypes.OBJECT : __resolveType(val1.getClass());

    if (type2 < 1)
      type2 = val2 == null ? DataTypes.OBJECT : __resolveType(val2.getClass());

    if (type1 == BIG_DECIMAL) {
      if (type2 == BIG_DECIMAL) {
        return doBigDecimalArithmetic((BigDecimal) val1, operation, (BigDecimal) val2, false, -1);
      }
      if (type2 > 99) {
        return doBigDecimalArithmetic((BigDecimal) val1, operation, asBigDecimal(val2), false, -1);
      } else {
        return _doOperations(type1, val1, operation, type2, val2);
      }
    }
    return _doOperations(type1, val1, operation, type2, val2);
  }

  private static Object doPrimWrapperArithmetic(final Number val1, final int operation, final Number val2, int returnTarget) {
    switch (operation) {
      case ADD:
        return toType(val1.doubleValue() + val2.doubleValue(), returnTarget);
      case DIV:
        return toType(val1.doubleValue() / val2.doubleValue(), returnTarget);
      case SUB:
        return toType(val1.doubleValue() - val2.doubleValue(), returnTarget);
      case MULT:
        return toType(val1.doubleValue() * val2.doubleValue(), returnTarget);
      case POWER:
        return toType(Math.pow(val1.doubleValue(), val2.doubleValue()), returnTarget);
      case MOD:
        return toType(val1.doubleValue() % val2.doubleValue(), returnTarget);
      case GTHAN:
        return val1.doubleValue() > val2.doubleValue();
      case GETHAN:
        return val1.doubleValue() >= val2.doubleValue();
      case LTHAN:
        return val1.doubleValue() < val2.doubleValue();
      case LETHAN:
        return val1.doubleValue() <= val2.doubleValue();
      case EQUAL:
        return val1.doubleValue() == val2.doubleValue();
      case NEQUAL:
        return val1.doubleValue() != val2.doubleValue();
    }
    return null;

  }

  private static Object toType(Number val, int returnType) {
    switch (returnType) {
      case DataTypes.W_DOUBLE:
      case DataTypes.DOUBLE:
        return val.doubleValue();
      case DataTypes.W_FLOAT:
      case DataTypes.FLOAT:
        return val.floatValue();
      case DataTypes.INTEGER:
      case DataTypes.W_INTEGER:
        return val.intValue();
      case DataTypes.W_LONG:
      case DataTypes.LONG:
        return val.longValue();
      case DataTypes.W_SHORT:
      case DataTypes.SHORT:
        return val.shortValue();
      case DataTypes.BIG_DECIMAL:
        return new BigDecimal(val.doubleValue());
      case DataTypes.BIG_INTEGER:
        return BigInteger.valueOf(val.longValue());

      case DataTypes.STRING:
        return val.doubleValue();
    }
    throw new RuntimeException("internal error: " + returnType);
  }

  private static Object doBigDecimalArithmetic(final BigDecimal val1, final int operation, final BigDecimal val2, boolean iNumber, int returnTarget) {
    switch (operation) {
      case ADD:
        if (iNumber) {
          return narrowType(val1.add(val2, MATH_CONTEXT), returnTarget);
        }
        else {
          return val1.add(val2, MATH_CONTEXT);
        }
      case DIV:
        if (iNumber) {
          return narrowType(val1.divide(val2, MATH_CONTEXT), returnTarget);
        }
        else {
          return val1.divide(val2, MATH_CONTEXT);
        }

      case SUB:
        if (iNumber) {
          return narrowType(val1.subtract(val2, MATH_CONTEXT), returnTarget);
        }
        else {
          return val1.subtract(val2, MATH_CONTEXT);
        }
      case MULT:
        if (iNumber) {
          return narrowType(val1.multiply(val2, MATH_CONTEXT), returnTarget);
        }
        else {
          return val1.multiply(val2, MATH_CONTEXT);
        }

      case POWER:
        if (iNumber) {
          return narrowType(val1.pow(val2.intValue(), MATH_CONTEXT), returnTarget);
        }
        else {
          return val1.pow(val2.intValue(), MATH_CONTEXT);
        }

      case MOD:
        if (iNumber) {
          return narrowType(val1.remainder(val2), returnTarget);
        }
        else {
          return val1.remainder(val2);
        }

      case GTHAN:
        return val1 != null && val2 != null && val1.compareTo(val2) > 0;
      case GETHAN:
        return val1 != null && val2 != null && val1.compareTo(val2) >= 0;
      case LTHAN:
        return val1 != null && val2 != null && val1.compareTo(val2) < 0;
      case LETHAN:
        return val1 != null && val2 != null && val1.compareTo(val2) <= 0;
      case EQUAL:
        return val1 == null ? val2 == null : val2 != null && val1.compareTo(val2) == 0;
      case NEQUAL:
        return val1 == null ? val2 != null : val2 == null || val1.compareTo(val2) != 0;
    }
    return null;
  }

  private static Object _doOperations(int type1, Object val1, int operation, int type2, Object val2) {
    if (operation < 20) {
      if (((type1 > 49 || operation == EQUAL || operation == NEQUAL) && type1 == type2) ||
              (isIntegerType(type1) && isIntegerType(type2) && operation >= BW_AND && operation <= BW_NOT)) {
        return doOperationsSameType(type1, val1, operation, val2);
      }
      else if (val2 != null && isNumericOperation(type1, val1, operation, type2, val2)) {
        return doPrimWrapperArithmetic(getNumber(val1, type1), operation,
            getNumber(val2, type2), Math.max(box(type2), box(type1)));
      }
      else if (operation != ADD &&
          (type1 == DataTypes.W_BOOLEAN || type2 == DataTypes.W_BOOLEAN) &&
          type1 != type2 && type1 != EMPTY && type2 != EMPTY) {

        return doOperationNonNumeric(type1, convert(val1, Boolean.class), operation, convert(val2, Boolean.class));
      }
      // Fix for: MVEL-56
      else if ((type1 == 1 || type2 == 1) && (type1 == 8 || type1 == 112 || type2 == 8 || type2 == 112)) {
        if (type1 == 1) {
          return doOperationNonNumeric(type1, val1, operation, valueOf(val2));
        }
        else {
          return doOperationNonNumeric(type1, valueOf(val1), operation, val2);
        }
      }
    }
    return doOperationNonNumeric(type1, val1, operation, val2);
  }

  private static boolean isNumericOperation(int type1, Object val1, int operation, int type2, Object val2) {
    return (type1 > 99 && type2 > 99)
        || (operation != ADD && (type1 > 99 || type2 > 99 || operation < LTHAN || operation > GETHAN) && isNumber(val1) && isNumber(val2));
  }

  private static boolean isIntegerType(int type) {
    return type == DataTypes.INTEGER || type == DataTypes.W_INTEGER || type == DataTypes.LONG || type == DataTypes.W_LONG;
  }

  private static Object doOperationNonNumeric(int type1, final Object val1, final int operation, final Object val2) {
    switch (operation) {
      case ADD:
        if (type1 == DataTypes.COLLECTION) {
          List list = new ArrayList((Collection) val1);
          list.add(val2);
          return list;
        }
        else {
          return valueOf(val1) + valueOf(val2);
        }

      case EQUAL:
        return safeEquals(val2, val1);

      case NEQUAL:
        return safeNotEquals(val2, val1);

      case SUB:
      case DIV:
      case MULT:
      case MOD:
        return null;
      case GTHAN:
        if (val1 instanceof Comparable) {
          try {
            return val2 != null && (((Comparable) val1).compareTo(val2) >= 1);
          }
          catch (ClassCastException e) {
            throw new RuntimeException("uncomparable values <<" + val1 + ">> and <<" + val2 + ">>", e);
          }
        }
        else {
          return Boolean.FALSE;
        }
        //     break;

      case GETHAN:
        if (val1 instanceof Comparable) {
          try {
            return val2 != null && ((Comparable) val1).compareTo(val2) >= 0;
          }
          catch (ClassCastException e) {
            throw new RuntimeException("uncomparable values <<" + val1 + ">> and <<" + val2 + ">>", e);
          }

        }
        else {
          return Boolean.FALSE;
        }


      case LTHAN:
        if (val1 instanceof Comparable) {
          //noinspection unchecked
          try {
            return val2 != null && ((Comparable) val1).compareTo(val2) <= -1;
          }
          catch (ClassCastException e) {
            throw new RuntimeException("uncomparable values <<" + val1 + ">> and <<" + val2 + ">>", e);
          }

        }
        else {
          return Boolean.FALSE;
        }


      case LETHAN:
        if (val1 instanceof Comparable) {
          try {
            return val2 != null && ((Comparable) val1).compareTo(val2) <= 0;
          }
          catch (ClassCastException e) {
            throw new RuntimeException("uncomparable values <<" + val1 + ">> and <<" + val2 + ">>", e);
          }

        }
        else {
          return Boolean.FALSE;
        }


      case SOUNDEX:
        return soundex(String.valueOf(val1)).equals(soundex(String.valueOf(val2)));

      case STR_APPEND:
        return valueOf(val1) + valueOf(val2);
    }

    throw new RuntimeException("could not perform numeric operation on non-numeric types: left-type="
        + (val1 != null ? val1.getClass().getName() : "null") + "; right-type="
        + (val2 != null ? val2.getClass().getName() : "null")
        + " [vals (" + valueOf(val1) + ", " + valueOf(val2) + ") operation=" + DebugTools.getOperatorName(operation) + " (opcode:" + operation + ") ]");
  }

  private static Boolean safeEquals(final Object val1, final Object val2) {
    if (val1 != null) {
      return val1.equals(val2);
    }
    else return val2 == null;
  }

  private static Boolean safeNotEquals(final Object val1, final Object val2) {
    if (val1 != null) {
      return !val1.equals(val2);
    }
    else return val2 != null;
  }

  private static Object doOperationsSameType(int type1, Object val1, int operation, Object val2) {
    switch (type1) {
      case DataTypes.COLLECTION:
        switch (operation) {
          case ADD:
            List list = new ArrayList((Collection) val1);
            list.addAll((Collection) val2);
            return list;

          case EQUAL:
            return val1.equals(val2);

          case NEQUAL:
            return !val1.equals(val2);

          default:
            throw new UnsupportedOperationException("illegal operation on Collection type");
        }

      case DataTypes.INTEGER:
      case DataTypes.W_INTEGER:
        switch (operation) {
          case ADD:
            return toInteger(val1) + toInteger(val2);
          case SUB:
            return toInteger(val1) - toInteger(val2);
          case DIV:
            return toDouble(val1) / toInteger(val2);
          case MULT:
            return toInteger(val1) * toInteger(val2);
          case POWER:
            double d = Math.pow(toInteger(val1), toInteger(val2));
            if (d > Integer.MAX_VALUE) return d;
            else return (int) d;
          case MOD:
            return toInteger(val1) % toInteger(val2);
          case GTHAN:
            return val1 != null && val2 != null && toInteger(val1) > toInteger(val2);
          case GETHAN:
            return val1 != null && val2 != null && toInteger(val1) >= toInteger(val2);
          case LTHAN:
            return val1 != null && val2 != null && toInteger(val1) < toInteger(val2);
          case LETHAN:
            return val1 != null && val2 != null && toInteger(val1) <= toInteger(val2);
          case EQUAL:
            return val1 != null && val2 != null ? toInteger(val1) == toInteger(val2) : val1 == val2;
          case NEQUAL:
            return val1 != null && val2 != null ? toInteger(val1) != toInteger(val2) : val1 != val2;
          case BW_AND:
            if (val2 instanceof Long) return (Integer) val1 & (Long) val2;
            return (Integer) val1 & (Integer) val2;
          case BW_OR:
            if (val2 instanceof Long) return (Integer) val1 | (Long) val2;
            return (Integer) val1 | (Integer) val2;
          case BW_SHIFT_LEFT:
            if (val2 instanceof Long) return (Integer) val1 << (Long) val2;
            return (Integer) val1 << (Integer) val2;
          case BW_SHIFT_RIGHT:
            if (val2 instanceof Long) return (Integer) val1 >> (Long) val2;
            return (Integer) val1 >> (Integer) val2;
          case BW_USHIFT_RIGHT:
            if (val2 instanceof Long) return (Integer) val1 >>> (Long) val2;
            return (Integer) val1 >>> (Integer) val2;
          case BW_XOR:
            if (val2 instanceof Long) return (Integer) val1 ^ (Long) val2;
            return (Integer) val1 ^ (Integer) val2;
        }

      case DataTypes.SHORT:
      case DataTypes.W_SHORT:
        switch (operation) {
          case ADD:
            return toShort(val1) + toShort(val2);
          case SUB:
            return toShort(val1) - toShort(val2);
          case DIV:
            return toDouble(val1) / toDouble(val2);
          case MULT:
            return toShort(val1) * toShort(val2);
          case POWER:
            double d = Math.pow(toShort(val1), toShort(val2));
            if (d > Short.MAX_VALUE) return d;
            else return (short) d;
          case MOD:
            return toShort(val1) % toShort(val2);
          case GTHAN:
            return val1 != null && val2 != null && toShort(val1) > toShort(val2);
          case GETHAN:
            return val1 != null && val2 != null && toShort(val1) >= toShort(val2);
          case LTHAN:
            return val1 != null && val2 != null && toShort(val1) < toShort(val2);
          case LETHAN:
            return val1 != null && val2 != null && toShort(val1) <= toShort(val2);
          case EQUAL:
            return val1 != null && val2 != null ? toShort(val1) == toShort(val2) : val1 == val2;
          case NEQUAL:
            return val1 != null && val2 != null ? toShort(val1) != toShort(val2) : val1 != val2;
          case BW_AND:
            return (Short) val1 & (Short) val2;
          case BW_OR:
            return (Short) val1 | (Short) val2;
          case BW_SHIFT_LEFT:
            return (Short) val1 << (Short) val2;
          case BW_SHIFT_RIGHT:
            return (Short) val1 >> (Short) val2;
          case BW_USHIFT_RIGHT:
            return (Short) val1 >>> (Short) val2;
          case BW_XOR:
            return (Short) val1 ^ (Short) val2;
        }

      case DataTypes.LONG:
      case DataTypes.W_LONG:
        switch (operation) {
          case ADD:
            return toLong(val1) + toLong(val2);
          case SUB:
            return toLong(val1) - toLong(val2);
          case DIV:
            return toDouble(val1) / toDouble(val2);
          case MULT:
            return toLong(val1) * toLong(val2);
          case POWER:
            double d = Math.pow(toLong(val1), toLong(val2));
            if (d > Long.MAX_VALUE) return d;
            else return (long) d;
          case MOD:
            return toLong(val1) % toLong(val2);
          case GTHAN:
            return val1 != null && val2 != null && toLong(val1) > toLong(val2);
          case GETHAN:
            return val1 != null && val2 != null && toLong(val1) >= toLong(val2);
          case LTHAN:
            return val1 != null && val2 != null && toLong(val1) < toLong(val2);
          case LETHAN:
            return val1 != null && val2 != null && toLong(val1) <= toLong(val2);
          case EQUAL:
            return val1 != null && val2 != null ? toLong(val1) == toLong(val2) : val1 == val2;
          case NEQUAL:
            return val1 != null && val2 != null ? toLong(val1) != toLong(val2) : val1 != val2;
          case BW_AND:
            if (val2 instanceof Integer) return (Long) val1 & (Integer) val2;
            return (Long) val1 & (Long) val2;
          case BW_OR:
            if (val2 instanceof Integer) return (Long) val1 | (Integer) val2;
            return (Long) val1 | (Long) val2;
          case BW_SHIFT_LEFT:
            if (val2 instanceof Integer) return (Long) val1 << (Integer) val2;
            return (Long) val1 << (Long) val2;
          case BW_USHIFT_LEFT:
            throw new UnsupportedOperationException("unsigned left-shift not supported");
          case BW_SHIFT_RIGHT:
            if (val2 instanceof Integer) return (Long) val1 >> (Integer) val2;
            return (Long) val1 >> (Long) val2;
          case BW_USHIFT_RIGHT:
            if (val2 instanceof Integer) return (Long) val1 >>> (Integer) val2;
            return (Long) val1 >>> (Long) val2;
          case BW_XOR:
            if (val2 instanceof Integer) return (Long) val1 ^ (Integer) val2;
            return (Long) val1 ^ (Long) val2;
        }

      case DataTypes.UNIT:
        val2 = ((Unit) val1).convertFrom(val2);
        val1 = ((Unit) val1).getValue();

      case DataTypes.DOUBLE:
      case DataTypes.W_DOUBLE:
        switch (operation) {
          case ADD:
            return toDouble(val1) + toDouble(val2);
          case SUB:
            return toDouble(val1) - toDouble(val2);
          case DIV:
            return toDouble(val1) / toDouble(val2);
          case MULT:
            return toDouble(val1) * toDouble(val2);
          case POWER:
            return Math.pow(toDouble(val1), toDouble(val2));
          case MOD:
            return toDouble(val1) % toDouble(val2);
          case GTHAN:
            return val1 != null && val2 != null && toDouble(val1) > toDouble(val2);
          case GETHAN:
            return val1 != null && val2 != null && toDouble(val1) >= toDouble(val2);
          case LTHAN:
            return val1 != null && val2 != null && toDouble(val1) < toDouble(val2);
          case LETHAN:
            return val1 != null && val2 != null && toDouble(val1) <= toDouble(val2);
          case EQUAL:
            return val1 != null && val2 != null ? toDouble(val1) == toDouble(val2) : val1 == val2;
          case NEQUAL:
            return val1 != null && val2 != null ? toDouble(val1) != toDouble(val2) : val1 != val2;
          case BW_AND:
          case BW_OR:
          case BW_SHIFT_LEFT:
          case BW_SHIFT_RIGHT:
          case BW_USHIFT_RIGHT:
          case BW_XOR:
            throw new RuntimeException("bitwise operation on a non-fixed-point number.");
        }

      case DataTypes.FLOAT:
      case DataTypes.W_FLOAT:
        switch (operation) {
          case ADD:
            return toFloat(val1) + toFloat(val2);
          case SUB:
            return toFloat(val1) - toFloat(val2);
          case DIV:
            return toDouble(val1) / toDouble(val2);
          case MULT:
            return toFloat(val1) * toFloat(val2);
          case POWER:
            return narrowType(asBigDecimal(val1).pow(((Number) val2).intValue(), MATH_CONTEXT), -1);
          case MOD:
            return toFloat(val1) % toFloat(val2);
          case GTHAN:
            return val1 != null && val2 != null && toFloat(val1) > toFloat(val2);
          case GETHAN:
            return val1 != null && val2 != null && toFloat(val1) >= toFloat(val2);
          case LTHAN:
            return val1 != null && val2 != null && toFloat(val1) < toFloat(val2);
          case LETHAN:
            return val1 != null && val2 != null && toFloat(val1) <= toFloat(val2);
          case EQUAL:
            return val1 != null && val2 != null ? toFloat(val1) == toFloat(val2) : val1 == val2;
          case NEQUAL:
            return val1 != null && val2 != null ? toFloat(val1) != toFloat(val2) : val1 != val2;
          case BW_AND:
          case BW_OR:
          case BW_SHIFT_LEFT:
          case BW_SHIFT_RIGHT:
          case BW_USHIFT_RIGHT:
          case BW_XOR:
            throw new RuntimeException("bitwise operation on a non-fixed-point number.");
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
            return ((BigInteger) val1).compareTo(((BigInteger) val2)) > 0;
          case GETHAN:
            return ((BigInteger) val1).compareTo(((BigInteger) val2)) >= 0;
          case LTHAN:
            return ((BigInteger) val1).compareTo(((BigInteger) val2)) < 0;
          case LETHAN:
            return ((BigInteger) val1).compareTo(((BigInteger) val2)) <= 0;
          case EQUAL:
            return val1 != null && val2 != null ? ((BigInteger) val1).compareTo(((BigInteger) val2)) == 0 : val1 == val2;
          case NEQUAL:
            return val1 != null && val2 != null ? ((BigInteger) val1).compareTo(((BigInteger) val2)) != 0 : val1 != val2;
          case BW_AND:
          case BW_OR:
          case BW_SHIFT_LEFT:
          case BW_SHIFT_RIGHT:
          case BW_USHIFT_RIGHT:
          case BW_XOR:
            throw new RuntimeException("bitwise operation on a number greater than 32-bits not possible");
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

  private static short toShort(Object val) {
    return val instanceof Short? (Short) val : ((Number) val).shortValue();
  }

  private static int toInteger(Object val) {
    return val instanceof Integer ? (Integer) val : ((Number) val).intValue();
  }

  private static long toLong(Object val) {
    return val instanceof Long ? (Long) val : ((Number) val).longValue();
  }

  private static double toDouble(Object val) {
    return val instanceof Double ? (Double) val : ((Number) val).doubleValue();
  }

  private static float toFloat(Object val) {
    return val instanceof Float ? (Float) val : ((Number) val).floatValue();
  }

  private static int box(int type) {
    switch (type) {
      case DataTypes.INTEGER:
        return DataTypes.W_INTEGER;
      case DataTypes.DOUBLE:
        return DataTypes.W_DOUBLE;
      case DataTypes.LONG:
        return DataTypes.W_LONG;
      case DataTypes.SHORT:
        return DataTypes.W_SHORT;
      case DataTypes.BYTE:
        return DataTypes.W_BYTE;
      case DataTypes.FLOAT:
        return DataTypes.W_FLOAT;
      case DataTypes.CHAR:
        return DataTypes.W_CHAR;
      case DataTypes.BOOLEAN:
        return DataTypes.W_BOOLEAN;
    }
    return type;
  }

  private static Double getNumber(Object in, int type) {
    if (in == null || in == BlankLiteral.INSTANCE)
      return 0d;
    switch (type) {
      case DataTypes.BIG_DECIMAL:
      case DataTypes.BIG_INTEGER:
      case DataTypes.INTEGER:
      case DataTypes.W_INTEGER:
      case DataTypes.LONG:
      case DataTypes.W_LONG:
      case DataTypes.FLOAT:
      case DataTypes.W_FLOAT:
      case DataTypes.SHORT:
      case DataTypes.W_SHORT:
      case DataTypes.DOUBLE:
      case DataTypes.W_DOUBLE:
        return ((Number) in).doubleValue();
      case DataTypes.CHAR:
      case DataTypes.W_CHAR:
        return Double.parseDouble(String.valueOf(in));
      case DataTypes.BOOLEAN:
      case DataTypes.W_BOOLEAN:
        return ((Boolean) in) ? 1d : 0d;
      case DataTypes.W_BYTE:
      case DataTypes.BYTE:
        return ((Byte) in).doubleValue();
      case DataTypes.STRING:
        return Double.parseDouble((String) in);
      case DataTypes.OBJECT:
        return in instanceof Number ? ((Number) in).doubleValue() : Double.parseDouble((String) in);
    }

    throw new RuntimeException("cannot convert <" + in + "> to a numeric type: " + in.getClass() + " [" + type + "]");


  }


  private static BigDecimal asBigDecimal(Object in) {
    if (in == null || in == BlankLiteral.INSTANCE) {
      return null;
    }
    if (in instanceof BigDecimal) {
      return (BigDecimal) in;
    }
    if (in instanceof String) {
      return new BigDecimal((String) in);
    }
    if (in instanceof Number) {
      return BigDecimal.valueOf(((Number) in).doubleValue());
    }

    throw new RuntimeException("cannot convert <" + in + "> to a numeric type: " + in.getClass());
  }
}
