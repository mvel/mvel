package org.mvel2.tests.core;

import junit.framework.TestCase;
import org.mvel2.MVEL;

/**
 * @author Mike Brock <cbrock@redhat.com>
 */
public class StacklangTests extends TestCase {

  public void testSimple1() {
    Object result = MVEL.eval("stacklang { push 'foo'; push 'bar'; push 0 }");

    assertEquals("foobar", result);
  }
}
