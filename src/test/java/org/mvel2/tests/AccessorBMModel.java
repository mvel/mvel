package org.mvel2.tests;

import java.util.List;
import java.util.Map;

import org.mvel2.DataConversion;
import org.mvel2.compiler.Accessor;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.PropertyHandler;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.GlobalListenerFactory;
import org.mvel2.tests.core.res.Foo;
import org.mvel2.util.StringAppender;


public class AccessorBMModel implements Accessor {
  private ExecutableStatement p0;
  private long foo;

  public PropertyHandler nullPropertyHandler;
  public PropertyHandler nullMethodHandler;

  public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
    return new String[][]{{"2008-04-01", "2008-05-10"}, {"2007-03-01", "2007-02-12"}};
  }

  public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
    Foo foo = (Foo) variableFactory.getVariableResolver("foo").getValue();

    if (value == null) {
      foo.charTestFld = 0;
    }
    else {
      foo.charTestFld = (Character) value;
    }

    return value;
  }

  public Class getKnownEgressType() {
    return Object.class;
  }

  public void setNullPropertyHandler(PropertyHandler handler) {
    this.nullPropertyHandler = handler;
  }

  public void setNullMethodHandler(PropertyHandler handler) {
    this.nullMethodHandler = handler;
  }

  public String toString() {
    return "FOFOSLDJALKJ";
  }
}
