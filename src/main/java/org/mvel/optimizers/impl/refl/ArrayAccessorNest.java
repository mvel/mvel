package org.mvel.optimizers.impl.refl;

import org.mvel.AccessorNode;
import static org.mvel.DataConversion.convert;
import org.mvel.compiler.ExecutableStatement;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.util.ParseTools;
import static org.mvel.util.PropertyTools.getBaseComponentType;

import java.lang.reflect.Array;

public class ArrayAccessorNest implements AccessorNode {
    private AccessorNode nextNode;
    private ExecutableStatement index;

    public ArrayAccessorNest() {
    }

    public ArrayAccessorNest(String index) {
        this.index = (ExecutableStatement) ParseTools.subCompileExpression(index);
    }

    public ArrayAccessorNest(ExecutableStatement stmt) {
        this.index = stmt;
    }


    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory vars) {
        if (nextNode != null) {
            return nextNode.getValue(((Object[]) ctx)[(Integer) index.getValue(ctx, elCtx, vars)], elCtx, vars);
        }
        else {
            return ((Object[]) ctx)[(Integer) index.getValue(ctx, elCtx, vars)];
        }
    }


    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        Object o = convert(value, getBaseComponentType(ctx.getClass()));
        Array.set(ctx, (Integer) index.getValue(ctx, elCtx, variableFactory), o);
        return o;
    }

    public ExecutableStatement getIndex() {
        return index;
    }

    public void setIndex(ExecutableStatement index) {
        this.index = index;
    }

    public AccessorNode getNextNode() {
        return nextNode;
    }

    public AccessorNode setNextNode(AccessorNode nextNode) {
        return this.nextNode = nextNode;
    }


    public String toString() {
        return "Array Accessor -> [" + index + "]";
    }
}
