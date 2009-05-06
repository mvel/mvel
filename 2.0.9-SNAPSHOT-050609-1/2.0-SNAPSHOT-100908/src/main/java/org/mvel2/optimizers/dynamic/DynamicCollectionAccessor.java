package org.mvel2.optimizers.dynamic;

import org.mvel2.compiler.Accessor;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.optimizers.OptimizerFactory;

import static java.lang.System.currentTimeMillis;

//todo: de-optimize in this class.
public class DynamicCollectionAccessor implements DynamicAccessor {
    private Object rootObject;
    private Class colType;
    private char[] property;
    private long stamp;
    private int type;

    private int runcount;

    private boolean opt = false;

    private Accessor _safeAccessor;
    private Accessor _accessor;

    public DynamicCollectionAccessor(Object rootObject, Class colType, char[] property, int type, Accessor _accessor) {
        this.rootObject = rootObject;
        this.colType = colType;
        this._safeAccessor = this._accessor = _accessor;
        this.type = type;
        this.property = property;
        stamp = currentTimeMillis();
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        if (!opt) {
            if (++runcount > DynamicOptimizer.tenuringThreshold) {
                if ((currentTimeMillis() - stamp) < DynamicOptimizer.timeSpan) {
                    opt = true;
                    return optimize(ctx, elCtx, variableFactory);
                }
                else {
                    runcount = 0;
                    stamp = currentTimeMillis();
                }
            }
        }

        return _accessor.getValue(ctx, elCtx, variableFactory);
    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        runcount++;
        return _accessor.setValue(ctx, elCtx, variableFactory, value);
    }

    private Object optimize(Object ctx, Object elCtx, VariableResolverFactory variableResolverFactory) {

        if (DynamicOptimizer.classLoader.isOverloaded()) {
            DynamicOptimizer.enforceTenureLimit();
        }

        _accessor = OptimizerFactory.getAccessorCompiler("ASM").optimizeCollection(rootObject, colType, property, ctx, elCtx, variableResolverFactory);
        return _accessor.getValue(ctx, elCtx, variableResolverFactory);

    }


    public void deoptimize() {
        this._accessor = this._safeAccessor;
        opt = false;
        runcount = 0;
        stamp = currentTimeMillis();
    }

    public long getStamp() {
        return stamp;
    }

    public int getRuncount() {
        return runcount;
    }
}