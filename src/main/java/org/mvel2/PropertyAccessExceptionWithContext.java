package org.mvel2;

public class PropertyAccessExceptionWithContext extends PropertyAccessException {
    private final Class ctx;

    public PropertyAccessExceptionWithContext(String message, char[] expr, int cursor, ParserContext pCtx, Class ctx) {
        super(message, expr, cursor, pCtx);
        this.ctx = ctx;
    }

    public Class getCtx() {
        return ctx;
    }
}
