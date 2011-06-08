package org.mvel2.tests.core.res.res2;

public class Outer {

  public Inner getInner() {
    return new Inner();
  }

  public class Inner extends Object {

    public int getValue() {
      return 2;
    }
  }
}
