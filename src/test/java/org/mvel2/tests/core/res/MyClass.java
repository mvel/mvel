package org.mvel2.tests.core.res;

import java.util.EnumSet;
import java.util.Set;

public class MyClass implements MyInterface {
  private Set<MY_ENUM> m_myEnumSet = EnumSet.noneOf(MY_ENUM.class);

  public boolean isType(MY_ENUM myenum) {
    return m_myEnumSet.contains(myenum);
  }

  public void setType(MY_ENUM myenum, boolean flag) {
    if (flag == true) {
      m_myEnumSet.add(myenum);
    }
    else {
      m_myEnumSet.remove(myenum);
    }
  }


}