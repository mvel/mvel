package org.mvel2.templates.res;

import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.templates.TemplateRuntime;
import org.mvel2.util.StringAppender;

public class EndNode extends Node {
    public Object eval(TemplateRuntime runtie, StringAppender appender, Object ctx, VariableResolverFactory factory) {
        return appender.toString();
    }

    public String toString() {
        return "EndNode";
    }

    public boolean demarcate(Node terminatingNode, char[] template) {
        return false;
    }
}
