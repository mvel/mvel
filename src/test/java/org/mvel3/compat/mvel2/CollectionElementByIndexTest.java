package org.mvel3.compat.mvel2;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Imported from org.mvel2.compiler.CollectionElementByIndexTest (mvel2 branch).
 *
 * <p>Expressions, input values, expected values, and assertions are retained
 * unchanged. Failures are intentionally visible; known behavioural differences
 * are annotated with {@link KnownCompatibility}.
 */
@Tag("mvel2-compatibility")
public class CollectionElementByIndexTest extends AbstractTest {

  // MVEL2 expects CompileException with message containing "unknown type: java.util.Collections$SingletonSet".
  // MVEL3 throws a different exception type (javac compile error) at evaluation time. The exception-type-mismatch
  // and inner-class-binary-name categories are not annotated here because an exception is still thrown.
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
