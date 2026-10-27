package org.mvel3.compat.mvel2.res;

public class PojoStatic {
  private String value;

  public PojoStatic(String value) {
    this.value = value;
  }

  public PojoStatic() {
  }

  public String getValue() {
    return value;
  }

  public void setValue(String string) {
    this.value = string;
  }
}
