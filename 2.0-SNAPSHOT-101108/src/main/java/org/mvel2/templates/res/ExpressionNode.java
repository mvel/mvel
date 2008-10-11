package org.mvel2.templates.res;

import org.mvel2.MVEL;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.templates.TemplateRuntime;
import static org.mvel2.util.ParseTools.subset;
import org.mvel2.util.StringAppender;

import static java.lang.String.valueOf;

public class ExpressionNode extends Node {

    public ExpressionNode() {
    }

    public ExpressionNode(int begin, String name, char[] template, int start, int end) {
        this.begin = begin;
        this.name = name;
        this.contents = subset(template, this.cStart = start, (this.end = this.cEnd = end) - start - 1);
    }

    public ExpressionNode(int begin, String name, char[] template, int start, int end, Node next) {
        this.name = name;
        this.begin = begin;
        this.contents = subset(template, this.cStart = start, (this.end = this.cEnd = end) - start - 1);
        this.next = next;
    }

    public Object eval(TemplateRuntime runtime, StringAppender appender, Object ctx, VariableResolverFactory factory) {
        appender.append(valueOf(MVEL.eval(contents, ctx, factory)));
        return next != null ? next.eval(runtime, appender, ctx, factory) : null;
    }

    public boolean demarcate(Node terminatingNode, char[] template) {
        return false;
    }

    public String toString() {
        return "ExpressionNode:" + name + "{" + (contents == null ? "" : new String(contents)) + "} (start=" + begin + ";end=" + end + ")";
    }
}
