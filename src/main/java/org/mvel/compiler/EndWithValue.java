package org.mvel.compiler;

/**
 * @author Christopher Brock
 */
public class EndWithValue extends RuntimeException {
    private Object value;


    public EndWithValue(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }
}
