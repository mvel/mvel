package org.mvel2.templates.res;

import org.mvel2.MVEL;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.SimpleTemplateRegistry;
import org.mvel2.templates.TemplateRuntime;
import static org.mvel2.util.ParseTools.subset;
import org.mvel2.util.StringAppender;

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

        //   String name = MVEL.eval(contents, ctx, factory, String.class);

        runtime.getNamedTemplateRegistry()
                .addNamedTemplate(MVEL.eval(contents, ctx, factory, String.class),
                        new CompiledTemplate(runtime.getTemplate(), nestedNode));

        return next != null ? next.eval(runtime, appender, ctx, factory) : null;
    }

    public boolean demarcate(Node terminatingNode, char[] template) {
        Node n = nestedNode = next;

        while (n.getNext() != null) n = n.next;

        n.next = new EndNode();

        next = terminus;
        return false;
    }
}
