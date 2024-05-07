package org.mvel2.tests.core.res;

import org.mvel2.MVEL;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;

import java.util.Map;

public class SharedFuncLib {

  final static VariableResolverFactory functionFactory = new MapVariableResolverFactory();

  static {
    MVEL.eval(
    "def round(numValue, decPlaces) { return numValue.setScale(decPlaces,java.math.RoundingMode.HALF_UP) };"
            + "\n"
            + "def sum(lst, startValue, accFunc){ s = startValue; foreach (i : lst){ s = s + accFunc(i); } return s;  };",
    functionFactory);
  }

  public <T> T eval(String formula, Map<String, Object> context, Class<T> toType) {
    VariableResolverFactory myVariableResolverFactory = new MapVariableResolverFactory();
    myVariableResolverFactory.setNextFactory(functionFactory);
    return MVEL.eval(formula, context, myVariableResolverFactory, toType);
  }
}
