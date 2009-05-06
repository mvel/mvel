package org.mvel.templates.res;

import org.mvel.MVEL;
import org.mvel.templates.TemplateRuntime;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ParseTools.subset;
import org.mvel.util.StringAppender;

public class TerminalExpressionNode extends Node {
    public TerminalExpressionNode() {
    }

    public TerminalExpressionNode(Node node) {
        this.begin = node.begin;
        this.name = node.name;
        this.contents = node.contents;
    }

    public TerminalExpressionNode(int begin, String name, char[] template, int start, int end) {
        this.begin = begin;
        this.name = name;
        this.contents = subset(template, this.cStart = start, (this.end = this.cEnd = end) - start - 1);
    }

    public TerminalExpressionNode(int begin, String name, char[] template, int start, int end, Node next) {
        this.name = name;
        this.begin = begin;
        this.contents = subset(template, this.cStart = start, (this.end = this.cEnd = end) - start - 1);
        this.next = next;
    }

    public Object eval(TemplateRuntime runtime, StringAppender appender, Object ctx, VariableResolverFactory factory) {
        return MVEL.eval(contents, ctx, factory);
    }

    public boolean demarcate(Node terminatingNode, char[] template) {
        return false;
    }
}
