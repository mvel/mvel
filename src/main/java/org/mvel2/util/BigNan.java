package org.mvel2.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

public class BigNan extends InternalNumber {
	public static final BigNan NaN = new BigNan();

	private BigNan() {
		super(BigInteger.valueOf(Long.MIN_VALUE), -Integer.MAX_VALUE);
	}

	@Override
	public BigDecimal add(BigDecimal augend) {
		return this;
	}

	@Override
	public BigDecimal add(BigDecimal augend, MathContext mc) {
		return this;
	}

	@Override
	public BigDecimal subtract(BigDecimal subtrahend) {
		return this;
	}

	@Override
	public BigDecimal subtract(BigDecimal subtrahend, MathContext mc) {
		return this;
	}

	@Override
	public BigDecimal multiply(BigDecimal multiplicand) {
		return this;
	}

	@Override
	public BigDecimal multiply(BigDecimal multiplicand, MathContext mc) {
		return this;
	}

	@Override
	public BigDecimal divide(BigDecimal divisor, int scale, int roundingMode) {
		return this;
	}

	@Override
	public BigDecimal divide(BigDecimal divisor, int scale, RoundingMode roundingMode) {
		return this;
	}

	@Override
	public BigDecimal divide(BigDecimal divisor, int roundingMode) {
		return this;
	}

	@Override
	public BigDecimal divide(BigDecimal divisor, RoundingMode roundingMode) {
		return this;
	}

	@Override
	public BigDecimal divide(BigDecimal divisor) {
		return this;
	}

	@Override
	public BigDecimal divide(BigDecimal divisor, MathContext mc) {
		return this;
	}

	@Override
	public BigDecimal divideToIntegralValue(BigDecimal divisor) {
		return this;
	}

	@Override
	public BigDecimal divideToIntegralValue(BigDecimal divisor, MathContext mc) {
		return this;
	}

	@Override
	public BigDecimal remainder(BigDecimal divisor) {
		return this;
	}

	@Override
	public BigDecimal remainder(BigDecimal divisor, MathContext mc) {
		return this;
	}

	@Override
	public BigDecimal[] divideAndRemainder(BigDecimal divisor) {
		return new BigDecimal[]{this, this};
	}

	@Override
	public BigDecimal[] divideAndRemainder(BigDecimal divisor, MathContext mc) {
		return new BigDecimal[]{this, this};
	}

	@Override
	public BigDecimal pow(int n) {
		return this;
	}

	@Override
	public BigDecimal pow(int n, MathContext mc) {
		return this;
	}

	@Override
	public BigDecimal abs() {
		return this;
	}

	@Override
	public BigDecimal abs(MathContext mc) {
		return this;
	}

	@Override
	public BigDecimal negate() {
		return this;
	}

	@Override
	public BigDecimal negate(MathContext mc) {
		return this;
	}

	@Override
	public BigDecimal plus() {
		return this;
	}

	@Override
	public BigDecimal plus(MathContext mc) {
		return this;
	}

	@Override
	public int signum() {
		return 0;
	}

	@Override
	public int scale() {
		return 0;
	}

	@Override
	public int precision() {
		return 0;
	}

	@Override
	public BigInteger unscaledValue() {
		throw new UnsupportedOperationException("unscaledValue for NaN not supported");
	}

	@Override
	public BigDecimal round(MathContext mc) {
		return this;
	}

	@Override
	public BigDecimal setScale(int newScale, RoundingMode roundingMode) {
		return this;
	}

	@Override
	public BigDecimal setScale(int newScale, int roundingMode) {
		return this;
	}

	@Override
	public BigDecimal setScale(int newScale) {
		return this;
	}

	@Override
	public BigDecimal movePointLeft(int n) {
		return this;
	}

	@Override
	public BigDecimal movePointRight(int n) {
		return this;
	}

	@Override
	public BigDecimal scaleByPowerOfTen(int n) {
		return this;
	}

	@Override
	public BigDecimal stripTrailingZeros() {
		return this;
	}

	@Override
	public int compareTo(BigDecimal val) {
		return this == val ? 0 : 1; // NaN is a singleton
	}

	@Override
	public boolean equals(Object x) {
		return x == this;
	}

	@Override
	public BigDecimal min(BigDecimal val) {
		return val;
	}

	@Override
	public BigDecimal max(BigDecimal val) {
		return this;
	}

	@Override
	public String toString() {
		return "NaN";
	}

	@Override
	public String toEngineeringString() {
		return toString();
	}

	@Override
	public String toPlainString() {
		return toString();
	}

	@Override
	public BigInteger toBigInteger() {
		throw new UnsupportedOperationException("toBigInteger not supported for NaN");
	}

	@Override
	public BigInteger toBigIntegerExact() {
		throw new UnsupportedOperationException("toBigIntegerExact not supported for NaN");
	}

	@Override
	public long longValue() {
		return (long)doubleValue();
	}

	@Override
	public long longValueExact() {
		throw new UnsupportedOperationException("longValueExact not supported for NaN");
	}

	@Override
	public int intValue() {
		return (int)doubleValue();
	}

	@Override
	public int intValueExact() {
		throw new UnsupportedOperationException("intValueExact not supported for NaN");
	}

	@Override
	public short shortValueExact() {
		throw new UnsupportedOperationException("shortValueExact not supported for NaN");
	}

	@Override
	public byte byteValueExact() {
		throw new UnsupportedOperationException("byteValueExact not supported for NaN");
	}

	@Override
	public float floatValue() {
		return (float)doubleValue();
	}

	@Override
	public double doubleValue() {
		return Double.NaN;
	}

	@Override
	public BigDecimal ulp() {
		throw new UnsupportedOperationException("ulp not supported for NaN");
	}
}
