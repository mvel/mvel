package org.mvel2.optimizers.impl.refl.nodes;

import org.mvel2.CompileException;
import static org.mvel2.util.ParseTools.getBestCandidate;
import static org.mvel2.DataConversion.convert;
import org.mvel2.compiler.AccessorNode;
import org.mvel2.integration.VariableResolverFactory;

import java.lang.reflect.Method;

public class SetterAccessor implements AccessorNode {
    private AccessorNode nextNode;
    private final Method method;
    private Class<?> targetType;

    private boolean coercionRequired = false;

    public static final Object[] EMPTY = new Object[0];

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        try {
            if (coercionRequired) {
                return method.invoke(ctx, convert(value, targetType));
            }
            else {
                return method.invoke(ctx, value);
            }
        }
        catch (IllegalArgumentException e) {
            if (ctx != null && method.getDeclaringClass() != ctx.getClass()) {
                Method o = getBestCandidate(EMPTY, method.getName(), ctx.getClass(), ctx.getClass().getMethods(), true);
                if (o != null) {
                    return executeOverrideTarget(o, ctx, value);
                }
            }

            if (!coercionRequired) {
                coercionRequired = true;
                return setValue(ctx, elCtx, variableFactory, value);
            }
            throw new CompileException("unable to bind property", e);
        }
        catch (Exception e) {
            System.out.println("ctx=" + ctx);
            throw new CompileException("error calling method: " + method.getDeclaringClass().getName() + "." + method.getName(), e);
        }
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory vars) {
        return null;
    }

    public SetterAccessor(Method method) {
        this.method = method;
        assert method != null;
        this.targetType = method.getParameterTypes()[0];
    }

    public Method getMethod() {
        return method;
    }

    public AccessorNode setNextNode(AccessorNode nextNode) {
        return this.nextNode = nextNode;
    }

    public AccessorNode getNextNode() {
        return nextNode;
    }

    public String toString() {
        return method.getDeclaringClass().getName() + "." + method.getName();
    }

    public Class getKnownEgressType() {
        return method.getReturnType();
    }

    private Object executeOverrideTarget(Method o, Object ctx, Object value) {
        try {
            return o.invoke(ctx, convert(value, targetType));
        }
        catch (Exception e2) {
            throw new CompileException("unable to invoke method", e2);
        }
    }
}
