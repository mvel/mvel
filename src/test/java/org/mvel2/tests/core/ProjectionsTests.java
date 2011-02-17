package org.mvel2.tests.core;

import org.mvel2.MVEL;
import org.mvel2.tests.core.res.Base;
import org.mvel2.util.Make;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class ProjectionsTests extends AbstractTest {
    public void testProjectionSupport() {
        assertEquals(true, test("(name in things)contains'Bob'"));
    }

    public void testProjectionSupport1() {
        assertEquals(true, test("(name in things) contains 'Bob'"));
    }

    public void testProjectionSupport2() {
        String ex = "(name in things).size()";
        Map vars = createTestMap();

        assertEquals(3, MVEL.eval(ex, new Base(), vars));

        assertEquals(3, test("(name in things).size()"));
    }

    public void testProjectionSupport3() {
        String ex = "(toUpperCase() in ['bar', 'foo'])[1]";
        Map vars = createTestMap();

        assertEquals("FOO", MVEL.eval(ex, new Base(), vars));

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
