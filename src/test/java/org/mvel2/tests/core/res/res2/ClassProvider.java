package org.mvel2.tests.core.res.res2;

public class ClassProvider {
    public PublicClass get() {
        //return new PublicClass();
        return new PrivateClass();
    }
}
