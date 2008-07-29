package org.mvel.util;

public interface MVELClassLoader {
    public Class defineClassX(String className, byte[] b, int start, int end);
}
