package org.mvel.templates;

import org.mvel.integration.VariableResolverFactory;
import org.mvel.integration.impl.MapVariableResolverFactory;
import static org.mvel.templates.TemplateCompiler.compileTemplate;
import org.mvel.templates.res.Node;
import org.mvel.util.StringAppender;

import java.util.Map;

public class TemplateRuntime {
    private char[] template;
    private TemplateRegistry namedTemplateRegistry;
    private Node rootNode;

    public TemplateRuntime(char[] template, TemplateRegistry namedTemplateRegistry, Node rootNode) {
        this.template = template;
        this.namedTemplateRegistry = namedTemplateRegistry;
        this.rootNode = rootNode;
    }

    public static Object eval(String template, Map vars) {
        return execute(compileTemplate(template), null, new MapVariableResolverFactory(vars));
    }

    public static Object eval(String template, Object ctx) {
        return execute(compileTemplate(template), ctx, null);
    }

    public static Object eval(String template, Object ctx, Map vars) {
        return execute(compileTemplate(template), ctx, new MapVariableResolverFactory(vars));
    }

    public static Object eval(String template, Object ctx, VariableResolverFactory vars) {
        return execute(compileTemplate(template), ctx, vars);
    }

    public static Object eval(String template, Map vars, TemplateRegistry registry) {
        return execute(compileTemplate(template), null, new MapVariableResolverFactory(vars), registry);
    }

    public static Object eval(String template, Object ctx, Map vars, TemplateRegistry registry) {
        return execute(compileTemplate(template), ctx, new MapVariableResolverFactory(vars), registry);
    }

    public static Object eval(String template, Object ctx, VariableResolverFactory vars, TemplateRegistry registry) {
        return execute(compileTemplate(template), ctx, vars, registry);
    }


    public static Object execute(CompiledTemplate compiled) {
        return execute(compiled.getRoot(), compiled.getTemplate(), new StringAppender(), null, null, null);
    }


    public static Object execute(CompiledTemplate compiled, Object context, VariableResolverFactory factory) {
        return execute(compiled.getRoot(), compiled.getTemplate(), new StringAppender(), context, factory, null);
    }

    public static Object execute(CompiledTemplate compiled, Object context, VariableResolverFactory factory, TemplateRegistry registry) {
         return execute(compiled.getRoot(), compiled.getTemplate(), new StringAppender(), context, factory, registry);
     }


    public static Object execute(Node root, char[] template,
                                 StringAppender appender, Object context,
                                 VariableResolverFactory factory, TemplateRegistry registry) {
        return new TemplateRuntime(template, registry, root).execute(appender, context, factory);
    }

    public Object execute(StringAppender appender, Object context, VariableResolverFactory factory) {
       return rootNode.eval(this, appender, context, factory);
    }

    public Node getRootNode() {
        return rootNode;
    }

    public void setRootNode(Node rootNode) {
        this.rootNode = rootNode;
    }

    public char[] getTemplate() {
        return template;
    }

    public void setTemplate(char[] template) {
        this.template = template;
    }

    public TemplateRegistry getNamedTemplateRegistry() {
        return namedTemplateRegistry;
    }

    public void setNamedTemplateRegistry(TemplateRegistry namedTemplateRegistry) {
        this.namedTemplateRegistry = namedTemplateRegistry;
    }
}
