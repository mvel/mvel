package org.mvel3.lambdaextractor;

public final class LambdaKey {
    private final String normalisedSource;

    LambdaKey(String normalisedSource) {
        this.normalisedSource = normalisedSource;
    }

    public String getNormalisedSource() {
        return normalisedSource;
    }

    @Override
    public int hashCode() {
        return normalisedSource.hashCode();
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
        return normalisedSource.equals(other.normalisedSource);
    }
}