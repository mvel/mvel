package org.mvel.templates;

public class TemplateError extends RuntimeException {
    public TemplateError() {
        super();    //To change body of overridden methods use File | Settings | File Templates.
    }

    public TemplateError(String s) {
        super(s);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public TemplateError(String s, Throwable throwable) {
        super(s, throwable);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public TemplateError(Throwable throwable) {
        super(throwable);    //To change body of overridden methods use File | Settings | File Templates.
    }
}
