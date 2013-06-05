package org.mvel2.tests.core;

import org.mvel2.MVEL;
import org.mvel2.ParserContext;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Mike Brock
 */
public class Main {
  public static class Foo {
    private String name;

    public Foo(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  public static void main(String[] args) {
    Foo foo = new Foo("Gorkum");

    Map vars = new HashMap();
    vars.put("foo", foo);

    final ParserContext ctx = ParserContext.create()
        .withInput("foo", Foo.class);

    final String expression = "foo";

    MVEL.setProperty(foo, "name", "mike");

    System.out.println(foo.getName());

  }
}
