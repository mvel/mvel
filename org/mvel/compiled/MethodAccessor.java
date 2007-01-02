package org.mvel.compiled;

import org.mvel.AccessorNode;
import org.mvel.ExpressionParser;

import java.lang.reflect.Method;
import java.io.Serializable;
import java.util.Map;

public class MethodAccessor implements AccessorNode {
    private AccessorNode nextNode;

    private Method method;
    private Serializable[] compiledParameters;

    public Object getValue(Object ctx, Object elCtx, Map vars) throws Exception {
        if (nextNode != null) {
            return nextNode.getValue(
                    method.invoke(ctx, ExpressionParser.executeAllExpression(compiledParameters, elCtx, vars))
                    , elCtx, vars);
        }
        else {
            return method.invoke(ctx, ExpressionParser.executeAllExpression(compiledParameters, elCtx, vars));
        }
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }


    public Serializable[] getCompiledParameters() {
        return compiledParameters;
    }

    public void setCompiledParameters(Serializable[] compiledParameters) {
        this.compiledParameters = compiledParameters;
    }

    public MethodAccessor() {
    }

    public AccessorNode getNextNode() {
        return nextNode;
    }

    public AccessorNode setNextNode(AccessorNode nextNode) {
        return this.nextNode = nextNode;
    }
}


