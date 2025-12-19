package org.mvel3.lambdaextractor.extended;

import java.util.List;

public final class LambdaKeyEx {

    private final String methodSignature;
    private final String normalisedBody;

    private MethodSignatureInfo methodSignatureInfo;

    private int hash; // murmur3 hash of normalisedBody

    LambdaKeyEx(String methodSignature, String normalisedBody) {
        this.methodSignature = methodSignature;
        this.normalisedBody = normalisedBody;
        this.hash = LambdaUtilsEx.calculateHash(normalisedBody);
    }

    LambdaKeyEx(String methodSignature, String normalisedBody, MethodSignatureInfo methodSignatureInfo) {
        this(methodSignature, normalisedBody);
        this.methodSignatureInfo = methodSignatureInfo;
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    public String getNormalisedBody() {
        return normalisedBody;
    }

    // maybe change to Optional
    public MethodSignatureInfo getMethodSignatureInfo() {
        return methodSignatureInfo;
    }

    @Override
    public int hashCode() {
        // TODO: revisit if normalisedSource.hashCode() is better than using the murmur3 hash (e.g. performance test)
        // note that String.hashCode() may have higher collision rate, but it's faster to compute
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof LambdaKeyEx)) {
            return false;
        }
        LambdaKeyEx other = (LambdaKeyEx) obj;
        return normalisedBody.equals(other.normalisedBody) && methodSignature.equals(other.methodSignature);
    }

    // test only
    void forceHash(int newHash) {
        this.hash = newHash;
    }

    public static class MethodSignatureInfo {

        // omit modifiers and throws clause, because they don't affect in mvel use cases

        Class<?> returnType;
        String methodName;
        List<Class<?>> parameterTypes;

        MethodSignatureInfo(Class<?> returnType, String methodName, List<Class<?>> parameterTypes) {
            this.returnType = returnType;
            this.methodName = methodName;
            this.parameterTypes = parameterTypes;
        }

        // We do not need "equals", because the comparison is done in LambdaKeyEx.equals()
        // This info is used for "subtype overload" analysis
    }
}