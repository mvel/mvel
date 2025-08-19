package org.mvel3.test;

import org.mvel3.ClassManager;

import java.lang.invoke.MethodHandles;

// This class exists to test using a different ClassManager in a different package. The key part is the Supplier, which retuns the lookup's package.
public class TestClassManager extends ClassManager {
    public TestClassManager() {
        super(() -> MethodHandles.lookup());
    }
}
