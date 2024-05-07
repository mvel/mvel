package org.mvel2.util;

/**
 * @author Mike Brock .
 */
public abstract class VariableSpaceModel {
  protected final String[] allVars;

  protected VariableSpaceModel(String[] allVars) {
    this.allVars = allVars;
  }
}
