package org.mvel2.templates;

public class TemplateSyntaxError extends RuntimeException {
    public TemplateSyntaxError() {
        super();    //To change body of overridden methods use File | Settings | File Templates.
    }

    public TemplateSyntaxError(String s) {
        super(s);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public TemplateSyntaxError(String s, Throwable throwable) {
        super(s, throwable);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public TemplateSyntaxError(Throwable throwable) {
        super(throwable);    //To change body of overridden methods use File | Settings | File Templates.
    }
}
