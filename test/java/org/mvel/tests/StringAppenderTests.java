package org.mvel.tests;

import junit.framework.TestCase;
import org.mvel.util.StringAppender;

public class StringAppenderTests extends TestCase {

    public void testMain() {
        assertEquals("foobarfoobar", new StringAppender().append("foo").append('b').append('a').append('r').append("foobar").toString());
    }

    public void testStringBuilder() {
        assertEquals("foobarfoobar", new StringBuilder().append("foo").append('b').append('a').append('r').append("foobar").toString());
    }

    public void testMainPerf() {
        for (int i = 0; i < 1000000; i++) {
            testMain();
        }
    }

    public void testStringBuilderPerf() {
        for (int i = 0; i < 1000000; i++) {
            testStringBuilder();
        }
    }

    public void testMainPerf2() {
        for (int i = 0; i < 1000000; i++) {
            testMain();
        }
    }

    public void testStringBuilderPerf2() {
        for (int i = 0; i < 1000000; i++) {
            testStringBuilder();
        }
    }
}
