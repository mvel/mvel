package org.mvel2.tests.core;

import java.util.Collection;
import java.util.Iterator;

public class ProjectionsTests extends AbstractTest {
    public void testProjectionSupport() {
        assertEquals(true, test("(name in things)contains'Bob'"));
    }

    public void testProjectionSupport1() {
        assertEquals(true, test("(name in things) contains 'Bob'"));
    }

    public void testProjectionSupport2() {
        assertEquals(3, test("(name in things).size()"));
    }

    public void testProjectionSupport3() {
        assertEquals("FOO", test("(toUpperCase() in ['bar', 'foo'])[1]"));
    }

    public void testProjectionSupport4() {
        Collection col = (Collection) test("(toUpperCase() in ['zero', 'zen', 'bar', 'foo'] if ($ == 'bar'))");
        assertEquals(1, col.size());
        assertEquals("BAR", col.iterator().next());
    }

    public void testProjectionSupport5() {
        Collection col = (Collection) test("(toUpperCase() in ['zero', 'zen', 'bar', 'foo'] if ($.startsWith('z')))");
        assertEquals(2, col.size());
        Iterator iter = col.iterator();
        assertEquals("ZERO", iter.next());
        assertEquals("ZEN", iter.next());
    }

}
