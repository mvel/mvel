package org.mvel2.tests.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.mvel2.MVEL;
import org.mvel2.optimizers.dynamic.DynamicOptimizer;
import org.mvel2.util.MethodStub;

public class ASMConsistencyTest extends AbstractTest {
  public void testInSetRepeated() throws InterruptedException {
    String str = "[\"001\", \"002\", \"003\", \"005\", \"007\", \"008\", \"009\"].contains(digest)";
    final Serializable stmt = MVEL.compileExpression(str);
    List<Thread> threads = new ArrayList<Thread>();
    final List<RuntimeException> rex = new Vector<RuntimeException>();
    for (int i = 0; i < 1000; ++i) {
      Thread thread = new Thread() {
        @Override
        public void run() {
          for (int i = 0; i < 1000; ++i) {
            try {
              Object result = MVEL.executeExpression(stmt, Collections.singletonMap("digest", "00" + (i%10)));
              assertTrue(result instanceof Boolean);
            }
            catch (RuntimeException ex) {
              rex.add(ex);
            }
          }
        }
      };
      threads.add(thread);
    }
    for (Thread thread : threads) {
      thread.start();
    }
    for (Thread thread : threads) {
      thread.join();
    }
    if (!rex.isEmpty()) {
      throw new IllegalStateException("Exception occurred " + rex.size() + " time(s)", rex.get(0));
    }
  }
  
  /**
   * used for asm optimize test
   */
  public static class TestFunction {

    public static boolean isNull(String str) {
      return str == null;
    }

  }

  public void testNullArgConvert() {
    // change DynamicOptimizer props，make sure of using asm optimize
    int oldThreashold = DynamicOptimizer.tenuringThreshold;
    long oldTimeSpan = DynamicOptimizer.timeSpan;
    DynamicOptimizer.tenuringThreshold = 1;
    DynamicOptimizer.timeSpan = 1000 * 60 * 60L;
    
    Map<String, Object> imports = new HashMap<>(2);
    imports.put("isNull", new MethodStub(TestFunction.class, "isNull"));
    Serializable expr = MVEL.compileExpression("isNull(var1)", imports);

    Map<String, Object> inputVars = new HashMap<>(2);
    inputVars.put("var1", "someStr");
    // trigger asm optimize,tenuringThreshold is 1
    for (int i = 0;i < 3;i++) {
      MVEL.executeExpression(expr, inputVars);
    }
    // use AsmAccessor
    inputVars.put("var1", null);
    assertTrue((boolean) MVEL.executeExpression(expr, inputVars));
    
    // revert the props
    DynamicOptimizer.tenuringThreshold = oldThreashold;
    DynamicOptimizer.timeSpan = oldTimeSpan;
  }
}
