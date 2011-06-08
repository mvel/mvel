package org.mvel2.tests.core.res;

/**
 * Created by IntelliJ IDEA.
 * User: christopherbrock
 * Date: 19-Feb-2009
 * Time: 12:32:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class Column {
  private String name;
  private int length;

  public Column(String name, int length) {
    this.name = name;
    this.length = length;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getLength() {
    return length;
  }

  public void setLength(int length) {
    this.length = length;
  }
}
