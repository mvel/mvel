package org.mvel2.tests.core.res;


public interface MyInterface {
  public enum MY_ENUM {
    ONE, TWO, THREE, FOUR
  }

  ;

  public boolean isType(MY_ENUM myenum);

  public void setType(MY_ENUM myenum, boolean flag);

  public static enum STATIC_ENUM {
    FOO, BAR;
  }

  public static interface MyInnerInterface {
    public static enum INNER_STATIC_ENUM {
      FOO, BAR;
    }
  }
}

