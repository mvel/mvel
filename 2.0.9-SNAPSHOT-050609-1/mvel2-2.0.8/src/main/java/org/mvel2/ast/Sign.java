package org.mvel2.ast;

import org.mvel2.CompileException;
import org.mvel2.integration.VariableResolverFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.io.Serializable;

public class Sign extends ASTNode {
    private Signer signer;

    public Sign(char[] expr, int start, int end, int fields) {
        super(expr, start + 1, end, fields);
        if ((fields & COMPILE_IMMEDIATE) != 0) {
            if (egressType != null && egressType != Object.class) {
                initSigner(egressType);
            }
        }
    }


    @Override
    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return sign(super.getReducedValueAccelerated(ctx, thisValue, factory));
    }

    @Override
    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return sign(super.getReducedValue(ctx, thisValue, factory));
    }

    private Object sign(Object o) {
        if (o == null) return null;
        if (signer == null) {
            if (egressType == null || egressType == Object.class) egressType = o.getClass();
            initSigner(egressType);
        }
        return signer.sign(o);
    }

    private void initSigner(Class type) {
        if (Integer.class.isAssignableFrom(type)) signer = new IntegerSigner();
        else if (Double.class.isAssignableFrom(type)) signer = new DoubleSigner();
        else if (Long.class.isAssignableFrom(type)) signer = new LongSigner();
        else if (Float.class.isAssignableFrom(type)) signer = new FloatSigner();
        else if (Short.class.isAssignableFrom(type)) signer = new ShortSigner();
        else if (BigInteger.class.isAssignableFrom(type)) signer = new BigIntSigner();
        else if (BigDecimal.class.isAssignableFrom(type)) signer = new BigDecSigner();
        else {
            throw new CompileException("illegal use of '-': cannot be applied to: " + type.getName());
        }

    }


    private interface Signer extends Serializable {
        public Object sign(Object o);
    }

    private class IntegerSigner implements Signer {
        public Object sign(Object o) {
            return -((Integer) o);
        }
    }

    private class ShortSigner implements Signer {
        public Object sign(Object o) {
            return -((Short) o);
        }
    }

    private class LongSigner implements Signer {
        public Object sign(Object o) {
            return -((Long) o);
        }
    }

    private class DoubleSigner implements Signer {
        public Object sign(Object o) {
            return -((Double) o);
        }
    }

    private class FloatSigner implements Signer {
        public Object sign(Object o) {
            return -((Float) o);
        }
    }

    private class BigIntSigner implements Signer {
        public Object sign(Object o) {
            return new BigInteger(String.valueOf(-(((BigInteger) o).longValue())));
        }
    }

    private class BigDecSigner implements Signer {
        public Object sign(Object o) {
            return new BigDecimal(-((BigInteger) o).longValue());
        }
    }


}



