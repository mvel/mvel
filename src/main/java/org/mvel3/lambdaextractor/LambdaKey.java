package org.mvel3.lambdaextractor;

final class LambdaKey {
    final String normalisedBody;
    final String signature;

    LambdaKey(String normalisedBody, String signature) {
        this.normalisedBody = normalisedBody;
        this.signature = signature;
    }

    @Override
    public int hashCode() {
        return normalisedBody.hashCode() + signature.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof LambdaKey)) {
            return false;
        }
        LambdaKey other = (LambdaKey) obj;
        return normalisedBody.equals(other.normalisedBody) && signature.equals(other.signature);
    }
}