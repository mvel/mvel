package org.mvel3.lambdaextractor;

public final class LambdaKey {
    private final String normalisedSource;

    private int hash; // murmur3 hash

    LambdaKey(String normalisedSource) {
        this.normalisedSource = normalisedSource;
        this.hash = LambdaUtils.calculateHash(normalisedSource);
    }

    public String getNormalisedSource() {
        return normalisedSource;
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
        if (!(obj instanceof LambdaKey)) {
            return false;
        }
        LambdaKey other = (LambdaKey) obj;
        return normalisedSource.equals(other.normalisedSource);
    }

    // test only
    void forceHash(int newHash) {
        this.hash = newHash;
    }
}