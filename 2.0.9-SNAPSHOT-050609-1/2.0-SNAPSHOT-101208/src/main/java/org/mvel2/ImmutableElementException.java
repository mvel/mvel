package org.mvel2;


public class ImmutableElementException extends RuntimeException {

    public ImmutableElementException() {
        super();
    }

    public ImmutableElementException(String s) {
        super(s);
    }

    public ImmutableElementException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public ImmutableElementException(Throwable throwable) {
        super(throwable);
    }
}
