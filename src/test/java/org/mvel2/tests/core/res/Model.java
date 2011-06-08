package org.mvel2.tests.core.res;

public class Model {
  private Column[] columns;

  public Model(Column[] columns) {
    this.columns = columns;
  }


  public Column[] getColumns() {
    return columns;
  }

  public void setColumns(Column[] columns) {
    this.columns = columns;
  }
}