package org.mvel.optimizers.impl.refl;

import org.mvel.AccessorNode;
import org.mvel.integration.VariableResolverFactory;

import java.util.List;

public class ListAccessor implements AccessorNode {
    private AccessorNode nextNode;
    private int index;


    public ListAccessor() {
    }

    public ListAccessor(int index) {
        this.index = index;
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory vars) {
        if (nextNode != null) {
            return nextNode.getValue(((List) ctx).get(index), elCtx, vars);
        }
        else {
            return ((List) ctx).get(index);
        }
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public AccessorNode getNextNode() {
        return nextNode;
    }

    public AccessorNode setNextNode(AccessorNode nextNode) {
        return this.nextNode = nextNode;
    }


    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        //noinspection unchecked
        ((List) ctx).set(index, value);
        return value;
    }

    public String toString() {
        return "Array Accessor -> [" + index + "]";
    }
}
