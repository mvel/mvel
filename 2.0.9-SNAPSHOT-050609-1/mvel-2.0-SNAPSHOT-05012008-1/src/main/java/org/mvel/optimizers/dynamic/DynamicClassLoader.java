package org.mvel.optimizers.dynamic;

import org.mvel.util.MVELClassLoader;

public class DynamicClassLoader extends ClassLoader implements MVELClassLoader {
    private int totalClasses;

    public DynamicClassLoader(ClassLoader classLoader) {
        super(classLoader);
    }

    public Class defineClassX(String className, byte[] b, int start, int end) {
        totalClasses++;
        return super.defineClass(className, b, start, end);
    }

    public int getTotalClasses() {
        return totalClasses;
    }
}
