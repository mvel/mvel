package org.mvel2.util;

public class JITClassLoader extends ClassLoader implements MVELClassLoader {
    public JITClassLoader(ClassLoader classLoader) {
        super(classLoader);
    }

    public Class<?> defineClassX(String className, byte[] b, int off, int len) {
        return super.defineClass(className, b, off, len);
    }
}
