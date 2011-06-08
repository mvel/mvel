package org.mvel2.tests.core;

import junit.framework.TestCase;
import org.mvel2.PropertyAccessor;
import org.mvel2.tests.core.res.Base;
import org.mvel2.tests.core.res.Foo;

public class PropertyAccessUnitTest extends TestCase {
  Base base = new Base();

  public void testPropertyRead() {
    assertEquals("cat", PropertyAccessor.get("data", base));
  }

  public void testDeepPropertyRead() {
    assertEquals("dog", PropertyAccessor.get("foo.bar.name", base));
  }

  public void testWrite() {
    PropertyAccessor.set(base, "foo.bar.name", "cat");
    assertEquals("cat", PropertyAccessor.get("foo.bar.name", base));
    PropertyAccessor.set(base, "foo.bar.name", "dog");
  }

  public void testCollectionsAccess() {
    PropertyAccessor.set(base, "funMap['foo'].bar.name", "cat");
    assertEquals("cat", PropertyAccessor.get("funMap['foo'].bar.name", base));
  }

  public void testMethodInvoke() {
    assertEquals("WOOF", PropertyAccessor.get("foo.toUC('woof')", base));
  }

  public void testMethodInvoke2() {
    assertEquals("happyBar", PropertyAccessor.get("foo.happy()", base));
  }

  public void testMapDirect() {
    assertTrue(PropertyAccessor.get("funMap['foo']", base) instanceof Foo);
  }

  public void testArrayAccess() {
    String[] a = new String[]{"foo", "bar",};
    assertEquals("foo", PropertyAccessor.get("[0]", a));
  }

  public void testArrayAccess2() {
    assertEquals("poo", PropertyAccessor.get("[0][0]", new Object[]{new String[]{"poo"}}));
  }

  public void testStaticMethodAccess() {
    assertEquals(String.class, PropertyAccessor.get("forName('java.lang.String')", Class.class));
  }
}
