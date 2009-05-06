package org.mvel.util;

import java.io.Serializable;

public class StackElement implements Serializable {
    public StackElement(StackElement next, Object value) {
        this.next = next;
        this.value = value;
    }

    public StackElement next;
    public Object value;
}
