package org.mvel2.sh;

public enum BuiltinFunction {
  eval("def eval(str) { org.mvel2.MVEL.eval(str, __SHELL_VARIABLE_CONTEXT); }"),
  print("def print(str) { System.out.println(str); }");

  public final String functionDeclaration;

  BuiltinFunction(String functionDeclaration) {
    this.functionDeclaration = functionDeclaration;
  }
}

