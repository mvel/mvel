package org.mvel.optimizers;

public class OptimizationNotSupported extends RuntimeException {

    public OptimizationNotSupported() {
        super();
    }

    public OptimizationNotSupported(String message) {
        super(message);
    }

    public OptimizationNotSupported(String message, Throwable cause) {
        super(message, cause);
    }

    public OptimizationNotSupported(Throwable cause) {
        super(cause);
    }
}
