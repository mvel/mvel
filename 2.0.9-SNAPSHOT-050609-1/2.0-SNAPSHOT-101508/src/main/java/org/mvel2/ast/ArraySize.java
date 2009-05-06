package org.mvel2.ast;

import java.io.Serializable;

public class ArraySize implements Serializable {
    public ArraySize(char[] value) {
        this.value = value;
    }

    public char[] value;
}
