package org.mvel.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

public class InternalNumber extends BigDecimal {
    public InternalNumber(char[] chars, int i, int i1) {
        super(chars, i, i1);
    }

    public InternalNumber(char[] chars, int i, int i1, MathContext mathContext) {
        super(chars, i, i1, mathContext);
    }

    public InternalNumber(char[] chars) {
        super(chars);
    }

    public InternalNumber(char[] chars, MathContext mathContext) {
        super(chars, mathContext);
    }

    public InternalNumber(String s) {
        super(s);
    }

    public InternalNumber(String s, MathContext mathContext) {
        super(s, mathContext);
    }

    public InternalNumber(double v) {
        super(v);
    }

    public InternalNumber(double v, MathContext mathContext) {
        super(v, mathContext);
    }

    public InternalNumber(BigInteger bigInteger) {
        super(bigInteger);
    }

    public InternalNumber(BigInteger bigInteger, MathContext mathContext) {
        super(bigInteger, mathContext);
    }

    public InternalNumber(BigInteger bigInteger, int i) {
        super(bigInteger, i);
    }

    public InternalNumber(BigInteger bigInteger, int i, MathContext mathContext) {
        super(bigInteger, i, mathContext);
    }

    public InternalNumber(int i) {
        super(i);
    }

    public InternalNumber(int i, MathContext mathContext) {
        super(i, mathContext);
    }

    public InternalNumber(long l) {
        super(l);
    }

    public InternalNumber(long l, MathContext mathContext) {
        super(l, mathContext);
    }
}
