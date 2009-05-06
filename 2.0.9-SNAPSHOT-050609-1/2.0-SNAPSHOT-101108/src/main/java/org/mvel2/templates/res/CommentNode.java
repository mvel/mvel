package org.mvel2.templates.res;

import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.templates.TemplateRuntime;
import org.mvel2.util.StringAppender;

public class CommentNode extends Node {
    public CommentNode() {
    }

    public CommentNode(int begin, String name, char[] template, int start, int end) {
        this.name = name;
        this.end = this.cEnd = end;
    }

    public CommentNode(int begin, String name, char[] template, int start, int end, Node next) {
        this.begin = begin;
        this.end = this.cEnd = end;
        this.next = next;
    }

    public Object eval(TemplateRuntime runtime, StringAppender appender, Object ctx, VariableResolverFactory factory) {
        if (next != null)
            return next.eval(runtime, appender, ctx, factory);
        else
            return null;
    }

    public boolean demarcate(Node terminatingNode, char[] template) {
        return false;
    }
}
