package org.mvel3.compat.mvel3;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mvel3.compat.mvel2.AbstractTest;
import org.mvel3.compat.mvel2.Mvel3TestSupport;

/**
 * MVEL3-adapted version of {@link org.mvel3.compat.mvel2.CollectionElementByIndexTest}.
 *
 * <p>Accessing a {@link java.util.Set} by index is not supported in MVEL3 (Sets are unordered).
 * The expression {@code set[0]} must throw an exception — the exact type differs from MVEL2
 * but the error contract (must throw) is the same.
 */
@Tag("mvel3-adapted")
public class CollectionElementByIndexAdaptedTest extends AbstractTest {

  @Test
  public void testGetElementByIndexInSet() {
    try {
      final Map<String, Set<String>> map = Collections.singletonMap("set", Collections.singleton("test"));
      final Serializable ser = Mvel3TestSupport.compileExpression("set[0]");
      Mvel3TestSupport.executeExpression(ser, map);
    }
    catch (Exception e) {
      return;
    }
    fail("should have thrown exception, but didn't");
  }
}
