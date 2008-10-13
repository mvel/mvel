package org.mvel2.templates.res;

import org.mvel2.MVEL;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.templates.TemplateRuntime;
import org.mvel2.util.StringAppender;

public class IfNode extends Node {
    private Node trueNode;
    private Node elseNode;

    public IfNode(int begin, String name, char[] template, int start, int end) {
        super(begin, name, template, start, end);
    }

    public Node getTrueNode() {
        return trueNode;
    }

    public void setTrueNode(ExpressionNode trueNode) {
        this.trueNode = trueNode;
    }

    public Node getElseNode() {
        return elseNode;
    }

    public void setElseNode(ExpressionNode elseNode) {
        this.elseNode = elseNode;
    }

    public boolean demarcate(Node terminatingNode, char[] template) {
        trueNode = next;
        next = terminus;
        return true;
    }

    public Object eval(TemplateRuntime runtime, StringAppender appender, Object ctx, VariableResolverFactory factory) {
        if (contents.length == 0 || MVEL.eval(contents, ctx, factory, Boolean.class)) {
            return trueNode.eval(runtime, appender, ctx, factory);
        }
        return next != null ? next.eval(runtime, appender, ctx, factory) : null;
    }
}
