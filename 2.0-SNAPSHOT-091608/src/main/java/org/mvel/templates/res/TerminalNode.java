package org.mvel.templates.res;

import org.mvel.util.StringAppender;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.templates.TemplateRuntime;

public class TerminalNode extends Node {
    public TerminalNode() {
        super();    //To change body of overridden methods use File | Settings | File Templates.
    }

    public TerminalNode(int begin, int end) {
        super();    //To change body of overridden methods use File | Settings | File Templates.
        this.begin = begin;
        this.end = end;
    }

    public Object eval(TemplateRuntime runtime, StringAppender appender, Object ctx, VariableResolverFactory factory) {
        return next != null ? next.eval(runtime, appender, ctx, factory) : null;
    }

    public boolean demarcate(Node terminatingNode, char[] template) {
        return false; 
    }

}
