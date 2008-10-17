package org.mvel2.optimizers.dynamic;

import org.mvel2.util.MVELClassLoader;

import java.util.ConcurrentModificationException;
import java.util.LinkedList;


public class DynamicClassLoader extends ClassLoader implements MVELClassLoader {
    private int totalClasses;
    private int tenureLimit;
    private final LinkedList<DynamicAccessor> allAccessors = new LinkedList<DynamicAccessor>();

    public DynamicClassLoader(ClassLoader classLoader, int tenureLimit) {
        super(classLoader);
        this.tenureLimit = tenureLimit;
    }

    public Class defineClassX(String className, byte[] b, int start, int end) {
        totalClasses++;
        return super.defineClass(className, b, start, end);
    }

    public int getTotalClasses() {
        return totalClasses;
    }

    public DynamicAccessor registerDynamicAccessor(DynamicAccessor accessor) {
        allAccessors.add(accessor);
        assert accessor != null;
        return accessor;
    }

    public void deoptimizeAll() {
        synchronized (allAccessors) {
            try {
                for (DynamicAccessor a : allAccessors) {
                    if (a != null) a.deoptimize();
                }
            }
            catch (ConcurrentModificationException e) {
                // just back out.
            }
        }
    }

    public boolean isOverloaded() {
        return tenureLimit < totalClasses;
    }
}
