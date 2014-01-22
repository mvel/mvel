package org.mule.mvel2.tests.core;

import org.mule.mvel2.MVEL;

public class EscapedPropertyAccessTests extends AbstractTest {
  public void testEscapeQuotesNoSpaces() {
    assertEquals("dog", test("foo.'bar'.name"));
  }

  public void testSpacesInProperty() {
    assertEquals("bar", test("properties.'property with spaces'"));
  }

  public void testSpacesAroundAndInProperty() {
    assertEquals("bar", test("properties . 'property with spaces' "));
  }

  public void testCommentInline() {
    assertEquals("bar", test("properties . /* foo */ 'property with spaces' "));
  }

  public void testEscapedQuote() {
    assertEquals("bar", test("properties . 'property with \\'' "));
  }

  public void testNewlines() {
    assertEquals("bar", test("properties .\n 'property with \\'' "));
  }

  public void testNullSafe() {
    MVEL.COMPILER_OPT_NULL_SAFE_DEFAULT = true;
    assertEquals(null, test("properties.'doesntexist'"));
  }

  public void testNullSafeDeep() {
    MVEL.COMPILER_OPT_NULL_SAFE_DEFAULT = true;
    assertEquals(null, test("properties.foo.bar.baz"));
  }
}
