package org.mvel.templates.res;

import org.mvel.templates.TemplateRuntime;
import org.mvel.util.StringAppender;
import static org.mvel.util.ParseTools.subset;
import org.mvel.integration.VariableResolverFactory;

public class CommentNode extends Node {
    public CommentNode() {
    }

    public CommentNode(int begin, String name, char[] template, int start, int end) {
        this.begin = begin;
        this.name = name;
        this.contents = subset(template, this.cStart = start, (this.end = this.cEnd = end) - start - 1);
    }

    public CommentNode(int begin, String name, char[] template, int start, int end, Node next) {
        this.name = name;
        this.begin = begin;
        this.contents = subset(template, this.cStart = start, (this.end = this.cEnd = end) - start - 1);
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
