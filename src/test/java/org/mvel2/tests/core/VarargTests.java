package org.mvel2.tests.core;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.mvel2.MVEL;
import org.mvel2.optimizers.OptimizerFactory;

public class VarargTests extends AbstractTest {
  public static class MyVarargTestee {
    private StringBuilder sb = new StringBuilder();
    public MyVarargTestee() {
    }
    public MyVarargTestee(String arg) {
      sb.append("singleArgCtor" + arg);
    }
    public MyVarargTestee(String ... arg) {
      sb.append("multipleArgsCtor" + Arrays.asList(arg));
    }
    public void add(String arg) {
      sb.append("singleArgMethod" + arg);
    }
    public void add(String ... arg) {
      sb.append("multipleArgsMethod" + Arrays.asList(arg));
    }
    public String toString() {
      return sb.toString();
    }
  }

  public void testSingleArgMethodPreference() {
    Serializable s = MVEL.compileExpression("testee.add('a')");
    MyVarargTestee testee = new MyVarargTestee();
    MVEL.executeExpression(s, Collections.singletonMap("testee", testee));
    Assert.assertEquals("singleArgMethoda", testee.toString());
  }

  public void testVarargMethod() {
      Serializable s = MVEL.compileExpression("testee.add('a', 'b')");
      MyVarargTestee testee = new MyVarargTestee();
      MVEL.executeExpression(s, Collections.singletonMap("testee", testee));
      Assert.assertEquals("multipleArgsMethod[a, b]", testee.toString());
  }

  public void testNoArgCtorPreference() {
      Serializable s = MVEL.compileExpression("new " + MyVarargTestee.class.getName() + "()");
      MyVarargTestee result = (MyVarargTestee) MVEL.executeExpression(s);
      Assert.assertEquals("", result.toString());
  }

  public void testSingleArgCtorPreference() {
      Serializable s = MVEL.compileExpression("new " + MyVarargTestee.class.getName() + "('a')");
      MyVarargTestee result = (MyVarargTestee) MVEL.executeExpression(s);
      Assert.assertEquals("singleArgCtora", result.toString());
  }

  public void testVarargCtor() {
      Serializable s = MVEL.compileExpression("new " + MyVarargTestee.class.getName() + "('a', 'b')");
      MyVarargTestee result = (MyVarargTestee) MVEL.executeExpression(s);
      Assert.assertEquals("multipleArgsCtor[a, b]", result.toString());
  }
}
