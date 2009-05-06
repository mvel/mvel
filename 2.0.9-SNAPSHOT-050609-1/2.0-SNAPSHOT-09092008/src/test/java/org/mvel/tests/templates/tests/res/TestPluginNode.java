package org.mvel.tests.templates.tests.res;

import org.mvel.templates.res.Node;
import org.mvel.templates.TemplateRuntime;
import org.mvel.util.StringAppender;
import org.mvel.integration.VariableResolverFactory;

public class TestPluginNode extends Node {

    public Object eval(TemplateRuntime runtime, StringAppender appender, Object ctx, VariableResolverFactory factory) {
        appender.append("THIS_IS_A_TEST");
        return next != null ? next.eval(runtime, appender, ctx, factory) : null;
    }

    public boolean demarcate(Node terminatingNode, char[] template) {
        return false;
    }
}
