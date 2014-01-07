package org.mule.mvel2.tests.templates.tests.res;

import org.mule.mvel2.integration.VariableResolverFactory;
import org.mule.mvel2.templates.TemplateRuntime;
import org.mule.mvel2.templates.util.TemplateOutputStream;
import org.mule.mvel2.templates.res.Node;
import org.mule.mvel2.util.StringAppender;

import java.io.PrintStream;
import java.io.PrintWriter;

public class TestPluginNode extends Node {

  public Object eval(TemplateRuntime runtime, TemplateOutputStream appender, Object ctx, VariableResolverFactory factory) {
    appender.append("THIS_IS_A_TEST");
    return next != null ? next.eval(runtime, appender, ctx, factory) : null;
  }

  public boolean demarcate(Node terminatingNode, char[] template) {
    return false;
  }
}
