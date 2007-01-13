package org.mvel;

public class OptimizationFailure extends RuntimeException {

    public OptimizationFailure() {
        super();
    }

    public OptimizationFailure(String message) {
        super(message);
    }

    public OptimizationFailure(String message, Throwable cause) {
        super(message, cause);
    }

    public OptimizationFailure(Throwable cause) {
        super(cause);
    }
}
