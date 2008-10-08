package org.mvel.templates.res;

import org.mvel.util.StringAppender;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.templates.TemplateRuntime;

public class TerminalNode extends Node {
    public TerminalNode() {
    }

    public TerminalNode(int begin, int end) {
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
