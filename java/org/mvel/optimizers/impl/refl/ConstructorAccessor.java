package org.mvel.optimizers.impl.refl;

import static org.mvel.DataConversion.convert;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.ExecutableStatement;

import java.lang.reflect.Constructor;

public class ConstructorAccessor extends BaseAccessor {
    private Constructor constructor;
    private Class[] parmTypes;
    private ExecutableStatement[] parms;
    private int length;
    private boolean coercionNeeded = false;

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) throws Exception {
        if (!coercionNeeded) {
            try {
                if (nextNode != null) {
                    return nextNode.getValue(constructor.newInstance(executeAll(elCtx, variableFactory)), elCtx, variableFactory);
                }
                else {
                    return constructor.newInstance(executeAll(elCtx, variableFactory));
                }
            }
            catch (IllegalArgumentException e) {
                coercionNeeded = true;
                return getValue(ctx, elCtx, variableFactory);
            }

        }
        else {
            if (nextNode != null) {
                return nextNode.getValue(constructor.newInstance(executeAndCoerce(parmTypes, elCtx, variableFactory)),
                        elCtx, variableFactory);
            }
            else {
                return constructor.newInstance(executeAndCoerce(parmTypes, elCtx, variableFactory));
            }
        }
    }


    private Object[] executeAll(Object ctx, VariableResolverFactory vars) {
        if (length == 0) return GetterAccessor.EMPTY;

        Object[] vals = new Object[length];
        for (int i = 0; i < length; i++) {
            vals[i] = parms[i].getValue(ctx, vars);
        }
        return vals;
    }

    private Object[] executeAndCoerce(Class[] target, Object elCtx, VariableResolverFactory vars) {
        Object[] values = new Object[length];
        for (int i = 0; i < length; i++) {
            //noinspection unchecked
            values[i] = convert(parms[i].getValue(elCtx, vars), target[i]);
        }
        return values;
    }

    public ConstructorAccessor(Constructor constructor, ExecutableStatement[] parms) {
        this.constructor = constructor;
        this.length = (this.parmTypes = constructor.getParameterTypes()).length;
        this.parms = parms;
    }

}
