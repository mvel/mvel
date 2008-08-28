package org.mvel.templates.res;

import org.mvel.util.StringAppender;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.templates.TemplateRuntime;

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
