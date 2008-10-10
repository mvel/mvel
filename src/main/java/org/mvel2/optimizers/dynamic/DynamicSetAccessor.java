package org.mvel2.optimizers.dynamic;

import org.mvel2.compiler.Accessor;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.optimizers.AccessorOptimizer;
import org.mvel2.optimizers.OptimizerFactory;

import static java.lang.System.currentTimeMillis;

public class DynamicSetAccessor implements DynamicAccessor {
    private char[] property;
    private boolean opt = false;
    private int runcount = 0;
    private long stamp;

    private final Accessor _safeAccessor;
    private Accessor _accessor;

    private String description;

    public DynamicSetAccessor(char[] property, Accessor _accessor) {
        assert _accessor != null;
        this._safeAccessor = this._accessor = _accessor;
        this.property = property;
        this.stamp = System.currentTimeMillis();
    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        if (!opt) {
            if (++runcount > DynamicOptimizer.tenuringThreshold) {
                if ((currentTimeMillis() - stamp) < DynamicOptimizer.timeSpan) {
                    opt = true;
                    return optimize(ctx, elCtx, variableFactory, value);
                }
                else {
                    runcount = 0;
                    stamp = currentTimeMillis();
                }
            }
        }

        _accessor.setValue(ctx, elCtx, variableFactory, value);
        return value;
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        throw new RuntimeException("value cannot be read with this accessor");
    }

    private Object optimize(Object ctx, Object elCtx, VariableResolverFactory variableResolverFactory, Object value) {
        if (DynamicOptimizer.classLoader.isOverloaded()) {
            DynamicOptimizer.enforceTenureLimit();
        }

        AccessorOptimizer ao = OptimizerFactory.getAccessorCompiler("ASM");
        _accessor = ao.optimizeSetAccessor(property, ctx, elCtx, variableResolverFactory, false, value);
        assert _accessor != null;

        return value;
    }

    public void deoptimize() {
        this._accessor = this._safeAccessor;
        opt = false;
        runcount = 0;
        stamp = currentTimeMillis();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Class getKnownEgressType() {
        return _safeAccessor.getKnownEgressType();
    }
}