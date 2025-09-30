package org.mvel3;

public interface Evaluator<C, W, O> {
    default O eval(C c) {
        throw new RuntimeException("Not Implemented");
    }

    default O eval(C c, W w) {
        throw new RuntimeException("Not Implemented");
    }

    default O evalWith(W w) {
        throw new RuntimeException("Not Implemented");

    }
}
