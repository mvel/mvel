package org.mvel2.templates.res;

import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.templates.TemplateRuntime;
import org.mvel2.util.StringAppender;

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
