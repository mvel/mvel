package org.mvel.templates.res;

import org.mvel.integration.VariableResolverFactory;
import org.mvel.templates.TemplateRuntime;
import org.mvel.templates.SimpleTemplateRegistry;
import org.mvel.templates.CompiledTemplate;
import static org.mvel.util.ParseTools.subset;
import org.mvel.util.StringAppender;
import org.mvel.MVEL;

public class DeclareNode extends Node {
    private Node nestedNode;

    public DeclareNode(int begin, String name, char[] template, int start, int end) {
        this.begin = begin;
        this.name = name;
        this.contents = subset(template, this.cStart = start, (this.end = this.cEnd = end) - start - 1);
    }

    public Object eval(TemplateRuntime runtime, StringAppender appender, Object ctx, VariableResolverFactory factory) {
        if (runtime.getNamedTemplateRegistry() == null) {
            runtime.setNamedTemplateRegistry(new SimpleTemplateRegistry());
        }

        String name = MVEL.eval(contents, ctx, factory, String.class);

        runtime.getNamedTemplateRegistry().addNamedTemplate(name, new CompiledTemplate(runtime.getTemplate(), nestedNode));

        return next != null ? next.eval(runtime, appender, ctx, factory) : null;
    }

    public boolean demarcate(Node terminatingNode, char[] template) {
        Node n = nestedNode = next;

        while (n.getNext() != null) n = n.getNext();
        n.setNext(new EndNode());

        next = terminus;
        return false;
    }
}
