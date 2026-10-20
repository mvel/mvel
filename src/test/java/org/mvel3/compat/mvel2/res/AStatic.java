package org.mvel3.compat.mvel2.res;

public class AStatic {
  public static String Process(String arg) {
    return arg;
  }

  public static String process(String... args){
      StringBuilder result = new StringBuilder();
      if(args == null){
          return "null";
      }
      for (String arg : args)
      {
          result.append(arg);
          result.append(",");
      }
      return result.toString();
  }

    public static void main(String[] args)
    {
        System.out.println("exec(null) = " + process(null));
        System.out.println("exec(null) = " + process());
        System.out.println("exec(null) = " + process("hello", "world"));
    }
}
