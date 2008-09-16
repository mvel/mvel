package org.mvel.templates.res;

import org.mvel.MVEL;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.templates.TemplateRuntime;
import org.mvel.templates.util.TemplateTools;
import static org.mvel.templates.util.TemplateTools.captureToEOS;
import static org.mvel.util.ParseTools.subset;
import org.mvel.util.StringAppender;

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
            MVEL.eval(preExpression, ctx,  factory);
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
