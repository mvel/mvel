package org.mvel.templates.res;

import org.mvel.util.StringAppender;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.templates.TemplateRuntime;

public class TextNode extends Node {
    public TextNode(int begin, int end) {
        this.begin = begin;
        this.end = end;
    }

    public TextNode(int begin, int end, ExpressionNode next) {
        this.begin = begin;
        this.end = end;
        this.next = next;
    }

    public Object eval(TemplateRuntime runtime, StringAppender appender, Object ctx, VariableResolverFactory factory) {
        int len = end - begin;
        if (len != 0) {
            appender.append(runtime.getTemplate(), begin, len);
        }
        return next != null ? next.eval(runtime, appender, ctx, factory) : null;
    }

    public String toString() {
        return "TextNode(" + begin + "," + end + ")";
    }

    public boolean demarcate(Node terminatingNode, char[] template) {
        return false;
    }

    public void calculateContents(char[] template) {
    }
}
