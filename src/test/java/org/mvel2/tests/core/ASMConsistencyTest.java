package org.mvel2.tests.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.mvel2.MVEL;

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

}
