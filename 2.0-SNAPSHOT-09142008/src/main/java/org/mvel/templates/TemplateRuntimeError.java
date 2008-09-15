package org.mvel.templates;

public class TemplateRuntimeError extends RuntimeException {
    public TemplateRuntimeError() {
        super();    //To change body of overridden methods use File | Settings | File Templates.
    }

    public TemplateRuntimeError(String s) {
        super(s);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public TemplateRuntimeError(String s, Throwable throwable) {
        super(s, throwable);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public TemplateRuntimeError(Throwable throwable) {
        super(throwable);    //To change body of overridden methods use File | Settings | File Templates.
    }
}
