package org.mvel2.tests.core.res;

import java.util.HashMap;
import java.util.Map;

public class TestClass {
  private Map<String, Object> extra = new HashMap<String, Object>();

  public Map<String, Object> getExtra() {
    return extra;
  }

  public void setExtra(Map<String, Object> extra) {
    this.extra = extra;
  }
}
