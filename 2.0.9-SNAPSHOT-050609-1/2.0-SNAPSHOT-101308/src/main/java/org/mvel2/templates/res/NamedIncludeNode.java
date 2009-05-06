package org.mvel2.templates.res;

import org.mvel2.MVEL;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.templates.TemplateRuntime;
import static org.mvel2.templates.util.TemplateTools.captureToEOS;
import static org.mvel2.util.ParseTools.subset;
import org.mvel2.util.StringAppender;

public class NamedIncludeNode extends Node {
    private char[] includeExpression;
    private char[] preExpression;

    public NamedIncludeNode(int begin, String name, char[] template, int start, int end) {
        this.begin = begin;
        this.name = name;
        this.contents = subset(template, this.cStart = start, (this.end = this.cEnd = end) - start - 1);

        int mark;
        this.includeExpression = subset(contents, 0, mark = captureToEOS(contents, 0));
        if (mark != contents.length) this.preExpression = subset(contents, ++mark, contents.length - mark);
    }

    public Object eval(TemplateRuntime runtime, StringAppender appender, Object ctx, VariableResolverFactory factory) {
        if (preExpression != null) {
            MVEL.eval(preExpression, ctx, factory);
        }

        if (next != null) {
            return next.eval(runtime, appender.append(TemplateRuntime.execute(runtime.getNamedTemplateRegistry().getNamedTemplate(MVEL.eval(includeExpression, ctx, factory, String.class)), ctx, factory)), ctx, factory);
        }
        else {
            return appender.append(TemplateRuntime.execute(runtime.getNamedTemplateRegistry().getNamedTemplate(MVEL.eval(includeExpression, ctx, factory, String.class)), ctx, factory));
        }
    }

    public boolean demarcate(Node terminatingNode, char[] template) {
        return false;
    }
}
