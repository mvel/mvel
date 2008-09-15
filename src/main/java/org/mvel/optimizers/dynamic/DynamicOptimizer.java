package org.mvel.optimizers.dynamic;

import org.mvel.compiler.Accessor;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.AbstractOptimizer;
import org.mvel.optimizers.AccessorOptimizer;
import static org.mvel.optimizers.OptimizerFactory.SAFE_REFLECTIVE;
import static org.mvel.optimizers.OptimizerFactory.getAccessorCompiler;
import static org.mvel.optimizers.impl.asm.ASMAccessorOptimizer.setMVELClassLoader;

import static java.lang.Thread.currentThread;

public class DynamicOptimizer extends AbstractOptimizer implements AccessorOptimizer {
    private AccessorOptimizer firstStage = getAccessorCompiler(SAFE_REFLECTIVE);

    public static DynamicClassLoader classLoader;
    public static int tenuringThreshold = 10;
    public static long timeSpan = 100; 
    public static int maximumTenure = 1500;
    public static int totalRecycled = 0;

    public void init() {
        _init();
    }

    private static void _init() {
        setMVELClassLoader(classLoader = new DynamicClassLoader(currentThread().getContextClassLoader(), maximumTenure));
    }

    public static void enforceTenureLimit() {
        if (classLoader.isOverloaded()) {
            classLoader.deoptimizeAll();
            totalRecycled =+ classLoader.getTotalClasses();
            _init();
        }
    }

    public static final int REGULAR_ACCESSOR = 0;
    
    public Accessor optimizeAccessor(char[] property, Object ctx, Object thisRef, VariableResolverFactory factory, boolean rootThisRef) {
        return classLoader.registerDynamicAccessor(new DynamicGetAccessor(property, 0, firstStage.optimizeAccessor(property, ctx, thisRef, factory, rootThisRef)));
    }

    public static final int SET_ACCESSOR = 1;

    public Accessor optimizeSetAccessor(char[] property, Object ctx, Object thisRef, VariableResolverFactory factory, boolean rootThisRef, Object value) {
        return classLoader.registerDynamicAccessor(new DynamicSetAccessor(property, firstStage.optimizeSetAccessor(property, ctx, thisRef, factory, rootThisRef, value)));
    }

    public static final int COLLECTION = 2;

    public Accessor optimizeCollection(Object rootObject, char[] property, Object ctx, Object thisRef, VariableResolverFactory factory) {
       return classLoader.registerDynamicAccessor(new DynamicCollectionAccessor(rootObject, property, 2, firstStage.optimizeCollection(rootObject, property, ctx, thisRef, factory)));
    }

    public static final int OBJ_CREATION = 3;

    public Accessor optimizeObjectCreation(char[] property, Object ctx, Object thisRef, VariableResolverFactory factory) {
        return classLoader.registerDynamicAccessor(new DynamicGetAccessor(property, 3, firstStage.optimizeObjectCreation(property, ctx, thisRef, factory)));
    }

    public static final int FOLD = 4;

    public Accessor optimizeFold(char[] property, Object ctx, Object thisRef, VariableResolverFactory factory) {
        enforceTenureLimit();
        return classLoader.registerDynamicAccessor(new DynamicGetAccessor(property, 4, firstStage.optimizeFold(property, ctx, thisRef, factory)));
    }

    public Object getResultOptPass() {
        return firstStage.getResultOptPass();
    }

    public Class getEgressType() {
        return firstStage.getEgressType();
    }

    public boolean isLiteralOnly() {
        return firstStage.isLiteralOnly();
    }
}
